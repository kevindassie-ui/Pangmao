import assert from "node:assert/strict";
import test from "node:test";
import {
  createMissedSearchRecorder,
  exportMissedSearchJournal,
  loadMissedSearchJournal,
  MAX_MISSED_SEARCHES,
  recordMissedSearch,
  sanitizeMissedSearchJournal,
  saveMissedSearchJournal,
} from "../src/missed-searches.js";

const first = new Date("2026-10-03T12:00:00Z");
const later = new Date("2026-10-03T13:00:00Z");

function harness(enabled = true) {
  let journal = { enabled, entries: [] };
  const recorder = createMissedSearchRecorder({
    getJournal: () => journal,
    onRecord: (next) => { journal = next; },
  });
  return { recorder, get journal() { return journal; }, set journal(value) { journal = value; } };
}

test("recording is disabled by default and survives an explicit local round trip", () => {
  const values = new Map();
  const storage = { getItem: (key) => values.get(key), setItem: (key, value) => values.set(key, value) };
  assert.deepEqual(loadMissedSearchJournal(storage), { enabled: false, entries: [] });
  const disabled = { enabled: false, entries: [] };
  assert.equal(recordMissedSearch(disabled, "bidule"), disabled);
  const journal = recordMissedSearch({ enabled: true, entries: [] }, "bidule", first);
  assert.equal(saveMissedSearchJournal(journal, storage), true);
  assert.deepEqual(loadMissedSearchJournal(storage), journal);
  saveMissedSearchJournal({ ...journal, enabled: false }, storage);
  assert.equal(loadMissedSearchJournal(storage).enabled, false);
  assert.equal(loadMissedSearchJournal(storage).entries.length, 1);
  saveMissedSearchJournal({ enabled: false, entries: [] }, storage);
  assert.deepEqual(loadMissedSearchJournal(storage), disabled);
});

test("repeated words are counted, whitespace and case normalized, accents retained", () => {
  let journal = recordMissedSearch({ enabled: true, entries: [] }, "  Bidule  ", first);
  journal = recordMissedSearch(journal, "bidule", later);
  journal = recordMissedSearch(journal, "être", later);
  journal = recordMissedSearch(journal, "etre", later);
  journal = recordMissedSearch(journal, "  太   离谱  ", later);
  assert.deepEqual(journal.entries.map((entry) => entry.query), ["太 离谱", "etre", "être", "bidule"]);
  assert.equal(journal.entries[0].language, "zh");
  assert.equal(journal.entries[3].count, 2);
  assert.equal(journal.entries[3].firstSeen, first.toISOString());
  assert.equal(journal.entries[3].lastSeen, later.toISOString());
});

test("journal evicts the oldest distinct query and rejects pasted long text", () => {
  let journal = { enabled: true, entries: [] };
  for (let i = 0; i <= MAX_MISSED_SEARCHES; i++) journal = recordMissedSearch(journal, `mot${i}`, first);
  assert.equal(journal.entries.length, MAX_MISSED_SEARCHES);
  assert.equal(journal.entries.at(-1).query, "mot1");
  for (const query of [" ", "?!", "1234", "x".repeat(121), null]) {
    assert.equal(recordMissedSearch(journal, query), journal);
  }
});

test("malformed or inaccessible local data fails safely", () => {
  const blocked = { getItem() { throw new Error("blocked"); }, setItem() { throw new Error("full"); } };
  assert.deepEqual(loadMissedSearchJournal(blocked), { enabled: false, entries: [] });
  assert.equal(saveMissedSearchJournal({ enabled: true, entries: [] }, blocked), false);
  assert.deepEqual(loadMissedSearchJournal({ getItem: () => "invalid JSON" }), { enabled: false, entries: [] });
  const valid = recordMissedSearch({ enabled: true, entries: [] }, "bidule", first).entries[0];
  const entries = [null, {}, { ...valid, count: -1 }, { ...valid, firstSeen: "bad" },
    { ...valid, query: "x".repeat(121) }, valid, { ...valid, query: "BIDULE" }];
  assert.deepEqual(sanitizeMissedSearchJournal({ enabled: "yes", entries }), { enabled: false, entries: [valid] });
});

test("only an explicit submitted query with a confirmed empty result is recorded", () => {
  const h = harness();
  const finish = (query, submitted, outcome) => h.recorder.finish(h.recorder.begin(query, submitted), outcome);
  assert.equal(finish("partiel", false, { resultCount: 0 }), false);
  assert.equal(finish("bonjour", true, { resultCount: 1 }), false);
  assert.equal(finish("放屁", true, { resultCount: 0, fallbackResultCount: 1 }), false);
  assert.equal(finish("补充", true, { resultCount: 0, fallbackFailed: true }), false);
  assert.equal(finish("absent", true, { resultCount: 0 }), true);
  assert.deepEqual(h.journal.entries.map((entry) => entry.query), ["absent"]);
});

test("delayed lookup is ignored after typing, a newer search, clear or opting out", () => {
  const h = harness();
  const delayed = h.recorder.begin("旧查询", true);
  h.recorder.begin("nouvelle", false);
  assert.equal(h.recorder.finish(delayed, { resultCount: 0 }), false);
  const clearing = h.recorder.begin("à effacer", true);
  h.recorder.invalidate();
  assert.equal(h.recorder.finish(clearing, { resultCount: 0 }), false);
  const disabling = h.recorder.begin("désactivé", true);
  h.journal = { enabled: false, entries: [] };
  assert.equal(h.recorder.finish(disabling, { resultCount: 0 }), false);
  const beforeOptIn = h.recorder.begin("avant accord", true);
  h.journal = { enabled: true, entries: [] };
  assert.equal(h.recorder.finish(beforeOptIn, { resultCount: 0 }), false);
  assert.equal(h.journal.entries.length, 0);
});

test("the same completed attempt cannot be counted twice", () => {
  const h = harness();
  const attempt = h.recorder.begin("absent", true);
  assert.equal(h.recorder.finish(attempt, { resultCount: 0 }), true);
  assert.equal(h.recorder.finish(attempt, { resultCount: 0 }), false);
  assert.equal(h.journal.entries[0].count, 1);
});

test("export contains bounded lexical records and release context, no other device data", () => {
  const journal = recordMissedSearch({ enabled: true, entries: [] }, "缺词", first);
  const exported = JSON.parse(exportMissedSearchJournal(journal, "0.3.3", later));
  assert.deepEqual(exported, { schemaVersion: 1, releaseVersion: "0.3.3", exportedAt: later.toISOString(), entries: journal.entries });
});
