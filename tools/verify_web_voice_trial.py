"""Verify shipped trial audio integrity without models or generation dependencies."""
import hashlib
import json
import wave
from pathlib import Path

if __package__:
    from .web_voice_text import prepare_speech
else:
    from web_voice_text import prepare_speech


def verify_trial(root: Path) -> None:
    trial = root / "voice-trial"
    for name in ["index.html", "styles.css", "controller.js", "player.js", "sw.js", "ranges.js", "NOTICE.md", "texts.json", "manifest.json"]:
        if not (trial / name).is_file():
            raise ValueError(f"Missing voice trial asset: {name}")
    manifest = json.loads((trial / "manifest.json").read_text(encoding="utf-8"))
    version = manifest["version"]
    for asset in ["player.js", "sw.js"]:
        if f'TRIAL_VERSION = "{version}"' not in (trial / asset).read_text():
            raise ValueError("Trial cache/manifest version mismatch")
    texts = json.loads((trial / "texts.json").read_text(encoding="utf-8"))
    if len(texts) != 6 or len(manifest["samples"]) != 6:
        raise ValueError("Expected six fixed comparison texts")
    for sample, text in zip(manifest["samples"], texts):
        if any(sample[k] != text[k] for k in ["id", "label", "text"]):
            raise ValueError("Generated speech labels differ from fixed texts")
        prepared = prepare_speech(text["text"])
        if any(sample.get(k) != v for k, v in prepared.items()):
            raise ValueError("Speech preparation differs from its recorded input")
    voice_ids = [v["id"] for v in manifest["voices"]]
    if manifest["schemaVersion"] != 2 or voice_ids != ["female", "male", "siwis", "mls"]:
        raise ValueError("Unexpected trial voices")
    paths = set()
    total = 0
    for sample in manifest["samples"]:
        for gender in voice_ids:
            clip = sample["clips"][gender]
            expected = f'audio/{gender}-{sample["id"]}.mp3'
            if clip["file"] != expected or expected in paths:
                raise ValueError("Unexpected audio filename")
            paths.add(expected)
            payload = (trial / expected).read_bytes()
            if not payload or len(payload) > 250_000 or len(payload) != clip["bytes"]:
                raise ValueError(f"Audio size mismatch: {expected}")
            if hashlib.sha256(payload).hexdigest() != clip["sha256"]:
                raise ValueError(f"Audio hash mismatch: {expected}")
            if not 0 < clip["durationSeconds"] <= 60:
                raise ValueError("Invalid speech duration")
            total += len(payload)
    if total > 1_000_000 or total != manifest["totalAudioBytes"]:
        raise ValueError("Trial exceeds its audio budget")
    if {f'audio/{p.name}' for p in (trial / "audio").iterdir()} != paths:
        raise ValueError("Unexpected audio files shipped")
    verify_diagnostic(trial)


def verify_diagnostic(trial: Path) -> None:
    """Check controlled A/B provenance, not subjective pronunciation quality."""
    diagnostic = trial / "diagnostic"
    for name in ["index.html", "styles.css", "controller.js", "player.js", "manifest.json", "NOTICE.md"]:
        if not (diagnostic / name).is_file():
            raise ValueError(f"Missing diagnostic file: {name}")
    manifest = json.loads((diagnostic / "manifest.json").read_text())
    if manifest["version"] != "2026-10-08-d1" or manifest["purpose"] != "input-codec-diagnostic":
        raise ValueError("Unexpected diagnostic version")
    if f'VERSION = "{manifest["version"]}"' not in (diagnostic / "player.js").read_text():
        raise ValueError("Diagnostic player/manifest versions differ")
    trial_manifest = json.loads((trial / "manifest.json").read_text())
    source_samples = {s["id"]: s for s in trial_manifest["samples"]}
    if [s["id"] for s in manifest["samples"]] != ["medecin", "avocat", "nombres", "quotidien"]:
        raise ValueError("Unexpected diagnostic witnesses")
    paths = set()
    total = 0
    for sample in manifest["samples"]:
        source = source_samples[sample["id"]]
        if sample["text"] != source["text"] or any(sample[k] != v for k, v in prepare_speech(source["text"]).items()):
            raise ValueError("Diagnostic control changed trial wording/preparation")
        for voice in ["female", "male"]:
            if sample["referenceClips"][voice] != source["clips"][voice]["sha256"]:
                raise ValueError("Diagnostic lost original audio references")
            clips = sample["clips"][voice]
            expected_keys = {"mp3", "wav", "explicit"} if sample["id"] in ["medecin", "avocat"] else {"mp3", "wav"}
            if set(clips) != expected_keys:
                raise ValueError("Unexpected diagnostic conditions")
            for key, clip in clips.items():
                condition = "explicit" if key == "explicit" else "current"
                extension = "mp3" if key == "mp3" else "wav"
                expected = f"audio/{voice}-{sample['id']}-{condition}.{extension}"
                if clip["file"] != expected or expected in paths:
                    raise ValueError("Unsafe or duplicated diagnostic file")
                paths.add(expected)
                path = diagnostic / expected
                if not 0 < path.stat().st_size <= 500_000 or path.stat().st_size != clip["bytes"] or hashlib.sha256(path.read_bytes()).hexdigest() != clip["sha256"]:
                    raise ValueError("Corrupted diagnostic audio")
                total += clip["bytes"]
                if clip["pcmFrames"] != clip["decodedFrames"]:
                    raise ValueError("Diagnostic audio lost PCM frames")
                if extension == "wav":
                    with wave.open(str(path)) as wav:
                        if wav.getnchannels() != 1 or wav.getsampwidth() != 2 or wav.getframerate() != 22050 or wav.getnframes() != clip["pcmFrames"]:
                            raise ValueError("Unexpected diagnostic WAV format")
                        if hashlib.sha256(wav.readframes(wav.getnframes())).hexdigest() != clip["sourcePcmSha256"]:
                            raise ValueError("Diagnostic source PCM differs from recorded hash")
            for key in ["sourcePcmSha256", "pcmFrames", "sampleRate", "phonemes", "synthesisText"]:
                if clips["mp3"][key] != clips["wav"][key]:
                    raise ValueError("A/B comparison does not share one synthesis")
            if clips["wav"]["synthesisText"] != sample["synthesisText"]:
                raise ValueError("Control synthesis input changed")
            if "explicit" in clips and (clips["explicit"]["synthesisText"] != f'[[{sample["explicitPhonemes"]}]]' or clips["explicit"]["phonemes"] != [sample["explicitPhonemes"]]):
                raise ValueError("Explicit diagnostic input was not used")
    if total != manifest["totalAudioBytes"] or total > 2_000_000 or paths != {f'audio/{p.name}' for p in (diagnostic / "audio").iterdir()}:
        raise ValueError("Diagnostic audio inventory/budget differs")
