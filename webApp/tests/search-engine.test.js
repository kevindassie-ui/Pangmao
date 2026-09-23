import assert from "node:assert/strict";
import test from "node:test";

import {
  containsHan,
  createDictionaryIndex,
  normalizeFrench,
  searchDictionary,
} from "../src/search-engine.js";

const pack = {
  schemaVersion: 1,
  entries: [
    {
      id: "fr:freedict:1",
      headword: "avocat",
      forms: ["avocat"],
      pronunciations: ["a.vɔ.ka"],
      partsOfSpeech: ["n"],
      genders: ["masc"],
      senses: [
        { definitions: ["Professionnel du droit"], chinese: ["律师", "律師"] },
        { definitions: ["Fruit de l'avocatier"], chinese: ["牛油果", "鳄梨"] },
      ],
    },
    {
      id: "fr:freedict:2",
      headword: "être",
      forms: ["être"],
      pronunciations: ["ɛtʁ"],
      partsOfSpeech: ["v"],
      genders: [],
      senses: [{ definitions: ["Définir un état"], chinese: ["是"] }],
    },
    {
      id: "fr:freedict:3",
      headword: "aller",
      forms: ["aller"],
      pronunciations: ["a.le"],
      partsOfSpeech: ["v"],
      genders: [],
      senses: [{ definitions: ["Se déplacer"], chinese: ["去"] }],
    },
  ],
};

const index = createDictionaryIndex(pack);

test("normalization is accent insensitive and safe", () => {
  assert.equal(normalizeFrench(" ÊTRE, l’ami ! "), "etre l'ami");
  assert.equal(normalizeFrench("%_\""), "");
  assert.equal(containsHan("律师"), true);
  assert.equal(containsHan("avocat"), false);
});

test("French query preserves separate senses", () => {
  const [result] = searchDictionary(index, "avocat");
  assert.equal(result.entry.headword, "avocat");
  assert.equal(result.entry.senses.length, 2);
  assert.deepEqual(result.entry.senses[0].chinese, ["律师", "律師"]);
  assert.deepEqual(result.entry.senses[1].chinese, ["牛油果", "鳄梨"]);
});

test("Chinese query returns the matching French sense first", () => {
  const [result] = searchDictionary(index, "律师");
  assert.equal(result.entry.headword, "avocat");
  assert.deepEqual(result.matchedSenseIndexes, [0]);
});

test("accent and common inflection fallbacks resolve to a lemma", () => {
  assert.equal(searchDictionary(index, "etre")[0].entry.headword, "être");
  assert.equal(searchDictionary(index, "avocates")[0].entry.headword, "avocat");
  assert.equal(searchDictionary(index, "sont")[0].entry.headword, "être");
  assert.equal(searchDictionary(index, "allées")[0].entry.headword, "aller");
});

test("unknown and punctuation-only queries return an empty state", () => {
  assert.deepEqual(searchDictionary(index, "%_\"'"), []);
  assert.deepEqual(searchDictionary(index, "mot-totalement-inexistant"), []);
});
