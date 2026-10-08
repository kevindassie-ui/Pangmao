import assert from "node:assert/strict";
import test from "node:test";
import { readFileSync } from "node:fs";
import { createHash } from "node:crypto";
import { execFileSync } from "node:child_process";
import { createTrialPlayer, validateManifest } from "../voice-trial/player.js";
import { withByteRange } from "../voice-trial/ranges.js";

const root = new URL("../voice-trial/", import.meta.url);
const manifest = JSON.parse(readFileSync(new URL("manifest.json", root), "utf8"));

function prepareTexts(texts) {
  return JSON.parse(execFileSync("python3", [new URL("../../tools/web_voice_text.py", import.meta.url).pathname],
    { input: JSON.stringify(texts), encoding: "utf8" }));
}

test("French clock times expand for speech without altering numbers or invalid times", () => {
  const input = ["à 18h30", "à 18 h 30", "à 18\u202fh\u202f30", "1H00", "23h59",
    "25h30 et 18h60", "h = 18 ; 30 euros", "18h300", "a18h30", "18h30a"];
  assert.deepEqual(prepareTexts(input).map((p) => p.spokenText),
    ["à 18 heures 30", "à 18 heures 30", "à 18 heures 30", "1 heure", "23 heures 59", ...input.slice(5)]);
});

test("the reviewed purchase phrase retains its wording and receives explicit pronunciation only there", () => {
  const input = ["Bonjour, je voudrais acheter une baguette.", "Je voudrais acheter.",
    "je voudrais prendre le train", "voudrais", "acheter", "je voudrais acheterais"];
  const output = prepareTexts(input);
  assert.deepEqual(output.map((p) => p.spokenText), input);
  assert.match(output[0].synthesisText, /\[\[ʒə vudʁˈɛ aʃətˈe\]\]/);
  assert.equal(output[0].pronunciationOverrides[0].text, "je voudrais acheter");
  assert.equal(output[1].pronunciationOverrides.length, 1);
  for (const p of output.slice(2)) {
    assert.equal(p.synthesisText, p.spokenText);
    assert.deepEqual(p.pronunciationOverrides, []);
  }
});

test("shipped corrected audio records expanded hours and the exact phrase phonemes for both voices", () => {
  const prepared = prepareTexts(manifest.samples.map((s) => s.text));
  manifest.samples.forEach((s, i) => {
    for (const key of ["spokenText", "synthesisText", "pronunciationOverrides"]) assert.deepEqual(s[key], prepared[i][key]);
  });
  const clock = manifest.samples.find((s) => s.id === "nombres");
  const purchase = manifest.samples.find((s) => s.id === "quotidien");
  assert.match(clock.text, /18 h 30/);
  assert.match(clock.spokenText, /18 heures 30/);
  for (const gender of ["female", "male"]) {
    assert.doesNotMatch(clock.clips[gender].phonemes.join(" "), /ˈaʃ/);
    assert.match(clock.clips[gender].phonemes.join(" "), /ˈœʁ/);
    assert.match(purchase.clips[gender].phonemes.join(" "), /vudʁˈɛ aʃətˈe/);
    assert.equal(purchase.clips[gender].generatedForVersion, "2026-10-08-v2");
  }
  const worker = readFileSync(new URL("sw.js", root), "utf8");
  assert.ok(worker.includes(`TRIAL_VERSION = "${manifest.version}"`));
});

test("new voices use the same prepared text and keep all generated PCM samples through MP3", () => {
  for (const sample of manifest.samples) {
    for (const id of ["siwis", "mls"]) {
      const clip = sample.clips[id];
      assert.equal(clip.generatedForVersion, manifest.version);
      assert.equal(clip.pcmFrames, clip.decodedFrames);
      assert.ok(clip.pcmFrames > 0);
      assert.ok(clip.phonemes.length > 0);
    }
  }
  assert.equal(manifest.voices.find((v) => v.id === "mls").profile, "unassigned");
  assert.equal(manifest.models.siwis.license, "CC-BY-4.0");
  assert.equal(manifest.models.mls.license, "CC-BY-4.0");
});

