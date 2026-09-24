# Plan de profondeur lexicale — Pangmao Web français–chinois

Date de cadrage: 24 septembre 2026.

## Décision

La couverture actuelle suffit à éprouver la boucle dictionnaire, mais pas à
tenir la promesse d'un compagnon de français quotidien. Les absences de
`péter`, `bananer` et de plusieurs locutions familières, ainsi que le mauvais
classement initial de `放屁`, montrent deux lacunes différentes:

1. l'inventaire français est trop modéré pour le registre oral, l'argot et les
   expressions;
2. une recherche exacte doit toujours passer avant une correspondance de
   sous-chaîne, même lorsqu'elle vient d'un complément chargé à la demande.

Web 0.3.2 corrige le second problème et fournit une couche éditoriale durable
pour les cas revus. Les versions suivantes doivent maintenant élargir le corpus
par lots reproductibles, pas seulement ajouter les mots remontés un à un.

## Principes non négociables

- priorité au français réellement rencontré en France par l'utilisatrice
  pilote, puis extension aux variantes francophones clairement étiquetées;
- conservation du registre (`familier`, `argotique`, `vulgaire`, `péjoratif`,
  `plaisant`, régional), du sens et de la provenance;
- sens distincts conservés séparément; aucune fusion destinée uniquement à
  augmenter artificiellement la couverture;
- traduction chinoise directe et revue avant présentation comme donnée de
  dictionnaire;
- une proposition issue d'un pont sémantique ou d'une IA reste marquée comme
  hypothèse et ne devient jamais silencieusement une traduction validée;
- respect des licences et construction déterministe avant toute publication;
- aucun dictionnaire propriétaire d'argot n'est copié sans licence explicite.

## Sources à évaluer

### 1. Wiktionnaire français structuré

Première piste: extraire les entrées françaises du Wiktionnaire au moyen de
Wiktextract/Kaikki, en conservant formes, prononciations, locutions, labels de
registre, exemples et attribution CC BY-SA. Pangmao utilise déjà cette chaîne
pour des explications chinoises; l'extension doit toutefois être évaluée comme
une source française distincte, avec son propre filtre de qualité.

### 2. Dictionnaire des francophones et données publiques

Évaluer la couverture des usages régionaux et contemporains, le format
d'export, la stabilité des identifiants et les obligations exactes de licence.
La source n'entre dans le produit qu'après un rapport reproductible de
couverture et de compatibilité commerciale.

### 3. Corpus Pangmao revu

Conserver la couche `PANGMAO-EDITORIAL` pour les corrections urgentes, les
expressions très usuelles absentes et les arbitrages de pertinence pédagogique.
Chaque ajout doit inclure une définition française originale, des équivalents
chinois revus, un registre et un cas de test.

## Chaîne de traitement prévue

1. Constituer un corpus témoin équilibré: mots courants, locutions, français
   familier, argot, vulgarismes, insultes légères, verbes pronominaux et requêtes
   chinoises inverses.
2. Ajouter une liste locale exportable des recherches sans résultat. Elle reste
   sur l'appareil; aucune télémétrie n'est activée implicitement.
3. Mesurer chaque source candidate: couverture du témoin, doublons, sens sans
   traduction chinoise, registres, exemples, taille et licence.
4. Importer par lots revus et attribués. Les ajouts automatiques sans équivalent
   fiable vont dans une file de revue, pas dans le paquet livré.
5. Classer exact > forme fléchie > locution > préfixe > sous-chaîne, puis tester
   les deux sens français→chinois et chinois→français.
6. Ajouter ensuite les exemples authentiques; les exemples générés à la demande
   restent séparés et marqués `Généré par IA`.

## Portes de qualité

- 100 % des entrées nouvelles ont une source, un registre lorsque nécessaire,
  au moins un sens français et un équivalent chinois contrôlé;
- aucun faux positif dans le corpus bloquant; les recherches exactes ne sont
  jamais masquées par une sous-chaîne;
- aucune régression sur les 26 témoins actuels, dont `affiche`, `avocat`,
  `banane`, `bananer`, `péter` et `放屁`;
- le rapport publie la couverture par catégorie, pas seulement le nombre total
  d'entrées;
- la taille du paquet initial et le démarrage hors ligne restent mesurés; les
  extensions volumineuses pourront être fragmentées et chargées à la demande;
- une revue humaine porte sur les écarts à fort usage avant chaque promotion.

## Séquençage

1. **Inventaire**: corpus témoin et export local des recherches manquées.
2. **Étude de sources**: rapport couverture/licence/taille, sans modifier le
   paquet public.
3. **Premier lot oral France**: expressions quotidiennes, verbes familiers et
   insultes légères, avec tests bidirectionnels.
4. **Lots argot et francophonie**: registres plus marqués et variantes
   régionales, clairement étiquetés.
5. **Exemples et pédagogie**: phrases authentiques attribuées, puis génération
   IA facultative et signalée.

Le succès sera jugé sur les recherches réellement résolues et la justesse des
sens proposés, pas sur une inflation brute du nombre de mots.
