#!/usr/bin/env python3
"""Filter Chinese Wiktionary French glosses for Pangmao's Web dictionary.

The upstream Kaikki JSONL extract is intentionally not committed. This script
keeps only exact, unambiguous French headwords already present in Pangmao and
converts common traditional forms with the project's pinned Chinese database.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import re
import sqlite3
import unicodedata
from collections import defaultdict
from pathlib import Path
from typing import Callable, Iterable


SCHEMA_VERSION = 1
MAX_GLOSS_LENGTH = 280
SPACE_PATTERN = re.compile(r"\s+")
HAN_PATTERN = re.compile(r"[\u3400-\u4dbf\u4e00-\u9fff\uf900-\ufaff]")


def normalized_text(value: object) -> str:
    return SPACE_PATTERN.sub(" ", unicodedata.normalize("NFC", str(value)).strip())


def lookup_key(value: object) -> str:
    return normalized_text(value).casefold()


def contains_han(value: str) -> bool:
    return bool(HAN_PATTERN.search(value))


def unique(values: Iterable[str]) -> list[str]:
    result: list[str] = []
    seen: set[str] = set()
    for raw_value in values:
        value = normalized_text(raw_value)
        marker = value.casefold()
        if value and marker not in seen:
            seen.add(marker)
            result.append(value)
    return result


def file_sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for block in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(block)
    return digest.hexdigest()


def load_unique_headwords(pack_path: Path) -> dict[str, dict[str, str]]:
    pack = json.loads(pack_path.read_text(encoding="utf-8"))
    grouped: dict[str, list[dict[str, str]]] = defaultdict(list)
    for entry in pack.get("entries") or []:
        grouped[lookup_key(entry.get("headword", ""))].append(
            {"id": str(entry.get("id", "")), "headword": str(entry.get("headword", ""))}
        )
    return {
        key: values[0]
        for key, values in grouped.items()
        if key and len(values) == 1 and values[0]["id"]
    }


def build_simplifier(database: Path) -> Callable[[str], str]:
    connection = sqlite3.connect(f"file:{database}?mode=ro", uri=True)
    try:
        pairs = connection.execute(
            """
            SELECT traditional, simplified
            FROM entries
            WHERE traditional != simplified
              AND traditional != ''
              AND simplified != ''
            ORDER BY length(traditional) DESC, traditional
            """
        ).fetchall()
    finally:
        connection.close()

    trie: dict[str, dict] = {}
    for traditional, simplified in pairs:
        node = trie
        for character in str(traditional):
            node = node.setdefault(character, {})
        node[""] = str(simplified)

    def simplify(value: str) -> str:
        result: list[str] = []
        position = 0
        while position < len(value):
            node = trie
            cursor = position
            replacement: tuple[int, str] | None = None
            while cursor < len(value) and value[cursor] in node:
                node = node[value[cursor]]
                cursor += 1
                if "" in node:
                    replacement = (cursor, node[""])
            if replacement is None:
                result.append(value[position])
                position += 1
            else:
                position, simplified = replacement
                result.append(simplified)
        return "".join(result)

    return simplify


def filter_glosses(
    source: Path,
    pack_path: Path,
    database: Path,
    *,
    source_revision: str,
    extraction_date: str,
) -> dict:
    headwords = load_unique_headwords(pack_path)
    simplify = build_simplifier(database)
    grouped: dict[str, dict] = {}
    matched_rows = 0

    with source.open(encoding="utf-8") as handle:
        for line_number, line in enumerate(handle, start=1):
            try:
                row = json.loads(line)
            except json.JSONDecodeError as error:
                raise ValueError(f"Invalid JSONL at {source}:{line_number}") from error
            if row.get("lang_code") != "fr":
                continue
            match = headwords.get(lookup_key(row.get("word", "")))
            if match is None:
                continue

            glosses = unique(
                simplify(str(gloss))
                for sense in row.get("senses") or []
                for gloss in sense.get("glosses") or []
                if isinstance(gloss, str)
                and 0 < len(normalized_text(gloss)) <= MAX_GLOSS_LENGTH
                and contains_han(normalized_text(gloss))
            )
            if not glosses:
                continue
            matched_rows += 1
            part_of_speech = normalized_text(row.get("pos", "unknown")) or "unknown"
            part_of_speech_label = simplify(
                normalized_text(row.get("pos_title", ""))
            )
            entry = grouped.setdefault(
                match["id"],
                {
                    "id": match["id"],
                    "headword": match["headword"],
                    "groups": {},
                },
            )
            group_key = f"{part_of_speech}\u0000{part_of_speech_label}"
            group = entry["groups"].setdefault(
                group_key,
                {
                    "partOfSpeech": part_of_speech,
                    "label": part_of_speech_label,
                    "glosses": [],
                },
            )
            group["glosses"] = unique([*group["glosses"], *glosses])

    entries = []
    for identifier in sorted(grouped):
        item = grouped[identifier]
        entries.append(
            {
                "id": item["id"],
                "headword": item["headword"],
                "groups": [item["groups"][key] for key in sorted(item["groups"])],
            }
        )

    return {
        "schemaVersion": SCHEMA_VERSION,
        "source": {
            "code": "zhwiktionary-french",
            "name": "中文维基词典法语词条",
            "url": "https://kaikki.org/zhwiktionary/%E6%B3%95%E8%AA%9E/",
            "revision": source_revision,
            "extractionDate": extraction_date,
            "downloadSha256": file_sha256(source),
            "license": "CC BY-SA 4.0",
            "attribution": "中文维基词典贡献者 · Kaikki / Wiktextract",
        },
        "entryCount": len(entries),
        "matchedRowCount": matched_rows,
        "entries": entries,
    }


def write_json(value: dict, output: Path) -> None:
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(
        json.dumps(value, ensure_ascii=False, separators=(",", ":")) + "\n",
        encoding="utf-8",
    )


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--source", type=Path, required=True)
    parser.add_argument(
        "--pack",
        type=Path,
        default=Path("webApp/data/french-pack.json"),
    )
    parser.add_argument(
        "--database",
        type=Path,
        default=Path("app/src/main/assets/databases/pangmao.db"),
    )
    parser.add_argument(
        "--output",
        type=Path,
        default=Path("tools/web_data/zhwiktionary_french_glosses.json"),
    )
    parser.add_argument("--source-revision", default="2026-09-01")
    parser.add_argument("--extraction-date", default="2026-09-22")
    arguments = parser.parse_args()
    result = filter_glosses(
        arguments.source,
        arguments.pack,
        arguments.database,
        source_revision=arguments.source_revision,
        extraction_date=arguments.extraction_date,
    )
    write_json(result, arguments.output)
    print(
        json.dumps(
            {
                "output": str(arguments.output),
                "entries": result["entryCount"],
                "matchedRows": result["matchedRowCount"],
                "sourceSha256": result["source"]["downloadSha256"],
            },
            ensure_ascii=False,
            sort_keys=True,
        )
    )


if __name__ == "__main__":
    main()
