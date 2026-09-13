from __future__ import annotations

import sqlite3
import sys
import tempfile
import unittest
from pathlib import Path


sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from build_dictionary import create_database, load_learning_dictionary  # noqa: E402


TEI_FIXTURE = """<?xml version="1.0" encoding="UTF-8"?>
<TEI xmlns="http://www.tei-c.org/ns/1.0">
  <text><body xml:lang="fr">
    <entry>
      <form><orth>chat</orth><orth>félin</orth><pron>ʃa</pron></form>
      <gramGrp><pos>n</pos><gen>masc</gen></gramGrp>
      <sense>
        <cit type="trans" xml:lang="zh"><quote>猫</quote><quote>貓</quote><quote>cat</quote></cit>
        <sense><def>Mammifère félin.</def></sense>
      </sense>
    </entry>
    <entry>
      <form><orth>aimer</orth><pron>&lt;France&gt; e.me</pron><pron>e.me</pron>
        <pron>ɛ.me</pron><pron>e.mɛ</pron><pron>ɛ.mɛ</pron><pron>aɪm</pron></form>
      <gramGrp><pos>v</pos></gramGrp>
      <sense><cit type="trans" xml:lang="zh"><quote>爱</quote></cit></sense>
    </entry>
    <entry>
      <form><orth>chat</orth><pron>ʃa</pron></form>
      <gramGrp><pos>v</pos></gramGrp>
      <sense><cit type="trans" xml:lang="zh"><quote>闲聊</quote></cit></sense>
    </entry>
    <entry>
      <form><orth>technique</orth></form>
      <sense><cit type="trans" xml:lang="zh"><quote>[[#</quote></cit></sense>
    </entry>
    <entry><sense><cit type="trans" xml:lang="zh"><quote>空</quote></cit></sense></entry>
  </body></text>
</TEI>
"""


class LearningDictionaryTest(unittest.TestCase):
    def setUp(self) -> None:
        self.temporary = tempfile.TemporaryDirectory()
        self.root = Path(self.temporary.name)
        self.source = self.root / "fra-zho.tei"
        self.source.write_text(TEI_FIXTURE, encoding="utf-8")

    def tearDown(self) -> None:
        self.temporary.cleanup()

    def test_import_filters_noise_caps_pronunciations_and_preserves_homonyms(self) -> None:
        entries, metrics = load_learning_dictionary(
            self.source,
            "fr",
            "FreeDict-fra-zho",
        )

        self.assertEqual(3, len(entries))
        self.assertEqual(2, sum(entry.forms[0] == "chat" for entry in entries))
        aimer = next(entry for entry in entries if entry.forms[0] == "aimer")
        self.assertEqual(4, len(aimer.pronunciations))
        self.assertEqual("e.me", aimer.pronunciations[0])
        self.assertEqual(1, metrics["pronunciations_cleaned"])
        self.assertEqual(1, metrics["entries_with_truncated_pronunciations"])
        self.assertEqual(2, metrics["non_han_equivalents_rejected"])
        self.assertEqual(1, metrics["rejected_without_valid_sense"])
        self.assertEqual(1, metrics["rejected_without_headword"])
        self.assertEqual(("猫", "貓"), entries[0].senses[0].chinese_equivalents)

    def test_schema_v4_keeps_forms_senses_and_provenance_queryable(self) -> None:
        learning_entries, metrics = load_learning_dictionary(
            self.source,
            "fr",
            "FreeDict-fra-zho",
        )
        output = self.root / "pangmao.db"
        metadata = {
            "schema_version": "4",
            "cc_cedict_revision": "fixture",
            "cfdict_downloaded": "fixture",
            "pangmao_supplement_version": "fixture",
            "frwiktionary_revision": "fixture",
            "FreeDict-fra-zho_revision": "fixture",
            "learning_entry_count": str(len(learning_entries)),
            "learning_entry_count_fr": str(len(learning_entries)),
            "learning_entry_count_en": "0",
            "learning_filter_fr_imported_entries": str(metrics["imported_entries"]),
        }

        create_database(
            output=output,
            entries={},
            examples=[],
            unihan={},
            preferred_pinyin={},
            metadata=metadata,
            learning_entries=learning_entries,
        )

        connection = sqlite3.connect(output)
        self.assertEqual(4, connection.execute("PRAGMA user_version").fetchone()[0])
        self.assertEqual(3, connection.execute("SELECT count(*) FROM learning_entries").fetchone()[0])
        self.assertEqual(
            2,
            connection.execute(
                "SELECT count(*) FROM learning_entries WHERE primary_form = 'chat'"
            ).fetchone()[0],
        )
        self.assertEqual(
            [("猫",), ("貓",)],
            connection.execute(
                """
                SELECT q.chinese FROM learning_equivalents q
                JOIN learning_senses s ON s.id = q.sense_id
                JOIN learning_entries e ON e.id = s.entry_id
                WHERE e.primary_form = 'chat' AND e.parts_of_speech = 'n'
                ORDER BY q.position
                """
            ).fetchall(),
        )
        self.assertEqual(
            1,
            connection.execute(
                "SELECT count(*) FROM learning_entries_fts WHERE learning_entries_fts MATCH 'felin'"
            ).fetchone()[0],
        )
        self.assertEqual(
            ("fr", "fixture", "CC BY-SA 3.0"),
            connection.execute(
                "SELECT language, revision, license FROM learning_sources"
            ).fetchone(),
        )
        self.assertEqual([], connection.execute("PRAGMA foreign_key_check").fetchall())
        connection.close()

    def test_rejects_wrong_source_language(self) -> None:
        with self.assertRaisesRegex(ValueError, "Expected source language"):
            load_learning_dictionary(self.source, "en", "FreeDict-eng-zho")


if __name__ == "__main__":
    unittest.main()
