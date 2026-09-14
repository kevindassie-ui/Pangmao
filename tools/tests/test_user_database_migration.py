import re
import sqlite3
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
DATABASE_SOURCE = ROOT / (
    "app/src/main/java/fr/kairossolum/pangmao/data/user/PangmaoUserDatabase.kt"
)


class UserDatabaseMigrationTest(unittest.TestCase):
    def test_version_two_to_three_creates_word_knowledge_table(self):
        source = DATABASE_SOURCE.read_text(encoding="utf-8")
        blocks = re.findall(
            r'"""\s*(CREATE TABLE IF NOT EXISTS `word_knowledge`.*?)\s*"""',
            source,
            re.DOTALL,
        )

        self.assertEqual(1, len(blocks))
        self.assertIn("version = 3", source)
        self.assertIn("WordKnowledgeEntity::class", source)
        self.assertIn("addMigrations(MIGRATION_1_2, MIGRATION_2_3)", source)

        connection = sqlite3.connect(":memory:")
        try:
            connection.execute(blocks[0])
            columns = connection.execute("PRAGMA table_info(word_knowledge)").fetchall()
        finally:
            connection.close()

        self.assertEqual(
            [
                ("entryId", "INTEGER", 1, 1),
                ("status", "TEXT", 1, 0),
                ("updatedAt", "INTEGER", 1, 0),
            ],
            [(row[1], row[2], row[3], row[5]) for row in columns],
        )


if __name__ == "__main__":
    unittest.main()
