from __future__ import annotations

import sys
import tempfile
import unittest
from pathlib import Path


sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from evaluate_learning_sources import evaluate_source  # noqa: E402


TEI_FIXTURE = """<?xml version="1.0" encoding="UTF-8"?>
<TEI xmlns="http://www.tei-c.org/ns/1.0">
  <teiHeader>
    <fileDesc>
      <editionStmt><edition>fixture</edition></editionStmt>
      <extent>3 headwords</extent>
      <publicationStmt><availability status="free"><p>Licensed under
        <ref target="https://creativecommons.org/licenses/by-sa/3.0/legalcode">CC BY-SA</ref>
      </p></availability></publicationStmt>
      <sourceDesc><p>Fixture source</p></sourceDesc>
    </fileDesc>
  </teiHeader>
  <text><body xml:lang="fr">
    <entry>
      <form><orth>chat</orth><pron>ʃa</pron></form>
      <gramGrp><pos>n</pos><gen>masc</gen></gramGrp>
      <sense><cit type="trans" xml:lang="zh"><quote>猫</quote><quote>貓</quote></cit>
        <sense><def>Mammifère félin.</def></sense>
      </sense>
    </entry>
    <entry>
      <form><orth>aimer</orth><pron>e.me</pron></form>
      <gramGrp><pos>v</pos></gramGrp>
      <sense><cit type="trans" xml:lang="zh"><quote>爱</quote></cit></sense>
    </entry>
    <entry>
      <form><orth>chat</orth><pron>&lt;dialect&gt;</pron></form>
      <gramGrp><pos>v</pos></gramGrp>
      <sense><cit type="trans" xml:lang="zh"><quote /></cit></sense>
    </entry>
  </body></text>
</TEI>
"""


class EvaluateLearningSourcesTest(unittest.TestCase):
    def setUp(self) -> None:
        self.temporary = tempfile.TemporaryDirectory()
        self.source = Path(self.temporary.name) / "fixture.tei"
        self.source.write_text(TEI_FIXTURE, encoding="utf-8")

    def tearDown(self) -> None:
        self.temporary.cleanup()

    def test_measures_coverage_duplicates_and_filter_candidates(self) -> None:
        report = evaluate_source(self.source, "fr", sample_words=("chat",))

        self.assertEqual(3, report["counts"]["entries"])
        self.assertEqual(2, report["counts"]["unique_headwords"])
        self.assertEqual(1, report["counts"]["headwords_with_multiple_entries"])
        self.assertEqual(2, report["counts"]["entries_with_chinese_translation"])
        self.assertEqual(1, report["counts"]["empty_translation_nodes"])
        self.assertEqual(1, report["counts"]["entries_with_suspicious_pronunciation"])
        self.assertEqual(1, report["counts"]["noun_entries"])
        self.assertEqual(1, report["counts"]["noun_entries_with_gender"])
        self.assertEqual(100.0, report["coverage"]["noun_gender"]["percentage"])
        self.assertEqual(2, len(report["samples"]["chat"]))

    def test_rejects_a_mismatched_source_language(self) -> None:
        with self.assertRaisesRegex(ValueError, "Expected source language"):
            evaluate_source(self.source, "en")


if __name__ == "__main__":
    unittest.main()
