import assert from "node:assert/strict";
import test from "node:test";

import { WEB_VERSION, versionedAsset } from "../src/release.js";

test("release version and cache-busting URLs stay deterministic", () => {
  assert.equal(WEB_VERSION, "0.3.3");
  assert.equal(versionedAsset("./styles.css"), "./styles.css?v=0.3.3");
  assert.equal(versionedAsset("./data.json?part=1"), "./data.json?part=1&v=0.3.3");
});
