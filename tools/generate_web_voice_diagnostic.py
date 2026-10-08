#!/usr/bin/env python3
"""Manual, controlled input/codec experiment. Never a dictionary pronunciation fix.

Use the separate Piper 1.4.1 environment and pinned UPMC inputs from the trial.
Acquire the 2023.11.14-4 legacy phonemizer separately for the compatibility audit.
No model, executable, user text or telemetry is shipped to the browser.
"""
from __future__ import annotations

import argparse
import hashlib
import importlib.metadata
import json
import os
from pathlib import Path
import subprocess
import wave

from generate_web_voice_trial import EXPECTED_INPUTS, REVISION
from web_voice_text import prepare_speech

VERSION = "2026-10-08-d1"
SAMPLE_IDS = ["medecin", "avocat", "nombres", "quotidien"]
EXPLICIT = {"medecin": "medsˈɛ̃", "avocat": "avɔkˈa"}
LEGACY_HASH = "3fb3d58b4ac42bd69d38948acdbeab335eee7e599984169d28fb0082496649ad"


def plans(trial: dict, pack: dict) -> list[dict]:
    """Only two sourced IPA hypotheses; keep trial preparation unchanged."""
    samples = {s["id"]: s for s in trial["samples"]}
    result = []
    for sample_id in SAMPLE_IDS:
        source = samples[sample_id]
        sample = {"id": sample_id, "text": source["text"],
                  **prepare_speech(source["text"]), "explicitPhonemes": EXPLICIT.get(sample_id),
                  "referenceClips": {v: source["clips"][v]["sha256"] for v in ["female", "male"]}}
        if sample_id in EXPLICIT:
            entry = next(e for e in pack["entries"] if e["headword"] == source["text"])
            # Stress is Piper notation, not part of the dictionary IPA.
            ipa = EXPLICIT[sample_id].replace("ˈ", "")
            if ipa not in [p.replace(".", "") for p in entry["pronunciations"]]:
                raise ValueError("Explicit hypothesis does not match dictionary IPA")
            sample["ipaSource"] = {"entryId": entry["id"], "pronunciations": entry["pronunciations"],
                                   "license": pack["source"]["license"], "revision": pack["source"]["revision"]}
        result.append(sample)
    return result


