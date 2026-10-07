import assert from "node:assert/strict";
import test from "node:test";
import { readFileSync } from "node:fs";
import { createHash } from "node:crypto";
import { createTrialPlayer, validateManifest } from "../voice-trial/player.js";
import { withByteRange } from "../voice-trial/ranges.js";

const root = new URL("../voice-trial/", import.meta.url);
const manifest = JSON.parse(readFileSync(new URL("manifest.json", root), "utf8"));

test("both French trial voices ship intact and stay under the audio budget", () => {
  validateManifest(manifest);
  assert.deepEqual(manifest.voices.map((v) => [v.id, v.speakerId]), [["female", 0], ["male", 1]]);
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
