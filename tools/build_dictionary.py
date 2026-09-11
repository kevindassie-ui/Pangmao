#!/usr/bin/env python3
"""Build Pangmao's immutable offline dictionary database.

The script intentionally accepts local source paths. It never scrapes Pleco and it
does not automate MDBG downloads. See tools/SOURCES.md for provenance and licenses.
"""

from __future__ import annotations

import argparse
import csv
import json
import re
import sqlite3
import unicodedata
from collections import Counter, defaultdict
from dataclasses import dataclass, field
from pathlib import Path
from typing import Iterable


CEDICT_LINE = re.compile(r"^(\S+)\s+(\S+)\s+\[([^]]+)]\s+/(.*)/\s*$")
HAN_RANGES = (
    (0x3400, 0x4DBF),
    (0x4E00, 0x9FFF),
    (0xF900, 0xFAFF),
    (0x20000, 0x323AF),
)


def is_han(character: str) -> bool:
    if not character:
        return False
    codepoint = ord(character)
    return any(start <= codepoint <= end for start, end in HAN_RANGES)


def pinyin_plain(value: str) -> str:
    value = value.replace("u:", "v").replace("ü", "v").lower()
    value = "".join(
        char
        for char in unicodedata.normalize("NFD", value)
        if unicodedata.category(char) != "Mn" and not char.isdigit()
    )
    return re.sub(r"[^a-zv]+", "", value)


def unique(items: Iterable[str]) -> list[str]:
    seen: set[str] = set()
    result: list[str] = []
    for item in items:
        item = item.strip()
        if item and item not in seen:
            seen.add(item)
            result.append(item)
    return result


@dataclass
class MutableEntry:
    traditional: str
    simplified: str
    pinyin: str
    definitions_en: list[str] = field(default_factory=list)
    definitions_fr: list[str] = field(default_factory=list)
    sources: set[str] = field(default_factory=set)
    frequency: int = 0


def entry_key(traditional: str, simplified: str, pinyin: str) -> tuple[str, str, str]:
    return traditional, simplified, re.sub(r"\s+", " ", pinyin.strip()).lower()


def load_cc_cedict(path: Path, entries: dict[tuple[str, str, str], MutableEntry]) -> None:
    raw = path.read_text(encoding="utf-8-sig").strip()
    if not raw.startswith("export default "):
        raise ValueError(f"Unexpected cc-cedict data format: {path}")
    payload = raw[len("export default ") :].rstrip(";\ufeff")
    data = json.loads(payload)
    for row in data["all"]:
        traditional, simplified, pinyin, definitions = row[:4]
        if isinstance(definitions, str):
            definitions = [definitions]
        key = entry_key(traditional, simplified, pinyin)
        entry = entries.setdefault(key, MutableEntry(traditional, simplified, pinyin))
        entry.definitions_en = unique([*entry.definitions_en, *definitions])
        entry.sources.add("CC-CEDICT")


def load_cfdict(path: Path, entries: dict[tuple[str, str, str], MutableEntry]) -> None:
    with path.open(encoding="utf-8-sig", errors="replace") as handle:
        for line in handle:
            line = line.strip().lstrip("\ufeff")
            if not line or line.startswith("#"):
                continue
            match = CEDICT_LINE.match(line)
            if not match:
                continue
            traditional, simplified, pinyin, definitions_blob = match.groups()
            definitions = unique(definitions_blob.split("/"))
            key = entry_key(traditional, simplified, pinyin)
            entry = entries.setdefault(key, MutableEntry(traditional, simplified, pinyin))
            entry.definitions_fr = unique([*entry.definitions_fr, *definitions])
            entry.sources.add("CFDICT")


def load_pangmao_entries(entries: dict[tuple[str, str, str], MutableEntry]) -> None:
    custom = [
        (
            "胖貓",
            "胖猫",
            "pang4 mao1",
            ["fat cat; chubby cat", "Pangmao, name of this independent dictionary application"],
            ["chat dodu; gros chat", "Pangmao, nom de cette application indépendante de dictionnaire"],
        ),
    ]
    for traditional, simplified, pinyin, english, french in custom:
        key = entry_key(traditional, simplified, pinyin)
        entries[key] = MutableEntry(
            traditional=traditional,
            simplified=simplified,
            pinyin=pinyin,
            definitions_en=english,
            definitions_fr=french,
            sources={"Pangmao"},
        )