def sha(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--model-dir", type=Path, default=Path("build/voice-models"))
    parser.add_argument("--legacy-dir", type=Path, default=Path("build/legacy-phonemize/piper_phonemize"))
    parser.add_argument("--legacy-archive", type=Path, default=Path("build/legacy-phonemize.tar.gz"))
    parser.add_argument("--output", type=Path, default=Path("webApp/voice-trial/diagnostic"))
    parser.add_argument("--describe", action="store_true", help="Inspect hypotheses without synthesis dependencies")
    args = parser.parse_args()
    trial = json.loads(Path("webApp/voice-trial/manifest.json").read_text())
    pack = json.loads(Path("webApp/data/french-pack.json").read_text())
    samples = plans(trial, pack)
    if args.describe:
        print(json.dumps(samples, ensure_ascii=False))
        return
    if importlib.metadata.version("piper-tts") != "1.4.1":
        raise SystemExit("Requires piper-tts==1.4.1 in the isolated generation environment")
    for name, expected in EXPECTED_INPUTS.items():
        if sha(args.model_dir / name) != expected:
            raise SystemExit(f"Unexpected pinned model input: {name}")
    if sha(args.legacy_archive) != LEGACY_HASH:
        raise SystemExit("Unexpected legacy phonemizer archive")
    os.environ["ORT_DISABLE_TELEMETRY"] = "1"
    import onnxruntime as ort
    ort.disable_telemetry_events()
    from piper import PiperVoice, SynthesisConfig
    from piper.config import PiperConfig
    from piper.phonemize_espeak import EspeakPhonemizer

    config = PiperConfig.from_dict(json.loads((args.model_dir / "fr_FR-upmc-medium.onnx.json").read_text()))
    if config.speaker_id_map != {"jessica": 0, "pierre": 1}:
        raise SystemExit("Unexpected UPMC speakers")
    phonemizer = EspeakPhonemizer()
    audit_texts = ["médecin", "avocat", "fixé", "une baguette et prendre le train"]
    audit_texts += [s["spokenText"] for s in samples[2:]]
    legacy_env = {**os.environ, "LD_LIBRARY_PATH": str((args.legacy_dir / "lib").resolve())}
    output = subprocess.check_output([
        str(args.legacy_dir / "bin/piper_phonemize"), "-l", "fr", "--espeak_data",
        str(args.legacy_dir / "share/espeak-ng-data"),
    ], input="\n".join(audit_texts) + "\n", text=True, env=legacy_env)
    legacy = [json.loads(line) for line in output.splitlines()]
    if len(legacy) != len(audit_texts):
        raise SystemExit("Legacy audit returned an unexpected number of texts")
    audit = []
    for text, old in zip(audit_texts, legacy):
        current = ["".join(s) for s in phonemizer.phonemize("fr", text)]
        prior = "".join(old["phonemes"])
        if old["text"] != text:
            raise SystemExit("Legacy audit reordered the inputs")
        # The historical CLI flattens sentence groups; compare the sequence,
        # not that CLI representation with Piper's current sentence arrays.
        audit.append({"text": text, "current": current, "legacyFlat": prior,
                      "phonemeSequenceEqual": "".join(current) == prior})

    def new_voice() -> PiperVoice:
        # Fresh session per hypothesis: same random seed and inference settings.
        ort.set_seed(0)
        options = ort.SessionOptions()
        options.intra_op_num_threads = options.inter_op_num_threads = 1
        return PiperVoice(config=config, session=ort.InferenceSession(
            str(args.model_dir / "fr_FR-upmc-medium.onnx"), sess_options=options,
            providers=["CPUExecutionProvider"]))

    audio = args.output / "audio"
    audio.mkdir(parents=True, exist_ok=True)
    for sample in samples:
        sample["clips"] = {}
        for voice_id, speaker_id in [("female", 0), ("male", 1)]:
            sample["clips"][voice_id] = {}
            for condition in ["current", "explicit"]:
                if condition == "explicit" and not sample["explicitPhonemes"]:
                    continue
                text = sample["synthesisText"] if condition == "current" else f'[[{sample["explicitPhonemes"]}]]'
                model = new_voice()
                phonemes = ["".join(s) for s in model.phonemize(text)]
                if set("".join(phonemes)) - set(config.phoneme_id_map):
                    raise SystemExit("Unsupported pronunciation symbol")
                stem = f"{voice_id}-{sample['id']}-{condition}"
                wav = audio / f"{stem}.wav"
                with wave.open(str(wav), "wb") as f:
                    model.synthesize_wav(text, f, syn_config=SynthesisConfig(speaker_id=speaker_id, length_scale=1.0))
                with wave.open(str(wav)) as f:
                    frames, rate, pcm = f.getnframes(), f.getframerate(), f.readframes(f.getnframes())
                pcm_hash = hashlib.sha256(pcm).hexdigest()
                formats = ["wav", "mp3"] if condition == "current" else ["wav"]
                for fmt in formats:
                    path = audio / f"{stem}.{fmt}"
                    key = "explicit" if condition == "explicit" else fmt
                    decoded_frames = frames
                    if fmt == "mp3":
                        subprocess.run(["ffmpeg", "-nostdin", "-hide_banner", "-loglevel", "error", "-y",
                                        "-i", str(wav), "-ac", "1", "-codec:a", "libmp3lame", "-b:a", "64k",
                                        "-map_metadata", "-1", str(path)], check=True)
                        decoded = subprocess.check_output(["ffmpeg", "-nostdin", "-hide_banner", "-loglevel", "error",
                                                           "-i", str(path), "-f", "s16le", "-acodec", "pcm_s16le", "-"])
                        decoded_frames = len(decoded) // 2
                        if decoded_frames != frames:
                            raise SystemExit("Diagnostic MP3 frame count differs from source WAV")
                    sample["clips"][voice_id][key] = {
                        "file": f"audio/{path.name}", "sha256": sha(path), "bytes": path.stat().st_size,
                        "pcmFrames": frames, "decodedFrames": decoded_frames, "sampleRate": rate,
                        "durationSeconds": round(frames / rate, 3), "sourcePcmSha256": pcm_hash,
                        "phonemes": phonemes, "synthesisText": text,
                    }
                print(stem, f"{frames / rate:.3f}s", flush=True)
    manifest = {
        "schemaVersion": 1, "version": VERSION, "purpose": "input-codec-diagnostic",
        "language": "fr-FR", "referenceVersion": trial["version"], "engine": "Piper 1.4.1",
        "runtime": importlib.metadata.version("onnxruntime"), "modelRevision": REVISION,
        "modelInputsSha256": EXPECTED_INPUTS, "voices": trial["voices"][:2],
        "synthesisConfig": {"lengthScale": 1.0, "seed": 0, "threads": 1,
                            "noiseScale": config.noise_scale, "noiseWidthScale": config.noise_w_scale},
        "g2pAudit": {"legacyRelease": "2023.11.14-4", "archiveSha256": LEGACY_HASH,
                     "note": "Historical Piper chain; exact training phonemizer provenance is not established.",
                     "samples": audit}, "samples": samples,
        "totalAudioBytes": sum(c["bytes"] for s in samples for clips in s["clips"].values() for c in clips.values()),
    }
    if manifest["totalAudioBytes"] > 2_000_000:
        raise SystemExit("Diagnostic audio exceeds two MB")
    (args.output / "manifest.json").write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + "\n")
    print("Saved controlled diagnostic:", manifest["totalAudioBytes"], "bytes")


if __name__ == "__main__":
    main()
