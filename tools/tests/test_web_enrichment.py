import unittest

from tools.export_web_chinese_fallback import english_signature, shard_index


class WebEnrichmentTest(unittest.TestCase):
    def test_english_signature_removes_usage_labels(self) -> None:
        self.assertEqual("self important", english_signature("(coll.) self-important"))

    def test_shard_index_is_stable(self) -> None:
        self.assertEqual(27, shard_index("臭屁", 32))


if __name__ == "__main__":
    unittest.main()
