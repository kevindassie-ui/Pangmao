import assert from "node:assert/strict";
import test from "node:test";

import { loadFavorites, saveFavorites } from "../src/storage.js";

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
