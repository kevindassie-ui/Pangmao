# Évaluation des exemples français Tatoeba

Date de l'évaluation: 2026-09-13

Décision: **LIMITER — constituer une file de revue, ne pas importer en masse**

## Question de qualité

Le corpus Pangmao contient 64 902 identifiants de phrases mandarin distincts
issus de l'export Tatoeba du 20 mai 2026. L'objectif est de compléter leur
traduction française sans passer par l'anglais, sans rapprochement approximatif
et sans présenter une paire non relue comme une traduction garantie.

L'unité analysée est donc une relation directe:

`identifiant mandarin épinglé → lien Tatoeba direct → identifiant français`

## Sources reproductibles

Les exports officiels Tatoeba sont publiés chaque semaine et annoncés sous
licence CC BY 2.0 FR sur la [page de téléchargement](https://tatoeba.org/en/downloads).
La page précise aussi que les avis de phrases sont expérimentaux.

| Fichier | Instantané utilisé | SHA-256 |
|---|---:|---|
| Paires mandarin–anglais déjà épinglées | 2026-05-20 | `430a2c57b78a7ae57e28d5d70c4ce9c4fca1a1680279d8685a89b4caa6f3bf7c` |
| `cmn_sentences.tsv.bz2` | export hebdomadaire 2026-09-12 | `dd97bb3f76c323e0a9f25b899cdb5309d6d7a7a67bcead6447616a5de1082a89` |
| `fra_sentences.tsv.bz2` | export hebdomadaire 2026-09-12 | `7d7c2b46ffd43c738c03c866bcfdcff08ea0d9b69f33985bb0cf309ce8dc0a2d` |
| `links.tar.bz2` | export hebdomadaire 2026-09-12 | `88f8d9032f2ca6010a65c50237bb690a1334d8281c67d884385f63b021c81d91` |
| `users_sentences.csv` | export hebdomadaire 2026-09-12 | `9347f716703975cc0a169884b98b03fbcd0eac8add02d41d6e628fbca46d4882` |

Le mélange de deux dates est contrôlé explicitement: une paire n'est retenue
comme candidate que si l'identifiant mandarin existe encore en septembre et si
son texte normalisé est strictement identique à celui de mai.

## Résultats

| Contrôle | Résultat | Risque traité |
|---|---:|---|
| Relations parcourues | 28 462 566 | graphe complet, pas un échantillon |
| Phrases Pangmao ayant au moins un lien français direct | 14 600 | potentiel brut |
| Paires directes mandarin–français | 16 434 | plusieurs traductions possibles |
| Identifiants liés absents de l'export mandarin actuel | 517 | suppression ou reclassement depuis mai |
| Textes mandarin modifiés depuis mai | 35 | paire devenue temporellement incohérente |
| Identifiants liés et texte mandarin inchangé | 14 048 | cohérence d'identité minimale |
| Candidats avec ≥ 1 avis positif, aucun négatif | 826 | file de revue conservatrice |
| Candidats avec ≥ 2 avis positifs, aucun négatif | 23 | sous-ensemble prioritaire |
| Phrases bilingues projetées si les 826 étaient acceptées | 846 / 64 912 | 20 actuellement |

Le filtre choisit, lorsqu'il reste plusieurs traductions, celle qui a le plus
d'avis positifs, puis le moins d'avis indécis, puis le plus petit identifiant.
Il élimine tout candidat comportant un avis négatif. Il ne produit aucun doublon
par phrase chinoise et les 826 lignes compléteraient toutes une phrase française
actuellement vide.

## Pourquoi ce filtre ne suffit pas à autoriser l'import

Un avis Tatoeba porte sur la phrase elle-même, pas sur la fidélité ni le registre
de la relation de traduction. Le contrôle a trouvé des paires techniquement
admissibles mais éditorialement discutables, par exemple:

- `别烦我！` associé à `Ne me casse pas les couilles !`: français authentique,
  mais registre beaucoup plus vulgaire que le chinois;
- `他喜欢火车。` associé à `Il adore les trains.`: formulation naturelle, mais
  intensité supérieure à `喜欢`;
- certaines phrases ont été corrigées ou supprimées entre les deux instantanés,
  ce qui justifie le contrôle strict de texte ajouté à l'évaluateur.

La sévérité est **élevée** pour un import automatique: ces écarts contredisent
directement l'objectif de langage authentique et fidèle. La confiance dans ce
diagnostic est élevée, car les identifiants, liens, avis et changements de texte
ont tous été vérifiés sur les exports complets.

## Décision et suite

1. Ne pas intégrer automatiquement les 826 candidats.
2. Utiliser cette liste comme file de revue reproductible.
3. Les 23 candidats ayant au moins deux avis positifs ont constitué le premier
   lot de revue: 18 ont été acceptés et cinq rejetés. Voir
   [V0.5_D1_EXAMPLES_REPORT.md](V0.5_D1_EXAMPLES_REPORT.md).
4. Continuer à vérifier manuellement fidélité, registre, ponctuation et naturel
   français, en conservant les identifiants Tatoeba dans la base.
5. Limiter chaque apport suivant à un lot accompagné d'un test de
   non-régression et d'une mesure avant/après.

## Reproduire l'évaluation

Après téléchargement des quatre exports officiels et du miroir anglais épinglé:

```bash
python3 tools/evaluate_tatoeba_french_examples.py \
  --english-pairs "Sentence pairs in Mandarin Chinese-English - 2026-05-20.tsv" \
  --mandarin-sentences cmn_sentences.tsv.bz2 \
  --french-sentences fra_sentences.tsv.bz2 \
  --links links.tar.bz2 \
  --reviews users_sentences.csv \
  --dictionary app/src/main/assets/databases/pangmao.db \
  --include-hashes \
  --json-out build/reports/tatoeba-french-evaluation.json \
  --selected-out build/reports/tatoeba-french-positive.tsv
```

La sortie TSV est une file de candidats, pas une source de production. Le
script ne modifie jamais la base et n'est pas appelé pendant la construction de
l'APK.
