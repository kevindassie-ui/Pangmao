#!/usr/bin/env python3
"""Generate a small, attributed offline voice comparison; never runs in normal CI.

Requires a separate environment with piper-tts==1.4.1 and system ffmpeg.
Model weights stay in build/, never in the deployable application.
"""
from __future__ import annotations

import argparse
import hashlib
import importlib.metadata
import json
import os
from pathlib import Path
import shutil
import subprocess
import tempfile
import time
import urllib.request
import wave

from web_voice_text import prepare_speech

REVISION = "c10ece1aade47bb51c153c893d14e5bf8e5b7117"
MODEL_NAME = "fr_FR-upmc-medium.onnx"
EXPECTED_INPUTS = {
    "fr_FR-upmc-medium.onnx": "9abb3800c199148897a9ed64e100d224f3de83579f100044174ad19418f1786f",
    "fr_FR-upmc-medium.onnx.json": "e8636ec15dfd5d72db37a02cb5320a20f2b8d339f2a0e4337da64c58a33a5868",
    "MODEL_CARD": "cf1ac7309e0e04bb159b7e6ae80bc8f66bfce9bdfb3e62ae93a6f57e210ab98a",
}
BASE_URL = f"https://huggingface.co/rhasspy/piper-voices/resolve/{REVISION}/fr/fr_FR/upmc/medium/"
TRIAL_VERSION = "2026-10-08-v2"
VOICES = [
    {"id": "female", "label": "Femme · 女声", "speaker": "jessica", "speakerId": 0},
    {"id": "male", "label": "Homme · 男声", "speaker": "pierre", "speakerId": 1},
]


def digest(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--model-dir", type=Path, default=Path("build/voice-models"))
    parser.add_argument("--output", type=Path, default=Path("webApp/voice-trial"))
    parser.add_argument("--samples", nargs="+", help="Regenerate only named texts; preserve the other audio files")
    args = parser.parse_args()
    if importlib.metadata.version("piper-tts") != "1.4.1":
        raise SystemExit("Use piper-tts==1.4.1 in a separate environment")
    if not shutil.which("ffmpeg"):
        raise SystemExit("ffmpeg is required")
    # Full process-lifetime opt-out, before the first runtime import. The API
    # alone can be too late for initialization telemetry on official builds.
    os.environ["ORT_DISABLE_TELEMETRY"] = "1"
    import onnxruntime
    onnxruntime.disable_telemetry_events()
    onnxruntime.set_seed(0)
    from piper import PiperVoice, SynthesisConfig

    args.model_dir.mkdir(parents=True, exist_ok=True)
    inputs = {}
    for name in [MODEL_NAME, MODEL_NAME + ".json", "MODEL_CARD"]:
        path = args.model_dir / name
        if not path.exists():
            with urllib.request.urlopen(BASE_URL + name, timeout=90) as response:
                with path.with_suffix(path.suffix + ".download").open("wb") as target:
                    shutil.copyfileobj(response, target)
            path.with_suffix(path.suffix + ".download").replace(path)
        inputs[name] = digest(path)
        if inputs[name] != EXPECTED_INPUTS[name]:
            raise SystemExit(f"Unexpected source hash: {name}")
    config = json.loads((args.model_dir / (MODEL_NAME + ".json")).read_text())
    if config["speaker_id_map"] != {"jessica": 0, "pierre": 1}:
        raise SystemExit("Unexpected speaker mapping")
    model = PiperVoice.load(args.model_dir / MODEL_NAME)
    texts = json.loads((args.output / "texts.json").read_text(encoding="utf-8"))
    audio_dir = args.output / "audio"
    audio_dir.mkdir(exist_ok=True)
    samples = []
    previous_path = args.output / "manifest.json"
    previous = json.loads(previous_path.read_text(encoding="utf-8")) if previous_path.exists() else {}
    if args.samples and not set(args.samples) <= {text["id"] for text in texts}:
        raise SystemExit("Unknown sample requested")
    for text in texts:
        prepared = prepare_speech(text["text"])
        sample = {**text, **prepared, "clips": {}}
        if args.samples and text["id"] not in args.samples:
            old = next((s for s in previous.get("samples", []) if s["id"] == text["id"]), None)
            if not old or old["text"] != text["text"] or old.get("synthesisText", old["text"]) != prepared["synthesisText"]:
                raise SystemExit(f"Cannot preserve changed sample: {text['id']}")
            if (previous.get("modelRevision") != REVISION or previous.get("voices") != VOICES or
                    previous.get("engine") != "Piper 1.4.1" or previous.get("modelInputsSha256") != inputs):
                raise SystemExit("Cannot preserve audio from another voice model")
            for clip in old["clips"].values():
                if digest(args.output / clip["file"]) != clip["sha256"]:
                    raise SystemExit("Preserved clip differs from its hash")
            sample["clips"] = {gender: {**clip, "generatedForVersion": clip.get("generatedForVersion", previous["version"])}
                               for gender, clip in old["clips"].items()}
            samples.append(sample)
            continue
        for voice in VOICES:
            started = time.perf_counter()
            with tempfile.TemporaryDirectory() as temporary:
                wav_path = Path(temporary) / "clip.wav"
                with wave.open(str(wav_path), "wb") as wav:
                    model.synthesize_wav(prepared["synthesisText"], wav, syn_config=SynthesisConfig(
                        speaker_id=voice["speakerId"], length_scale=1.0,
                    ))
                elapsed = time.perf_counter() - started
                phonemes = ["".join(sentence) for sentence in model.phonemize(prepared["synthesisText"])]
                unknown = set("".join(phonemes)) - set(model.config.phoneme_id_map)
                if unknown:
                    raise SystemExit(f"Unsupported pronunciation symbols: {unknown}")
                with wave.open(str(wav_path)) as wav:
                    duration = wav.getnframes() / wav.getframerate()
                target = audio_dir / f'{voice["id"]}-{text["id"]}.mp3'
                subprocess.run([
                    "ffmpeg", "-nostdin", "-hide_banner", "-loglevel", "error",
                    "-y", "-i", str(wav_path), "-ac", "1", "-codec:a", "libmp3lame",
                    "-b:a", "64k", "-map_metadata", "-1", str(target),
                ], check=True)
            sample["clips"][voice["id"]] = {
                "file": f"audio/{target.name}", "bytes": target.stat().st_size,
                "sha256": digest(target), "durationSeconds": round(duration, 3),
                "generationSeconds": round(elapsed, 3),
                "generatedForVersion": TRIAL_VERSION, "phonemes": phonemes,
            }
        samples.append(sample)
    manifest = {
        "schemaVersion": 1, "version": TRIAL_VERSION, "language": "fr-FR",
        "purpose": "comparison-only", "engine": "Piper 1.4.1",
        "model": "fr_FR-upmc-medium", "modelRevision": REVISION,
        "modelInputsSha256": inputs, "voices": VOICES, "samples": samples,
        "license": "CC-BY-SA-4.0", "notice": "NOTICE.md",
        "totalAudioBytes": sum(c["bytes"] for s in samples for c in s["clips"].values()),
    }
    (args.output / "manifest.json").write_text(
        json.dumps(manifest, ensure_ascii=False, indent=2) + "\n", encoding="utf-8",
    )
    print(f'{len(samples) * len(VOICES)} clips, {manifest["totalAudioBytes"]} bytes')


if __name__ == "__main__":
    main()
