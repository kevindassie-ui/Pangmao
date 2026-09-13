# Évaluation Kaikki / Wiktionnaire français

Date de l'évaluation: 2026-09-13
Décision du lot B: **LIMITER**

La source est utile comme file de candidats éditoriaux et pour quelques exemples,
mais elle n'est pas suffisamment homogène pour être fusionnée automatiquement à
la base Pangmao.

## Source et reproductibilité

- Jeu évalué:
  [`kaikki.org-dictionary-Chinois.jsonl`](https://kaikki.org/frwiktionary/Chinois/kaikki.org-dictionary-Chinois.jsonl)
  (33 497 962 octets).
- SHA-256 observé:
  `847718c8d03743b5f29b9c16263a67efa6c468bdf3b2ad666d5896f85cb9139e`.
- La [page du dictionnaire chinois](https://kaikki.org/frwiktionary/Chinois/index.html)
  indique une extraction du 2026-09-08 depuis le dump frwiktionary du
  2026-09-01. Le fichier téléchargé a été publié le 2026-09-12.
- Kaikki précise que les éditions non anglaises sont encore en cours de
  développement et peuvent contenir erreurs et omissions. Le téléchargement
  post-traité utilisé ici est annoncé comme obsolète et susceptible de
  disparaître; il ne doit donc pas devenir une dépendance directe de la CI.
- Les données suivent les licences de Wiktionnaire, CC BY-SA et GFDL. Pangmao
  retiendra la voie CC BY-SA 4.0, avec attribution de Wiktionnaire et de Kaikki
  et conservation de la provenance par ligne. Voir la
  [politique de droits de Wiktionnaire](https://en.wiktionary.org/wiki/Wiktionary:Copyrights).

Commande de reproduction, après construction de la base Pangmao:

```bash
python3 tools/evaluate_french_candidate.py \
  --database app/src/main/assets/databases/pangmao.db \
  --candidate kaikki.org-dictionary-Chinois.jsonl \
  --json-out build/reports/kaikki-french-evaluation.json
```

L'évaluateur est en lecture seule. Il normalise Unicode, espaces, définitions et
pinyin, puis distingue correspondances exactes, homographes ambigus, conflits de
prononciation et nouveaux mots.

## Profil et grain

Le fichier contient un enregistrement par couple mot/catégorie grammaticale,
pas nécessairement un article lexical Pangmao. Plusieurs formes Kaikki peuvent
donc rejoindre la même entrée Pangmao.

| Mesure | Résultat |
|---|---:|
| Enregistrements | 33 556 |
| Mots distincts | 30 350 |
| Sens annoncés | 36 663 |
| Sens sans définition exploitable | 18 706 |
| Mots possédant au moins une définition | 13 011 |
| Définitions françaises extraites | 17 962 |
| Exemples chinois–français explicitement structurés | 440 |
| Définitions avec résidu de balisage détecté | 0 |
| Renvois plutôt que définitions directes | 70 |
| Définitions de plus de 240 caractères | 24 |

La forte proportion de caractères sans définition explique l'écart entre les
36 663 sens affichés par la source et les 13 011 mots réellement utilisables.

## Correspondance avec Pangmao

| Résultat par mot candidat | Nombre |
|---|---:|
| Correspondance par pinyin | 9 243 |
| Correspondance sûre par mot unique | 564 |
| Pinyin ambigu | 846 |
| Pinyin incompatible | 323 |
| Mot ambigu sans pinyin discriminant | 17 |
| Nouveau mot absent de Pangmao | 2 018 |

Les 9 807 mots candidats rattachables sans ambiguïté convergent vers 6 945
entrées Pangmao distinctes, car formes simplifiées, traditionnelles et variantes
peuvent désigner le même article.

### Gain potentiel avant revue

| Mesure | Résultat |
|---|---:|
| Entrées rattachées sans définition française | 1 695 |
| Entrées principales concernées | 1 632 |
| Entrées principales observées au moins une fois | 386 |
| Entrées principales observées au moins cinq fois | 46 |
| Entrées principales observées au moins vingt fois | 10 |
| Entrées principales observées au moins cent fois | 1 |
| Entrées déjà françaises avec formulation supplémentaire | 5 250 |
| Chaînes françaises nouvelles après déduplication littérale | 7 350 |
| Poids UTF-8 brut de ces chaînes | 194 500 octets |

Une formulation textuellement nouvelle n'est pas nécessairement un nouveau
sens. Le gain volumique est faible; le risque principal est sémantique, pas
technique.

Pour les exemples, huit phrases chinoises recouvrent exactement le corpus
Pangmao et peuvent recevoir une traduction française manquante. Les 432 autres
exemples restent utiles comme candidats futurs, mais ne possèdent pas
d'alignement anglais exact dans le corpus actuel.

## Contrôle qualitatif ciblé

L'échantillon couvre les entrées fréquentes sans français, les sens
supplémentaires, les termes culturels, les homographes et les nouveaux mots.

| Cas | Observation | Décision |
|---|---|---|
| `真的` | « En effet, vraiment » est naturel mais partiel | Accepter après découpage des sens |
| `游戏` | « jeu; jouer » correspond à l'anglais | Accepter |
| `所有人` | Distingue « tout le monde » et le sens juridique « propriétaire » | Accepter en deux sens |
| `没问题` | « Pas de problème » est naturel | Accepter |
| `驾驶执照` | « Permis de conduire » est exact | Accepter |
| `肉夹馍` | « Sorte de petit sandwich de Xi'an » est correct mais moins précis que Pangmao | Conserver Pangmao en premier; source secondaire facultative |
| `枪` | « pistolet, revolver, fusil; lance » est juste mais incomplet | Réviser avant ajout |
| `苏` | Deux sens justes, nombreuses abréviations absentes | Réviser par sens |
| `须` | Fusion de « devoir » et des sens barbe/moustache issus de graphies traditionnelles distinctes | Réviser avec les variantes |
| `里` | Exemple chinois, pinyin et traduction ont fui dans les définitions | Rejeter la ligne brute |
| `家` | « Ustensile » est rattaché à une variante et ne doit pas enrichir l'article principal | Rejeter la ligne brute |
| `杰` | Contient des métadonnées HSK et « définition manquante » | Rejeter |
| `阿里` | « Ari » ne correspond pas à Ali/Alibaba dans Pangmao | Rejeter |
| `才`, `你` | Sens utiles mais rattachement ambigu entre graphies/entrées | Exiger une résolution éditoriale |
| Symboles et caractères rares nouveaux | Apport/usage disproportionné pour l'apprentissage courant | Ne pas créer automatiquement d'entrée |

CFDICT reste la source française générale de premier rang. Kaikki apporte des
formulations souvent naturelles et des catégories utiles, mais son extraction
et son alignement ne sont pas assez réguliers pour remplacer ou compléter
CFDICT sans revue.

## Décision et garde-fous du lot C

1. Ne pas intégrer le fichier Kaikki complet dans l'APK ou dans la CI.
2. Utiliser Kaikki comme file de candidats, en commençant par les 46 entrées
   principales observées au moins cinq fois et les huit exemples exactement
   alignés.
3. Enregistrer uniquement un petit sous-ensemble revu dans le dépôt avec
   graphies, pinyin, catégorie grammaticale, texte français, URL de l'article,
   date du dump, licence et décision éditoriale.
4. Ne jamais écraser une définition Pangmao ou CFDICT. Séparer la provenance
   par définition avant d'exposer des sources secondaires dans l'interface.
5. Exclure par défaut les 2 018 nouveaux mots, les correspondances ambiguës et
   les résidus d'extraction; ils auront une file de revue distincte dédiée.
6. Étendre ensuite la revue aux 340 entrées principales observées une à quatre
   fois seulement si le premier lot passe les tests de naturel et de sens.

Cette approche transforme Kaikki en source éditoriale contrôlée et stable,
sans faire passer un agrégat hétérogène pour un dictionnaire français homogène.
