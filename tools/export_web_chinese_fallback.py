#!/usr/bin/env python3
"""Export a lazy, sharded Chinese-to-French fallback lexicon for Pangmao Web."""

from __future__ import annotations

import argparse
import hashlib
import json
import re
import sqlite3
from collections import defaultdict
from dataclasses import dataclass
from pathlib import Path


SCHEMA_VERSION = 1
DEFAULT_SHARD_COUNT = 32
ANNOTATION_PATTERN = re.compile(r"\([^)]*\)")
WORD_PATTERN = re.compile(r"[a-z]+(?:'[a-z]+)?")
IGNORED_ENGLISH_TOKENS = {
    "a",
    "abbr",
    "also",
    "an",
    "archaic",
    "coll",
    "derog",
    "dialect",
    "esp",
    "fig",
    "idiom",
    "literary",
    "lit",
    "of",
    "slang",
    "the",
    "to",
    "used",
    "usu",
}


def non_blank_lines(value: str) -> list[str]:
    return list(dict.fromkeys(line.strip() for line in value.splitlines() if line.strip()))


def english_signature(value: str) -> str:
    normalized = ANNOTATION_PATTERN.sub(" ", value.casefold().replace("-", " "))
    return " ".join(
        token
        for token in WORD_PATTERN.findall(normalized)
        if token not in IGNORED_ENGLISH_TOKENS
    )


def useful_signature(value: str) -> bool:
    tokens = value.split()
    return bool(tokens) and (len(tokens) >= 2 or len(tokens[0]) >= 4)


def shard_index(value: str, shard_count: int = DEFAULT_SHARD_COUNT) -> int:
    value_hash = 2166136261
    for character in value:
        value_hash ^= ord(character)
        value_hash = (value_hash * 16777619) & 0xFFFFFFFF
    return value_hash % shard_count


def sha256_bytes(value: bytes) -> str:
    return hashlib.sha256(value).hexdigest()


@dataclass(frozen=True)
class DictionaryRow:
    identifier: int
    traditional: str
    simplified: str
    pinyin: str
    english: tuple[str, ...]
    french: tuple[str, ...]
    sources: str
    frequency: int


def load_rows(database: Path) -> list[DictionaryRow]:
    connection = sqlite3.connect(f"file:{database}?mode=ro", uri=True)
    try:
        rows = connection.execute(
            """
            SELECT id, traditional, simplified, pinyin,
                   definitions_en, definitions_fr, sources, frequency
            FROM entries
            ORDER BY id
            """
        ).fetchall()
    finally:
        connection.close()
    return [
        DictionaryRow(
            identifier=int(row[0]),
            traditional=str(row[1]),
            simplified=str(row[2]),
            pinyin=str(row[3]),
            english=tuple(non_blank_lines(str(row[4]))),
            french=tuple(non_blank_lines(str(row[5]))),
            sources=str(row[6]),
            frequency=int(row[7]),
        )
        for row in rows
    ]


def preferred_direct_matches(rows: list[DictionaryRow]) -> dict[str, list[DictionaryRow]]:
    matches: dict[str, list[DictionaryRow]] = defaultdict(list)
    for row in rows:
        if not row.french:
            continue
        for definition in row.english:
            signature = english_signature(definition)
            if useful_signature(signature):
                matches[signature].append(row)
    for signature in matches:
        matches[signature].sort(
            key=lambda row: (
                -row.frequency,
                len(row.french),
                len(row.simplified),
                row.simplified,
                row.identifier,
            )
        )
    return matches


def inferred_record(
    row: DictionaryRow,
    direct_matches: dict[str, list[DictionaryRow]],
) -> dict | None:
    candidates: list[tuple[str, DictionaryRow]] = []
    for definition in row.english:
        signature = english_signature(definition)
        if useful_signature(signature):
            candidates.extend(
                (signature, candidate)
                for candidate in direct_matches.get(signature, [])
                if candidate.identifier != row.identifier
            )
    if not candidates:
        return None
    candidates.sort(
        key=lambda item: (
            -item[1].frequency,
            len(item[1].french),
            len(item[1].simplified),
            item[1].simplified,
            item[1].identifier,
        )
    )
    signature, basis = candidates[0]
    # Keep only conservative bridges: a multi-word semantic signature and a
    # French-bearing basis that occurs in the reviewed/frequency-ranked corpus.
    # A broader single-word bridge creates too many plausible-looking false
    # friends for a learner-facing product.
    if len(signature.split()) < 2 or basis.frequency <= 0:
        return None
    return {
        "traditional": row.traditional,
        "simplified": row.simplified,
        "pinyin": row.pinyin,
        "possibleFrench": list(basis.french[:6]),
        "kind": "inferred",
        "inferredFrom": basis.simplified,
        "confidence": "strong",
        "source": f"{row.sources} → {basis.sources}",
    }


