# Pangmao · 胖猫

Pangmao is a private, offline-first Android Chinese dictionary and learning
companion. It combines a large bilingual dictionary, a tappable text reader,
on-device Chinese OCR and lightweight spaced repetition in one independent app.

## Version 0.2 highlights

- 132,000+ Chinese entries with simplified/traditional forms and pinyin.
- French (CFDICT) and English (CC-CEDICT) definitions.
- 76,000+ authentic Mandarin–English examples from Tatoeba.
- Unihan character, radical, stroke and variant information.
- Live camera OCR and image OCR with an ML model bundled in the APK.
- Tappable OCR regions for choosing the exact line to translate.
- Single-character Chinese handwriting input with an on-demand offline model.
- French, English and Simplified Chinese interfaces; system/light/dark themes.
- Favorites, history and local flashcards.
- No account, ads, analytics, server or subscription. Internet is used only to
  download the optional handwriting model once; all language features then run locally.

## Install

Download the APK attached to the latest GitHub prerelease, open it on Android,
and allow installation from the browser or GitHub app when Android asks. Use the
smaller `arm64` APK on recent Android phones; the `universal` APK is the fallback
for other devices. Both are signed with Pangmao's persistent personal release
key, so later versions can be installed as updates without the Play Store.

## Build

The build downloads pinned, checksum-verified public linguistic sources and
normal free Maven dependencies:

```bash
tools/fetch_and_build_dictionary.sh
./gradlew testDebugUnitTest lintDebug assembleDebug
```

Every push to `main` runs data validation, unit tests, Android lint and a debug
APK build. A `v*` tag publishes tested ARM64 and universal APKs as a GitHub
prerelease.

## Architecture

- Kotlin, Jetpack Compose and Material 3.
- MVVM with `StateFlow` and repositories.
- Immutable, pre-indexed SQLite dictionary asset.
- Separate Room database for user-owned favorites, history and SRS state.
- CameraX plus bundled ML Kit Chinese Text Recognition v2.
- Minimum Android 8.0 (API 26), target Android 15 (API 35).

See the [changelog](CHANGELOG.md), [product roadmap](docs/ROADMAP.md),
[product decisions](docs/PRODUCT_DECISIONS.md), [MVP scope](docs/MVP_SCOPE.md),
[source data and licenses](tools/SOURCES.md), and [notices](NOTICE.md).

Pangmao is independent and unaffiliated with Pleco Software. It contains no
proprietary Pleco content.
