# Journal des versions

Ce fichier consigne les changements effectivement livrés. Les évolutions
envisagées restent dans [docs/ROADMAP.md](docs/ROADMAP.md).

Le format suit [Keep a Changelog](https://keepachangelog.com/fr/1.1.0/) et les
numéros suivent le versionnage sémantique.

## Non publié

### Problèmes confirmés

- Une suite de caractères absente comme entrée exacte, par exemple `大笨蛋`, ne
  produit ni traduction globale ni résultats décomposés.
- Le lecteur montre des segments visuellement collés, sans traduction, pinyin
  ni définition directement visible.
- Le téléchargement d'un APK depuis ChatGPT peut ne pas aboutir; le même fichier
  se télécharge depuis l'application GitHub.

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

[0.2.1]: https://github.com/kevindassie-ui/Pangmao/releases/tag/v0.2.1
[0.2.0]: https://github.com/kevindassie-ui/Pangmao/releases/tag/v0.2.0
[0.1.0-mvp]: https://github.com/kevindassie-ui/Pangmao/releases/tag/v0.1.0-mvp
