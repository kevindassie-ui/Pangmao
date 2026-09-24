import assert from "node:assert/strict";
import test from "node:test";

import {
  loadFavorites,
  loadReaderDraft,
  saveFavorites,
  saveReaderDraft,
} from "../src/storage.js";

class MemoryStorage {
  values = new Map();
  getItem(key) { return this.values.get(key) ?? null; }
  setItem(key, value) { this.values.set(key, value); }
}

test("favorites survive a round trip with namespaced ids", () => {
  const storage = new MemoryStorage();
  saveFavorites(new Set(["fr:freedict:42", "fr:freedict:7"]), storage);
  assert.deepEqual([...loadFavorites(storage)], ["fr:freedict:42", "fr:freedict:7"]);
});

test("invalid local data is ignored safely", () => {
  const storage = new MemoryStorage();
  storage.setItem("pangmao.web.favorites.v1", "not-json");
  assert.deepEqual([...loadFavorites(storage)], []);
});

test("reader draft stays on the current device", () => {
  const storage = new MemoryStorage();
  saveReaderDraft("Une affiche rouge.", storage);
  assert.equal(loadReaderDraft(storage), "Une affiche rouge.");
});

test("storage failures never break the offline interface", () => {
  const storage = {
    getItem() { throw new Error("blocked"); },
    setItem() { throw new Error("full"); },
  };
  assert.equal(loadReaderDraft(storage), "");
  assert.equal(saveReaderDraft("texte", storage), false);
  assert.equal(saveFavorites(new Set(["fr:test:1"]), storage), false);
});
