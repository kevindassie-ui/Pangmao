import json
import sqlite3
import sys
import tempfile
import unittest
from pathlib import Path


sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from evaluate_french_candidate import evaluate, pinyin_plain


class EvaluateFrenchCandidateTest(unittest.TestCase):
    def setUp(self) -> None:
        self.temporary = tempfile.TemporaryDirectory()
        root = Path(self.temporary.name)
        self.database = root / "pangmao.db"
        self.candidate = root / "candidate.jsonl"
        connection = sqlite3.connect(self.database)
        connection.executescript(
            """
            CREATE TABLE entries (
                id INTEGER PRIMARY KEY,
                traditional TEXT NOT NULL,
                simplified TEXT NOT NULL,
                pinyin TEXT NOT NULL,
                pinyin_plain TEXT NOT NULL,
                definitions_en TEXT NOT NULL,
                definitions_fr TEXT NOT NULL,
                sources TEXT NOT NULL,
                frequency INTEGER NOT NULL
            );
            CREATE TABLE examples (
                chinese TEXT NOT NULL,
                english TEXT NOT NULL,
                french TEXT NOT NULL
            );
            CREATE TABLE headwords (
                word TEXT PRIMARY KEY,
                preferred_entry_id INTEGER NOT NULL,
                length INTEGER NOT NULL
            );
            """
        )
        connection.executemany(
            "INSERT INTO entries VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
            [
                (1, "肉夾饃", "肉夹馍", "rou4 jia1 mo2", "roujiamo", "roujiamo", "", "CC-CEDICT", 9),
                (2, "行", "行", "xing2", "xing", "to walk", "marcher", "CC-CEDICT · CFDICT", 4),
                (3, "行", "行", "hang2", "hang", "row", "rangée", "CC-CEDICT · CFDICT", 4),
            ],
        )
        connection.execute(
            "INSERT INTO examples VALUES (?, ?, ?)",
            ("我吃肉夹馍。", "I eat roujiamo.", ""),
        )
        connection.executemany(
            "INSERT INTO headwords VALUES (?, ?, ?)",
            [("肉夹馍", 1, 3), ("肉夾饃", 1, 3), ("行", 2, 1)],
        )
        connection.commit()
        connection.close()
        rows = [
            {
                "word": "肉夹馍",
                "lang_code": "zh",
                "pos": "noun",
                "sounds": [{"zh_pron": "ròujiāmó", "tags": ["Mandarin", "Pinyin"]}],
                "senses": [
                    {
                        "glosses": ["Sorte de petit sandwich de Xi'an."],
                        "examples": [
                            {"text": "我吃肉夹馍。", "translation": "Je mange un roujiamo."}
                        ],
                    }
                ],
            },
            {
                "word": "行",
                "lang_code": "zh",
                "pos": "verb",
                "sounds": [{"zh_pron": "xíng", "tags": ["Pinyin"]}],
                "senses": [{"glosses": ["Marcher."]}],
            },
            {
                "word": "新词",
                "lang_code": "zh",
                "pos": "noun",
                "senses": [{"glosses": ["Néologisme."]}],
            },
        ]
        self.candidate.write_text(
            "".join(f"{json.dumps(row, ensure_ascii=False)}\n" for row in rows),
            encoding="utf-8",
        )

    def tearDown(self) -> None:
        self.temporary.cleanup()

    def test_pinyin_normalization(self) -> None:
        self.assertEqual("roujiamo", pinyin_plain("ròu jiā mó"))
        self.assertEqual("lv", pinyin_plain("lǜ"))

    def test_evaluation_separates_safe_and_new_matches(self) -> None:
        report = evaluate(self.database, self.candidate, sample_size=5)
        self.assertEqual(3, report["candidate"]["distinct_words"])
        self.assertEqual(2, report["matching"]["pinyin"])
        self.assertEqual(1, report["matching"]["new_headword"])
        self.assertEqual(1, report["potential_definition_gain"]["entries_missing_french"])
        self.assertEqual(1, report["potential_definition_gain"]["entries_missing_french_frequency_5_plus"])
        self.assertEqual(1, report["potential_definition_gain"]["preferred_entries_missing_french"])
        self.assertEqual(1, report["potential_definition_gain"]["primary_entries_missing_french"])

    def test_evaluation_finds_fillable_example(self) -> None:
        report = evaluate(self.database, self.candidate, sample_size=5)
        self.assertEqual(1, report["examples"]["candidate_explicit_translated_pairs"])
        self.assertEqual(1, report["examples"]["exact_chinese_overlap_with_pangmao"])
        self.assertEqual(1, report["examples"]["pangmao_examples_fillable_in_french"])


if __name__ == "__main__":
    unittest.main()
