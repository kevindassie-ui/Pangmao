#!/usr/bin/env python3
"""Build Pangmao's immutable, compressed stroke-order database."""

from __future__ import annotations

import argparse
import json
import sqlite3
import zlib
from dataclasses import dataclass
from pathlib import Path


@dataclass(frozen=True)
class StrokeRecord:
    character: str
    strokes: tuple[str, ...]
    medians: tuple[tuple[tuple[float, float], ...], ...]


def encode_payload(record: StrokeRecord) -> bytes:
    """Encode one character without requiring a JSON parser in the Android app."""
    lines: list[str] = []
    for path, median in zip(record.strokes, record.medians, strict=True):
        if "\t" in path or "\n" in path:
            raise ValueError(f"Unsupported whitespace in SVG path for {record.character}")
        points = ";".join(f"{x:.6g},{y:.6g}" for x, y in median)
        lines.append(f"{path}\t{points}")
    return zlib.compress("\n".join(lines).encode("utf-8"), level=9)


def decode_payload(payload: bytes) -> tuple[tuple[str, tuple[tuple[float, float], ...]], ...]:
    """Reference decoder used by the builder tests and database validation."""
    decoded = zlib.decompress(payload).decode("utf-8")
    rows = []
    for line in decoded.splitlines():
        path, separator, raw_points = line.partition("\t")
        if not separator:
            raise ValueError("Malformed stroke payload")
        points = tuple(
            tuple(map(float, pair.split(",", maxsplit=1)))
            for pair in raw_points.split(";")
        )
        rows.append((path, points))
    return tuple(rows)


def load_record(path: Path) -> StrokeRecord:
    source = json.loads(path.read_text(encoding="utf-8"))
    character = path.stem
    if len(character) != 1:
        raise ValueError(f"Expected one Unicode character in filename: {path.name}")
    strokes = source.get("strokes")
    medians = source.get("medians")
    if not isinstance(strokes, list) or not isinstance(medians, list):
        raise ValueError(f"Missing strokes or medians for {character}")
    if not strokes or len(strokes) != len(medians):
        raise ValueError(f"Mismatched stroke data for {character}")

    parsed_medians: list[tuple[tuple[float, float], ...]] = []
    for index, median in enumerate(medians):
        if not isinstance(median, list) or len(median) < 2:
            raise ValueError(f"Stroke {index + 1} has no usable median for {character}")
        points: list[tuple[float, float]] = []
        for point in median:
            if (
                not isinstance(point, list)
                or len(point) != 2
                or not all(
                    isinstance(coordinate, (int, float)) and not isinstance(coordinate, bool)
                    for coordinate in point
                )
            ):
                raise ValueError(f"Invalid median point for {character}")
            points.append((float(point[0]), float(point[1])))
        parsed_medians.append(tuple(points))

    if not all(isinstance(stroke, str) and stroke.strip() for stroke in strokes):
        raise ValueError(f"Invalid SVG path for {character}")
    return StrokeRecord(character, tuple(strokes), tuple(parsed_medians))


def create_database(data_dir: Path, output: Path, revision: str) -> dict[str, int]:
    paths = sorted(
        (item for item in data_dir.glob("*.json") if len(item.stem) == 1),
        key=lambda item: ord(item.stem),
    )
    if not paths:
        raise ValueError(f"No Hanzi Writer JSON files found in {data_dir}")

    rows: list[tuple[str, int, bytes]] = []
    raw_bytes = 0
    compressed_bytes = 0
    for path in paths:
        record = load_record(path)
        payload = encode_payload(record)
        decoded = decode_payload(payload)
        if len(decoded) != len(record.strokes):
            raise ValueError(f"Stroke payload round-trip failed for {record.character}")
        rows.append((record.character, len(record.strokes), payload))
        raw_bytes += path.stat().st_size
        compressed_bytes += len(payload)

    output.parent.mkdir(parents=True, exist_ok=True)
    output.unlink(missing_ok=True)
    connection = sqlite3.connect(output)
    connection.executescript(
        """
        PRAGMA page_size = 16384;
        PRAGMA journal_mode = OFF;
        PRAGMA synchronous = OFF;
        PRAGMA temp_store = MEMORY;
        CREATE TABLE stroke_orders (
            character TEXT PRIMARY KEY,
            stroke_count INTEGER NOT NULL CHECK(stroke_count > 0),
            payload BLOB NOT NULL
        ) WITHOUT ROWID;
        CREATE TABLE metadata (
            key TEXT PRIMARY KEY,
            value TEXT NOT NULL
        ) WITHOUT ROWID;
        """
    )
    connection.executemany("INSERT INTO stroke_orders VALUES (?, ?, ?)", rows)
    metadata = {
        "schema_version": "1",
        "source": "Hanzi Writer Data / Make Me a Hanzi",
        "source_revision": revision,
        "source_url": "https://github.com/chanind/hanzi-writer-data",
        "license": "Arphic Public License",
        "character_count": str(len(rows)),
        "raw_source_bytes": str(raw_bytes),
        "compressed_payload_bytes": str(compressed_bytes),
        "transformation_notice": (
            "Pangmao converted the source JSON files into per-character "
            "zlib-compressed SQLite records in September 2026."
        ),
    }
    connection.executemany("INSERT INTO metadata VALUES (?, ?)", sorted(metadata.items()))
    connection.execute("PRAGMA user_version = 1")
    connection.commit()
    connection.execute("VACUUM")
    if connection.execute("PRAGMA integrity_check").fetchone()[0] != "ok":
        raise RuntimeError("Stroke database integrity check failed")
    connection.close()
    return {
        "characters": len(rows),
        "raw_bytes": raw_bytes,
        "compressed_bytes": compressed_bytes,
        "database_bytes": output.stat().st_size,
    }


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--data-dir", required=True, type=Path)
    parser.add_argument("--output", required=True, type=Path)
    parser.add_argument("--revision", required=True)
    args = parser.parse_args()
    metrics = create_database(args.data_dir, args.output, args.revision)
    print(json.dumps(metrics, indent=2, sort_keys=True))


if __name__ == "__main__":
    main()
