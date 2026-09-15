#!/usr/bin/env python3
"""Fast integrity and provenance checks for Pangmao's stroke-order asset."""

from __future__ import annotations

import sqlite3
import sys
from pathlib import Path

from build_stroke_database import decode_payload


def fail(message: str) -> None:
    raise AssertionError(message)


path = Path(sys.argv[1] if len(sys.argv) > 1 else "app/src/main/assets/databases/strokes.db")
if not path.exists():
    fail(f"Missing stroke-order asset: {path}")

connection = sqlite3.connect(f"file:{path}?mode=ro", uri=True)
if connection.execute("PRAGMA integrity_check").fetchone()[0] != "ok":
    fail("SQLite integrity_check failed")
if connection.execute("PRAGMA user_version").fetchone()[0] != 1:
    fail("Unexpected stroke-order schema")

metadata = dict(connection.execute("SELECT key, value FROM metadata"))
expected_revision = "68d10a4b21150cae5e1ebbd223eed289cf32d90c"
if metadata.get("source_revision") != expected_revision:
    fail("Unexpected Hanzi Writer Data revision")
if metadata.get("license") != "Arphic Public License":
    fail("Stroke-order licence metadata is missing")

count = connection.execute("SELECT count(*) FROM stroke_orders").fetchone()[0]
if count < 9_000 or count != int(metadata.get("character_count", "-1")):
    fail(f"Stroke-order coverage is unexpectedly small: {count}")

for character, expected_count in {"一": 1, "好": 6, "猫": 11, "學": 16}.items():
    row = connection.execute(
        "SELECT stroke_count, payload FROM stroke_orders WHERE character = ?",
        (character,),
    ).fetchone()
    if row is None or row[0] != expected_count:
        fail(f"Missing or invalid stroke-order witness: {character}")
    decoded = decode_payload(row[1])
    if len(decoded) != expected_count or any(len(points) < 2 for _, points in decoded):
        fail(f"Invalid stroke-order payload: {character}")

connection.close()
print(f"Verified {count} stroke-order records")
