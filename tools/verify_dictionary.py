#!/usr/bin/env python3
"""Fast CI checks for the shipped immutable dictionary asset."""

from __future__ import annotations

import sqlite3
import sys
from pathlib import Path


def fail(message: str) -> None:
    raise AssertionError(message)


path = Path(sys.argv[1] if len(sys.argv) > 1 else "app/src/main/assets/databases/pangmao.db")
if not path.exists():
    fail(f"Missing dictionary asset: {path}")

connection = sqlite3.connect(f"file:{path}?mode=ro", uri=True)
if connection.execute("PRAGMA integrity_check").fetchone()[0] != "ok":
    fail("SQLite integrity_check failed")

entry_count = connection.execute("SELECT count(*) FROM entries").fetchone()[0]
example_count = connection.execute("SELECT count(*) FROM examples").fetchone()[0]
character_count = connection.execute("SELECT count(*) FROM characters").fetchone()[0]
if entry_count < 120_000:
    fail(f"Dictionary unexpectedly small: {entry_count}")
if example_count < 70_000:
    fail(f"Example corpus unexpectedly small: {example_count}")
if character_count < 10_000:
    fail(f"Character database unexpectedly small: {character_count}")

china = connection.execute(
    "SELECT definitions_en, definitions_fr FROM entries WHERE simplified = '中国' LIMIT 1"
).fetchone()
if not china or "China" not in china[0] or "Chine" not in china[1]:
    fail("Expected bilingual 中国 entry not found")

metadata = dict(connection.execute("SELECT key, value FROM metadata"))
required = {"schema_version", "cc_cedict_revision", "tatoeba_release", "unihan_version"}
if not required.issubset(metadata):
    fail(f"Missing metadata: {required - metadata.keys()}")

connection.close()
print(
    f"Dictionary verified: {entry_count} entries, "
    f"{example_count} examples, {character_count} characters"
)

