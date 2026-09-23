# Pangmao notices

Pangmao is an independent language-learning project with an Android application
and a separately built Web client. It is not affiliated with, endorsed by, or
derived from Pleco Software. “Pleco” is used in project documentation only to
describe the category of experience studied from public product information.

The offline dictionary database bundles CC-CEDICT, CFDICT, Tatoeba, Unicode
Unihan and FreeDict/WikDict `fra-zho` and `eng-zho` data. The FreeDict-derived
learning dictionaries are distributed under CC BY-SA 3.0. Full provenance and
license links are in `tools/SOURCES.md` and in the About/Licences surface of
each client that distributes the corresponding data.

The French-learning Web pack is derived only from sources explicitly listed in
its own versioned manifest. Its core FreeDict data is complemented by an exact,
filtered subset of French entries from Chinese Wiktionary, extracted through
Kaikki/Wiktextract and distributed under CC BY-SA 4.0. Chinese fallback results
derive from the already attributed CC-CEDICT and CFDICT data; indirect semantic
bridges are visibly labelled as possible meanings. The Web client preserves
source, revision, licence and attribution metadata and does not silently inherit
every source present in the larger Android database.

Google ML Kit's bundled Chinese text recognition model is used by the Android
application for on-device OCR under the applicable Google ML Kit terms. Android
and Jetpack components retain their respective notices. Optional Google ML Kit
Digital Ink and Translation models are downloaded only after an explicit user
action and then run on-device under the applicable Google ML Kit terms.

Stroke-order vector paths and medians come from Hanzi Writer Data / Make Me a
Hanzi, derived from fonts © 1999 Arphic Technology Co., Ltd. and redistributed
under the Arphic Public License. Pangmao converts the pinned source JSON into a
per-character zlib-compressed SQLite database; the source, transformation tools
and full unaltered licence are included with the project and application.
