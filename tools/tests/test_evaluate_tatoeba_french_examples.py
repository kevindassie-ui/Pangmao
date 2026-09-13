import bz2
import csv
import sqlite3
import sys
import tarfile
import tempfile
import unittest
from pathlib import Path


sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from evaluate_tatoeba_french_examples import evaluate, normalized_text


class EvaluateTatoebaFrenchExamplesTest(unittest.TestCase):
    def setUp(self) -> None:
        self.temporary = tempfile.TemporaryDirectory()
        root = Path(self.temporary.name)
        self.english = root / "english.tsv"
        self.french = root / "fra.tsv.bz2"
        self.mandarin = root / "cmn.tsv.bz2"
        self.links = root / "links.tar.bz2"
        self.reviews = root / "reviews.tsv"
        self.database = root / "pangmao.db"

        self.english.write_text(
            "1\t你好。\t101\tHello.\n"
            "1\t你好。\t102\tHi.\n"
            "2\t再见。\t103\tGoodbye.\n"
            "3\t谢谢。\t104\tThank you.\n",
            encoding="utf-8",
        )
        with bz2.open(self.french, "wt", encoding="utf-8", newline="") as handle:
            writer = csv.writer(handle, delimiter="\t", lineterminator="\n")
            writer.writerows(
                [
                    (201, "fra", "Bonjour."),
                    (202, "fra", "Salut."),
                    (203, "fra", "Au revoir."),
                    (204, "fra", "Merci."),
                ]
            )
        with bz2.open(self.mandarin, "wt", encoding="utf-8", newline="") as handle:
            writer = csv.writer(handle, delimiter="\t", lineterminator="\n")
            writer.writerows(
                [
                    (1, "cmn", "你好。"),
                    (2, "cmn", "再见了。"),
                ]
            )
        link_text = "1\t201\n1\t202\n2\t203\n3\t204\n201\t1\n"
        link_file = root / "links.csv"
        link_file.write_text(link_text, encoding="utf-8")
        with tarfile.open(self.links, "w:bz2") as archive:
            archive.add(link_file, arcname="links.csv")
        self.reviews.write_text(
            "alice\t201\t1\t2020-01-01\t2020-01-01\n"
            "bob\t201\t1\t2020-01-01\t2020-01-01\n"
            "alice\t202\t1\t2020-01-01\t2020-01-01\n"
            "bob\t203\t1\t2020-01-01\t2020-01-01\n"
            "eve\t203\t-1\t2020-01-01\t2020-01-01\n"
            "alice\t204\t1\t2020-01-01\t2020-01-01\n"
            "alice\t204\t-1\t2020-01-01\t2021-01-01\n",
            encoding="utf-8",
        )
        connection = sqlite3.connect(self.database)
        connection.execute(
            "CREATE TABLE examples (tatoeba_chinese_id INTEGER NOT NULL, french TEXT NOT NULL)"
        )
        connection.executemany(
            "INSERT INTO examples VALUES (?, ?)",
            [(1, ""), (2, "Déjà traduit."), (3, "")],
        )
        connection.commit()
        connection.close()

    def tearDown(self) -> None:
        self.temporary.cleanup()

    def test_normalizes_unicode_and_whitespace(self) -> None:
        self.assertEqual("Été chinois", normalized_text("  E\u0301te\u0301\n chinois "))

    def test_selects_highest_reviewed_direct_translation(self) -> None:
        report, selected = evaluate(
            self.english,
            self.french,
            self.links,
            self.reviews,
            dictionary=self.database,
        )
        self.assertEqual(4, report["direct_alignment"]["direct_pairs"])
        self.assertEqual(1, report["selection"]["selected_translations"])
        self.assertEqual("Bonjour.", selected[0].french)
        self.assertEqual(2, selected[0].positive_reviews)
        self.assertEqual(1, report["projection"]["fillable_in_french"])

    def test_negative_review_excludes_a_sentence(self) -> None:
        report, selected = evaluate(
            self.english,
            self.french,
            self.links,
            self.reviews,
        )
        self.assertEqual([1], [item.chinese_id for item in selected])
        self.assertEqual(2, report["selection"]["pairs_rejected_negative_review"])

    def test_two_positive_review_gate_is_supported(self) -> None:
        report, selected = evaluate(
            self.english,
            self.french,
            self.links,
            self.reviews,
            minimum_positive=2,
        )
        self.assertEqual(1, len(selected))
        self.assertEqual(1, report["selection"]["selected_translations"])

    def test_current_mandarin_export_blocks_stale_or_changed_rows(self) -> None:
        report, selected = evaluate(
            self.english,
            self.french,
            self.links,
            self.reviews,
            mandarin_sentences=self.mandarin,
        )
        self.assertEqual([1], [item.chinese_id for item in selected])
        self.assertEqual(
            1,
            report["source_consistency"]["linked_chinese_ids_with_changed_text"],
        )
        self.assertEqual(
            1,
            report["source_consistency"]["linked_chinese_ids_missing_from_current_export"],
        )


if __name__ == "__main__":
    unittest.main()
