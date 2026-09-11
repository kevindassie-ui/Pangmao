# Pangmao MVP 0.1 — product scope

## Product promise

Pangmao should let one Mandarin learner move from an unknown written expression
to a useful, retainable explanation in a few taps, including when the source is
on paper or in an image. Core study paths work without a network connection.

## Public Pleco patterns retained

The public Android listing describes Pleco as an integrated dictionary, OCR,
document reader, handwriting input, pronunciation and flashcard system. The MVP
retains the most valuable cross-feature loop:

1. Search, paste, share, open a text file, point the camera, or choose an image.
2. Segment Chinese locally into the longest known words.
3. Tap a word for an immediate compact definition.
4. Open a dense bilingual entry with pinyin, examples and character data.
5. Favorite it or add it to spaced repetition in one tap.

The interface is independently designed with Material 3 and Pangmao branding.
No Pleco screens, assets, private behavior, dictionaries or code were copied.

## Included

- Offline simplified/traditional Chinese headword search.
- Tone-insensitive pinyin search with numbered or marked input.
- French and English definition full-text search.
- 132k+ merged entries from CC-CEDICT and CFDICT.
- Pinyin with tone marks and tone colors.
- 76k+ authentic Tatoeba Mandarin–English example pairs.
- Unihan readings, radical/stroke data and variants for 14k+ characters.
- Android system Mandarin text-to-speech when a local voice is installed.
- Live on-device Chinese OCR using the bundled ML Kit model.
- OCR for an image selected from the phone.
- Text reader with share/process-text entry points and `.txt` import.
- Local history, favorites, flashcards and a simple SM-2-style review loop.
- No ads, analytics, account, cloud sync or Internet permission.

## Deliberately deferred

- Dedicated handwriting recognizer and stroke-order animation.
- PDF/EPUB layout-aware document reader.
- Floating screen reader over other apps (sensitive accessibility/overlay scope).
- Cantonese, Zhuyin and recorded native audio packs.
- User dictionary import/export and advanced flashcard configuration.
- OCR bounding-box word selection and manual crop/rotation controls.

These are candidates for phone-tested iterations, not prerequisites for a
coherent first install.