def load_examples(path: Path) -> list[tuple[int, str, int, str]]:
    examples: list[tuple[int, str, int, str]] = []
    seen: set[tuple[str, str]] = set()
    with path.open(encoding="utf-8-sig", newline="") as handle:
        for row in csv.reader(handle, delimiter="\t"):
            if len(row) < 4:
                continue
            chinese_id, chinese, english_id, english = row[:4]
            pair = chinese.strip(), english.strip()
            if not pair[0] or not pair[1] or pair in seen:
                continue
            seen.add(pair)
            examples.append((int(chinese_id), pair[0], int(english_id), pair[1]))
    return examples


def greedy_tokens(text: str, headwords: set[str], maximum_length: int) -> list[str]:
    tokens: list[str] = []
    index = 0
    while index < len(text):
        best: str | None = None
        upper = min(maximum_length, len(text) - index)
        for length in range(upper, 0, -1):
            candidate = text[index : index + length]
            if candidate in headwords:
                best = candidate
                break
        if best is None:
            best = text[index]
        tokens.append(best)
        index += len(best)
    return tokens


def apply_frequency_and_pinyin(
    entries: dict[tuple[str, str, str], MutableEntry],
    examples: list[tuple[int, str, int, str]],
) -> tuple[Counter[str], dict[str, str]]:
    headwords = {
        word
        for entry in entries.values()
        for word in (entry.simplified, entry.traditional)
        if word and len(word) <= 8
    }
    maximum_length = max(map(len, headwords), default=1)
    frequencies: Counter[str] = Counter()
    preferred_pinyin: dict[str, str] = {}
    for entry in entries.values():
        for word in (entry.simplified, entry.traditional):
            if word and word not in preferred_pinyin:
                preferred_pinyin[word] = entry.pinyin
    for _, chinese, _, _ in examples:
        for token in greedy_tokens(chinese, headwords, maximum_length):
            if token in headwords:
                frequencies[token] += 1
    for entry in entries.values():
        entry.frequency = max(
            frequencies.get(entry.simplified, 0),
            frequencies.get(entry.traditional, 0),
        )
    return frequencies, preferred_pinyin


def sentence_pinyin(
    sentence: str,
    headwords: set[str],
    maximum_length: int,
    preferred_pinyin: dict[str, str],
) -> str:
    rendered: list[str] = []
    for token in greedy_tokens(sentence, headwords, maximum_length):
        pronunciation = preferred_pinyin.get(token)
        if pronunciation:
            rendered.append(pronunciation)
        elif rendered and not is_han(token) and token.strip() and token not in "，。！？；：、“”‘’（）…—":
            rendered.append(token)
    return " ".join(rendered)


def parse_unihan(unihan_dir: Path, wanted: set[str]) -> dict[str, dict[str, str]]:
    properties = {
        "kMandarin",
        "kDefinition",
        "kRSUnicode",
        "kTotalStrokes",
        "kSimplifiedVariant",
        "kTraditionalVariant",
    }
    records: dict[str, dict[str, str]] = defaultdict(dict)
    for path in sorted(unihan_dir.glob("Unihan_*.txt")):
        with path.open(encoding="utf-8") as handle:
            for line in handle:
                if not line.startswith("U+"):
                    continue
                codepoint, prop, value = line.rstrip("\n").split("\t", 2)
                if prop not in properties:
                    continue
                character = chr(int(codepoint[2:], 16))
                if character in wanted:
                    records[character][prop] = value
    return records


def variants_to_text(value: str | None) -> str:
    if not value:
        return ""
    result: list[str] = []
    for codepoint in re.findall(r"U\+([0-9A-F]{4,6})", value):
        result.append(chr(int(codepoint, 16)))
    return " ".join(result)


