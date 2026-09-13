# Offline linguistic sources

Pangmao contains no Pleco data, code, models, visual assets, or private APIs.
The generated `pangmao.db` is an aggregation of independently licensed sources.

| Source | Purpose | Version in MVP | License / attribution |
|---|---|---|---|
| CC-CEDICT | Chinese–English headwords, pinyin and definitions | Mirror revision `3e29e175d6186f76a6978d8716d0976a2016923f` | © CC-CEDICT contributors, [CC BY-SA 4.0](https://creativecommons.org/licenses/by-sa/4.0/), source: [MDBG CC-CEDICT](https://www.mdbg.net/chinese/dictionary?page=cc-cedict) |
| CFDICT | Chinese–French definitions | Downloaded 2026-09-11 | © CFDICT / Chine Informations contributors, [CC BY-SA](https://creativecommons.org/licenses/by-sa/3.0/), source: [CFDICT](https://chine.in/mandarin/dictionnaire/CFDICT/) |
| Tatoeba | Authentic Mandarin–English pairs and reviewed direct French relations | English export 2026-05-20; French subset 2026-09-12 | © Tatoeba contributors, [CC BY 2.0 FR](https://creativecommons.org/licenses/by/2.0/fr/), source: [Tatoeba](https://tatoeba.org/) |
| Unicode Unihan | Character readings, radicals, stroke counts, variants and definitions | Unicode 17.0.0 | © 1991–2026 Unicode, Inc., [Unicode License v3](https://www.unicode.org/license.txt) |
| Pangmao editorial supplement | Reviewed definitions and bilingual examples for documented gaps | 0.5.0 | Original project content; source files in `tools/data/` |
| FreeDict / WikDict `fra-zho` | French headwords, IPA, grammar and Chinese equivalents for the v0.6 French profile | 2025.11.23 | WikDict/Wiktionary/DBnary contributors, [CC BY-SA 3.0](https://creativecommons.org/licenses/by-sa/3.0/), [source archive](https://download.freedict.org/dictionaries/fra-zho/2025.11.23/) |
| FreeDict / WikDict `eng-zho` | English headwords, pronunciation, grammar and Chinese equivalents for the v0.6 English profile | 2025.11.23 | WikDict/Wiktionary/DBnary contributors, [CC BY-SA 3.0](https://creativecommons.org/licenses/by-sa/3.0/), [source archive](https://download.freedict.org/dictionaries/eng-zho/2025.11.23/) |

## Source candidate under editorial review

Kaikki's French-Wiktionary Chinese extract is not part of the generated
database. Snapshot `847718c8d03743b5f29b9c16263a67efa6c468bdf3b2ad666d5896f85cb9139e`
(2026-09-12 publication, based on the 2026-09-01 frwiktionary dump) was assessed
with `tools/evaluate_french_candidate.py`. The decision is to use only a small,
reviewed and attributed subset; the full post-processed download is deprecated
by Kaikki and contains extraction noise. See
[`docs/KAIKKI_FRENCH_SOURCE_EVALUATION.md`](../docs/KAIKKI_FRENCH_SOURCE_EVALUATION.md).

FreeDict/WikDict `fra-zho` and `eng-zho` 2025.11.23 passed the source gate and
are imported into isolated schema-v4 tables. The builder rejects empty and
non-Han equivalents, preserves homonyms and parts of speech, strips residual
pronunciation tags and retains at most four ordered variants. It does not merge
these rows into the established Chinese dictionary. The deterministic evaluator,
measured coverage and staged-import decision are documented in
[`docs/V0.6_LEARNING_SOURCE_EVALUATION.md`](../docs/V0.6_LEARNING_SOURCE_EVALUATION.md).

The CC-CEDICT and Tatoeba input files used for this build were obtained from
public, versioned GitHub mirrors so the build can be pinned and audited. The
original projects remain the attributed data authors. CFDICT and Unihan were
downloaded from their official download endpoints.

The database builder preserves attribution metadata inside the database and the
application exposes the same notices from its About screen.

Dictionary schema 4 records `CC-CEDICT` as the default source for English and
`CFDICT` as the default source for French. Only reviewed or secondary-source
attributions are stored as compact exceptions, referenced by definition index;
this preserves per-definition provenance without duplicating the definition
text. Reviewed rows use the strict format in
[`tools/data/reviewed_definitions.tsv`](data/reviewed_definitions.tsv).

The same schema stores French/English learning entries in normalized tables for
forms, pronunciations, senses and Chinese equivalents. Each sense points back to
its pinned FreeDict source and license; distinct source entries are never merged
merely because their spelling matches.

The example builder normalizes Unicode and whitespace, then keeps one record
per Chinese sentence. When several Tatoeba translations share the same Chinese
text, the first pinned direct translation is retained. A Pangmao-reviewed row
with the same Chinese sentence replaces its target translations while retaining
separate source labels. Machine translation is never presented as an authentic
example.

The official 2026-09-12 Tatoeba Mandarin and French sentence, direct-link and
sentence-review exports were evaluated without changing the database. Although
14,600 pinned Mandarin sentences have a direct French link, sentence reviews do
not validate translation fidelity or register. The 826 identity-checked,
positively reviewed candidates therefore formed a review queue rather than an
automatic import. Eighteen relations with at least two positive reviews passed
an additional relation-level editorial review and are now pinned in
`tools/data/tatoeba_french_examples.tsv`; all remaining candidates stay outside
the production database. Method, hashes and decision:
[`docs/TATOEBA_FRENCH_EXAMPLES_EVALUATION.md`](../docs/TATOEBA_FRENCH_EXAMPLES_EVALUATION.md).

## Reproduire l’audit de qualité

Après la construction de la base, exécuter:

```bash
python3 tools/audit_dictionary_quality.py \
  --json-out build/reports/dictionary-quality.json
```

Le rapport mesure la couverture français/anglais par fréquence interne, la
couverture bilingue des exemples, la provenance et un petit ensemble de
candidats de revue. Il ne modifie jamais la base. Sa méthodologie et la mesure
de référence v0.4.1 sont détaillées dans
[`docs/DICTIONARY_QUALITY_BASELINE.md`](../docs/DICTIONARY_QUALITY_BASELINE.md).
