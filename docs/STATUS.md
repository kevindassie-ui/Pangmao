# Pangmao — release status

Checkpoint date: 2026-09-14

## Implemented

- Offline Chinese dictionary search in Hanzi, pinyin, French and English.
- Simplified/traditional forms, tone-coloured hanzi and tone-marked pinyin.
- Entry details, authentic examples, character metadata and Android TTS.
- Context-aware search and reader segmentation with global and block-by-block
  interpretation, pinyin and definitions.
- Optional on-device sentence translation after explicit model download.
- Camera and gallery OCR using the bundled ML Kit Chinese model, with tappable
  text-region selection.
- Pleco-like handwriting canvas with safe automatic offline recognition after
  the optional, one-time Chinese handwriting model download.
- Separate local histories for searches and viewed entries, favourites and SRS
  flashcards.
- Rounded Material 3 / Compose interface with Porcelaine light and Sceau de nuit
  dark palettes.
- App language selection (system, French, English or Simplified Chinese) and
  theme selection (system, light or dark), stored locally.
- Persistent choice of French, English or both definition languages.
- Consistent reader actions, including a compact clear control inside the text
  field.
- Reproducible dictionary builder and validator.
- GitHub Actions workflows for build, unit tests, lint and APK artifacts.

## Verified data

- Dictionary integrity: 132,342 entries, 64,912 unique Chinese examples and
  14,622 character records.
- Thirty-eight reviewed examples currently provide aligned French and English;
  broader bilingual coverage is tracked for v0.5.0.

## Verified on GitHub Actions

- `main` is published at `kevindassie-ui/Pangmao`.
- Run `34635287544` passed dictionary validation, all eight unit tests, Android
  lint and debug APK assembly on commit `1a7a681`.
- Run `34636124452` passed the same checks after adding the release workflow on
  commit `b48d749`.
- A persistent RSA-4096 Android release key is stored only in encrypted GitHub
  Actions secrets, allowing later APKs to update the first installation.
- Phase 2 run `34659129708` passed dictionary validation, all eleven unit tests,
  Android lint and debug APK assembly on commit `1c92f89`.
- Final Phase 2 CI run `34659579861` passed the same checks and uploaded the
  debug APK on release commit `caaf6c1`.

## Previous release

- Tag: `v0.1.0-mvp` at commit `b48d749`.
- Release run `34636691630` rebuilt the dictionary, ran tests and lint, assembled
  the release APK, and verified its Android signature successfully.
- APK: `Pangmao-v0.1.0-mvp.apk` (88,575,070 bytes).
- SHA-256: `07b8b2aeed103072a3a02f0c63c0fbb4091cc9e66cfa07ff85f1eb6511638081`.
- Download: <https://github.com/kevindassie-ui/Pangmao/releases/download/v0.1.0-mvp/Pangmao-v0.1.0-mvp.apk>

## Phase 2 publication

- Tag: `v0.2.0` at commit `caaf6c1`.
- Release run `34659758215` rebuilt the dictionary, ran all tests and lint,
  assembled the release APK, and verified its Android signature successfully.
- APK: `Pangmao-v0.2.0.apk` (118,716,908 bytes).
- SHA-256: `d64365f5208ad146adb1c39106a90a45b6b5e4d20d6e7791b7c090c6f14c65ee`.
- Download: <https://github.com/kevindassie-ui/Pangmao/releases/download/v0.2.0/Pangmao-v0.2.0.apk>

## Corrective release 0.2.1

- Tag: `v0.2.1` at commit `eda8183`.
- Implementation CI run `34679865764` and final CI run `34680165813` passed
  dictionary validation, unit tests, Android lint and debug APK assembly.
- Release run `34680206681` repeated validation and tests, built both release
  variants, and verified their Android signatures successfully.
- ARM64 APK: `Pangmao-v0.2.1-arm64.apk` (66,857,764 bytes), SHA-256
  `a5aa63a8c49be59419554a26ea4d728e88f99a388366a4cea388fb7caf0c03b9`.
- ARM64 download: <https://github.com/kevindassie-ui/Pangmao/releases/download/v0.2.1/Pangmao-v0.2.1-arm64.apk>
- Universal APK: `Pangmao-v0.2.1-universal.apk` (119,370,092 bytes), SHA-256
  `f282b83e398dc328271a597b322e32d7d8a3a08a14acf5f7c507445bb59fbf4f`.
- Universal download: <https://github.com/kevindassie-ui/Pangmao/releases/download/v0.2.1/Pangmao-v0.2.1-universal.apk>
- Device acceptance completed: the handwriting crash is resolved and the
  `zh-Hans` interface switches correctly.

## Text understanding release 0.3.0

