import assert from "node:assert/strict";
import test from "node:test";

import {
  availableVoices,
  formatFrenchVoiceDiagnostics,
  isFrenchVoice,
  listFrenchVoices,
  selectFrenchVoice,
  speakWithFrenchVoice,
  waitForFrenchVoice,
  voiceIdentifier,
} from "../src/tts.js";

const voices = [
  { name: "Mandarin", lang: "zh-CN", voiceURI: "zh", localService: true },
  { name: "French Canada", lang: "fr-CA", voiceURI: "fr-ca", localService: true },
  { name: "French France", lang: "fr_FR", voiceURI: "fr-fr", localService: true },
  { name: "French Cloud", lang: "fr-FR", voiceURI: "fr-cloud", localService: false },
];

test("French voice discovery excludes every Chinese voice", () => {
  assert.equal(isFrenchVoice(voices[0]), false);
  assert.deepEqual(listFrenchVoices(voices).map(voiceIdentifier), [
    "fr-fr",
    "fr-cloud",
    "fr-ca",
  ]);
});

test("an explicit French voice preference wins without accepting another language", () => {
  assert.equal(voiceIdentifier(selectFrenchVoice(voices, "fr-ca")), "fr-ca");
  assert.equal(selectFrenchVoice([voices[0]], "zh"), null);
});

test("voice inventory failures are treated as an empty device inventory", () => {
  assert.deepEqual(availableVoices(null), []);
  assert.deepEqual(availableVoices({ getVoices() { throw new Error("not ready"); } }), []);
});

test("voice diagnostics expose both saved profiles and only French candidates", () => {
  const diagnostics = formatFrenchVoiceDiagnostics(voices, {
    releaseVersion: "0.3.3",
    activeGender: "male",
    profileVoices: { female: "fr-fr", male: "fr-ca" },
  });
  assert.match(diagnostics, /Pangmao Web 0\.3\.3/);
  assert.match(diagnostics, /activeProfile=male/);
  assert.match(diagnostics, /femaleVoice=fr-fr/);
  assert.match(diagnostics, /maleVoice=fr-ca/);
  assert.match(diagnostics, /frenchVoiceCount=3/);
  assert.doesNotMatch(diagnostics, /Mandarin/);
});

test("voice discovery waits for Chrome-style delayed voiceschanged population", async () => {
  let currentVoices = [];
  let listener = null;
  const synth = {
    getVoices: () => currentVoices,
    addEventListener: (_event, callback) => { listener = callback; },
    removeEventListener: (_event, callback) => {
      if (listener === callback) listener = null;
    },
  };
  globalThis.setTimeout(() => {
    currentVoices = [voices[0], voices[2]];
    listener?.();
  }, 5);

  const result = await waitForFrenchVoice({
    synth,
    timeoutMs: 80,
    pollIntervalMs: 5,
  });

  assert.equal(voiceIdentifier(result), "fr-fr");
  assert.equal(listener, null);
});

test("voice discovery times out rather than selecting a Chinese default", async () => {
  const result = await waitForFrenchVoice({
    synth: { getVoices: () => [voices[0]] },
    timeoutMs: 5,
    pollIntervalMs: 5,
  });
  assert.equal(result, null);
});

test("speech always binds an advertised French voice before playback", () => {
  const calls = [];
  const synth = {
    getVoices: () => voices,
    cancel: () => calls.push("cancel"),
    resume: () => calls.push("resume"),
    speak: (utterance) => calls.push(["speak", utterance]),
  };
  class Utterance {
    constructor(text) { this.text = text; }
  }

  const result = speakWithFrenchVoice({
    synth,
    Utterance,
    text: "Bonjour",
    preferredIdentifier: "fr-ca",
  });

  assert.equal(result.ok, true);
  assert.equal(result.utterance.text, "Bonjour");
  assert.equal(result.utterance.voice.voiceURI, "fr-ca");
  assert.equal(result.utterance.lang, "fr-CA");
  assert.deepEqual(calls.map((call) => Array.isArray(call) ? call[0] : call), [
    "cancel",
    "resume",
    "speak",
  ]);
});

test("speech fails visibly instead of falling back to a Chinese voice", () => {
  let spoken = false;
  const result = speakWithFrenchVoice({
    synth: {
      getVoices: () => [voices[0]],
      cancel() {},
      speak() { spoken = true; },
    },
    Utterance: class {},
    text: "Bonjour",
  });
  assert.equal(result.ok, false);
  assert.equal(result.reason, "no-french-voice");
  assert.equal(spoken, false);
});
