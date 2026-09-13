from __future__ import annotations

import sqlite3
import sys
import tempfile
import unittest
from pathlib import Path


sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from audit_dictionary_quality import audit_database, human_summary  # noqa: E402


class DictionaryQualityAuditTest(unittest.TestCase):
    def setUp(self) -> None:
        self.temporary_directory = tempfile.TemporaryDirectory()
        self.database = Path(self.temporary_directory.name) / "fixture.db"
        connection = sqlite3.connect(self.database)
        connection.executescript(
            """
            CREATE TABLE entries (
                id INTEGER PRIMARY KEY,
                simplified TEXT NOT NULL,
                pinyin TEXT NOT NULL,
                definitions_en TEXT NOT NULL,
                definitions_fr TEXT NOT NULL,
                sources TEXT NOT NULL,
                frequency INTEGER NOT NULL
            );
            CREATE TABLE examples (
                id INTEGER PRIMARY KEY,
                chinese TEXT NOT NULL,
                english TEXT NOT NULL,
                french TEXT NOT NULL,
                chinese_source TEXT NOT NULL,
                english_source TEXT NOT NULL,
                french_source TEXT NOT NULL
            );
            CREATE TABLE metadata (key TEXT PRIMARY KEY, value TEXT NOT NULL);
            """
        )
        connection.executemany(
            "INSERT INTO entries VALUES (?, ?, ?, ?, ?, ?, ?)",
            (
                (1, "茶", "cha2", "tea", "thé", "CC-CEDICT · CFDICT", 120),
                (2, "猫", "mao1", "cat", "", "CC-CEDICT", 25),
                (3, "走", "zou3", "", "used to do something", "CFDICT", 7),
                (4, "罕", "han3", "", "", "CC-CEDICT", 2),
                (5, "词", "ci2", "same\n Same ", "entrée [ci1]", "Test", 0),
            ),
        )
        connection.executemany(
            "INSERT INTO examples VALUES (?, ?, ?, ?, ?, ?, ?)",
            (
                (1, "你好", "Hello.", "Bonjour.", "Tatoeba", "Tatoeba", "Tatoeba"),
                (2, "我走。", "I am leaving.", "", "Tatoeba", "Tatoeba", ""),
                (3, " 你好 ", "", "Salut.", "Tatoeba", "", ""),
            ),
        )
        connection.execute("INSERT INTO metadata VALUES ('schema_version', 'fixture')")
        connection.commit()
        connection.close()

    def tearDown(self) -> None:
        self.temporary_directory.cleanup()

    def test_measures_coverage_by_language_and_frequency(self) -> None:
        report = audit_database(self.database)

        self.assertEqual(
            {
                "bilingual": 2,
                "english_only": 1,
                "french_only": 1,
                "none": 1,
            },
            report["entries"]["coverage"]["counts"],
        )
        self.assertEqual(40.0, report["entries"]["coverage"]["percentages"]["bilingual"])
        for bucket in ("100_plus", "20_99", "5_19", "1_4", "0"):
            self.assertEqual(1, report["entries"]["frequency_buckets"][bucket]["total"])
        self.assertEqual(
            {"bilingual": 1, "english_only": 1, "french_only": 1, "none": 0},
            report["examples"]["coverage"]["counts"],
        )

    def test_reports_review_candidates_without_modifying_data(self) -> None:
        report = audit_database(self.database, sample_limit=1)

        self.assertEqual(
            {
                "duplicate_definition": 1,
                "duplicate_example_chinese": 1,
                "entry_missing_all_definitions": 1,
                "example_missing_french_source": 1,
                "french_looks_english": 1,
            },
            report["anomalies"]["counts"],
        )
        self.assertEqual(
            "走",
            report["anomalies"]["samples"]["french_looks_english"][0]["headword"],
        )
        self.assertEqual(
            {"cedict_pinyin_cross_reference": 1},
            report["entries"]["formatting_counts"],
        )
        self.assertEqual(
            {"english": 4, "french": 3},
            report["entries"]["definition_counts"],
        )

    def test_output_is_deterministic_and_human_readable(self) -> None:
        first = audit_database(self.database)
        second = audit_database(self.database)

        self.assertEqual(first, second)
        self.assertNotIn("generated_at", first)
        self.assertEqual(
            "occurrences in the bundled Tatoeba-derived corpus",
            first["methodology"]["frequency_source"],
        )
        summary = human_summary(first)
        self.assertIn("Entries: 5 total", summary)
        self.assertIn("Examples: 3 total", summary)


if __name__ == "__main__":
    unittest.main()
