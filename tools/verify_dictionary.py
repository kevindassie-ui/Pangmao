#!/usr/bin/env python3
"""Fast CI checks for the shipped immutable dictionary asset."""

from __future__ import annotations

import sqlite3
import sys
from pathlib import Path


HAN_RANGES = (
    (0x3400, 0x4DBF),
    (0x4E00, 0x9FFF),
    (0xF900, 0xFAFF),
    (0x20000, 0x323AF),
)


def contains_han(value: str) -> bool:
    return any(
        any(start <= ord(character) <= end for start, end in HAN_RANGES)
        for character in value
    )


def fail(message: str) -> None:
    raise AssertionError(message)


path = Path(sys.argv[1] if len(sys.argv) > 1 else "app/src/main/assets/databases/pangmao.db")
if not path.exists():
    fail(f"Missing dictionary asset: {path}")

connection = sqlite3.connect(f"file:{path}?mode=ro", uri=True)
if connection.execute("PRAGMA integrity_check").fetchone()[0] != "ok":
    fail("SQLite integrity_check failed")

entry_count = connection.execute("SELECT count(*) FROM entries").fetchone()[0]
example_count = connection.execute("SELECT count(*) FROM examples").fetchone()[0]
character_count = connection.execute("SELECT count(*) FROM characters").fetchone()[0]
if entry_count < 120_000:
    fail(f"Dictionary unexpectedly small: {entry_count}")
if example_count < 60_000:
    fail(f"Example corpus unexpectedly small: {example_count}")
if character_count < 10_000:
    fail(f"Character database unexpectedly small: {character_count}")

china = connection.execute(
    "SELECT definitions_en, definitions_fr FROM entries WHERE simplified = '中国' LIMIT 1"
).fetchone()
if not china or "China" not in china[0] or "Chine" not in china[1]:
    fail("Expected bilingual 中国 entry not found")

metadata = dict(connection.execute("SELECT key, value FROM metadata"))
required = {
    "schema_version",
    "cc_cedict_revision",
    "tatoeba_release",
    "tatoeba_french_release",
    "unihan_version",
    "FreeDict-fra-zho_revision",
    "FreeDict-eng-zho_revision",
    "learning_entry_count",
    "learning_entry_count_fr",
    "learning_entry_count_en",
}
if not required.issubset(metadata):
    fail(f"Missing metadata: {required - metadata.keys()}")

if metadata["schema_version"] != "4":
    fail(f"Unexpected dictionary schema: {metadata['schema_version']}")
if connection.execute("PRAGMA user_version").fetchone()[0] != 4:
    fail("Unexpected SQLite user_version")
if metadata["FreeDict-fra-zho_revision"] != "2025.11.23":
    fail("Unexpected French FreeDict revision")
if metadata["FreeDict-eng-zho_revision"] != "2025.11.23":
    fail("Unexpected English FreeDict revision")

learning_tables = {
    row[0]
    for row in connection.execute(
        "SELECT name FROM sqlite_master WHERE type = 'table' AND name IN "
        "('learning_sources', 'learning_entries', 'learning_forms', "
        "'learning_pronunciations', 'learning_senses', 'learning_equivalents', "
        "'learning_entries_fts')"
    )
}
required_learning_tables = {
    "learning_sources",
    "learning_entries",
    "learning_forms",
    "learning_pronunciations",
    "learning_senses",
    "learning_equivalents",
    "learning_entries_fts",
}
if learning_tables != required_learning_tables:
    fail(f"Learning-profile tables are missing: {required_learning_tables - learning_tables}")

learning_counts = dict(
    connection.execute(
        "SELECT language, count(*) FROM learning_entries GROUP BY language"
    )
)
if learning_counts.get("fr", 0) < 10_000:
    fail(f"French learning dictionary unexpectedly small: {learning_counts.get('fr', 0)}")
if learning_counts.get("en", 0) < 25_000:
    fail(f"English learning dictionary unexpectedly small: {learning_counts.get('en', 0)}")
if sum(learning_counts.values()) != int(metadata.get("learning_entry_count", "-1")):
    fail("Learning entry count does not match metadata")
for language in ("fr", "en"):
    if learning_counts.get(language, 0) != int(
        metadata.get(f"learning_entry_count_{language}", "-1")
    ):
        fail(f"{language} learning entry count does not match metadata")
    if learning_counts.get(language, 0) != int(
        metadata.get(f"learning_filter_{language}_imported_entries", "-1")
    ):
        fail(f"{language} learning filter count does not match imported rows")

expected_source_counts = {"fr": 10_947, "en": 26_660}
for language, expected in expected_source_counts.items():
    actual = int(metadata.get(f"learning_filter_{language}_source_entries", "-1"))
    if actual != expected:
        fail(f"Unexpected {language} FreeDict source count: {actual}")

learning_sources = dict(
    connection.execute("SELECT code, language FROM learning_sources")
)
if learning_sources != {"FreeDict-fra-zho": "fr", "FreeDict-eng-zho": "en"}:
    fail(f"Unexpected learning sources: {learning_sources}")

