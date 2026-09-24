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


def load_reviewed_supplements(path: Path | None) -> tuple[list[dict[str, Any]], dict | None]:
    if path is None:
        return [], None
    if not path.is_file():
        raise FileNotFoundError(f"Reviewed French supplements not found: {path}")
    value = json.loads(path.read_text(encoding="utf-8"))
    if value.get("schemaVersion") != 1 or not isinstance(value.get("entries"), list):
        raise ValueError("Unsupported reviewed French supplement schema")
    source = value.get("source")
    if not isinstance(source, dict) or not all(
        source.get(key) for key in ("code", "name", "url", "revision", "license")
    ):
        raise ValueError("Reviewed French supplement source metadata is incomplete")

    entries: list[dict[str, Any]] = []
    identifiers: set[str] = set()
    for item in value["entries"]:
        if not isinstance(item, dict):
            raise ValueError("Reviewed French supplement entry must be an object")
        identifier = item.get("id")
        headword = item.get("headword")
        forms = item.get("forms")
        senses = item.get("senses")
        if (
            not isinstance(identifier, str)
            or not identifier.startswith(f'fr:{source["code"]}:')
            or identifier in identifiers
            or not isinstance(headword, str)
            or not headword.strip()
            or not isinstance(forms, list)
            or headword not in forms
            or not all(isinstance(form, str) and form.strip() for form in forms)
            or not isinstance(senses, list)
            or not senses
        ):
            raise ValueError(f"Invalid reviewed French supplement entry: {identifier}")
        clean_senses = []
        for sense in senses:
            definitions = sense.get("definitions") if isinstance(sense, dict) else None
            chinese = sense.get("chinese") if isinstance(sense, dict) else None
            if (
                not isinstance(definitions, list)
                or not all(
                    isinstance(definition, str) and definition.strip()
                    for definition in definitions
                )
                or not isinstance(chinese, list)
                or not chinese
                or not all(isinstance(equivalent, str) and equivalent.strip() for equivalent in chinese)
            ):
                raise ValueError(f"Invalid reviewed French supplement sense: {identifier}")
            clean_senses.append(
                {
                    "definitions": list(dict.fromkeys(definitions)),
                    "chinese": list(dict.fromkeys(chinese)),
                }
            )
        entries.append(
            {
                "id": identifier,
                "headword": headword.strip(),
                "forms": list(dict.fromkeys(form.strip() for form in forms)),
                "pronunciations": list(
                    dict.fromkeys(
                        str(candidate).strip()
                        for candidate in item.get("pronunciations", [])
                        if str(candidate).strip()
                    )
                ),
                "partsOfSpeech": list(
                    dict.fromkeys(
                        str(candidate).strip()
                        for candidate in item.get("partsOfSpeech", [])
                        if str(candidate).strip()
                    )
                ),
                "genders": list(
                    dict.fromkeys(
                        str(candidate).strip()
                        for candidate in item.get("genders", [])
                        if str(candidate).strip()
                    )
                ),
                "senses": clean_senses,
            }
        )
        identifiers.add(identifier)
    if value.get("entryCount") != len(entries):
        raise ValueError("Reviewed French supplement entryCount mismatch")
    return entries, source


def export_french_pack(
    database: Path,
    chinese_glosses: Path | None = None,
    reviewed_supplements: Path | None = None,
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

        base_headwords = {entry["headword"].casefold() for entry in entries}
        supplement_entries, supplement_source = load_reviewed_supplements(reviewed_supplements)
        duplicate_headwords = sorted(
            entry["headword"]
            for entry in supplement_entries
            if entry["headword"].casefold() in base_headwords
        )
        if duplicate_headwords:
            raise ValueError(
                "Reviewed supplements duplicate base headwords: " + ", ".join(duplicate_headwords)
            )
        base_identifiers = {entry["id"] for entry in entries}
        if any(entry["id"] in base_identifiers for entry in supplement_entries):
            raise ValueError("Reviewed supplement identifier collides with the base dictionary")
        entries.extend(supplement_entries)
        entries.sort(
            key=lambda entry: (entry["headword"].casefold(), entry["headword"], entry["id"])
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
        if supplement_source is not None:
            result["supplementSources"] = [supplement_source]
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
    parser.add_argument(
        "--reviewed-supplements",
        type=Path,
        default=Path("tools/web_data/french_reviewed_supplements.json"),
    )
    parser.add_argument("--pretty", action="store_true")
    arguments = parser.parse_args()

    pack = export_french_pack(
        arguments.database,
        arguments.chinese_glosses,
        arguments.reviewed_supplements,
    )
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
