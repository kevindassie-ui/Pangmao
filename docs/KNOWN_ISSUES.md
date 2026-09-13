# Problèmes connus et retours appareil

Dernière mise à jour: 2026-09-13.

Ce fichier contient uniquement les défauts reproduits mais non encore livrés.
Ils restent séparés des idées produit de la roadmap et quittent cette liste dès
qu’un correctif publié les couvre.

## Correctif ultérieur à la v0.4.1

### Divulgation de l’analyse de texte

Dans le dictionnaire, le contrôle « analyse mot à mot » masque actuellement à la
fois l’interprétation linéaire et les cartes des blocs logiques.

Comportement attendu:

- les blocs logiques restent visibles par défaut et possèdent leur propre état;
- seule l’interprétation mot à mot linéaire est masquée par défaut et activable;
- ouvrir ou fermer l’une ne modifie pas la visibilité de l’autre.

### Traduction contextuelle avec `才`

La phrase signalée `我习惯每天晚上喝咖啡才去打球` produit des formulations
maladroites équivalentes à « boire du café tous les soirs pour jouer ».

Sens attendu selon le contexte: « Chaque soir, je ne vais jouer au ballon
qu’après avoir bu un café » / “Every evening, I only go play ball after having
coffee.” Le correctif devra couvrir la construction `…才…` et éviter de traiter
`打球` comme le simple verbe générique « jouer » lorsque le contexte permet
« jouer au ballon / pratiquer un sport de balle ».
