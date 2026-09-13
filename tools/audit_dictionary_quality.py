#!/usr/bin/env python3
"""Measure Pangmao's bilingual dictionary quality without mutating the database."""

from __future__ import annotations

import argparse
import json
import re
import sqlite3
import unicodedata
from collections import Counter
from pathlib import Path
from typing import Any


REPORT_SCHEMA_VERSION = 1
DEFAULT_DATABASE = Path("app/src/main/assets/databases/pangmao.db")
FREQUENCY_BUCKETS = (
    ("100_plus", 100, None),
    ("20_99", 20, 99),
    ("5_19", 5, 19),
    ("1_4", 1, 4),
    ("0", 0, 0),
)
ENGLISH_IN_FRENCH = re.compile(
    r"\b(?:used to|variant of|classifier for|abbr\.? for|see also|someone who|"
    r"something that|to be|to have|English slang|Chinese surname)\b",
    re.IGNORECASE,
)
RAW_PINYIN_REFERENCE = re.compile(
    r"\[(?:[A-Za-züÜvV]+[1-5])(?:\s+[A-Za-züÜvV]+[1-5])*\]"
)
HTML_RESIDUE = re.compile(
    r"</?[A-Za-z][A-Za-z0-9]*(?:\s+[^<>]*)?>|&(?:amp|lt|gt|quot|apos);",
    re.IGNORECASE,
)


def normalized_text(value: str) -> str:
    return re.sub(r"\s+", " ", unicodedata.normalize("NFC", value).strip())


def normalized_definition(value: str) -> str:
    return normalized_text(value).casefold().strip(" .;,:，。；：")


def split_definitions(value: str) -> list[str]:
    return [line.strip() for line in value.splitlines() if line.strip()]


def coverage_name(english: str, french: str) -> str:
    has_english = bool(english.strip())
    has_french = bool(french.strip())
    if has_english and has_french:
        return "bilingual"
    if has_english:
        return "english_only"
    if has_french:
        return "french_only"
    return "none"


def frequency_bucket(frequency: int) -> str:
    for name, minimum, maximum in FREQUENCY_BUCKETS:
        if frequency >= minimum and (maximum is None or frequency <= maximum):
            return name
    raise ValueError(f"Negative entry frequency: {frequency}")


def percentage(part: int, total: int) -> float:
    return round(part * 100 / total, 2) if total else 0.0


def coverage_payload(counts: Counter[str]) -> dict[str, Any]:
    total = sum(counts.values())
    keys = ("bilingual", "english_only", "french_only", "none")
    return {
        "total": total,
        "counts": {key: counts[key] for key in keys},
        "percentages": {key: percentage(counts[key], total) for key in keys},
    }


class AnomalyCollector:
    def __init__(self, sample_limit: int) -> None:
        self.sample_limit = sample_limit
        self.counts: Counter[str] = Counter()
        self.samples: dict[str, list[dict[str, Any]]] = {}

    def add(self, kind: str, sample: dict[str, Any]) -> None:
        self.counts[kind] += 1
        values = self.samples.setdefault(kind, [])
        if len(values) < self.sample_limit:
            values.append(sample)

    def payload(self) -> dict[str, Any]:
        return {
            "total_candidates": sum(self.counts.values()),
            "counts": dict(sorted(self.counts.items())),
            "samples": {key: self.samples[key] for key in sorted(self.samples)},
        }


def ensure_schema(connection: sqlite3.Connection) -> None:
    tables = {
        row[0]
        for row in connection.execute(
            "SELECT name FROM sqlite_master WHERE type = 'table'"
        )
    }
    required_tables = {"entries", "examples", "metadata"}
    missing_tables = required_tables - tables
    if missing_tables:
        raise ValueError(f"Missing audit tables: {sorted(missing_tables)}")

    required_columns = {
        "entries": {
            "id",
            "simplified",
            "pinyin",
            "definitions_en",
            "definitions_fr",
            "sources",
            "frequency",
        },
        "examples": {
            "id",
            "chinese",
            "english",
            "french",
            "chinese_source",
            "english_source",
            "french_source",
        },
    }
    for table, expected in required_columns.items():
        actual = {row[1] for row in connection.execute(f"PRAGMA table_info({table})")}
        missing = expected - actual
        if missing:
            raise ValueError(f"Missing {table} columns: {sorted(missing)}")


