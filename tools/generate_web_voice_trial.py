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
TRIAL_VERSION = "2026-10-08-v3"
MODEL_INPUTS = {
    "upmc": {"quality": "medium", "files": EXPECTED_INPUTS},
    "siwis": {"quality": "medium", "files": {
        "fr_FR-siwis-medium.onnx": "641d1ab097da2b81128c076810edb052b385decc8be3381814802a64a73baf99",
        "fr_FR-siwis-medium.onnx.json": "39479916c2db192b5ac9764daddd0c744d83e023ad890c6976c0633ae4df8959",
        "MODEL_CARD": "bd8b8d079d4d3d2c4cc20345889bbcd586d72f021a132f93357a763eb5232cce",
    }},
    "mls": {"quality": "medium", "files": {
        "fr_FR-mls-medium.onnx": "0ed223f78466917f2bae05ee90096ce69ab1fdeb251f55590d0e7422d234e162",
        "fr_FR-mls-medium.onnx.json": "252b0b0a6e4cc4949e23eccb956f9c779986c32f934f2f7e2191e5fdc2edca61",
        "MODEL_CARD": "443ca90f1ed8e57d9fb802da6dc19c302d5d9b51ddbfbf27a17f3fb5c37ad154",
    }},
}
VOICES = [
    {"id": "female", "label": "Jessica · 女声", "speaker": "jessica", "speakerId": 0, "model": "upmc", "profile": "female"},
    {"id": "male", "label": "Pierre · 男声", "speaker": "pierre", "speakerId": 1, "model": "upmc", "profile": "male"},
    {"id": "siwis", "label": "SIWIS · 女声", "speaker": "siwis", "speakerId": 0, "model": "siwis", "profile": "female"},
    {"id": "mls", "label": "MLS 1840 · 声音 B", "speaker": "1840", "speakerId": 0, "model": "mls", "profile": "unassigned"},
]


def digest(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--model-dir", type=Path, default=Path("build/voice-models"))
    parser.add_argument("--output", type=Path, default=Path("webApp/voice-trial"))
    parser.add_argument("--samples", nargs="+", help="Regenerate only named texts; preserve the other audio files")
    parser.add_argument("--voices", nargs="+", choices=[v["id"] for v in VOICES], help="Regenerate only these voices, preserving verified comparison files")
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
    models = {}
    model_metadata = {}
    for key, spec in MODEL_INPUTS.items():
        directory = args.model_dir if key == "upmc" else args.model_dir / key
        directory.mkdir(parents=True, exist_ok=True)
        base = f"https://huggingface.co/rhasspy/piper-voices/resolve/{REVISION}/fr/fr_FR/{key}/{spec['quality']}/"
        for name, expected in spec["files"].items():
            path = directory / name
            if not path.exists():
                with urllib.request.urlopen(base + name, timeout=90) as response:
                    with path.with_suffix(path.suffix + ".download").open("wb") as target:
                        shutil.copyfileobj(response, target)
                path.with_suffix(path.suffix + ".download").replace(path)
            if digest(path) != expected:
                raise SystemExit(f"Unexpected source hash: {key}/{name}")
        model_name = f"fr_FR-{key}-{spec['quality']}.onnx"
        model_metadata[key] = {"name": model_name.removesuffix(".onnx"), "revision": REVISION,
                               "inputsSha256": spec["files"], "license": "CC-BY-SA-4.0" if key == "upmc" else "CC-BY-4.0"}
        models[key] = PiperVoice.load(directory / model_name)
    if models["upmc"].config.speaker_id_map != {"jessica": 0, "pierre": 1} or models["mls"].config.speaker_id_map.get("1840") != 0:
        raise SystemExit("Unexpected speaker mapping")
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
        old = next((s for s in previous.get("samples", []) if s["id"] == text["id"]), None)
        for voice in VOICES:
            preserve = (args.samples and text["id"] not in args.samples) or (args.voices and voice["id"] not in args.voices)
            if preserve:
                if not old or old["text"] != text["text"] or old.get("synthesisText", old["text"]) != prepared["synthesisText"]:
                    raise SystemExit(f"Cannot preserve changed sample: {text['id']}")
                old_model = previous.get("models", {}).get(voice["model"])
                if old_model is None and voice["model"] == "upmc":
                    old_model = {"name": previous.get("model"), "revision": previous.get("modelRevision"),
                                 "inputsSha256": previous.get("modelInputsSha256"), "license": previous.get("license")}
                if old_model != model_metadata[voice["model"]] or previous.get("engine") != "Piper 1.4.1":
                    raise SystemExit("Cannot preserve audio from another voice model")
                old_voice = next((v for v in previous.get("voices", []) if v["id"] == voice["id"]), {})
                if any(old_voice.get(k) != voice[k] for k in ["speaker", "speakerId"]):
                    raise SystemExit("Cannot preserve another speaker")
                clip = old["clips"][voice["id"]]
                if digest(args.output / clip["file"]) != clip["sha256"]:
                    raise SystemExit("Preserved clip differs from its hash")
                sample["clips"][voice["id"]] = {**clip, "generatedForVersion": clip.get("generatedForVersion", previous["version"])}
                continue
            model = models[voice["model"]]
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
                    pcm_frames = wav.getnframes()
                    sample_rate = wav.getframerate()
                    duration = pcm_frames / sample_rate
                target = audio_dir / f'{voice["id"]}-{text["id"]}.mp3'
                subprocess.run([
                    "ffmpeg", "-nostdin", "-hide_banner", "-loglevel", "error",
                    "-y", "-i", str(wav_path), "-ac", "1", "-codec:a", "libmp3lame",
                    "-b:a", "64k", "-map_metadata", "-1", str(target),
                ], check=True)
                decoded = subprocess.check_output([
                    "ffmpeg", "-nostdin", "-hide_banner", "-loglevel", "error", "-i", str(target),
                    "-f", "s16le", "-acodec", "pcm_s16le", "-",
                ])
                decoded_frames = len(decoded) // 2
                if decoded_frames != pcm_frames:
                    raise SystemExit(f"MP3 lost audio frames: {target.name}")
            sample["clips"][voice["id"]] = {
                "file": f"audio/{target.name}", "bytes": target.stat().st_size,
                "sha256": digest(target), "durationSeconds": round(duration, 3),
                "generationSeconds": round(elapsed, 3),
                "generatedForVersion": TRIAL_VERSION, "phonemes": phonemes,
                "pcmFrames": pcm_frames, "decodedFrames": decoded_frames, "sampleRate": sample_rate,
                "synthesisConfig": {"lengthScale": 1.0, "noiseScale": model.config.noise_scale, "noiseWidthScale": model.config.noise_w_scale},
            }
        samples.append(sample)
    manifest = {
        "schemaVersion": 2, "version": TRIAL_VERSION, "language": "fr-FR",
        "purpose": "comparison-only", "engine": "Piper 1.4.1",
        "models": model_metadata, "voices": VOICES, "samples": samples,
        "license": "CC-BY-SA-4.0", "notice": "NOTICE.md",
        "totalAudioBytes": sum(c["bytes"] for s in samples for c in s["clips"].values()),
    }
    (args.output / "manifest.json").write_text(
        json.dumps(manifest, ensure_ascii=False, indent=2) + "\n", encoding="utf-8",
    )
    print(f'{len(samples) * len(VOICES)} clips, {manifest["totalAudioBytes"]} bytes')


if __name__ == "__main__":
    main()