fts_learning_count = connection.execute(
    "SELECT count(*) FROM learning_entries_fts"
).fetchone()[0]
if fts_learning_count != sum(learning_counts.values()):
    fail("Learning FTS row count does not match entries")

orphan_learning_rows = connection.execute(
    """
    SELECT
      (SELECT count(*) FROM learning_entries e
       LEFT JOIN learning_sources r ON r.code = e.source_code
       WHERE r.code IS NULL)
      +
      (SELECT count(*) FROM learning_forms f
       LEFT JOIN learning_entries e ON e.id = f.entry_id
       WHERE e.id IS NULL)
      +
      (SELECT count(*) FROM learning_pronunciations p
       LEFT JOIN learning_entries e ON e.id = p.entry_id
       WHERE e.id IS NULL)
      +
      (SELECT count(*) FROM learning_senses s
       LEFT JOIN learning_entries e ON e.id = s.entry_id
       LEFT JOIN learning_sources r ON r.code = s.source_code
       WHERE e.id IS NULL OR r.code IS NULL)
      +
      (SELECT count(*) FROM learning_equivalents q
       LEFT JOIN learning_senses s ON s.id = q.sense_id
       WHERE s.id IS NULL)
    """
).fetchone()[0]
if orphan_learning_rows:
    fail(f"Orphan learning-profile rows: {orphan_learning_rows}")

entries_without_primary_form = connection.execute(
    """
    SELECT count(*) FROM learning_entries e
    LEFT JOIN learning_forms f ON f.entry_id = e.id AND f.position = 0
    WHERE f.form IS NULL OR f.form <> e.primary_form OR e.primary_form_search = ''
    """
).fetchone()[0]
if entries_without_primary_form:
    fail(f"Learning entries without a valid primary form: {entries_without_primary_form}")

entries_without_sense = connection.execute(
    """
    SELECT count(*) FROM learning_entries e
    LEFT JOIN learning_senses s ON s.entry_id = e.id
    WHERE s.id IS NULL
    """
).fetchone()[0]
if entries_without_sense:
    fail(f"Learning entries without a valid sense: {entries_without_sense}")

too_many_pronunciations = connection.execute(
    """
    SELECT count(*) FROM (
      SELECT entry_id FROM learning_pronunciations
      GROUP BY entry_id HAVING count(*) > 4
    )
    """
).fetchone()[0]
if too_many_pronunciations:
    fail(f"Learning entries exceed the pronunciation cap: {too_many_pronunciations}")

bad_equivalents = [
    value
    for (value,) in connection.execute("SELECT chinese FROM learning_equivalents")
    if not contains_han(value)
]
if bad_equivalents:
    fail(f"Non-Han learning equivalents escaped filtering: {bad_equivalents[:3]}")

duplicate_equivalents = connection.execute(
    """
    SELECT count(*) FROM (
      SELECT sense_id, chinese FROM learning_equivalents
      GROUP BY sense_id, chinese HAVING count(*) > 1
    )
    """
).fetchone()[0]
if duplicate_equivalents:
    fail(f"Duplicate learning equivalents remain: {duplicate_equivalents}")

learning_witnesses = {
    "fr": {"bonjour": "你好", "etre": "是", "chat": "猫", "manger": "吃"},
    "en": {"hello": "你好", "be": "是", "cat": "猫", "learn": "学"},
}
for language, witnesses in learning_witnesses.items():
    for form_search, chinese_prefix in witnesses.items():
        witness = connection.execute(
            """
            SELECT q.chinese FROM learning_entries e
            JOIN learning_senses s ON s.entry_id = e.id
            JOIN learning_equivalents q ON q.sense_id = s.id
            WHERE e.language = ? AND e.primary_form_search = ? AND q.chinese LIKE ?
            LIMIT 1
            """,
            (language, form_search, f"{chinese_prefix}%"),
        ).fetchone()
        if witness is None:
            fail(f"Missing learning-dictionary witness: {language}/{form_search}")

definition_tables = {
    row[0]
    for row in connection.execute(
        "SELECT name FROM sqlite_master WHERE type = 'table' AND name IN "
        "('definition_sources', 'definition_attributions')"
    )
}
if definition_tables != {"definition_sources", "definition_attributions"}:
    fail(f"Definition provenance tables are missing: {definition_tables}")

definition_source_codes = {
    row[0] for row in connection.execute("SELECT code FROM definition_sources")
}
required_definition_sources = {"CC-CEDICT", "CFDICT", "Pangmao", "Wiktionnaire"}
if not required_definition_sources.issubset(definition_source_codes):
    fail(f"Definition sources are missing: {required_definition_sources - definition_source_codes}")

if metadata.get("default_definition_source_en") != "CC-CEDICT":
    fail("Unexpected default English definition source")
if metadata.get("default_definition_source_fr") != "CFDICT":
    fail("Unexpected default French definition source")

definition_attribution_count = connection.execute(
    "SELECT count(*) FROM definition_attributions"
).fetchone()[0]
if definition_attribution_count != int(metadata.get("definition_attribution_count", "-1")):
    fail("Definition attribution count does not match metadata")
