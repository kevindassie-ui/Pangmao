import assert from "node:assert/strict";
import test from "node:test";

import {
  normalizeReaderText,
  readerLookupCandidates,
  segmentFrenchText,
  tokenizeFrenchSentence,
} from "../src/reader.js";

test("reader normalizes transport whitespace without flattening paragraphs", () => {
  assert.equal(normalizeReaderText("  Bonjour\r\n\r\n le monde.\u00a0 "), "Bonjour\n\n le monde.");
});

test("reader keeps sentences and visible punctuation in source order", () => {
  const sentences = segmentFrenchText("Bonjour ! L’affiche est rouge.");
  assert.deepEqual(sentences.map((sentence) => sentence.text), ["Bonjour !", "L’affiche est rouge."]);
  assert.equal(sentences[1].tokens.map((token) => token.text).join(""), "L’affiche est rouge.");
  assert.equal(sentences[1].wordCount, 3);
});

test("reader lookup understands common French elisions", () => {
  assert.deepEqual(readerLookupCandidates("L’affiche"), ["l'affiche", "affiche"]);
  assert.deepEqual(readerLookupCandidates("aujourd'hui"), ["aujourd'hui"]);
  assert.deepEqual(readerLookupCandidates("porte-monnaie"), ["porte-monnaie"]);
});

test("reader has a deterministic sentence fallback for older Safari versions", () => {
  const sentences = segmentFrenchText("Une phrase. Une autre ? La dernière !", undefined);
  assert.equal(sentences.length, 3);
  assert.equal(sentences[2].text, "La dernière !");
});

test("reader tokenization handles empty and punctuation-only input safely", () => {
  assert.deepEqual(segmentFrenchText("   "), []);
  assert.deepEqual(tokenizeFrenchSentence("…"), [
    { text: "…", isWord: false, lookupCandidates: [] },
  ]);
});
