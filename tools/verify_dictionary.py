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
if example_count < 60_000:
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

if metadata["schema_version"] != "2":
    fail(f"Unexpected dictionary schema: {metadata['schema_version']}")

duplicate_examples = connection.execute(
    "SELECT count(*) FROM (SELECT chinese FROM examples GROUP BY chinese HAVING count(*) > 1)"
).fetchone()[0]
if duplicate_examples:
    fail(f"Duplicate Chinese examples remain: {duplicate_examples}")

example_columns = {row[1] for row in connection.execute("PRAGMA table_info(examples)")}
if not {"english", "french", "english_source", "french_source"}.issubset(example_columns):
    fail("Bilingual example columns are missing")

for headword in ("奶茶婊", "肉夹馍", "爸比"):
    bilingual = connection.execute(
        "SELECT definitions_en, definitions_fr FROM entries WHERE simplified = ? LIMIT 1",
        (headword,),
    ).fetchone()
    if not bilingual or not bilingual[0] or not bilingual[1]:
        fail(f"Expected reviewed bilingual entry not found: {headword}")

reviewed_example = connection.execute(
    "SELECT english, french FROM examples WHERE chinese = '你很像你哥哥。' LIMIT 1"
).fetchone()
if not reviewed_example or "look" not in reviewed_example[0] or "ressembles" not in reviewed_example[1]:
    fail("Expected reviewed bilingual example not found")

connection.close()
print(
    f"Dictionary verified: {entry_count} entries, "
    f"{example_count} examples, {character_count} characters"
)
