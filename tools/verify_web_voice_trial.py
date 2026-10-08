"""Verify shipped trial audio integrity without models or generation dependencies."""
import hashlib
import json
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
