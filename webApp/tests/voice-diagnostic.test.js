import assert from "node:assert/strict";
import test from "node:test";
import { readFileSync } from "node:fs";
import { createHash } from "node:crypto";
import { execFileSync } from "node:child_process";
import { validateDiagnostic, createDiagnosticPlayer } from "../voice-trial/diagnostic/player.js";

const root = new URL("../voice-trial/diagnostic/", import.meta.url);
const manifest = JSON.parse(readFileSync(new URL("manifest.json", root), "utf8"));

test("codec comparison shares one PCM source and explicit input uses existing dictionary IPA", () => {
  validateDiagnostic(manifest);
  const plans = JSON.parse(execFileSync("python3", ["tools/generate_web_voice_diagnostic.py", "--describe"],
    { cwd: new URL("../../", import.meta.url), encoding: "utf8" }));
  for (const [i, sample] of manifest.samples.entries()) {
    for (const field of ["text", "synthesisText", "explicitPhonemes", "referenceClips"]) assert.deepEqual(sample[field], plans[i][field]);
    if (sample.ipaSource) assert.deepEqual(sample.ipaSource, plans[i].ipaSource);
    for (const clips of Object.values(sample.clips)) {
      assert.equal(clips.mp3.sourcePcmSha256, clips.wav.sourcePcmSha256);
      for (const clip of Object.values(clips)) {
        const data = readFileSync(new URL(clip.file, root));
        assert.equal(data.length, clip.bytes);
        assert.equal(createHash("sha256").update(data).digest("hex"), clip.sha256);
      }
    }
  }
  execFileSync("python3", ["-c", "from pathlib import Path; from tools.verify_web_voice_trial import verify_diagnostic; verify_diagnostic(Path('webApp/voice-trial'))"],
    { cwd: new URL("../../", import.meta.url) });
});

test("changed source, unsafe paths and mismatched comparison conditions are rejected", () => {
  for (const mutation of [
    (m) => { m.samples[0].clips.male.wav.sourcePcmSha256 = "a".repeat(64); },
    (m) => { m.samples[0].clips.male.wav.file = "https://example.org/test.wav"; },
    (m) => { m.samples[0].clips.male.wav.pcmFrames++; },
    (m) => { m.samples[0].clips.male.wav.bytes = 3_000_000; },
    (m) => { delete m.samples[0].clips.male.explicit; },
    (m) => { m.samples[0].clips.male.extra = m.samples[0].clips.male.mp3; },
    (m) => { m.samples[1].id = "medecin"; },
  ]) {
    const m = structuredClone(manifest); mutation(m); assert.throws(() => validateDiagnostic(m));
  }
});

function fakeAudio(play) {
  return { src: "", pauseCount: 0, playbackRate: 1.15, events: {},
    addEventListener(name, fn) { this.events[name] = fn; }, pause() { this.pauseCount++; },
    removeAttribute() { this.src = ""; }, load() {}, play };
}

test("WAV plays from a tap at 1x and switching conditions cancels stale failures", async () => {
  let rejectFirst;
  let count = 0;
  const audio = fakeAudio(() => ++count === 1 ? new Promise((_, reject) => { rejectFirst = reject; }) : Promise.resolve());
  const states = [];
  const player = createDiagnosticPlayer(audio, (s) => states.push(s));
  const old = player.play(manifest.samples[0], "male", "mp3");
  await player.play(manifest.samples[0], "male", "wav");
  rejectFirst(new Error("cancelled")); await old;
  assert.equal(states.at(-1).kind, "playing"); assert.equal(states.at(-1).current.key, "wav");
  assert.match(audio.src, /current\.wav\?v=/); assert.equal(audio.playbackRate, 1);
  audio.events.ended(); assert.equal(states.at(-1).kind, "ended");
  player.stop(); audio.events.error(); assert.equal(states.at(-1).kind, "stopped"); assert.equal(audio.src, "");
});

test("Safari gesture denial keeps the selected diagnostic clip available in native controls", async () => {
  const states = [];
  const audio = fakeAudio(() => Promise.reject(Object.assign(new Error("gesture"), { name: "NotAllowedError" })));
  const player = createDiagnosticPlayer(audio, (s) => states.push(s));
  await player.play(manifest.samples[0], "female", "explicit");
  assert.equal(states.at(-1).kind, "tap-play"); assert.match(audio.src, /explicit\.wav/);
  assert.throws(() => player.play(manifest.samples[0], "unknown", "wav"));
});
