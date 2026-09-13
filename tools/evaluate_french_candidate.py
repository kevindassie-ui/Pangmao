#!/usr/bin/env python3
"""Evaluate a Kaikki/frwiktionary Chinese extract against Pangmao.

The evaluator is deliberately read-only. It measures whether a candidate source
can safely complement Pangmao's French definitions before the source is wired
into the production dictionary builder.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import re
import sqlite3
import unicodedata
from collections import Counter, defaultdict
from dataclasses import dataclass, field
from pathlib import Path
from typing import Iterable


HAN_RANGES = (
    (0x3400, 0x4DBF),
    (0x4E00, 0x9FFF),
    (0xF900, 0xFAFF),
    (0x20000, 0x323AF),
)
MARKUP_PATTERN = re.compile(r"(?:\{\{|\}\}|<\/?[A-Za-z][^>]*>|\[https?://)")
CROSS_REFERENCE_PATTERN = re.compile(
    r"^(?:voir|variante|forme|orthographe|graphie|transcription|abréviation)\b",
    re.IGNORECASE,
)
REGRESSION_WORDS = (
    "肉夹馍",
    "奶茶婊",
    "爸比",
    "才",
    "打球",
    "像",
    "你",
    "您",
    "笨蛋",
    "傻比",
    "大笨蛋",
)


def normalized_text(value: str) -> str:
    return re.sub(r"\s+", " ", unicodedata.normalize("NFC", value).strip())


def comparison_text(value: str) -> str:
    value = normalized_text(value).casefold().strip(" .;:!?…")
    return re.sub(r"\s+", " ", value)


def contains_han(value: str) -> bool:
    for character in value:
        codepoint = ord(character)
        if any(start <= codepoint <= end for start, end in HAN_RANGES):
            return True
    return False


def pinyin_plain(value: str) -> str:
    value = value.translate(str.maketrans("üǖǘǚǜÜǕǗǙǛ", "vvvvvVVVVV"))
    value = value.replace("u:", "v").casefold()
    value = "".join(
        character
        for character in unicodedata.normalize("NFD", value)
        if unicodedata.category(character) != "Mn" and not character.isdigit()
    )
    return re.sub(r"[^a-zv]+", "", value)


def unique(values: Iterable[str]) -> list[str]:
    result: list[str] = []
    seen: set[str] = set()
    for value in values:
        value = normalized_text(value)
        marker = comparison_text(value)
        if value and marker not in seen:
            seen.add(marker)
            result.append(value)
    return result


@dataclass
class CandidateWord:
    word: str
    glosses: list[str] = field(default_factory=list)
    parts_of_speech: set[str] = field(default_factory=set)
    pinyins: set[str] = field(default_factory=set)
    examples: dict[str, set[str]] = field(default_factory=lambda: defaultdict(set))


@dataclass(frozen=True)
class PangmaoEntry:
    identifier: int
    traditional: str
    simplified: str
    pinyin: str
    pinyin_plain: str
    definitions_en: tuple[str, ...]
    definitions_fr: tuple[str, ...]
    sources: str
    frequency: int


def file_sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for block in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(block)
    return digest.hexdigest()


def load_candidate(path: Path) -> tuple[dict[str, CandidateWord], dict[str, int]]:
    words: dict[str, CandidateWord] = {}
    counters: Counter[str] = Counter()
    unique_examples: set[tuple[str, str]] = set()

    with path.open(encoding="utf-8") as handle:
        for line_number, line in enumerate(handle, start=1):
            try:
                row = json.loads(line)
            except json.JSONDecodeError as error:
                raise ValueError(f"Invalid JSONL at {path}:{line_number}") from error
            if row.get("lang_code") != "zh":
                counters["non_chinese_records"] += 1
                continue
            counters["records"] += 1
            word = normalized_text(str(row.get("word", "")))
            if not word:
                counters["records_without_word"] += 1
                continue
            candidate = words.setdefault(word, CandidateWord(word=word))
            if row.get("pos"):
                candidate.parts_of_speech.add(str(row["pos"]))
            for sound in row.get("sounds") or []:
                tags = {str(tag).casefold() for tag in sound.get("tags") or []}
                pronunciation = normalized_text(str(sound.get("zh_pron", "")))
                if pronunciation and "pinyin" in tags:
                    candidate.pinyins.add(pronunciation)

            senses = row.get("senses") or []
            counters["senses"] += len(senses)
            for sense in senses:
                glosses = unique(str(item) for item in sense.get("glosses") or [])
                if not glosses:
                    counters["senses_without_gloss"] += 1
                candidate.glosses = unique([*candidate.glosses, *glosses])
                counters["glosses"] += len(glosses)
                for gloss in glosses:
                    if MARKUP_PATTERN.search(gloss):
                        counters["glosses_with_markup"] += 1
                    if CROSS_REFERENCE_PATTERN.search(gloss):
                        counters["cross_reference_glosses"] += 1
                    if len(gloss) > 240:
                        counters["long_glosses"] += 1
                for example in sense.get("examples") or []:
                    french = normalized_text(str(example.get("translation", "")))
                    raw_chinese = str(example.get("text", ""))
                    if not french:
                        continue
                    for chinese in (normalized_text(part) for part in raw_chinese.splitlines()):
                        if not chinese or not contains_han(chinese):
                            continue
                        candidate.examples[chinese].add(french)
                        unique_examples.add((chinese, french))

    counters["distinct_words"] = len(words)
    counters["words_with_gloss"] = sum(bool(word.glosses) for word in words.values())
    counters["words_with_pinyin"] = sum(bool(word.pinyins) for word in words.values())
    counters["explicit_translated_examples"] = len(unique_examples)
    return words, dict(counters)


def split_definitions(value: str) -> tuple[str, ...]:
    return tuple(part for part in value.splitlines() if part.strip())


def load_pangmao(
    path: Path,
) -> tuple[
    list[PangmaoEntry],
    dict[str, list[PangmaoEntry]],
    dict[str, dict[str, str]],
    set[int],
    set[int],
]:
    connection = sqlite3.connect(f"file:{path}?mode=ro", uri=True)
    rows = connection.execute(
        """
        SELECT id, traditional, simplified, pinyin, pinyin_plain,
               definitions_en, definitions_fr, sources, frequency
        FROM entries
        """
    ).fetchall()
    examples = {
        row[0]: {"english": row[1], "french": row[2]}
        for row in connection.execute("SELECT chinese, english, french FROM examples")
    }
    has_headwords = connection.execute(
        "SELECT 1 FROM sqlite_master WHERE type = 'table' AND name = 'headwords'"
    ).fetchone()
    preferred_by_headword = (
        dict(connection.execute("SELECT word, preferred_entry_id FROM headwords"))
        if has_headwords
        else {}
    )
    connection.close()

    entries = [
        PangmaoEntry(
            identifier=row[0],
            traditional=row[1],
            simplified=row[2],
            pinyin=row[3],
            pinyin_plain=row[4],
            definitions_en=split_definitions(row[5]),
            definitions_fr=split_definitions(row[6]),
            sources=row[7],
            frequency=row[8],
        )
        for row in rows
    ]
    by_headword: dict[str, list[PangmaoEntry]] = defaultdict(list)
    for entry in entries:
        for headword in {entry.simplified, entry.traditional}:
            if headword:
                by_headword[headword].append(entry)
    preferred_entry_ids = set(preferred_by_headword.values()) or {entry.identifier for entry in entries}
    primary_entry_ids = {
        entry.identifier
        for entry in entries
        if not preferred_by_headword
        or preferred_by_headword.get(entry.simplified) == entry.identifier
    }
    return entries, dict(by_headword), examples, preferred_entry_ids, primary_entry_ids


def resolve_candidate(
    candidate: CandidateWord,
    matches: list[PangmaoEntry],
) -> tuple[PangmaoEntry | None, str]:
    unique_matches = {entry.identifier: entry for entry in matches}
    matches = list(unique_matches.values())
    candidate_pinyins = {pinyin_plain(value) for value in candidate.pinyins if pinyin_plain(value)}
    if candidate_pinyins:
        pinyin_matches = [entry for entry in matches if entry.pinyin_plain in candidate_pinyins]
        if len(pinyin_matches) == 1:
            return pinyin_matches[0], "pinyin"
        if len(pinyin_matches) > 1:
            return None, "ambiguous_pinyin"
        if matches:
            return None, "pinyin_mismatch"
    if len(matches) == 1:
        return matches[0], "unique_headword"
    if len(matches) > 1:
        return None, "ambiguous_headword"
    return None, "new_headword"


def sample_row(
    candidate: CandidateWord,
    entry: PangmaoEntry | None,
    resolution: str,
    preferred_entry_ids: set[int],
    primary_entry_ids: set[int],
) -> dict:
    return {
        "word": candidate.word,
        "candidate_pinyin": sorted(candidate.pinyins),
        "parts_of_speech": sorted(candidate.parts_of_speech),
        "candidate_french": candidate.glosses,
        "resolution": resolution,
        "pangmao_pinyin": entry.pinyin if entry else "",
        "pangmao_frequency": entry.frequency if entry else 0,
        "pangmao_preferred": bool(entry and entry.identifier in preferred_entry_ids),
        "pangmao_primary": bool(entry and entry.identifier in primary_entry_ids),
        "pangmao_english": list(entry.definitions_en) if entry else [],
        "pangmao_french": list(entry.definitions_fr) if entry else [],
        "pangmao_sources": entry.sources if entry else "",
    }


def top_rows(rows: Iterable[dict], limit: int, key) -> list[dict]:
    return sorted(rows, key=key)[:limit]


def evaluate(database: Path, candidate_path: Path, sample_size: int = 20) -> dict:
    candidate_words, candidate_stats = load_candidate(candidate_path)
    entries, by_headword, pangmao_examples, preferred_entry_ids, primary_entry_ids = load_pangmao(
        database
    )
    resolution_counts: Counter[str] = Counter()
    resolved: dict[int, dict] = {}
    candidate_only: list[dict] = []
    unresolved: list[dict] = []

    for candidate in candidate_words.values():
        if not candidate.glosses:
            continue
        entry, resolution = resolve_candidate(candidate, by_headword.get(candidate.word, []))
        resolution_counts[resolution] += 1
        row = sample_row(candidate, entry, resolution, preferred_entry_ids, primary_entry_ids)
        if entry is None:
            if resolution == "new_headword":
                candidate_only.append(row)
            else:
                unresolved.append(row)
            continue
        aggregate = resolved.setdefault(
            entry.identifier,
            {
                "entry": entry,
                "words": set(),
                "glosses": [],
                "parts_of_speech": set(),
                "resolution": set(),
            },
        )
        aggregate["words"].add(candidate.word)
        aggregate["glosses"] = unique([*aggregate["glosses"], *candidate.glosses])
        aggregate["parts_of_speech"].update(candidate.parts_of_speech)
        aggregate["resolution"].add(resolution)

    resolved_rows: list[dict] = []
    gain_counts: Counter[str] = Counter()
    for item in resolved.values():
        entry: PangmaoEntry = item["entry"]
        existing = {comparison_text(value) for value in entry.definitions_fr}
        new_glosses = [value for value in item["glosses"] if comparison_text(value) not in existing]
        row = {
            "word": entry.simplified,
            "traditional": entry.traditional,
            "matched_forms": sorted(item["words"]),
            "candidate_french": item["glosses"],
            "net_new_french": new_glosses,
            "parts_of_speech": sorted(item["parts_of_speech"]),
            "resolution": sorted(item["resolution"]),
            "pangmao_pinyin": entry.pinyin,
            "pangmao_frequency": entry.frequency,
            "pangmao_preferred": entry.identifier in preferred_entry_ids,
            "pangmao_primary": entry.identifier in primary_entry_ids,
            "pangmao_english": list(entry.definitions_en),
            "pangmao_french": list(entry.definitions_fr),
            "pangmao_sources": entry.sources,
        }
        resolved_rows.append(row)
        gain_counts["resolved_entries"] += 1
        if entry.identifier in preferred_entry_ids:
            gain_counts["resolved_preferred_entries"] += 1
        if entry.identifier in primary_entry_ids:
            gain_counts["resolved_primary_entries"] += 1
        if not entry.definitions_fr:
            gain_counts["entries_missing_french"] += 1
            if entry.identifier in preferred_entry_ids:
                gain_counts["preferred_entries_missing_french"] += 1
            if entry.identifier in primary_entry_ids:
                gain_counts["primary_entries_missing_french"] += 1
            if entry.frequency >= 1:
                gain_counts["entries_missing_french_frequency_1_plus"] += 1
                if entry.identifier in preferred_entry_ids:
                    gain_counts["preferred_entries_missing_french_frequency_1_plus"] += 1
                if entry.identifier in primary_entry_ids:
                    gain_counts["primary_entries_missing_french_frequency_1_plus"] += 1
            if entry.frequency >= 5:
                gain_counts["entries_missing_french_frequency_5_plus"] += 1
                if entry.identifier in preferred_entry_ids:
                    gain_counts["preferred_entries_missing_french_frequency_5_plus"] += 1
                if entry.identifier in primary_entry_ids:
                    gain_counts["primary_entries_missing_french_frequency_5_plus"] += 1
            if entry.frequency >= 20:
                gain_counts["entries_missing_french_frequency_20_plus"] += 1
                if entry.identifier in preferred_entry_ids:
                    gain_counts["preferred_entries_missing_french_frequency_20_plus"] += 1
                if entry.identifier in primary_entry_ids:
                    gain_counts["primary_entries_missing_french_frequency_20_plus"] += 1
            if entry.frequency >= 100:
                gain_counts["entries_missing_french_frequency_100_plus"] += 1
                if entry.identifier in preferred_entry_ids:
                    gain_counts["preferred_entries_missing_french_frequency_100_plus"] += 1
                if entry.identifier in primary_entry_ids:
                    gain_counts["primary_entries_missing_french_frequency_100_plus"] += 1
        else:
            gain_counts["entries_already_french"] += 1
        if new_glosses:
            gain_counts["entries_with_net_new_gloss"] += 1
            gain_counts["net_new_glosses"] += len(new_glosses)
            gain_counts["net_new_gloss_utf8_bytes"] += sum(
                len(gloss.encode("utf-8")) for gloss in new_glosses
            )

    candidate_examples = {
        (chinese, french)
        for candidate in candidate_words.values()
        for chinese, translations in candidate.examples.items()
        for french in translations
    }
    overlapping_examples = sorted(
        (chinese, french)
        for chinese, french in candidate_examples
        if chinese in pangmao_examples
    )
    fillable_examples = [
        {
            "chinese": chinese,
            "english": pangmao_examples[chinese]["english"],
            "candidate_french": french,
        }
        for chinese, french in overlapping_examples
        if not pangmao_examples[chinese]["french"]
    ]

    primary_missing = [
        row
        for row in resolved_rows
        if row["pangmao_primary"] and not row["pangmao_french"] and row["pangmao_frequency"] >= 1
    ]
    variant_missing = [
        row
        for row in resolved_rows
        if not row["pangmao_primary"] and not row["pangmao_french"] and row["pangmao_frequency"] >= 1
    ]
    frequent_extra = [
        row
        for row in resolved_rows
        if row["pangmao_french"] and row["net_new_french"] and row["pangmao_frequency"] >= 1
    ]
    polysemous = [row for row in resolved_rows if len(row["candidate_french"]) >= 3]

    regression_sample: list[dict] = []
    for word in REGRESSION_WORDS:
        candidate = candidate_words.get(word)
        if not candidate:
            continue
        entry, resolution = resolve_candidate(candidate, by_headword.get(word, []))
        regression_sample.append(
            sample_row(candidate, entry, resolution, preferred_entry_ids, primary_entry_ids)
        )

    samples = {
        "primary_missing_french": top_rows(
            primary_missing,
            sample_size,
            key=lambda row: (-row["pangmao_frequency"], row["word"]),
        ),
        "variant_missing_french": top_rows(
            variant_missing,
            sample_size,
            key=lambda row: (-row["pangmao_frequency"], row["word"]),
        ),
        "frequent_additional_senses": top_rows(
            frequent_extra,
            sample_size,
            key=lambda row: (-row["pangmao_frequency"], row["word"]),
        ),
        "polysemous": top_rows(
            polysemous,
            sample_size,
            key=lambda row: (-row["pangmao_frequency"], row["word"]),
        ),
        "unresolved": top_rows(
            unresolved,
            sample_size,
            key=lambda row: (-row["pangmao_frequency"], row["word"]),
        ),
        "candidate_only": top_rows(
            candidate_only,
            sample_size,
            key=lambda row: (len(row["word"]), row["word"]),
        ),
        "regressions": regression_sample,
        "fillable_examples": fillable_examples[:sample_size],
    }

    pangmao_missing_french = sum(not entry.definitions_fr for entry in entries)
    pangmao_missing_french_5_plus = sum(
        not entry.definitions_fr and entry.frequency >= 5 for entry in entries
    )
    return {
        "source": {
            "path": candidate_path.name,
            "bytes": candidate_path.stat().st_size,
            "sha256": file_sha256(candidate_path),
        },
        "candidate": candidate_stats,
        "pangmao": {
            "entries": len(entries),
            "entries_missing_french": pangmao_missing_french,
            "entries_missing_french_frequency_5_plus": pangmao_missing_french_5_plus,
            "examples": len(pangmao_examples),
            "examples_missing_french": sum(
                not value["french"] for value in pangmao_examples.values()
            ),
        },
        "matching": dict(sorted(resolution_counts.items())),
        "potential_definition_gain": dict(sorted(gain_counts.items())),
        "examples": {
            "candidate_explicit_translated_pairs": len(candidate_examples),
            "exact_chinese_overlap_with_pangmao": len({item[0] for item in overlapping_examples}),
            "pangmao_examples_fillable_in_french": len(fillable_examples),
        },
        "samples": samples,
    }


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--database", required=True, type=Path)
    parser.add_argument("--candidate", required=True, type=Path)
    parser.add_argument("--json-out", type=Path)
    parser.add_argument("--sample-size", type=int, default=20)
    args = parser.parse_args()
    report = evaluate(args.database, args.candidate, args.sample_size)
    rendered = json.dumps(report, ensure_ascii=False, indent=2, sort_keys=True)
    if args.json_out:
        args.json_out.parent.mkdir(parents=True, exist_ok=True)
        args.json_out.write_text(f"{rendered}\n", encoding="utf-8")
    print(rendered)


if __name__ == "__main__":
    main()