- Tag: `v0.3.0` at commit `3a39d88`.
- CI run `34687560717` passed dictionary validation, unit tests, Android lint
  and debug APK assembly.
- Release run `34687861761` repeated validation and tests, built both release
  variants, and verified their Android signatures successfully.
- ARM64 APK: `Pangmao-v0.3.0-arm64.apk` (83,416,414 bytes), SHA-256
  `8ea26d99791256e63c7d9023a97c760cfa5642918381f926a498b673125a28a6`.
- ARM64 download: <https://github.com/kevindassie-ui/Pangmao/releases/download/v0.3.0/Pangmao-v0.3.0-arm64.apk>
- Universal APK: `Pangmao-v0.3.0-universal.apk` (182,192,068 bytes), SHA-256
  `088b4e92af8b556b8dcfd500daea8a0efb7575977f7ddb510ccac643e371e069`.
- Universal download: <https://github.com/kevindassie-ui/Pangmao/releases/download/v0.3.0/Pangmao-v0.3.0-universal.apk>
- Device acceptance remains to be completed for automatic handwriting,
  segmentation, model download and the new reader workflow.

## Reader quality release 0.3.1

- Tag: `v0.3.1` at commit `fe7874f`.
- CI run `34689603981` passed dictionary validation, unit tests, Android lint
  and debug APK assembly.
- Release run `34689854701` repeated validation and tests, built both variants,
  and verified their Android signatures successfully.
- ARM64 APK: `Pangmao-v0.3.1-arm64.apk` (83,416,402 bytes), SHA-256
  `c4880e964c76a4ded198688efebd1811cf39465e0c7c8e430205cd0bfd29956a`.
- ARM64 download: <https://github.com/kevindassie-ui/Pangmao/releases/download/v0.3.1/Pangmao-v0.3.1-arm64.apk>
- Universal APK: `Pangmao-v0.3.1-universal.apk` (182,192,056 bytes), SHA-256
  `bd461e6ab4ea1df4a914fef53c51afcd2c4f376515a0475d6949e4dd885c44b6`.
- Universal download: <https://github.com/kevindassie-ui/Pangmao/releases/download/v0.3.1/Pangmao-v0.3.1-universal.apk>
- Device acceptance confirms OCR, handwriting, general dictionary, FR/EN/both
  definitions, multiword search and the compact reader are functional.
- Follow-up observations are recorded for v0.4.0: re-centre the reader when
  pinyin is re-enabled, make word-by-word analysis optional, continue improving
  French phrasing, move OCR to the home actions and expose TTS failures.

## Dictionary depth release 0.4.0

- Tag: `v0.4.0` at commit `9eaafa9`.
- Implementation CI run `34702542913` and final CI run `34702712090` passed
  full dictionary validation, unit tests, Android lint and debug APK assembly.
- Release run `34703031162` repeated validation and tests, built both release
  variants, verified their Android v2 signatures and published the assets.
- ARM64 APK: `Pangmao-v0.4.0-arm64.apk` (84,475,458 bytes), SHA-256
  `440e93a9caa1d5e1bfdd4c992463f663c94021a2c12086e5c972342507f7c3da`.
- ARM64 download: <https://github.com/kevindassie-ui/Pangmao/releases/download/v0.4.0/Pangmao-v0.4.0-arm64.apk>
- Universal APK: `Pangmao-v0.4.0-universal.apk` (183,251,112 bytes), SHA-256
  `713d736b26eddfbf14e5f1f0c5cbd2fe869efc9448a94499e925ab029e1ed160`.
- Universal download: <https://github.com/kevindassie-ui/Pangmao/releases/download/v0.4.0/Pangmao-v0.4.0-universal.apk>
- Device acceptance remains to be completed for the new tabs, character
  navigation, word explorer, OCR relocation, pinyin repositioning and TTS
  diagnostic.

## Corrective release 0.4.1

- Tag: `v0.4.1` at commit `295a709`.
- Implementation CI run `34725277909` and final CI run `34725441325` passed
  full dictionary validation, unit tests, Android lint and debug APK assembly.
- Release run `34725716490` repeated validation and tests, built both release
  variants, verified their Android v2 signatures and published the assets.
- ARM64 APK: `Pangmao-v0.4.1-arm64.apk` (84,475,982 bytes), SHA-256
  `e9e5cde8998453dffb5f1783e60ab0e6174583a82db9bfc28de83000e2927306`.
- ARM64 download: <https://github.com/kevindassie-ui/Pangmao/releases/download/v0.4.1/Pangmao-v0.4.1-arm64.apk>
- Universal APK: `Pangmao-v0.4.1-universal.apk` (183,251,636 bytes), SHA-256
  `19933c7588bb8e321264710aed0816f8b8dff368c47b579424858aadc4713e46`.
