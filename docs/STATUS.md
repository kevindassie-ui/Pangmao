# Pangmao MVP — handoff status

Checkpoint date: 2026-09-11

## Implemented

- Offline Chinese dictionary search in Hanzi, pinyin, French and English.
- Simplified/traditional forms, tone-marked and tone-coloured pinyin.
- Entry details, authentic examples, character metadata and Android TTS.
- Tappable reader with longest-match dictionary segmentation.
- Camera and gallery OCR using the bundled ML Kit Chinese model.
- Local history, favourites and SRS flashcards.
- French-first Material 3 / Compose interface with no INTERNET permission.
- Reproducible dictionary builder and validator.
- GitHub Actions workflows for build, unit tests, lint and APK artifacts.

## Verified locally

- Dictionary integrity: 132,342 entries, 76,606 examples, 14,622 character records.
- Gradle 8.9 successfully evaluated the complete Android project and listed all
  Android build/test/lint tasks.
- The first full pipeline attempt failed only because AGP requested Build Tools
  34.0.0 while this environment had 35.0.0. The project now explicitly selects
  Build Tools 35.0.0.
- The second pipeline attempt passed SDK initialization, `preBuild`,
  `preDebugBuild`, `generateDebugBuildConfig`, and reached
  `checkDebugAarMetadata`. It was intentionally stopped to make this checkpoint;
  no source compilation failure had been reported.

## Verified on GitHub Actions

- `main` is published at `kevindassie-ui/Pangmao`.
- Run `34635287544` passed dictionary validation, all eight unit tests, Android
  lint and debug APK assembly on commit `1a7a681`.
- A persistent RSA-4096 Android release key is stored only in encrypted GitHub
  Actions secrets, allowing later APKs to update the first installation.

## Resume sequence

1. Commit and validate the stable release-signing workflow.
2. Tag the green commit as `v0.1.0-mvp`.
3. Let `Android APK Release` rebuild, test, lint, sign and verify the APK.
4. Download the release asset, verify its checksum and provide the installable APK.

The generated 66 MiB SQLite database is intentionally not committed. CI rebuilds
it from pinned, hash-verified CC-CEDICT, CFDICT, Tatoeba and Unihan sources.
