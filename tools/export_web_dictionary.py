#!/usr/bin/env python3
"""Export Pangmao's French learning dictionary as a compact, deterministic web pack."""

from __future__ import annotations

import argparse
import hashlib
import json
import sqlite3
from pathlib import Path
from typing import Any


WEB_PACK_SCHEMA_VERSION = 2
SUPPORTED_DATABASE_SCHEMA = 4


def non_blank_lines(value: str) -> list[str]:
    return [line for line in value.splitlines() if line.strip()]


def fetch_grouped_values(
    connection: sqlite3.Connection,
    query: str,
) -> dict[int, list[str]]:
    grouped: dict[int, list[str]] = {}
    for owner_id, value in connection.execute(query):
        grouped.setdefault(int(owner_id), []).append(str(value))
    return grouped


def load_chinese_glosses(path: Path | None) -> tuple[dict[str, list[dict[str, Any]]], dict | None]:
    if path is None:
        return {}, None
    if not path.is_file():
        raise FileNotFoundError(f"Chinese glossary enrichment not found: {path}")
    value = json.loads(path.read_text(encoding="utf-8"))
    if value.get("schemaVersion") != 1 or not isinstance(value.get("entries"), list):
        raise ValueError("Unsupported Chinese glossary enrichment schema")
    source = value.get("source")
    if not isinstance(source, dict) or not source.get("code") or not source.get("license"):
        raise ValueError("Chinese glossary enrichment source metadata is incomplete")

    by_identifier: dict[str, list[dict[str, Any]]] = {}
    for item in value["entries"]:
        identifier = str(item.get("id", ""))
        groups = item.get("groups")
        if not identifier or not isinstance(groups, list) or identifier in by_identifier:
            raise ValueError(f"Invalid or duplicate Chinese glossary entry: {identifier}")
        clean_groups = []
        for group in groups:
            glosses = group.get("glosses") if isinstance(group, dict) else None
            if not isinstance(glosses, list) or not glosses or not all(
                isinstance(gloss, str) and gloss.strip() for gloss in glosses
            ):
                raise ValueError(f"Invalid Chinese gloss group for {identifier}")
            clean_groups.append(
                {
                    "partOfSpeech": str(group.get("partOfSpeech", "unknown")),
                    "label": str(group.get("label", "")),
                    "glosses": list(dict.fromkeys(gloss.strip() for gloss in glosses)),
                }
            )
        by_identifier[identifier] = clean_groups
    if value.get("entryCount") != len(by_identifier):
        raise ValueError("Chinese glossary enrichment entryCount mismatch")
    return by_identifier, source


