import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import test from "node:test";

import {
  createChineseFallbackLoader,
  fallbackShardName,
} from "../src/chinese-fallback.js";

const dataRoot = new URL("../data/chinese-fallback/", import.meta.url);
const manifest = JSON.parse(await readFile(new URL("manifest.json", dataRoot), "utf8"));

test("the conservative fallback explains 臭屁 without presenting a direct translation", async () => {
  const filename = fallbackShardName("臭屁", manifest.shardCount);
  const shard = JSON.parse(await readFile(new URL(filename, dataRoot), "utf8"));
  const [record] = shard.entries["臭屁"];

  assert.equal(record.kind, "inferred");
  assert.equal(record.confidence, "strong");
  assert.equal(record.inferredFrom, "拿大");
  assert.ok(record.possibleFrench.includes("suffisant"));
  assert.ok(record.possibleFrench.includes("arrogant"));
});

test("the loader fetches only the manifest and one cached shard", async () => {
  const requested = [];
  const fetchImpl = async (url) => {
    requested.push(url);
    const filename = url.split("/").at(-1);
    const payload = await readFile(new URL(filename, dataRoot), "utf8");
    return { ok: true, json: async () => JSON.parse(payload) };
  };
  const lookup = createChineseFallbackLoader({ fetchImpl, base: "./fallback" });

  assert.ok((await lookup("臭屁")).length);
  assert.ok((await lookup("臭屁")).length);
  assert.equal(requested.length, 2);
  assert.equal(requested[0], "./fallback/manifest.json");
  assert.equal(requested[1], `./fallback/${fallbackShardName("臭屁", manifest.shardCount)}`);
});