test("all French trial voices ship intact and stay under the audio budget", () => {
  validateManifest(manifest);
  assert.deepEqual(manifest.voices.map((v) => [v.id, v.speakerId]), [["female", 0], ["male", 1], ["siwis", 0], ["mls", 0]]);
  for (const sample of manifest.samples) {
    for (const clip of Object.values(sample.clips)) {
      const payload = readFileSync(new URL(clip.file, root));
      assert.equal(payload.length, clip.bytes);
      assert.equal(createHash("sha256").update(payload).digest("hex"), clip.sha256);
    }
  }
});

test("a corrupted manifest cannot load remote, mismatched or excessive audio", () => {
  for (const mutation of [
    (m) => { m.samples[0].clips.female.file = "https://example.org/collect.mp3"; },
    (m) => { m.samples[0].clips.female.file = "../other.mp3"; },
    (m) => { m.samples[0].clips.female.bytes = 900_000; },
    (m) => { m.samples[0].clips.female.sha256 = "bad"; },
    (m) => { m.samples[0].clips.female.durationSeconds = 0; },
    (m) => { m.samples[0].text = ""; },
    (m) => { m.samples[0].id = m.samples[1].id; },
  ]) {
    const corrupt = structuredClone(manifest);
    mutation(corrupt);
    assert.throws(() => validateManifest(corrupt));
  }
});

function fakeAudio(play) {
  return { src: "", events: {}, pauseCount: 0, preload: "", playbackRate: 1,
    addEventListener(name, listener) { this.events[name] = listener; },
    pause() { this.pauseCount++; }, removeAttribute() { this.src = ""; }, load() {}, play,
  };
}

test("tapping a second clip cancels the first and ignores its late playback rejection", async () => {
  let rejectFirst;
  let calls = 0;
  const audio = fakeAudio(() => ++calls === 1 ? new Promise((_, reject) => { rejectFirst = reject; }) : Promise.resolve());
  const states = [];
  const player = createTrialPlayer(audio, (state) => states.push(state));
  const first = player.play(manifest.samples[0], "female");
  await player.play(manifest.samples[0], "male");
  rejectFirst(new Error("old audio cancelled"));
  await first;
  assert.equal(states.at(-1).kind, "playing");
  assert.equal(states.at(-1).sample.gender, "male");
  assert.equal(states.filter((s) => s.kind === "error").length, 0);
  player.stop();
  assert.equal(audio.src, "");
  assert.equal(states.at(-1).kind, "stopped");
});

test("speed changes are bounded and Safari playback denial offers the native play control", async () => {
  const states = [];
  const audio = fakeAudio(() => Promise.reject(Object.assign(new Error("gesture required"), { name: "NotAllowedError" })));
  const player = createTrialPlayer(audio, (s) => states.push(s));
  assert.equal(player.setRate(0.85), 0.85);
  assert.equal(player.setRate(200), 1);
  assert.equal(player.setRate(1.15), 1.15);
  await player.play(manifest.samples[0], "female");
  assert.equal(audio.playbackRate, 1.15);
  assert.equal(audio.preservesPitch, true);
  assert.equal(states.at(-1).kind, "tap-play");
});

test("Safari ranges read the correct bytes from an offline cached audio file", async () => {
  const payload = Uint8Array.from([0, 1, 2, 3, 4, 5]);
  const make = () => new Response(payload, { headers: { "Content-Type": "audio/mpeg" } });
  for (const [range, expected, header] of [
    ["bytes=0-1", [0, 1], "bytes 0-1/6"],
    ["bytes=2-", [2, 3, 4, 5], "bytes 2-5/6"],
    ["bytes=-2", [4, 5], "bytes 4-5/6"],
    ["bytes=4-999", [4, 5], "bytes 4-5/6"],
  ]) {
    const result = await withByteRange(make(), range);
    assert.equal(result.status, 206);
    assert.equal(result.headers.get("Content-Range"), header);
    assert.deepEqual([...new Uint8Array(await result.arrayBuffer())], expected);
  }
  const whole = make();
  assert.equal(await withByteRange(whole, null), whole);
});

test("invalid or multiple audio ranges return 416 instead of malformed media", async () => {
  for (const range of ["bytes=9-10", "bytes=2-1", "bytes=-0", "bytes=-", "bytes=0-1,3-4", "bogus"]) {
    const result = await withByteRange(new Response(new Uint8Array(6)), range);
    assert.equal(result.status, 416);
    assert.equal(result.headers.get("Content-Range"), "bytes */6");
  }
});