def entry_sample(row: sqlite3.Row, language: str, value: str) -> dict[str, Any]:
    return {
        "entry_id": row["id"],
        "headword": row["simplified"],
        "language": language,
        "pinyin": row["pinyin"],
        "sources": row["sources"],
        "value": value,
    }


def audit_entries(
    connection: sqlite3.Connection,
    anomalies: AnomalyCollector,
) -> dict[str, Any]:
    coverage: Counter[str] = Counter()
    buckets: dict[str, Counter[str]] = {
        name: Counter() for name, _, _ in FREQUENCY_BUCKETS
    }
    sources: Counter[str] = Counter()
    definition_counts: Counter[str] = Counter()
    formatting_counts: Counter[str] = Counter()
    query = """
        SELECT id, simplified, pinyin, definitions_en, definitions_fr, sources, frequency
        FROM entries
        ORDER BY id
    """
    for row in connection.execute(query):
        english = row["definitions_en"]
        french = row["definitions_fr"]
        coverage_key = coverage_name(english, french)
        coverage[coverage_key] += 1
        buckets[frequency_bucket(row["frequency"])][coverage_key] += 1
        if coverage_key == "none":
            anomalies.add(
                "entry_missing_all_definitions",
                entry_sample(row, "none", ""),
            )
        for source in row["sources"].split(" · "):
            if source.strip():
                sources[source.strip()] += 1

        for language, raw_value in (("english", english), ("french", french)):
            if raw_value and any(not line.strip() for line in raw_value.splitlines()):
                anomalies.add(
                    "empty_definition_line",
                    entry_sample(row, language, raw_value),
                )
            definitions = split_definitions(raw_value)
            definition_counts[language] += len(definitions)
            seen: dict[str, str] = {}
            for definition in definitions:
                normalized = normalized_definition(definition)
                if normalized in seen:
                    anomalies.add(
                        "duplicate_definition",
                        entry_sample(row, language, definition),
                    )
                else:
                    seen[normalized] = definition
                if language == "french" and ENGLISH_IN_FRENCH.search(definition):
                    anomalies.add(
                        "french_looks_english",
                        entry_sample(row, language, definition),
                    )
                if RAW_PINYIN_REFERENCE.search(definition):
                    formatting_counts["cedict_pinyin_cross_reference"] += 1
                if "�" in definition:
                    anomalies.add(
                        "replacement_character",
                        entry_sample(row, language, definition),
                    )
                if HTML_RESIDUE.search(definition):
                    anomalies.add(
                        "html_residue",
                        entry_sample(row, language, definition),
                    )

    return {
        "coverage": coverage_payload(coverage),
        "frequency_buckets": {
            name: coverage_payload(buckets[name]) for name, _, _ in FREQUENCY_BUCKETS
        },
        "definition_counts": {
            language: definition_counts[language] for language in ("english", "french")
        },
        "formatting_counts": dict(sorted(formatting_counts.items())),
        "source_entry_counts": dict(sorted(sources.items())),
    }