def export_french_pack(
    database: Path,
    chinese_glosses: Path | None = None,
) -> dict[str, Any]:
    if not database.is_file():
        raise FileNotFoundError(f"Dictionary database not found: {database}")

    connection = sqlite3.connect(f"file:{database}?mode=ro", uri=True)
    try:
        schema_version = int(connection.execute("PRAGMA user_version").fetchone()[0])
        if schema_version != SUPPORTED_DATABASE_SCHEMA:
            raise ValueError(
                f"Expected dictionary schema {SUPPORTED_DATABASE_SCHEMA}, found {schema_version}"
            )

        metadata = dict(connection.execute("SELECT key, value FROM metadata"))
        source_row = connection.execute(
            """
            SELECT code, display_name, url, revision, license
            FROM learning_sources
            WHERE language = 'fr'
            ORDER BY code
            LIMIT 1
            """
        ).fetchone()
        if source_row is None:
            raise ValueError("French learning source is missing")

        forms = fetch_grouped_values(
            connection,
            """
            SELECT entry_id, form
            FROM learning_forms
            WHERE entry_id IN (SELECT id FROM learning_entries WHERE language = 'fr')
            ORDER BY entry_id, position
            """,
        )
        pronunciations = fetch_grouped_values(
            connection,
            """
            SELECT entry_id, pronunciation
            FROM learning_pronunciations
            WHERE entry_id IN (SELECT id FROM learning_entries WHERE language = 'fr')
            ORDER BY entry_id, position
            """,
        )

        senses_by_entry: dict[int, list[dict[str, Any]]] = {}
        senses_by_id: dict[int, dict[str, Any]] = {}
        for sense_id, entry_id, definitions in connection.execute(
            """
            SELECT s.id, s.entry_id, s.definitions
            FROM learning_senses s
            JOIN learning_entries e ON e.id = s.entry_id
            WHERE e.language = 'fr'
            ORDER BY s.entry_id, s.position
            """
        ):
            sense = {
                "definitions": non_blank_lines(str(definitions)),
                "chinese": [],
            }
            senses_by_entry.setdefault(int(entry_id), []).append(sense)
            senses_by_id[int(sense_id)] = sense

        for sense_id, chinese in connection.execute(
            """
            SELECT q.sense_id, q.chinese
            FROM learning_equivalents q
            JOIN learning_senses s ON s.id = q.sense_id
            JOIN learning_entries e ON e.id = s.entry_id
            WHERE e.language = 'fr'
            ORDER BY q.sense_id, q.position
            """
        ):
            senses_by_id[int(sense_id)]["chinese"].append(str(chinese))

        chinese_glosses_by_identifier, enrichment_source = load_chinese_glosses(chinese_glosses)
        entries: list[dict[str, Any]] = []
        enriched_entry_count = 0
        for row in connection.execute(
            """
            SELECT id, primary_form, parts_of_speech, genders,
                source_code, source_entry_index
            FROM learning_entries
            WHERE language = 'fr'
            ORDER BY primary_form_search, primary_form, id
            """
        ):
            (
                entry_id,
                primary_form,
                parts_of_speech,
                genders,
                entry_source_code,
                source_entry_index,
            ) = row
            entry_id = int(entry_id)
            entry_senses = senses_by_entry.get(entry_id, [])
            if not entry_senses or any(not sense["chinese"] for sense in entry_senses):
                raise ValueError(f"French entry {entry_id} has an unusable sense")
            stable_identifier = f"fr:{entry_source_code}:{source_entry_index}"
            entry = {
                # The SQLite id is rebuilt sequentially. The source identity remains
                # stable across pack rebuilds and is safe for local favourites/history.
                "id": stable_identifier,
                "headword": str(primary_form),
                "forms": forms.get(entry_id, [str(primary_form)]),
                "pronunciations": pronunciations.get(entry_id, []),
                "partsOfSpeech": non_blank_lines(str(parts_of_speech)),
                "genders": non_blank_lines(str(genders)),
                "senses": entry_senses,
            }
            if stable_identifier in chinese_glosses_by_identifier:
                entry["chineseGlosses"] = chinese_glosses_by_identifier[stable_identifier]
                enriched_entry_count += 1
            entries.append(entry)

        expected_count = int(metadata.get("learning_entry_count_fr", len(entries)))
        if len(entries) != expected_count:
            raise ValueError(
                f"French entry count mismatch: expected {expected_count}, exported {len(entries)}"
            )

        entries_bytes = json.dumps(
            entries,
            ensure_ascii=False,
            separators=(",", ":"),
            sort_keys=True,
        ).encode("utf-8")
        source_code, display_name, url, revision, license_name = map(str, source_row)
        result = {
            "schemaVersion": WEB_PACK_SCHEMA_VERSION,
            "language": "fr",
            "entryCount": len(entries),
            "enrichedEntryCount": enriched_entry_count,
            "entriesSha256": hashlib.sha256(entries_bytes).hexdigest(),
            "source": {
                "code": source_code,
                "name": display_name,
                "url": url,
                "revision": revision,
                "license": license_name,
            },
            "entries": entries,
        }
        if enrichment_source is not None:
            result["enrichmentSources"] = [enrichment_source]
        return result
    finally:
        connection.close()


def write_pack(pack: dict[str, Any], output: Path, pretty: bool = False) -> None:
    output.parent.mkdir(parents=True, exist_ok=True)
    serialized = json.dumps(
        pack,
        ensure_ascii=False,
        indent=2 if pretty else None,
        separators=None if pretty else (",", ":"),
    )
    output.write_text(serialized + "\n", encoding="utf-8")


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
        default=Path("webApp/data/french-pack.json"),
    )
    parser.add_argument(
        "--chinese-glosses",
        type=Path,
        default=Path("tools/web_data/zhwiktionary_french_glosses.json"),
    )
    parser.add_argument("--pretty", action="store_true")
    arguments = parser.parse_args()

    pack = export_french_pack(arguments.database, arguments.chinese_glosses)
    write_pack(pack, arguments.output, arguments.pretty)
    size_mb = arguments.output.stat().st_size / (1024 * 1024)
    print(
        json.dumps(
            {
                "output": str(arguments.output),
                "entries": pack["entryCount"],
                "enriched_entries": pack["enrichedEntryCount"],
                "sha256": pack["entriesSha256"],
                "size_mb": round(size_mb, 2),
            },
            ensure_ascii=False,
            indent=2,
        )
    )


if __name__ == "__main__":
    main()
