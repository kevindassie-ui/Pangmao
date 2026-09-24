import assert from "node:assert/strict";
import test from "node:test";

import {
  loadFrenchVoiceProfile,
  loadFrenchVoiceId,
  loadFavorites,
  loadReaderDraft,
  saveFrenchVoiceProfile,
  saveFrenchVoiceId,
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

test("the selected French voice stays local to the device", () => {
  const storage = new MemoryStorage();
  saveFrenchVoiceId("com.apple.voice.compact.fr-FR.Thomas", storage);
  assert.equal(loadFrenchVoiceId(storage), "com.apple.voice.compact.fr-FR.Thomas");
});

test("female and male French voice profiles stay independent", () => {
  const storage = new MemoryStorage();
  saveFrenchVoiceProfile({
    activeGender: "male",
    voices: {
      female: "com.apple.voice.compact.fr-FR.Amelie",
      male: "com.apple.voice.compact.fr-FR.Thomas",
    },
  }, storage);
  assert.deepEqual(loadFrenchVoiceProfile(storage), {
    activeGender: "male",
    voices: {
      female: "com.apple.voice.compact.fr-FR.Amelie",
      male: "com.apple.voice.compact.fr-FR.Thomas",
    },
  });
});

test("invalid voice profile data falls back to an empty female profile", () => {
  const storage = new MemoryStorage();
  storage.setItem("pangmao.web.french-voice-profile.v1", JSON.stringify({
    activeGender: "unknown",
    voices: { female: 42, male: null },
  }));
  assert.deepEqual(loadFrenchVoiceProfile(storage), {
    activeGender: "female",
    voices: { female: "", male: "" },
  });
});

test("storage failures never break the offline interface", () => {
  const storage = {
    getItem() { throw new Error("blocked"); },
    setItem() { throw new Error("full"); },
  };
  assert.equal(loadReaderDraft(storage), "");
  assert.equal(loadFrenchVoiceId(storage), "");
  assert.deepEqual(loadFrenchVoiceProfile(storage), {
    activeGender: "female",
    voices: { female: "", male: "" },
  });
  assert.equal(saveReaderDraft("texte", storage), false);
  assert.equal(saveFrenchVoiceId("fr", storage), false);
  assert.equal(saveFrenchVoiceProfile({ activeGender: "male" }, storage), false);
  assert.equal(saveFavorites(new Set(["fr:test:1"]), storage), false);
});
