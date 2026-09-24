import json
import sqlite3
import tempfile
import unittest
from pathlib import Path

from tools.export_web_dictionary import export_french_pack, write_pack


class ExportWebDictionaryTest(unittest.TestCase):
    def setUp(self) -> None:
        self.temporary_directory = tempfile.TemporaryDirectory()
        self.database = Path(self.temporary_directory.name) / "dictionary.db"
        connection = sqlite3.connect(self.database)
        connection.executescript(
            """
            PRAGMA user_version = 4;
            CREATE TABLE metadata (key TEXT PRIMARY KEY, value TEXT NOT NULL);
            CREATE TABLE learning_sources (
                code TEXT PRIMARY KEY,
                language TEXT NOT NULL,
                display_name TEXT NOT NULL,
                url TEXT NOT NULL,
                revision TEXT NOT NULL,
                license TEXT NOT NULL
            );
            CREATE TABLE learning_entries (
                id INTEGER PRIMARY KEY,
                language TEXT NOT NULL,
                primary_form TEXT NOT NULL,
                primary_form_search TEXT NOT NULL,
                parts_of_speech TEXT NOT NULL,
                genders TEXT NOT NULL,
                source_code TEXT NOT NULL,
                source_entry_index INTEGER NOT NULL
            );
            CREATE TABLE learning_forms (
                entry_id INTEGER NOT NULL,
                position INTEGER NOT NULL,
                form TEXT NOT NULL
            );
            CREATE TABLE learning_pronunciations (
                entry_id INTEGER NOT NULL,
                position INTEGER NOT NULL,
                pronunciation TEXT NOT NULL
            );
            CREATE TABLE learning_senses (
                id INTEGER PRIMARY KEY,
                entry_id INTEGER NOT NULL,
                position INTEGER NOT NULL,
                definitions TEXT NOT NULL
            );
            CREATE TABLE learning_equivalents (
                sense_id INTEGER NOT NULL,
                position INTEGER NOT NULL,
                chinese TEXT NOT NULL
            );
            INSERT INTO metadata VALUES ('learning_entry_count_fr', '1');
            INSERT INTO learning_sources VALUES (
                'FreeDict-fra-zho', 'fr', 'FreeDict / WikDict',
                'https://example.test', '2025.11.23', 'CC BY-SA 3.0'
            );
            INSERT INTO learning_entries VALUES (
                7, 'fr', 'avocat', 'avocat', 'n', 'masc', 'FreeDict-fra-zho', 42
            );
            INSERT INTO learning_forms VALUES (7, 0, 'avocat');
            INSERT INTO learning_forms VALUES (7, 1, 'avocate');
            INSERT INTO learning_pronunciations VALUES (7, 0, 'a.vɔ.ka');
            INSERT INTO learning_senses VALUES (70, 7, 0, 'profession juridique');
            INSERT INTO learning_senses VALUES (71, 7, 1, 'fruit');
            INSERT INTO learning_equivalents VALUES (70, 0, '律师');
            INSERT INTO learning_equivalents VALUES (70, 1, '律師');
            INSERT INTO learning_equivalents VALUES (71, 0, '牛油果');
            """
        )
        connection.commit()
        connection.close()

    def tearDown(self) -> None:
        self.temporary_directory.cleanup()

    def test_exports_bidirectional_entries_without_merging_senses(self) -> None:
        pack = export_french_pack(self.database, release_version="0.3.1-test")

        self.assertEqual(2, pack["schemaVersion"])
        self.assertEqual("0.3.1-test", pack["releaseVersion"])
        self.assertEqual(1, pack["entryCount"])
        self.assertEqual(0, pack["enrichedEntryCount"])
        self.assertEqual("fr:FreeDict-fra-zho:42", pack["entries"][0]["id"])
        self.assertEqual(["avocat", "avocate"], pack["entries"][0]["forms"])
        self.assertEqual(["律师", "律師"], pack["entries"][0]["senses"][0]["chinese"])
        self.assertEqual(["牛油果"], pack["entries"][0]["senses"][1]["chinese"])
        self.assertEqual("CC BY-SA 3.0", pack["source"]["license"])

    def test_output_is_deterministic_and_utf8(self) -> None:
        first = export_french_pack(self.database)
        second = export_french_pack(self.database)
        self.assertEqual(first["entriesSha256"], second["entriesSha256"])

        output = Path(self.temporary_directory.name) / "pack.json"
        write_pack(first, output)
        restored = json.loads(output.read_text(encoding="utf-8"))
        self.assertEqual("律师", restored["entries"][0]["senses"][0]["chinese"][0])
        self.assertNotIn("\\u5f8b", output.read_text(encoding="utf-8"))

    def test_adds_attributed_chinese_glosses_without_replacing_senses(self) -> None:
        enrichment = Path(self.temporary_directory.name) / "glosses.json"
        enrichment.write_text(
            json.dumps(
                {
                    "schemaVersion": 1,
                    "source": {
                        "code": "zhwiktionary-french",
                        "name": "中文维基词典法语词条",
                        "license": "CC BY-SA 4.0",
                    },
                    "entryCount": 1,
                    "entries": [
                        {
                            "id": "fr:FreeDict-fra-zho:42",
                            "headword": "avocat",
                            "groups": [
                                {
                                    "partOfSpeech": "noun",
                                    "label": "名词",
                                    "glosses": ["律师", "牛油果"],
                                }
                            ],
                        }
                    ],
                },
                ensure_ascii=False,
            ),
            encoding="utf-8",
        )

        pack = export_french_pack(self.database, enrichment)

        self.assertEqual(1, pack["enrichedEntryCount"])
        self.assertEqual("CC BY-SA 4.0", pack["enrichmentSources"][0]["license"])
        self.assertEqual(
            ["律师", "牛油果"],
            pack["entries"][0]["chineseGlosses"][0]["glosses"],
        )
        self.assertEqual("律师", pack["entries"][0]["senses"][0]["chinese"][0])

    def test_adds_reviewed_supplements_with_separate_source_metadata(self) -> None:
        supplements = Path(self.temporary_directory.name) / "supplements.json"
        supplements.write_text(
            json.dumps(
                {
                    "schemaVersion": 1,
                    "source": {
                        "code": "CFDICT",
                        "name": "CFDICT",
                        "url": "https://example.test/cfdict",
                        "revision": "2026-09-11",
                        "license": "CC BY-SA 3.0",
                    },
                    "entryCount": 1,
                    "entries": [
                        {
                            "id": "fr:CFDICT:reviewed-affiche-v1",
                            "headword": "affiche",
                            "forms": ["affiche", "affiches"],
                            "pronunciations": [],
                            "partsOfSpeech": ["n"],
                            "genders": ["fem"],
                            "senses": [
                                {
                                    "definitions": [],
                                    "chinese": ["海报", "海報"],
                                }
                            ],
                        }
                    ],
                },
                ensure_ascii=False,
            ),
            encoding="utf-8",
        )

        pack = export_french_pack(self.database, reviewed_supplements=supplements)

        self.assertEqual(2, pack["entryCount"])
        affiche = next(entry for entry in pack["entries"] if entry["headword"] == "affiche")
        self.assertEqual("fr:CFDICT:reviewed-affiche-v1", affiche["id"])
        self.assertEqual(["海报", "海報"], affiche["senses"][0]["chinese"])
        self.assertEqual("CFDICT", pack["supplementSources"][0]["code"])


if __name__ == "__main__":
    unittest.main()
