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

## Publication blocker

The connected GitHub identity is `kevindassie-ui` and GitHub reports owner/admin
permission on `kevindassie-ui/Pangmao`, but the GitHub connector reports no app
installation and rejects writes with HTTP 403 `Resource not accessible by
integration`. The repository is still empty until the GitHub app is authorised
for this repository.

## Resume sequence

1. Authorise the ChatGPT/Codex GitHub app for `kevindassie-ui/Pangmao`.
2. Publish the committed source tree to `main`.
3. Let `Android CI` rebuild the legal source datasets, run unit tests and lint,
   then assemble the debug APK.
4. On a failure, inspect the failing job log, patch locally, commit and rerun.
5. Download `Pangmao-MVP-APK`, verify its checksum and provide the installable APK.

The generated 66 MiB SQLite database is intentionally not committed. CI rebuilds
it from pinned, hash-verified CC-CEDICT, CFDICT, Tatoeba and Unihan sources.