- Universal download: <https://github.com/kevindassie-ui/Pangmao/releases/download/v0.4.1/Pangmao-v0.4.1-universal.apk>
- Device acceptance remains to be completed for multiline hanzi, the existing
  Chinese TTS voice and `肉夹馍` in both translation surfaces.

## Bilingual quality baseline — v0.5 lot A

- Commit: `d016d31`.
- CI run `34727407033` passed the deterministic bilingual audit and its tests,
  full dictionary reconstruction and validation, Android unit tests, lint and
  debug APK assembly.
- Baseline: 47,071 of 132,342 lexical entries are bilingual; coverage rises to
  81.65% among entries observed at least five times in the bundled corpus.
- Examples are the main measured gap: the C2 supplement raised the baseline
  from 12 to 20 of 64,912 unique Chinese sentences with both French and English.
- This checkpoint changes neither the generated dictionary nor the APK. Lot B
  will evaluate candidate direct Chinese-French sources before any import.

## Bilingual source and data checkpoints — v0.5 lots B–D0

- Kaikki / Wiktionnaire français was limited to reviewed candidates; the full
  extraction is not safe for automatic ingestion.
- Schema 3 records compact per-definition provenance without increasing the
  database size. CI run `34742487247` passed on commit `13fff9b`.
- C2 added 77 reviewed French definitions for 46 frequent entries and eight
  aligned examples. CI run `34742814129` passed on commit `7c198dc`.
- D0 inspected the complete official Tatoeba link graph. Of 14,600 direct
  Mandarin–French alignments, 826 pass the identity and sentence-review gate,
  but they still require relation-level editorial review before import.
- D1 accepted 18 of the 23 strongest candidates after relation-level review.
  The other five remain excluded for register, intensity, fidelity or
  near-duplication. The bilingual example count is now 38 of 64,912. CI run
  `34755490616` passed on commit `83246fc`.

## Corpus quality release 0.5.0

- Tag: `v0.5.0` at commit `234576f`.
- Final CI run `34755749553` passed dictionary reconstruction and validation,
  all 16 Python tests, Android unit tests, lint and debug APK assembly.
- Release run `34756006883` repeated the complete validation, restored the
  persistent signing key, built both variants and verified their Android
  signatures before publication.
- ARM64 APK: `Pangmao-v0.5.0-arm64.apk` (84,483,074 bytes), SHA-256
  `9ef08ae5cc9cd6916d0a70b2cc6067206c66ae6cec05c53fe9bfdb41b7480a29`.
- ARM64 download: <https://github.com/kevindassie-ui/Pangmao/releases/download/v0.5.0/Pangmao-v0.5.0-arm64.apk>
- Universal APK: `Pangmao-v0.5.0-universal.apk` (183,258,728 bytes), SHA-256
  `cf8706f17c358f2c1035a230544ed695104fed316e468859087b528abb082873`.
- Universal download: <https://github.com/kevindassie-ui/Pangmao/releases/download/v0.5.0/Pangmao-v0.5.0-universal.apk>

## Corrective release 0.5.1

- Tag: `v0.5.1` at commit `a22c095`.
- Implementation CI run `34761953644` and final CI run `34762205792` passed
  dictionary validation, Python and Android unit tests, lint and debug APK
  assembly.
- Release run `34762555388` repeated the complete validation, restored the
  persistent signing key, built both variants and verified their Android
  signatures before publication.
- ARM64 APK: `Pangmao-v0.5.1-arm64.apk` (84,483,134 bytes), SHA-256
  `06f9b7af79239c0f080658611af406fe6279123956e29a4994167fadf7cfe40d`.
- ARM64 download: <https://github.com/kevindassie-ui/Pangmao/releases/download/v0.5.1/Pangmao-v0.5.1-arm64.apk>
- Universal APK: `Pangmao-v0.5.1-universal.apk` (183,258,788 bytes), SHA-256
  `90c4ccf9b6dbf4b790c52daa81fd0913522cb48b4a87a42c0ca64edfc0bf856c`.
- Universal download: <https://github.com/kevindassie-ui/Pangmao/releases/download/v0.5.1/Pangmao-v0.5.1-universal.apk>
- Device acceptance confirms that TTS now works on the reported ColorOS device
  and that logical blocks remain visible independently of the optional linear
  word gloss. Acceptance of the reviewed `才` sentence remains to be recorded.

## Learning profiles — v0.6 foundation

- Commit `19e667e` adds a persistent learning-language axis independent from
  the app locale and definition preference. Existing installations migrate to
  `CHINESE`; CI run `34770974051` passed migration tests, lint and assembly.
