# Décisions produit

Dernière mise à jour: 2026-09-13.

## D-001 — séparer interface et apprentissage

**Décision:** la langue des menus (`système`, français, anglais, chinois) ne
déterminera pas la langue étudiée. Un futur profil d'apprentissage pilotera la
hiérarchie des informations.

**Pourquoi:** un sinophone peut vouloir une interface chinoise pour apprendre le
français, mais un francophone avancé peut également préférer cette interface en
continuant d'apprendre le chinois.

## D-002 — choix des définitions

**Décision:** proposer un choix persistant `français`, `anglais` ou `les deux`.
Le réglage sera modifiable sans quitter le parcours dictionnaire.

**Valeur par défaut:** français pour l'interface française, anglais pour
l'interface anglaise; le profil d'apprentissage déterminera ensuite le défaut
le plus pertinent. Aucun changement automatique ne devra écraser un choix
explicite.

## D-003 — harmonisation traçable

**Décision:** ne jamais remplacer silencieusement CFDICT par une traduction de
CC-CEDICT. Les définitions humaines restent attribuées à leur source. Les sens
supplémentaires issus d'une autre source ou d'une traduction locale sont
fusionnés dans la vue simple mais marqués dans la vue détaillée.

Ordre de confiance envisagé:

1. définition française humaine validée;
2. autre source française libre attribuée;
3. complément automatique traduit depuis un sens anglais identifié.

## D-004 — pas d'interface chinoise entièrement séparée

**Décision:** construire des composants communs dont la priorité et les champs
changent selon le profil, plutôt que deux applications internes divergentes.

La page d'accueil aura un sélecteur compact de profil. Une fiche française pour
sinophone montrera par exemple le mot français, l'IPA, le genre, les formes et
l'audio avant le sens chinois; une fiche chinoise conservera la structure
hanzi–pinyin actuelle.

## D-005 — lecteur à divulgation progressive

**Décision:** le lecteur gardera un seul texte principal. Pinyin, traduction et
détails par bloc seront des couches activables. Toucher un bloc ouvrira un
aperçu; la fiche complète restera une action secondaire.

Cela reprend les bénéfices des lecteurs assistés sans transformer l'écran en
tableau permanent de données.

## D-006 — audio contextuel avant audio isolé

**Décision:** dans le lecteur, la lecture de la phrase entière avec surlignage et
reprise à un bloc est prioritaire. Elle fournit prosodie et désambiguïsation que
la lecture isolée d'un mot ne peut pas offrir.

Une voix système de haute qualité sera proposée d'abord. Un modèle local libre
optionnel ne sera ajouté que si son gain justifie son poids et sa complexité.

## D-007 — historique local unifié

**Décision:** distinguer et réunir dans une même vue filtrable:

- requêtes saisies;
- fiches consultées;
- textes ouverts dans le lecteur;
- textes OCR, sans conserver les images par défaut.

Suppression individuelle, effacement total et désactivation resteront possibles.

## D-008 — maîtrise de la densité UX

**Décision:** une fonction n'obtient un nouvel onglet principal que si elle
constitue un usage quotidien autonome. Les radicaux, composants, arborescences,
réglages de source et outils d'étude resteront des vues secondaires tant que les
tests d'usage ne justifient pas davantage.

## D-009 — OCR comme action rapide

**Décision:** l’OCR reste accessible en un geste depuis l’accueil, mais ne
conserve pas un onglet principal permanent. La navigation basse se concentre sur
Dictionnaire, Lecteur et Cartes.

**Pourquoi:** l’OCR est un mode d’entrée du dictionnaire, au même titre que le
clavier et l’écriture, plutôt qu’une destination de consultation durable.

## D-010 — analyse mot à mot à la demande

**Décision:** la recherche montre d’abord le sens global et les blocs logiques.
Seule l’interprétation mot à mot linéaire est repliée par défaut. Les blocs
logiques possèdent un état distinct, restent visibles par défaut et ne sont
jamais masqués par l’ouverture ou la fermeture de l’interprétation linéaire.

## D-011 — registre français contextuel

**Décision:** ne pas transformer automatiquement chaque `你`/`vous` en `tu`.
Pangmao privilégie une traduction française attestée ou révisée; les corrections
automatiques de registre ne sont appliquées que lorsque le contexte et la
conjugaison sont maîtrisés. `您` conserve le vouvoiement.

## D-012 — aucune panne audio silencieuse

**Décision:** si le TTS Android ne possède pas de voix chinoise utilisable,
Pangmao affiche le diagnostic et l’action corrective. Une icône simplement
désactivée n’est pas une réponse suffisante.

## D-013 — un exemple, une phrase chinoise

**Décision:** l’identité d’un exemple est sa phrase chinoise normalisée, pas la
paire chinois–traduction. Plusieurs traductions concurrentes ne créent donc plus
plusieurs cartes identiques. Les ajouts éditoriaux revus peuvent compléter ou
remplacer une langue cible tout en conservant leur source.

## D-014 — aucune authenticité automatique

**Décision:** une traduction produite par un modèle local peut aider à comprendre
un texte, mais ne peut pas être présentée comme une définition harmonisée ou un
exemple authentique. Les enrichissements du dictionnaire exigent une source
libre attribuée ou une révision éditoriale explicite.

## D-015 — exploration dans la fiche

**Décision:** Définitions, Exemples et Caractères/Mots sont des onglets internes
à la fiche, pas de nouvelles destinations principales. Les filtres de position,
fréquence et tri sont combinables, mais restent repliés dans ce contexte.

## D-016 — hors ligne par défaut, en ligne par consentement

**Décision:** le dictionnaire, le Reader, l'OCR, l'écriture, les cartes et les
fonctions essentielles restent utilisables hors ligne. Un mode « En ligne
amélioré » pourra être activé explicitement pour une meilleure traduction de
phrase, une voix distante plus naturelle ou d'autres traitements coûteux.

Chaque appel distant indiquera sa nature et les données envoyées. Une panne, un
quota épuisé ou la désactivation du mode ramènera automatiquement au service
local disponible. Les résultats automatiques en ligne ne deviendront jamais des
données « authentiques » sans source ou révision éditoriale.

## D-017 — parole comme quatrième mode d'entrée

**Décision:** le STT rejoint Clavier, Écriture et OCR dans les actions rapides de
l'accueil. Il capture une courte prise de parole, affiche clairement l'écoute en
cours, puis laisse relire et corriger la transcription avant la recherche ou le
passage au Reader.

Le moteur local sera privilégié lorsqu'il offre une qualité suffisante; un STT
en ligne plus précis pourra suivre D-016. Le microphone n'est jamais activé
implicitement et l'audio brut n'est pas conservé par défaut.

## D-018 — briques audio partageables, produits indépendants

**Décision:** Pangmao et la future application de transcription de réunions
pourront partager les contrats de moteur STT, le découpage audio, les modèles de
langue et leurs tests. Ils conserveront toutefois des flux distincts: une saisie
vocale courte n'a ni les contraintes de durée, ni la reprise après interruption,
ni le stockage persistant d'une réunion.
