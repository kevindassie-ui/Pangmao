# Journal des versions

Ce fichier consigne les changements effectivement livrés. Les évolutions
envisagées restent dans [docs/ROADMAP.md](docs/ROADMAP.md).

Le format suit [Keep a Changelog](https://keepachangelog.com/fr/1.1.0/) et les
numéros suivent le versionnage sémantique.

## Non publié

## 0.3.1 — 2026-09-12

### Corrigé

- Les mots déjà couverts par le dictionnaire n’exigent plus le téléchargement
  d’un modèle de traduction.
- Les traductions aberrantes signalées pour `傻比` et `好喜欢` sont remplacées
  par des formulations françaises et anglaises révisées.
- La phrase d’exemple du lecteur dispose d’une traduction humaine naturelle.

### Modifié

- Le pinyin continu apparaît juste sous le texte du lecteur et reste activable.
- Le bloc `Comprendre la phrase` et son long aperçu mot à mot sont supprimés;
  la traduction compacte précède désormais le détail par blocs logiques.
- Le téléchargement ponctuel des modèles de traduction est présenté comme une
  opération unique; les modèles installés restent disponibles hors ligne.

## 0.3.0 — 2026-09-12

### Ajouté

- Interprétation globale des expressions et phrases, puis détail en blocs
  logiques avec hanzi, pinyin, définition et accès à la fiche.
- Traduction automatique facultative sur l'appareil, après téléchargement
  explicite des modèles gratuits requis.
- Choix persistant des définitions : français, anglais ou les deux.
- Couches pinyin, traduction et définitions activables dans le lecteur.
- Historique distinct des requêtes avec suppression individuelle ou totale.
- Couleur des hanzi selon le ton et mascotte compacte dans l'accueil `胖猫`.

### Modifié

- Segmentation contextuelle commune à la recherche et au lecteur, notamment
  pour `大 / 笨蛋` et `一 / 起床`.
- Reconnaissance manuscrite de nouveau automatique après une courte pause,
  tout en conservant la sérialisation qui empêche les crashs.
- Lecture vocale disponible pour le texte complet du lecteur.

## 0.2.1 — 2026-09-12

### Corrigé

- Reconnaissance manuscrite déclenchée explicitement, sérialisée et protégée
  contre les erreurs et résultats obsolètes afin d'éviter le crash au tracé.
- Ressources chinoises alignées sur la balise Android `zh-Hans`.

### Ajouté

- APK ARM64 allégé pour les téléphones récents, en complément de l'APK universel.

## 0.2.0 — 2026-09-11

### Ajouté

- Nouvelle identité graphique claire `Porcelaine douce` et sombre `Sceau de nuit`.
- Paramètres de langue et de thème.
- Interfaces française, anglaise et chinoise simplifiée.
- Saisie manuscrite mono-caractère avec modèle local téléchargé à la demande.
- Sélection visuelle des zones de texte reconnues par l'OCR.

### Modifié

- Icône Pangmao avec chat et livre bilingue.
- Commande d'effacement du lecteur intégrée au champ de texte.

## 0.1.0-mvp — 2026-09-11

### Ajouté

- Dictionnaire chinois hors ligne en hanzi, pinyin, français et anglais.
- Fiches avec formes simplifiée/traditionnelle, pinyin tonal, exemples et
  informations Unihan.
- Lecteur segmenté tactile, OCR caméra/image local, favoris, historique des
  fiches consultées et cartes SRS.
- CI GitHub, tests, lint et publication d'un APK signé installable.

[0.3.1]: https://github.com/kevindassie-ui/Pangmao/releases/tag/v0.3.1
[0.3.0]: https://github.com/kevindassie-ui/Pangmao/releases/tag/v0.3.0
[0.2.1]: https://github.com/kevindassie-ui/Pangmao/releases/tag/v0.2.1
[0.2.0]: https://github.com/kevindassie-ui/Pangmao/releases/tag/v0.2.0
[0.1.0-mvp]: https://github.com/kevindassie-ui/Pangmao/releases/tag/v0.1.0-mvp
