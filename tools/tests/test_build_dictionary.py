from __future__ import annotations

import sqlite3
import sys
import tempfile
import unittest
from pathlib import Path


sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from build_dictionary import (  # noqa: E402
    MutableEntry,
    MutableExample,
    add_definitions,
    create_database,
    entry_key,
    load_pangmao_examples,
    load_reviewed_tatoeba_french,
    load_reviewed_definitions,
)


class BuildDictionaryProvenanceTest(unittest.TestCase):
    def setUp(self) -> None:
        self.temporary = tempfile.TemporaryDirectory()
        self.root = Path(self.temporary.name)

    def tearDown(self) -> None:
        self.temporary.cleanup()

    def test_definition_text_is_deduplicated_but_sources_are_preserved(self) -> None:
        entry = MutableEntry("遊戲", "游戏", "you2 xi4")
        add_definitions(entry, "fr", ["jeu", "jeu"], "CFDICT")
        add_definitions(entry, "fr", ["jeu"], "Wiktionnaire", reviewed=True)

        self.assertEqual(["jeu"], entry.definitions_fr)
        self.assertEqual(2, len(entry.definition_records))
        self.assertEqual({"CFDICT", "Wiktionnaire"}, entry.sources)

    def test_reviewed_supplement_requires_an_exact_entry(self) -> None:
        entry = MutableEntry("遊戲", "游戏", "you2 xi4")
        entries = {entry_key(entry.traditional, entry.simplified, entry.pinyin): entry}
        supplement = self.root / "reviewed.tsv"
        supplement.write_text(
            "traditional\tsimplified\tpinyin\tlanguage\tdefinition\tsource\t"
            "source_page\tpart_of_speech\n"
            "遊戲\t游戏\tyou2 xi4\tfr\tjeu; jouer\tWiktionnaire\t"
            "https://fr.wiktionary.org/wiki/游戏\tnoun\n",
            encoding="utf-8",
        )

        load_reviewed_definitions(supplement, entries)
        self.assertEqual(["jeu; jouer"], entry.definitions_fr)
        self.assertTrue(entry.definition_records[0].reviewed)

        supplement.write_text(
            supplement.read_text(encoding="utf-8").replace("you2 xi4", "you2xi4"),
            encoding="utf-8",
        )
        with self.assertRaisesRegex(ValueError, "does not match an exact entry"):
            load_reviewed_definitions(supplement, entries)

    def test_database_contains_normalized_definition_provenance(self) -> None:
        entry = MutableEntry("遊戲", "游戏", "you2 xi4")
        add_definitions(entry, "en", ["game", "to play"], "CC-CEDICT")
        add_definitions(entry, "fr", ["jeu; jouer"], "Wiktionnaire", reviewed=True)
        output = self.root / "pangmao.db"
        metadata = {
            "schema_version": "4",
            "cc_cedict_revision": "fixture",
            "cfdict_downloaded": "fixture",
            "pangmao_supplement_version": "fixture",
            "frwiktionary_revision": "fixture",
            "definition_record_count": "3",
        }

        create_database(
            output=output,
            entries={entry_key(entry.traditional, entry.simplified, entry.pinyin): entry},
            examples=[],
            unihan={},
            preferred_pinyin={"游戏": "you2 xi4", "遊戲": "you2 xi4"},
            metadata=metadata,
        )

        connection = sqlite3.connect(output)
        self.assertEqual(4, connection.execute("PRAGMA user_version").fetchone()[0])
        self.assertEqual(
            1,
            connection.execute("SELECT count(*) FROM definition_attributions").fetchone()[0],
        )
        row = connection.execute(
            """
            SELECT d.language, d.definition_index, d.source_code, d.reviewed,
                   e.definitions_fr
            FROM definition_attributions d
            JOIN entries e ON e.id = d.entry_id
            WHERE e.simplified = '游戏' AND d.language = 'fr'
            """
        ).fetchone()
        self.assertEqual(("fr", 0, "Wiktionnaire", 1, "jeu; jouer"), row)
        connection.close()

    def test_reviewed_translation_can_keep_the_pinned_tatoeba_english(self) -> None:
        supplement = self.root / "examples.tsv"
        supplement.write_text(
            "chinese\tenglish\tfrench\tenglish_source\tfrench_source\n"
            "你会说中文吗?\tDo you speak Chinese?\tParles-tu chinois ?\tTatoeba\tPangmao\n",
            encoding="utf-8",
        )
        examples = [
            MutableExample(
                chinese_id=10,
                chinese="你会说中文吗?",
                english_id=20,
                english="Do you speak Chinese?",
            )
        ]

        load_pangmao_examples(supplement, examples)

        self.assertEqual(20, examples[0].english_id)
        self.assertEqual("Tatoeba", examples[0].english_source)
        self.assertEqual("Parles-tu chinois ?", examples[0].french)
        self.assertEqual("Pangmao", examples[0].french_source)

    def test_reviewed_tatoeba_french_requires_ids_text_and_two_reviews(self) -> None:
        supplement = self.root / "tatoeba_french.tsv"
        supplement.write_text(
            "chinese_id\tchinese\tfrench_id\tfrench\tpositive_reviews\n"
            "10\t你好。\t30\tBonjour !\t2\n",
            encoding="utf-8",
        )
        examples = [
            MutableExample(
                chinese_id=10,
                chinese="你好。",
                english_id=20,
                english="Hello!",
            )
        ]

        load_reviewed_tatoeba_french(supplement, examples)

        self.assertEqual(30, examples[0].french_id)
        self.assertEqual("Bonjour !", examples[0].french)
        self.assertEqual("Tatoeba", examples[0].french_source)

        supplement.write_text(
            supplement.read_text(encoding="utf-8").replace("\t2\n", "\t1\n"),
            encoding="utf-8",
        )
        with self.assertRaisesRegex(ValueError, "lacks two positive reviews"):
            load_reviewed_tatoeba_french(supplement, [MutableExample(10, "你好。")])


if __name__ == "__main__":
    unittest.main()