def audit_examples(
    connection: sqlite3.Connection,
    anomalies: AnomalyCollector,
) -> dict[str, Any]:
    coverage: Counter[str] = Counter()
    sources: dict[str, Counter[str]] = {
        "chinese": Counter(),
        "english": Counter(),
        "french": Counter(),
    }
    seen_chinese: dict[str, int] = {}
    query = """
        SELECT id, chinese, english, french, chinese_source, english_source, french_source
        FROM examples
        ORDER BY id
    """
    for row in connection.execute(query):
        coverage[coverage_name(row["english"], row["french"])] += 1
        normalized_chinese = normalized_text(row["chinese"])
        if normalized_chinese in seen_chinese:
            anomalies.add(
                "duplicate_example_chinese",
                {
                    "example_id": row["id"],
                    "first_example_id": seen_chinese[normalized_chinese],
                    "chinese": row["chinese"],
                },
            )
        else:
            seen_chinese[normalized_chinese] = row["id"]

        for language in ("chinese", "english", "french"):
            value = row[language]
            source = row[f"{language}_source"]
            if value.strip() and not source.strip():
                anomalies.add(
                    f"example_missing_{language}_source",
                    {
                        "example_id": row["id"],
                        "chinese": row["chinese"],
                        "value": value,
                    },
                )
            if source.strip():
                sources[language][source.strip()] += 1

    return {
        "coverage": coverage_payload(coverage),
        "source_counts": {
            language: dict(sorted(counts.items()))
            for language, counts in sources.items()
        },
    }


def audit_connection(
    connection: sqlite3.Connection,
    sample_limit: int = 20,
) -> dict[str, Any]:
    if sample_limit < 0:
        raise ValueError("sample_limit must be non-negative")
    connection.row_factory = sqlite3.Row
    ensure_schema(connection)
    anomalies = AnomalyCollector(sample_limit)
    metadata = dict(
        connection.execute("SELECT key, value FROM metadata ORDER BY key")
    )
    entries = audit_entries(connection, anomalies)
    examples = audit_examples(connection, anomalies)
    return {
        "report_schema_version": REPORT_SCHEMA_VERSION,
        "methodology": {
            "entry_unit": "one lexical row: traditional, simplified and pronunciation",
            "frequency_source": "occurrences in the bundled Tatoeba-derived corpus",
            "review_policy": "anomaly candidates are informational and never mutate data",
        },
        "dictionary_metadata": metadata,
        "entries": entries,
        "examples": examples,
        "anomalies": anomalies.payload(),
    }


def audit_database(path: Path, sample_limit: int = 20) -> dict[str, Any]:
    if not path.exists():
        raise FileNotFoundError(f"Missing dictionary database: {path}")
    connection = sqlite3.connect(f"file:{path}?mode=ro", uri=True)
    try:
        return audit_connection(connection, sample_limit)
    finally:
        connection.close()


def human_summary(report: dict[str, Any]) -> str:
    entry_coverage = report["entries"]["coverage"]
    example_coverage = report["examples"]["coverage"]
    entry_counts = entry_coverage["counts"]
    example_counts = example_coverage["counts"]
    anomaly_counts = report["anomalies"]["counts"]
    anomaly_line = ", ".join(
        f"{name}={count}" for name, count in anomaly_counts.items()
    ) or "none"
    return "\n".join(
        (
            "Pangmao dictionary quality audit",
            (
                f"Entries: {entry_coverage['total']} total; "
                f"bilingual={entry_counts['bilingual']} "
                f"({entry_coverage['percentages']['bilingual']}%); "
                f"English-only={entry_counts['english_only']}; "
                f"French-only={entry_counts['french_only']}; "
                f"none={entry_counts['none']}"
            ),
            (
                f"Examples: {example_coverage['total']} total; "
                f"bilingual={example_counts['bilingual']} "
                f"({example_coverage['percentages']['bilingual']}%); "
                f"English-only={example_counts['english_only']}; "
                f"French-only={example_counts['french_only']}; "
                f"none={example_counts['none']}"
            ),
            f"Anomaly candidates (informational): {anomaly_line}",
        )
    )


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("database", nargs="?", type=Path, default=DEFAULT_DATABASE)
    parser.add_argument("--json-out", type=Path)
    parser.add_argument("--sample-limit", type=int, default=20)
    args = parser.parse_args()

    report = audit_database(args.database, args.sample_limit)
    if args.json_out:
        args.json_out.parent.mkdir(parents=True, exist_ok=True)
        args.json_out.write_text(
            json.dumps(report, ensure_ascii=False, indent=2, sort_keys=True) + "\n",
            encoding="utf-8",
        )
    print(human_summary(report))


if __name__ == "__main__":
    main()
