# Pangmao · 胖猫

Pangmao is a private, offline-first Android Chinese dictionary and learning
companion. It combines a large bilingual dictionary, a tappable text reader,
on-device Chinese OCR and lightweight spaced repetition in one independent app.

## Version 0.10.0 highlights

- Reader in explicit `Reading` and `Editing` modes: analysis is refreshed once
  editing is finished, while native cursor and selection remain available.
- Tap any sentence in the Chinese text to select and highlight the next TTS
  starting point; tapping during playback jumps there immediately.
- Long-press a word in the Chinese text to open its dictionary preview directly.
- The Chinese text stays visible while secondary Reader controls scroll away,
  leaving substantially more room for pinyin, translations and segmented reading.
- Text vocabulary coverage now follows the translation it describes.
- Dictionary entries expose a full-width card action and explain why vocabulary
  status and spaced-repetition cards remain independent.
- Private development builds are ARM64-only and reuse a source-keyed dictionary
  cache; a Play-ready App Bundle can restore other architectures later.

- Independent local statuses `Learning`, `Known` and `Unmarked` for Chinese
  words, without changing favorites or SRS cards.
- Reader vocabulary coverage that separates occurrences, distinct words,
  unknown blocks and profile completeness instead of claiming a CEFR level.
- Optional highlighting of words to review and a compact coverage detail view.
- Compact previous/current/next sentence navigation with direct access to the
  complete sentence list.
- Dedicated `Learning`, `Known` and `Favorites` lists inside Cards, with counts
  and direct access to dictionary entries.

- Compact four-stop speech-speed slider; voice test and engine details remain
  available from a single overflow menu without hiding Reader content.

- Interactive Chinese text-to-speech with precise pause/resume when supported,
  sentence fallback, restart, stop and playback from any selected sentence.
- Active-sentence highlighting, optional spoken-range tracking and persistent
  `0.6×`, `0.8×`, `1×` and `1.2×` speeds.
- Native long-press text selection followed by an explicit definition action:
  exact entries open directly and longer selections expose recognized blocks.
- One-tap copy for the full text, tone-marked pinyin, each translation, hanzi,
  pinyin and visible word definitions.
- Voice test and an inspectable panel for the active Android TTS engine, voice,
  locale and offline/network requirement.
- Add or remove a selected Reader word from local SRS cards without opening
  its full entry; repeated additions preserve the existing review schedule.

- Independent learning profile: Chinese, French or English, regardless of the
  app interface and Chinese-definition language.
- New offline French–Chinese dictionary with 10,923 filtered lexical entries,
  including available IPA, grammatical gender, forms and distinct senses.
- New offline English–Chinese dictionary with 26,549 filtered lexical entries,
  including available pronunciation, forms, grammar and distinct senses.
- Accent-insensitive French lookup, exact/prefix/full-text search and reverse
  lookup from a Chinese meaning.
- Profile-specific lexical cards with tappable links back to matching Chinese
  entries; the established Chinese Reader remains unchanged.

- 132,342 Chinese entries with simplified/traditional forms and pinyin.
- French (CFDICT) and English (CC-CEDICT) definitions with compact source
  provenance and a deterministic bilingual-quality audit.
- 64,912 unique Chinese example sentences, including a first reviewed set with
  aligned French and English translations.
- Unihan character, radical, stroke and variant information.
- Live camera OCR and image OCR with an ML model bundled in the APK.
- Tappable OCR regions for choosing the exact line to translate.
- Automatic single-character handwriting recognition with an on-demand offline model.
- Context-aware analysis of expressions and sentences into logical word blocks.
- Optional linear word-by-word interpretation, independently of the logical blocks.
- Structured reader with pinyin, definitions and optional on-device translations.
- Tappable characters and words, dictionary tabs and a related-word explorer with
  position, frequency and pinyin sorting filters.
- Persistent choice of French, English or both definition languages.
- Tone-coloured hanzi and separate histories for searches and viewed entries.
- Android Chinese text-to-speech with installed-engine discovery and fallback.
- French, English and Simplified Chinese interfaces; system/light/dark themes.
- Favorites, history and local flashcards.
- No account, ads, analytics, server or subscription. Internet is used only to
  download optional handwriting and translation models; processing then stays local.

## Install

Download [Pangmao v0.10.0](https://github.com/kevindassie-ui/Pangmao/releases/tag/v0.10.0),
open the APK on Android, and allow installation from the browser or GitHub app
when Android asks. Private development releases target recent ARM64 phones. A
Play-ready Android App Bundle can restore all supported architectures later
without changing the application ID or signing key. Releases use Pangmao's
persistent personal key, so later versions can be installed as updates without
the Play Store.

## Build

The build downloads pinned, checksum-verified public linguistic sources and
normal free Maven dependencies:

```bash
tools/fetch_and_build_dictionary.sh
tools/fetch_and_build_strokes.sh
./gradlew testDebugUnitTest lintDebug assembleDebug
```

Every push to `main` runs data validation, unit tests, Android lint and a debug
APK build. During private development, a `v*` tag publishes one tested and
signed ARM64 APK as a GitHub release.

## Architecture

- Kotlin, Jetpack Compose and Material 3.
- MVVM with `StateFlow` and repositories.
- Immutable, pre-indexed SQLite dictionary asset.
- Separate compressed SQLite asset for offline vector stroke-order data.
- Separate Room database for user-owned favorites, history and SRS state.
- CameraX plus bundled ML Kit Chinese Text Recognition v2.
- Optional ML Kit Digital Ink and on-device Translation models.
- Minimum Android 8.0 (API 26), target Android 15 (API 35).

See the [changelog](CHANGELOG.md), [product roadmap](docs/ROADMAP.md),
[product decisions](docs/PRODUCT_DECISIONS.md), [MVP scope](docs/MVP_SCOPE.md),
[source data and licenses](tools/SOURCES.md), and [notices](NOTICE.md).

Pangmao is independent and unaffiliated with Pleco Software. It contains no
proprietary Pleco content.
