# Pangmao MVP — release status

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

## Verified data

- Dictionary integrity: 132,342 entries, 76,606 examples, 14,622 character records.

## Verified on GitHub Actions

- `main` is published at `kevindassie-ui/Pangmao`.
- Run `34635287544` passed dictionary validation, all eight unit tests, Android
  lint and debug APK assembly on commit `1a7a681`.
- Run `34636124452` passed the same checks after adding the release workflow on
  commit `b48d749`.
- A persistent RSA-4096 Android release key is stored only in encrypted GitHub
  Actions secrets, allowing later APKs to update the first installation.

## Published release

- Tag: `v0.1.0-mvp` at commit `b48d749`.
- Release run `34636691630` rebuilt the dictionary, ran tests and lint, assembled
  the release APK, and verified its Android signature successfully.
- APK: `Pangmao-v0.1.0-mvp.apk` (88,575,070 bytes).
- SHA-256: `07b8b2aeed103072a3a02f0c63c0fbb4091cc9e66cfa07ff85f1eb6511638081`.
- Download: <https://github.com/kevindassie-ui/Pangmao/releases/download/v0.1.0-mvp/Pangmao-v0.1.0-mvp.apk>

## Next iteration

Collect device feedback, screenshots and reproducible bug reports. Keep the same
application ID and GitHub signing secrets so future signed APKs update this install.

The generated 66 MiB SQLite database is intentionally not committed. CI rebuilds
it from pinned, hash-verified CC-CEDICT, CFDICT, Tatoeba and Unihan sources.
