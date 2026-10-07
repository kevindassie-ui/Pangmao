"""Conservative French preparation for generated Web trial audio, not display text."""
from __future__ import annotations

import json
import re
import sys


TIME = re.compile(r"(?<!\w)([01]?\d|2[0-3])[ \t\u00a0\u202f]*h[ \t\u00a0\u202f]*([0-5]\d)(?!\w)", re.IGNORECASE)
PURCHASE = re.compile(r"\bje voudrais acheter\b", re.IGNORECASE)
PURCHASE_PHONEMES = "ʒə vudʁˈɛ aʃətˈe"


def prepare_speech(text: str) -> dict:
    """Keep wording intact; expand valid clock times and one reviewed phrase."""
    if not isinstance(text, str):
        raise TypeError("Speech text must be a string")

    def time_words(match: re.Match) -> str:
        hour, minute = map(int, match.groups())
        unit = "heure" if hour <= 1 else "heures"
        return f"{hour} {unit}" + (f" {minute}" if minute else "")

    spoken = TIME.sub(time_words, text)
    overrides = [
        {"text": match.group(), "phonemes": PURCHASE_PHONEMES,
         "reason": "Explicit acheter schwa and no added consonant after voudrais"}
        for match in PURCHASE.finditer(spoken)
    ]
    synthesis = PURCHASE.sub(f"[[{PURCHASE_PHONEMES}]]", spoken)
    return {"spokenText": spoken, "synthesisText": synthesis,
            "pronunciationOverrides": overrides}


if __name__ == "__main__":
    # Lightweight regression checks can call this without Piper or model weights.
    print(json.dumps([prepare_speech(text) for text in json.load(sys.stdin)], ensure_ascii=False))