def direct_record(row: DictionaryRow) -> dict:
    return {
        "traditional": row.traditional,
        "simplified": row.simplified,
        "pinyin": row.pinyin,
        "french": list(row.french[:8]),
        "kind": "direct",
        "source": row.sources,
    }


def build_fallback(rows: list[DictionaryRow], shard_count: int) -> tuple[dict, list[dict]]:
    direct_matches = preferred_direct_matches(rows)
    shards: list[dict[str, list[dict]]] = [defaultdict(list) for _ in range(shard_count)]
    direct_count = 0
    inferred_count = 0

    for row in rows:
        record = direct_record(row) if row.french else inferred_record(row, direct_matches)
        if record is None:
            continue
        if record["kind"] == "direct":
            direct_count += 1
        else:
            inferred_count += 1
        for headword in dict.fromkeys((row.simplified, row.traditional)):
            if not headword:
                continue
            shards[shard_index(headword, shard_count)][headword].append(record)

    payloads = []
    total_keys = 0
    total_records = 0
    for entries in shards:
        ordered_entries = {}
        for headword in sorted(entries):
            values = sorted(
                entries[headword],
                key=lambda item: (
                    item["kind"] != "direct",
                    item.get("pinyin", ""),
                    item.get("simplified", ""),
                ),
            )[:6]
            ordered_entries[headword] = values
            total_records += len(values)
        total_keys += len(ordered_entries)
        payloads.append({"schemaVersion": SCHEMA_VERSION, "entries": ordered_entries})

    summary = {
        "schemaVersion": SCHEMA_VERSION,
        "shardCount": shard_count,
        "keyCount": total_keys,
        "recordCount": total_records,
        "directEntryCount": direct_count,
        "inferredEntryCount": inferred_count,
        "source": {
            "name": "Pangmao Chinese dictionary",
            "direct": "CFDICT and reviewed French definitions",
            "inference": "Exact English-gloss bridge from CC-CEDICT to French definitions",
            "notice": "Inferred meanings are possibilities, not direct dictionary translations.",
        },
    }
    return summary, payloads


def write_fallback(summary: dict, payloads: list[dict], output: Path) -> dict:
    output.mkdir(parents=True, exist_ok=True)
    shard_metadata = []
    for index, payload in enumerate(payloads):
        filename = f"{index:02x}.json"
        encoded = (
            json.dumps(payload, ensure_ascii=False, separators=(",", ":")) + "\n"
        ).encode("utf-8")
        (output / filename).write_bytes(encoded)
        shard_metadata.append(
            {
                "file": filename,
                "bytes": len(encoded),
                "sha256": sha256_bytes(encoded),
                "keyCount": len(payload["entries"]),
                "recordCount": sum(len(values) for values in payload["entries"].values()),
            }
        )
    manifest = {**summary, "shards": shard_metadata}
    (output / "manifest.json").write_text(
        json.dumps(manifest, ensure_ascii=False, separators=(",", ":")) + "\n",
        encoding="utf-8",
    )
    return manifest


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "--database",
        type=Path,
        default=Path("app/src/main/assets/databases/pangmao.db"),
    )
    parser.add_argument(
        "--output",
        type=Path,
        default=Path("webApp/data/chinese-fallback"),
    )
    parser.add_argument("--shards", type=int, default=DEFAULT_SHARD_COUNT)
    arguments = parser.parse_args()
    if arguments.shards <= 0 or arguments.shards > 256:
        raise ValueError("--shards must be between 1 and 256")
    summary, payloads = build_fallback(load_rows(arguments.database), arguments.shards)
    manifest = write_fallback(summary, payloads, arguments.output)
    print(
        json.dumps(
            {
                "output": str(arguments.output),
                "shards": manifest["shardCount"],
                "keys": manifest["keyCount"],
                "records": manifest["recordCount"],
                "directEntries": manifest["directEntryCount"],
                "inferredEntries": manifest["inferredEntryCount"],
                "bytes": sum(item["bytes"] for item in manifest["shards"]),
            },
            ensure_ascii=False,
            sort_keys=True,
        )
    )


if __name__ == "__main__":
    main()