if int(metadata.get("definition_record_count", "0")) < 250_000:
    fail("Conceptual definition provenance is unexpectedly sparse")
if definition_attribution_count < 80:
    fail(f"Reviewed definition supplement unexpectedly small: {definition_attribution_count}")

for language, definition_index, definitions in connection.execute(
    """
    SELECT a.language, a.definition_index,
           CASE a.language WHEN 'en' THEN e.definitions_en ELSE e.definitions_fr END
    FROM definition_attributions a
    JOIN entries e ON e.id = a.entry_id
    """
):
    parts = definitions.splitlines()
    if definition_index < 0 or definition_index >= len(parts):
        fail(f"Definition attribution index out of range: {language}/{definition_index}")

orphan_definitions = connection.execute(
    """
    SELECT count(*) FROM definition_attributions d
    LEFT JOIN entries e ON e.id = d.entry_id
    LEFT JOIN definition_sources s ON s.code = d.source_code
    WHERE e.id IS NULL OR s.code IS NULL
    """
).fetchone()[0]
if orphan_definitions:
    fail(f"Orphan definition provenance rows: {orphan_definitions}")

duplicate_examples = connection.execute(
    "SELECT count(*) FROM (SELECT chinese FROM examples GROUP BY chinese HAVING count(*) > 1)"
).fetchone()[0]
if duplicate_examples:
    fail(f"Duplicate Chinese examples remain: {duplicate_examples}")

bilingual_example_count = connection.execute(
    "SELECT count(*) FROM examples WHERE french <> ''"
).fetchone()[0]
if bilingual_example_count != int(metadata.get("bilingual_example_count", "-1")):
    fail("Bilingual example count does not match metadata")
if bilingual_example_count < 38:
    fail(f"Reviewed bilingual example corpus unexpectedly small: {bilingual_example_count}")

tatoeba_french_count = connection.execute(
    "SELECT count(*) FROM examples WHERE french_source = 'Tatoeba' AND tatoeba_french_id > 0"
).fetchone()[0]
if tatoeba_french_count < 18:
    fail(f"Reviewed Tatoeba French subset unexpectedly small: {tatoeba_french_count}")

tatoeba_french_example = connection.execute(
    """
    SELECT french, tatoeba_french_id, french_source
    FROM examples WHERE tatoeba_chinese_id = 333158
    """
).fetchone()
if tatoeba_french_example != ("Bonsoir !", 333159, "Tatoeba"):
    fail("Expected relation-reviewed Tatoeba French example not found")

example_columns = {row[1] for row in connection.execute("PRAGMA table_info(examples)")}
if not {"english", "french", "english_source", "french_source"}.issubset(example_columns):
    fail("Bilingual example columns are missing")

for headword in ("奶茶婊", "肉夹馍", "爸比"):
    bilingual = connection.execute(
        "SELECT definitions_en, definitions_fr FROM entries WHERE simplified = ? LIMIT 1",
        (headword,),
    ).fetchone()
    if not bilingual or not bilingual[0] or not bilingual[1]:
        fail(f"Expected reviewed bilingual entry not found: {headword}")

reviewed_example = connection.execute(
    "SELECT english, french FROM examples WHERE chinese = '你很像你哥哥。' LIMIT 1"
).fetchone()
if not reviewed_example or "look" not in reviewed_example[0] or "ressembles" not in reviewed_example[1]:
    fail("Expected reviewed bilingual example not found")

for headword in ("真的", "游戏", "所有人", "驾驶执照", "杰", "阿里", "须"):
    french = connection.execute(
        "SELECT definitions_fr FROM entries WHERE simplified = ? LIMIT 1",
        (headword,),
    ).fetchone()
    if not french or not french[0]:
        fail(f"Expected v0.5 reviewed French definition not found: {headword}")

bad_reviewed_markers = ("Définition manquante", "(Ajouter)")
reviewed_french = [
    definition
    for row in connection.execute(
        """
        SELECT e.definitions_fr
        FROM definition_attributions a
        JOIN entries e ON e.id = a.entry_id
        WHERE a.language = 'fr' AND a.reviewed = 1
        """
    )
    for definition in row[0].splitlines()
]
for marker in bad_reviewed_markers:
    if any(marker in definition for definition in reviewed_french):
        fail(f"Rejected candidate text leaked into reviewed definitions: {marker}")
if "Ari" in reviewed_french:
    fail("Rejected candidate text leaked into reviewed definitions: Ari")

source_split_example = connection.execute(
    """
    SELECT english, french, english_source, french_source
    FROM examples WHERE chinese = '你会说中文吗?' LIMIT 1
    """
).fetchone()
if source_split_example != (
    "Do you speak Chinese?",
    "Parles-tu chinois ?",
    "Tatoeba",
    "Pangmao",
):
    fail("Reviewed French example did not preserve its Tatoeba English source")

connection.close()
print(
    f"Dictionary verified: {entry_count} entries, "
    f"{example_count} examples, {character_count} characters, "
    f"{learning_counts.get('fr', 0)} French and {learning_counts.get('en', 0)} English entries"
)
