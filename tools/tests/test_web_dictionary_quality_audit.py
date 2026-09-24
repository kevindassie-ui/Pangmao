from __future__ import annotations

import json
import sqlite3
import tempfile
import unittest
from pathlib import Path

from tools.audit_web_dictionary_quality import audit_web_dictionary, human_summary


class WebDictionaryQualityAuditTest(unittest.TestCase):
    def setUp(self) -> None:
        self.temporary_directory = tempfile.TemporaryDirectory()
        root = Path(self.temporary_directory.name)
        self.pack = root / "pack.json"
        self.database = root / "dictionary.db"
        self.witnesses = root / "witnesses.json"
        self.pack.write_text(
            json.dumps(
                {
                    "schemaVersion": 2,
                    "releaseVersion": "test",
                    "entries": [
                        {
                            "id": "fr:test:affiche",
                            "headword": "affiche",
                            "forms": ["affiche", "affiches"],
                            "senses": [{"chinese": ["海报"], "definitions": []}],
                        },
                        {
                            "id": "fr:test:avocat",
                            "headword": "avocat",
                            "forms": ["avocat"],
                            "senses": [
                                {"chinese": ["律师"], "definitions": []},
                                {"chinese": ["牛油果"], "definitions": []},
                            ],
                        },
                    ],
                },
                ensure_ascii=False,
            ),
            encoding="utf-8",
        )
        self.witnesses.write_text(
            json.dumps(
                {
                    "schemaVersion": 1,
                    "entries": [
                        {
                            "headword": "affiche",
                            "requiredGroups": [["海报"]],
                            "forbiddenChinese": ["图钉"],
                        },
                        {
                            "headword": "avocat",
                            "requiredGroups": [["律师"], ["牛油果"]],
                            "distinctSenseGroups": [["律师"], ["牛油果"]],
                        },
                    ],
                },
                ensure_ascii=False,
            ),
            encoding="utf-8",
        )
        connection = sqlite3.connect(self.database)
        connection.executescript(
            """
            CREATE TABLE entries (
                id INTEGER PRIMARY KEY,
                simplified TEXT NOT NULL,
                traditional TEXT NOT NULL,
                definitions_fr TEXT NOT NULL,
                frequency INTEGER NOT NULL
            );
            INSERT INTO entries VALUES (1, '海报', '海報', 'affiche', 20);
            INSERT INTO entries VALUES (2, '律师', '律師', 'avocat (métier)', 10);
            INSERT INTO entries VALUES (3, '牛油果', '牛油果', 'avocat', 2);
            """
        )
        connection.commit()
        connection.close()

    def tearDown(self) -> None:
        self.temporary_directory.cleanup()

    def test_audits_all_pairs_and_corroborates_reverse_definitions(self) -> None:
        report = audit_web_dictionary(
            self.pack,
            self.database,
            self.witnesses,
            sample_limit=5,
        )

        self.assertEqual(3, report["corpus"]["pairs"])
        self.assertEqual(3, report["crossCheck"]["reverseEvidenceAvailable"])
        self.assertEqual(3, report["crossCheck"]["exactlyCorroborated"])
        self.assertEqual([], report["witnesses"]["failures"])
        self.assertIn("Corpus: 2 entries", human_summary(report))

    def test_reports_review_candidates_without_mutating_the_pack(self) -> None:
        connection = sqlite3.connect(self.database)
        connection.execute("UPDATE entries SET definitions_fr = 'punaise' WHERE simplified = '海报'")
        connection.commit()
        connection.close()

        before = self.pack.read_bytes()
        report = audit_web_dictionary(self.pack, self.database, self.witnesses)

        self.assertEqual(1, report["crossCheck"]["reviewCandidates"])
        self.assertEqual("affiche", report["crossCheck"]["candidateSamples"][0]["headword"])
        self.assertEqual(before, self.pack.read_bytes())

    def test_reviewed_witnesses_fail_when_meanings_are_merged(self) -> None:
        value = json.loads(self.pack.read_text(encoding="utf-8"))
        value["entries"][1]["senses"] = [
            {"chinese": ["律师", "牛油果"], "definitions": []}
        ]
        self.pack.write_text(json.dumps(value, ensure_ascii=False), encoding="utf-8")

        report = audit_web_dictionary(self.pack, self.database, self.witnesses)

        self.assertTrue(
            any("merged" in failure for failure in report["witnesses"]["failures"])
        )


if __name__ == "__main__":
    unittest.main()