def create_database(
    output: Path,
    entries: dict[tuple[str, str, str], MutableEntry],
    examples: list[tuple[int, str, int, str]],
    unihan: dict[str, dict[str, str]],
    preferred_pinyin: dict[str, str],
    metadata: dict[str, str],
) -> None:
    output.parent.mkdir(parents=True, exist_ok=True)
    if output.exists():
        output.unlink()
    connection = sqlite3.connect(output)
    connection.executescript(
        """
        PRAGMA page_size = 4096;
        PRAGMA journal_mode = OFF;
        PRAGMA synchronous = OFF;
        PRAGMA temp_store = MEMORY;
        CREATE TABLE entries (
            id INTEGER PRIMARY KEY,
            traditional TEXT NOT NULL,
            simplified TEXT NOT NULL,
            pinyin TEXT NOT NULL,
            pinyin_plain TEXT NOT NULL,
            definitions_en TEXT NOT NULL DEFAULT '',
            definitions_fr TEXT NOT NULL DEFAULT '',
            sources TEXT NOT NULL,
            frequency INTEGER NOT NULL DEFAULT 0
        );
        CREATE INDEX entries_simplified_idx ON entries(simplified);
        CREATE INDEX entries_traditional_idx ON entries(traditional);
        CREATE INDEX entries_pinyin_plain_idx ON entries(pinyin_plain);
        CREATE INDEX entries_frequency_idx ON entries(frequency DESC);
        CREATE VIRTUAL TABLE entries_fts USING fts4(
            simplified,
            traditional,
            pinyin_plain,
            definitions_en,
            definitions_fr,
            tokenize=unicode61 "remove_diacritics=2"
        );
        CREATE TABLE headwords (
            word TEXT PRIMARY KEY,
            preferred_entry_id INTEGER NOT NULL,
            length INTEGER NOT NULL
        ) WITHOUT ROWID;
        CREATE INDEX headwords_length_idx ON headwords(length DESC);
        CREATE TABLE examples (
            id INTEGER PRIMARY KEY,
            tatoeba_chinese_id INTEGER NOT NULL,
            chinese TEXT NOT NULL,
            pinyin TEXT NOT NULL,
            tatoeba_english_id INTEGER NOT NULL,
            english TEXT NOT NULL
        );
        CREATE TABLE characters (
            character TEXT PRIMARY KEY,
            codepoint TEXT NOT NULL,
            mandarin TEXT NOT NULL DEFAULT '',
            definition TEXT NOT NULL DEFAULT '',
            radical INTEGER,
            additional_strokes INTEGER,
            total_strokes TEXT NOT NULL DEFAULT '',
            simplified_variants TEXT NOT NULL DEFAULT '',
            traditional_variants TEXT NOT NULL DEFAULT ''
        ) WITHOUT ROWID;
        CREATE TABLE metadata (
            key TEXT PRIMARY KEY,
            value TEXT NOT NULL
        ) WITHOUT ROWID;
        """
    )

    ordered_entries = sorted(
        entries.values(),
        key=lambda item: (-item.frequency, item.simplified, item.pinyin.lower()),
    )
    rows: list[tuple] = []
    entry_ids: dict[tuple[str, str, str], int] = {}
    for index, entry in enumerate(ordered_entries, start=1):
        row = (
            index,
            entry.traditional,
            entry.simplified,
            entry.pinyin,
            pinyin_plain(entry.pinyin),
            "\n".join(entry.definitions_en),
            "\n".join(entry.definitions_fr),
            " · ".join(sorted(entry.sources)),
            entry.frequency,
        )
        rows.append(row)
        entry_ids[entry_key(entry.traditional, entry.simplified, entry.pinyin)] = index
    connection.executemany(
        "INSERT INTO entries VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
        rows,
    )
    connection.execute(
        """
        INSERT INTO entries_fts(docid, simplified, traditional, pinyin_plain, definitions_en, definitions_fr)
        SELECT id, simplified, traditional, pinyin_plain, definitions_en, definitions_fr FROM entries
        """
    )

    preferred: dict[str, tuple[int, int]] = {}
    for entry in ordered_entries:
        identifier = entry_ids[entry_key(entry.traditional, entry.simplified, entry.pinyin)]
        for word in (entry.simplified, entry.traditional):
            if not word:
                continue
            score = entry.frequency * 100 + (10 if entry.definitions_fr else 0) + (1 if entry.definitions_en else 0)
            if word not in preferred or score > preferred[word][0]:
                preferred[word] = score, identifier
    connection.executemany(
        "INSERT INTO headwords(word, preferred_entry_id, length) VALUES (?, ?, ?)",
        ((word, value[1], len(word)) for word, value in preferred.items()),
    )

    headword_set = {word for word in preferred if len(word) <= 8}
    maximum_length = max(map(len, headword_set), default=1)
    example_rows = []
    for identifier, (chinese_id, chinese, english_id, english) in enumerate(examples, start=1):
        example_rows.append(
            (
                identifier,
                chinese_id,
                chinese,
                sentence_pinyin(chinese, headword_set, maximum_length, preferred_pinyin),
                english_id,
                english,
            )
        )
    connection.executemany("INSERT INTO examples VALUES (?, ?, ?, ?, ?, ?)", example_rows)

    character_rows = []
    for character, values in sorted(unihan.items(), key=lambda item: ord(item[0])):
        radical = None
        additional = None
        match = re.search(r"(\d+)'?\.(\d+)", values.get("kRSUnicode", ""))
        if match:
            radical = int(match.group(1))
            additional = int(match.group(2))
        character_rows.append(
            (
                character,
                f"U+{ord(character):04X}",
                values.get("kMandarin", ""),
                values.get("kDefinition", ""),
                radical,
                additional,
                values.get("kTotalStrokes", ""),
                variants_to_text(values.get("kSimplifiedVariant")),
                variants_to_text(values.get("kTraditionalVariant")),
            )
        )
    connection.executemany("INSERT INTO characters VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)", character_rows)
    connection.executemany("INSERT INTO metadata VALUES (?, ?)", sorted(metadata.items()))
    connection.execute("PRAGMA user_version = 1")
    connection.commit()
    connection.execute("ANALYZE")
    connection.execute("VACUUM")
    integrity = connection.execute("PRAGMA integrity_check").fetchone()[0]
    if integrity != "ok":
        raise RuntimeError(f"Database integrity check failed: {integrity}")
    connection.close()


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--cc-cedict", required=True, type=Path)
    parser.add_argument("--cfdict", required=True, type=Path)
    parser.add_argument("--tatoeba", required=True, type=Path)
    parser.add_argument("--unihan-dir", required=True, type=Path)
    parser.add_argument("--output", required=True, type=Path)
    parser.add_argument("--cc-revision", default="unknown")
    parser.add_argument("--tatoeba-release", default="unknown")
    args = parser.parse_args()

    entries: dict[tuple[str, str, str], MutableEntry] = {}
    load_cc_cedict(args.cc_cedict, entries)
    load_cfdict(args.cfdict, entries)
    load_pangmao_entries(entries)
    examples = load_examples(args.tatoeba)
    _, preferred_pinyin = apply_frequency_and_pinyin(entries, examples)
    wanted_characters = {
        character
        for entry in entries.values()
        for word in (entry.simplified, entry.traditional)
        for character in word
        if is_han(character)
    }
    unihan = parse_unihan(args.unihan_dir, wanted_characters)
    metadata = {
        "schema_version": "1",
        "cc_cedict_revision": args.cc_revision,
        "cfdict_downloaded": "2026-09-11",
        "tatoeba_release": args.tatoeba_release,
        "unihan_version": "17.0.0",
        "entry_count": str(len(entries)),
        "example_count": str(len(examples)),
        "character_count": str(len(unihan)),
    }
    create_database(args.output, entries, examples, unihan, preferred_pinyin, metadata)
    size_mb = args.output.stat().st_size / (1024 * 1024)
    print(json.dumps({**metadata, "size_mb": round(size_mb, 2)}, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
