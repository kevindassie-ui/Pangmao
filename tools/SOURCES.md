# Offline linguistic sources

Pangmao contains no Pleco data, code, models, visual assets, or private APIs.
The generated `pangmao.db` is an aggregation of independently licensed sources.

| Source | Purpose | Version in MVP | License / attribution |
|---|---|---|---|
| CC-CEDICT | Chinese–English headwords, pinyin and definitions | Mirror revision `3e29e175d6186f76a6978d8716d0976a2016923f` | © CC-CEDICT contributors, [CC BY-SA 4.0](https://creativecommons.org/licenses/by-sa/4.0/), source: [MDBG CC-CEDICT](https://www.mdbg.net/chinese/dictionary?page=cc-cedict) |
| CFDICT | Chinese–French definitions | Downloaded 2026-09-11 | © CFDICT / Chine Informations contributors, [CC BY-SA](https://creativecommons.org/licenses/by-sa/3.0/), source: [CFDICT](https://chine.in/mandarin/dictionnaire/CFDICT/) |
| Tatoeba | Authentic Mandarin–English sentence pairs | Export 2026-05-20 | © Tatoeba contributors, [CC BY 2.0 FR](https://creativecommons.org/licenses/by/2.0/fr/), source: [Tatoeba](https://tatoeba.org/) |
| Unicode Unihan | Character readings, radicals, stroke counts, variants and definitions | Unicode 17.0.0 | © 1991–2026 Unicode, Inc., [Unicode License v3](https://www.unicode.org/license.txt) |
| Pangmao editorial supplement | Reviewed definitions and bilingual examples for documented gaps | 0.4.0 | Original project content; source file: `tools/data/pangmao_examples.tsv` |

The CC-CEDICT and Tatoeba input files used for this build were obtained from
public, versioned GitHub mirrors so the build can be pinned and audited. The
original projects remain the attributed data authors. CFDICT and Unihan were
downloaded from their official download endpoints.

The database builder preserves attribution metadata inside the database and the
application exposes the same notices from its About screen.

The example builder normalizes Unicode and whitespace, then keeps one record
per Chinese sentence. When several Tatoeba translations share the same Chinese
text, the first pinned direct translation is retained. A Pangmao-reviewed row
with the same Chinese sentence replaces its target translations while retaining
separate source labels. Machine translation is never presented as an authentic
example.
