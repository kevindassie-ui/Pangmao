# Décisions produit

Dernière mise à jour: 2026-09-12.

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

