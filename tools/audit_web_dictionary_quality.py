#!/usr/bin/env python3
"""Audit every Pangmao Web French/Chinese pair and protect reviewed witnesses."""

from __future__ import annotations

import argparse
import json
import re
import sqlite3
import unicodedata
from collections import defaultdict
from pathlib import Path
from typing import Any


REPORT_SCHEMA_VERSION = 1
DEFAULT_PACK = Path("webApp/data/french-pack.json")
DEFAULT_DATABASE = Path("app/src/main/assets/databases/pangmao.db")
DEFAULT_WITNESSES = Path("tools/web_data/french_translation_witnesses.json")
PARENTHETICAL = re.compile(r"\([^)]*\)")
FRENCH_SEPARATOR = re.compile(r"\s*(?:[;,/]|\bou\b|\bet\b)\s*", re.IGNORECASE)


def read_object(path: Path) -> dict[str, Any]:
    value = json.loads(path.read_text(encoding="utf-8"))
    if not isinstance(value, dict):
        raise ValueError(f"Expected a JSON object: {path}")
    return value


def normalize_french(value: str) -> str:
    normalized = unicodedata.normalize("NFD", str(value).casefold().replace("’", "'"))
    normalized = "".join(
        character for character in normalized if unicodedata.category(character) != "Mn"
    )
    normalized = PARENTHETICAL.sub(" ", normalized)
    normalized = re.sub(r"[^a-z0-9'-]+", " ", normalized)
    return " ".join(normalized.split()).strip(" -'")


def reverse_definition_forms(value: str) -> set[str]:
    results: set[str] = set()
    for line in str(value).splitlines():
        if not line.strip():
            continue
        candidates = [line, *FRENCH_SEPARATOR.split(line)]
        results.update(
            normalized
            for candidate in candidates
            if (normalized := normalize_french(candidate))
        )
    return results


def reverse_index(connection: sqlite3.Connection) -> dict[str, list[dict[str, Any]]]:
    tables = {
        row[0]
        for row in connection.execute("SELECT name FROM sqlite_master WHERE type = 'table'")
    }
    if "entries" not in tables:
        raise ValueError("The dictionary database has no entries table")
    columns = {row[1] for row in connection.execute("PRAGMA table_info(entries)")}
    required = {"simplified", "traditional", "definitions_fr", "frequency"}
    if missing := required - columns:
        raise ValueError(f"The entries table is missing columns: {sorted(missing)}")

    values: dict[str, list[dict[str, Any]]] = defaultdict(list)
    query = """
        SELECT simplified, traditional, definitions_fr, frequency
        FROM entries
        ORDER BY id
    """
    for simplified, traditional, definitions_fr, frequency in connection.execute(query):
        record = {
            "forms": reverse_definition_forms(str(definitions_fr)),
            "definitions": [
                line.strip() for line in str(definitions_fr).splitlines() if line.strip()
            ],
            "frequency": int(frequency),
        }
        for chinese in {str(simplified), str(traditional)}:
            if chinese:
                values[chinese].append(record)
    return values


def audit_witnesses(
    entries: list[dict[str, Any]],
    witness_payload: dict[str, Any],
) -> dict[str, Any]:
    if witness_payload.get("schemaVersion") != 1:
        raise ValueError("Unsupported Web translation witness schema")
    witnesses = witness_payload.get("entries")
    if not isinstance(witnesses, list) or not witnesses:
        raise ValueError("The Web translation witness corpus is empty")

    by_headword: dict[str, list[dict[str, Any]]] = defaultdict(list)
    for entry in entries:
        by_headword[str(entry["headword"]).casefold()].append(entry)

    failures: list[str] = []
    for witness in witnesses:
        headword = str(witness.get("headword", ""))
        matches = by_headword.get(headword.casefold(), [])
        if not matches:
            failures.append(f"missing headword: {headword}")
            continue
        sense_sets = [
            set(map(str, sense.get("chinese", [])))
            for entry in matches
            for sense in entry.get("senses", [])
        ]
        all_chinese = set().union(*sense_sets) if sense_sets else set()
        for group in witness.get("requiredGroups", []):
            expected = set(map(str, group))
            if not all_chinese.intersection(expected):
                failures.append(
                    f"{headword}: none of the required equivalents are present: {sorted(expected)}"
                )
        forbidden = set(map(str, witness.get("forbiddenChinese", [])))
        if present := all_chinese.intersection(forbidden):
            failures.append(f"{headword}: forbidden equivalents are present: {sorted(present)}")

        distinct_groups = [
            set(map(str, group)) for group in witness.get("distinctSenseGroups", [])
        ]
        if distinct_groups:
            indexes = [
                {
                    index
                    for index, sense_values in enumerate(sense_sets)
                    if sense_values.intersection(group)
                }
                for group in distinct_groups
            ]
            if any(not candidates for candidates in indexes):
                failures.append(f"{headword}: a required distinct sense is missing")
            for left_index, left in enumerate(indexes):
                for right in indexes[left_index + 1 :]:
                    if left.intersection(right):
                        failures.append(f"{headword}: reviewed meanings were merged into one sense")

    return {
        "reviewedHeadwords": len(witnesses),
        "failures": failures,
    }