- Commits `e78fada` and `15a3f2c` add the translated home selector and honest
  bilingual-preview labels. CI run `34771566996` passed all checks.
- Commit `da94620` adds a non-mutating evaluation of direct FreeDict/WikDict
  sources. The measured snapshots contain 10,947 French-to-Chinese and 26,660
  English-to-Chinese entries; they qualify for a filtered pilot, not a raw
  import. CI run `34772064048` passed 18 data tests, Android tests, lint and
  debug APK assembly.
- Schema v4 and the profile-driven Android screens are published in v0.6.0;
  device acceptance remains to be recorded.

## Learning dictionary schema — v0.6 lot D1

- Schema v4 adds isolated, normalized tables for French/English lexical entries,
  forms, pronunciations, senses, Chinese equivalents and per-sense sources.
- The pinned filtered build imports 10,923 French and 26,549 English entries;
  166 source entries without a usable Chinese sense and 144 non-Han values are
  rejected deterministically.
- The existing 132,342 Chinese entries and stable identifiers are unchanged.
- The generated database grows from about 69 MiB to 92.34 MiB. Profile-driven
  Android search and entry rendering are now published; device QA remains.
- Commit `ccd7549` passed the full GitHub Actions run `34781393394`, including
  pinned-source reconstruction, 21 Python tests, Android tests, lint and debug
  APK assembly.
- Detailed measurements and invariants are recorded in
  [V0.6_SCHEMA_V4_REPORT.md](V0.6_SCHEMA_V4_REPORT.md).
- Commit `0f77fba` adds the Android learning-entry models and exact/prefix/FTS
  repository queries without changing visible behavior. Accented and Hanzi
  reverse queries are covered; CI run `34781815388` passed all data and Android
  checks.
- Commit `20db749` activates real offline French/English searches and lexical
  entry screens. It keeps the Chinese Reader unchanged and deliberately leaves
  favorites/cards disabled for the new entries until identifiers are safely
  namespaced. CI run `34786545793` passed corpus reconstruction, Android tests,
  lint and debug APK assembly.

## Learning profiles release 0.6.0

- Tag: `v0.6.0` at commit `b4a6eb0`.
- Final CI run `34812967331` passed reconstruction of all six pinned sources,
  dictionary validation, 21 Python tests, Android unit tests, lint and debug APK
  assembly.
- Release run `34813530300` repeated the complete validation, restored the
  persistent signing key, built both variants and verified their Android
  signatures before publication.
- ARM64 APK: `Pangmao-v0.6.0-arm64.apk` (95,229,198 bytes), SHA-256
  `f6e2532ff4bc378e19c68d73e1b3f83c300679a0b96a6b626c0ed72a85252334`.
- ARM64 download: <https://github.com/kevindassie-ui/Pangmao/releases/download/v0.6.0/Pangmao-v0.6.0-arm64.apk>
- Universal APK: `Pangmao-v0.6.0-universal.apk` (194,004,852 bytes), SHA-256
  `5e14e097d2c74009c34f9da85226e508d9c5884846d99754773f0da3eaefd02e`.
- Universal download: <https://github.com/kevindassie-ui/Pangmao/releases/download/v0.6.0/Pangmao-v0.6.0-universal.apk>
- Device acceptance remains to cover upgrade from v0.5.1, profile persistence,
  accent-insensitive French lookup, English lookup and Hanzi reverse lookup.

## Later iteration

Exhaustive bilingual corpus alignment, richer authentic examples, advanced
audio controls and interactive dictionary-source details are intentionally
deferred. Collect device feedback, screenshots and reproducible bug reports.
Keep the same application ID and GitHub signing secrets so future signed APKs
update this install.

The v0.6 learning-profile release is complete. Offline/online services,
short-form STT, reusable meeting-transcription components, commercialization
and iOS remain recorded for later work rather than mixed into that release.

The v0.2.1 corrective scope and remaining device acceptance are recorded in
[NEXT_RELEASE.md](NEXT_RELEASE.md). Longer-term product work is tracked in
[ROADMAP.md](ROADMAP.md), with accepted trade-offs recorded in
[PRODUCT_DECISIONS.md](PRODUCT_DECISIONS.md). The next isolated data-quality
milestones are specified in [V0.5_RELEASE_PLAN.md](V0.5_RELEASE_PLAN.md).
Confirmed device regressions awaiting a later corrective release are kept in
[KNOWN_ISSUES.md](KNOWN_ISSUES.md).

The generated 92.34 MiB SQLite database is intentionally not committed. CI
rebuilds it from pinned, hash-verified CC-CEDICT, CFDICT, Tatoeba, Unihan and
FreeDict/WikDict sources.
