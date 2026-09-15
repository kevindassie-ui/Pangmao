from __future__ import annotations

import json
import sqlite3
import sys
import tempfile
import unittest
from pathlib import Path


sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from build_stroke_database import (  # noqa: E402
    create_database,
    decode_payload,
    encode_payload,
    load_record,
)


class BuildStrokeDatabaseTest(unittest.TestCase):
    def setUp(self) -> None:
        self.temporary = tempfile.TemporaryDirectory()
        self.root = Path(self.temporary.name)
        self.data = self.root / "data"
        self.data.mkdir()

    def tearDown(self) -> None:
        self.temporary.cleanup()

    def write_character(self, character: str, stroke_count: int = 2) -> Path:
        path = self.data / f"{character}.json"
        path.write_text(
            json.dumps(
                {
                    "strokes": ["M 0 0 L 10 10 Z"] * stroke_count,
                    "medians": [[[0, 0], [10, 10]]] * stroke_count,
                },
                ensure_ascii=False,
            ),
            encoding="utf-8",
        )
        return path

    def test_payload_round_trip_preserves_order_paths_and_medians(self) -> None:
        record = load_record(self.write_character("猫", 3))

        decoded = decode_payload(encode_payload(record))

        self.assertEqual(3, len(decoded))
        self.assertEqual("M 0 0 L 10 10 Z", decoded[0][0])
        self.assertEqual(((0, 0), (10, 10)), decoded[2][1])

    def test_database_is_compact_indexed_and_attributed(self) -> None:
        self.write_character("一", 1)
        self.write_character("好", 6)
        output = self.root / "strokes.db"

        metrics = create_database(self.data, output, "fixture-revision")

        connection = sqlite3.connect(output)
        self.assertEqual(1, connection.execute("PRAGMA user_version").fetchone()[0])
        self.assertEqual(2, connection.execute("SELECT count(*) FROM stroke_orders").fetchone()[0])
        self.assertEqual(
            "fixture-revision",
            connection.execute(
                "SELECT value FROM metadata WHERE key = 'source_revision'"
            ).fetchone()[0],
        )
        payload = connection.execute(
            "SELECT payload FROM stroke_orders WHERE character = '好'"
        ).fetchone()[0]
        self.assertEqual(6, len(decode_payload(payload)))
        self.assertLess(metrics["compressed_bytes"], metrics["raw_bytes"])
        connection.close()

    def test_mismatched_data_is_rejected(self) -> None:
        path = self.write_character("坏")
        source = json.loads(path.read_text(encoding="utf-8"))
        source["medians"] = source["medians"][:1]
        path.write_text(json.dumps(source, ensure_ascii=False), encoding="utf-8")

        with self.assertRaisesRegex(ValueError, "Mismatched stroke data"):
            load_record(path)


if __name__ == "__main__":
    unittest.main()