def audit_web_dictionary(
    pack_path: Path,
    database_path: Path,
    witnesses_path: Path,
    sample_limit: int = 20,
) -> dict[str, Any]:
    if sample_limit < 0:
        raise ValueError("sample_limit must be non-negative")
    pack = read_object(pack_path)
    entries = pack.get("entries")
    if pack.get("schemaVersion") != 2 or not isinstance(entries, list):
        raise ValueError("Unsupported Pangmao Web dictionary pack")
    witnesses = read_object(witnesses_path)

    connection = sqlite3.connect(f"file:{database_path}?mode=ro", uri=True)
    try:
        reverse = reverse_index(connection)
    finally:
        connection.close()

    pair_count = 0
    reverse_available = 0
    corroborated = 0
    without_reverse = 0
    review_candidates: list[dict[str, Any]] = []
    corroborated_entries: set[str] = set()
    for entry in entries:
        entry_forms = {
            normalized
            for form in [entry.get("headword", ""), *entry.get("forms", [])]
            if (normalized := normalize_french(str(form)))
        }
        for sense_index, sense in enumerate(entry.get("senses", [])):
            for chinese in sense.get("chinese", []):
                pair_count += 1
                reverse_records = [
                    record for record in reverse.get(str(chinese), []) if record["definitions"]
                ]
                if not reverse_records:
                    without_reverse += 1
                    continue
                reverse_available += 1
                reverse_forms = set().union(
                    *(record["forms"] for record in reverse_records)
                )
                if entry_forms.intersection(reverse_forms):
                    corroborated += 1
                    corroborated_entries.add(str(entry.get("id", "")))
                    continue
                review_candidates.append(
                    {
                        "headword": entry.get("headword"),
                        "chinese": chinese,
                        "senseIndex": sense_index,
                        "reverseDefinitions": sorted(
                            {
                                definition
                                for record in reverse_records
                                for definition in record["definitions"]
                            }
                        )[:8],
                        "frequency": max(record["frequency"] for record in reverse_records),
                    }
                )

    review_candidates.sort(
        key=lambda item: (
            -item["frequency"],
            str(item["headword"]).casefold(),
            str(item["chinese"]),
            item["senseIndex"],
        )
    )
    witness_report = audit_witnesses(entries, witnesses)
    return {
        "reportSchemaVersion": REPORT_SCHEMA_VERSION,
        "releaseVersion": pack.get("releaseVersion", ""),
        "methodology": {
            "scope": "every shipped French sense and Chinese equivalent",
            "corroboration": (
                "exact normalized French lexical agreement with an independently indexed "
                "Chinese-to-French definition already bundled in Pangmao"
            ),
            "limitation": (
                "absence of exact agreement is a review candidate, not proof of an error; "
                "polysemy, paraphrases and grammatical differences are expected"
            ),
            "mutationPolicy": "the audit never edits or removes dictionary data",
        },
        "corpus": {
            "entries": len(entries),
            "senses": sum(len(entry.get("senses", [])) for entry in entries),
            "pairs": pair_count,
        },
        "crossCheck": {
            "reverseEvidenceAvailable": reverse_available,
            "exactlyCorroborated": corroborated,
            "reviewCandidates": len(review_candidates),
            "withoutReverseEvidence": without_reverse,
            "entriesWithExactCorroboration": len(corroborated_entries),
            "candidateSamples": review_candidates[:sample_limit],
        },
        "witnesses": witness_report,
    }


def human_summary(report: dict[str, Any]) -> str:
    corpus = report["corpus"]
    cross_check = report["crossCheck"]
    witnesses = report["witnesses"]
    return "\n".join(
        (
            "Pangmao Web translation quality audit",
            (
                f"Corpus: {corpus['entries']} entries; {corpus['senses']} senses; "
                f"{corpus['pairs']} French/Chinese pairs"
            ),
            (
                f"Reverse evidence: {cross_check['reverseEvidenceAvailable']} pairs; "
                f"exactly corroborated={cross_check['exactlyCorroborated']}; "
                f"review candidates={cross_check['reviewCandidates']}; "
                f"no reverse evidence={cross_check['withoutReverseEvidence']}"
            ),
            (
                f"Reviewed witnesses: {witnesses['reviewedHeadwords']} headwords; "
                f"failures={len(witnesses['failures'])}"
            ),
        )
    )


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--pack", type=Path, default=DEFAULT_PACK)
    parser.add_argument("--database", type=Path, default=DEFAULT_DATABASE)
    parser.add_argument("--witnesses", type=Path, default=DEFAULT_WITNESSES)
    parser.add_argument("--json-out", type=Path)
    parser.add_argument("--sample-limit", type=int, default=20)
    parser.add_argument("--strict", action="store_true")
    arguments = parser.parse_args()

    report = audit_web_dictionary(
        arguments.pack,
        arguments.database,
        arguments.witnesses,
        arguments.sample_limit,
    )
    if arguments.json_out:
        arguments.json_out.parent.mkdir(parents=True, exist_ok=True)
        arguments.json_out.write_text(
            json.dumps(report, ensure_ascii=False, indent=2, sort_keys=True) + "\n",
            encoding="utf-8",
        )
    print(human_summary(report))
    if report["witnesses"]["failures"]:
        for failure in report["witnesses"]["failures"]:
            print(f"- {failure}")
        return 1 if arguments.strict else 0
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
