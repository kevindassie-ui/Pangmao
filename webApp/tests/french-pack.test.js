import assert from "node:assert/strict";
import { createHash } from "node:crypto";
import { readFile } from "node:fs/promises";
import test from "node:test";

import { createDictionaryIndex, searchDictionary } from "../src/search-engine.js";

const packPath = new URL("../data/french-pack.json", import.meta.url);
const pack = JSON.parse(await readFile(packPath, "utf8"));
const index = createDictionaryIndex(pack);

function canonicalize(value) {
  if (Array.isArray(value)) return value.map(canonicalize);
  if (value !== null && typeof value === "object") {
    return Object.fromEntries(
      Object.keys(value)
        .sort()
        .map((key) => [key, canonicalize(value[key])]),
    );
  }
  return value;
}

function firstHeadword(query) {
  return searchDictionary(index, query, 1)[0]?.entry.headword;
}

test("complete French pack has deterministic metadata and identifiers", () => {
  assert.equal(pack.schemaVersion, 2);
  assert.equal(pack.enrichedEntryCount, 2_393);
  assert.equal(pack.language, "fr");
  assert.equal(pack.entryCount, 10_923);
  assert.equal(pack.entries.length, pack.entryCount);
  assert.equal(pack.source.code, "FreeDict-fra-zho");
  assert.equal(pack.source.revision, "2025.11.23");
  assert.equal(pack.source.license, "CC BY-SA 3.0");

  const identifiers = new Set(pack.entries.map((entry) => entry.id));
  assert.equal(identifiers.size, pack.entryCount);
  assert.ok([...identifiers].every((identifier) => identifier.startsWith("fr:FreeDict-fra-zho:")));

  const digest = createHash("sha256")
    .update(JSON.stringify(canonicalize(pack.entries)), "utf8")
    .digest("hex");
  assert.equal(digest, pack.entriesSha256);
});

test("real pack exposes attributed Chinese explanations", () => {
  assert.equal(pack.enrichmentSources[0].code, "zhwiktionary-french");
  assert.equal(pack.enrichmentSources[0].license, "CC BY-SA 4.0");
  const medecin = pack.entries.find((entry) => entry.headword === "médecin");
  assert.ok(medecin?.chineseGlosses?.length);
  assert.ok(medecin.chineseGlosses.some((group) => group.glosses.includes("医生，大夫")));
});

test("real pack resolves bidirectional avocat senses without merging them", () => {
  const frenchResult = searchDictionary(index, "avocat", 1)[0];
  assert.equal(frenchResult.entry.headword, "avocat");
  const legalSense = frenchResult.entry.senses.findIndex((sense) => sense.chinese.includes("律师"));
  const fruitSense = frenchResult.entry.senses.findIndex((sense) => sense.chinese.includes("牛油果"));
  assert.notEqual(legalSense, -1);
  assert.notEqual(fruitSense, -1);
  assert.notEqual(legalSense, fruitSense);

  for (const query of ["律师", "律師"]) {
    const result = searchDictionary(index, query, 1)[0];
    assert.equal(result.entry.headword, "avocat");
    assert.deepEqual(result.matchedSenseIndexes, [legalSense]);
  }
  for (const query of ["牛油果", "鳄梨"]) {
    const result = searchDictionary(index, query, 1)[0];
    assert.equal(result.entry.headword, "avocat");
    assert.deepEqual(result.matchedSenseIndexes, [fruitSense]);
  }
});

test("real pack supports accents and the MVP inflection fallbacks", () => {
  assert.equal(firstHeadword("être"), "être");
  assert.equal(firstHeadword("etre"), "être");
  assert.equal(firstHeadword("ÊTRE"), "être");
  assert.equal(firstHeadword("avocates"), "avocat");
  assert.equal(firstHeadword("suis"), "être");
  assert.equal(firstHeadword("étaient"), "être");
  assert.equal(firstHeadword("vais"), "aller");
  assert.equal(firstHeadword("allées"), "aller");
});

test("real pack keeps voler's principal meanings in separate senses", () => {
  const result = searchDictionary(index, "voler", 1)[0];
  assert.equal(result.entry.headword, "voler");
  const flyingSense = result.entry.senses.findIndex((sense) => sense.chinese.includes("飞"));
  const stealingSense = result.entry.senses.findIndex((sense) => sense.chinese.includes("偷"));
  assert.notEqual(flyingSense, -1);
  assert.notEqual(stealingSense, -1);
  assert.notEqual(flyingSense, stealingSense);
  assert.deepEqual(searchDictionary(index, "飞", 1)[0].matchedSenseIndexes, [flyingSense]);
  assert.deepEqual(searchDictionary(index, "偷", 1)[0].matchedSenseIndexes, [stealingSense]);
});

test("real pack handles invalid and unknown input without failure", () => {
  assert.deepEqual(searchDictionary(index, "%_\"'"), []);
  assert.deepEqual(searchDictionary(index, "mot-totalement-inexistant"), []);
});
