# Problèmes connus et retours appareil

Dernière mise à jour: 2026-09-13.

Ce fichier suit les défauts reproduits jusqu’à leur validation sur l’appareil.
Ils restent séparés des idées produit de la roadmap et quittent cette liste une
fois le correctif publié puis confirmé.

## Corrigé dans la candidate v0.5.1

### Divulgation de l’analyse de texte

Dans le dictionnaire, le contrôle « analyse mot à mot » masquait à la fois
l’interprétation linéaire et les cartes des blocs logiques.

Comportement livré:

- les blocs logiques restent visibles par défaut;
- seule l’interprétation mot à mot linéaire est masquée par défaut et activable;
- ouvrir ou fermer l’interprétation ne modifie pas les blocs.

### Traduction contextuelle avec `才`

La phrase signalée `我习惯每天晚上喝咖啡才去打球` produisait des formulations
maladroites équivalentes à « boire du café tous les soirs pour jouer ».

Sens désormais fourni selon le contexte: « Chaque soir, je ne vais jouer au ballon
qu’après avoir bu un café » / “Every evening, I only go play ball after having
coffee.” Cette phrase relue contourne le modèle local dans le Reader et dans
l’analyse du dictionnaire.

### TTS sur Android/ColorOS

La v0.5.0 pouvait afficher « Android text-to-speech is unavailable » alors
qu’une voix chinoise était installée. La candidate v0.5.1 ajoute la visibilité
de service exigée par Android 11+, essaie tous les moteurs installés et accepte
les balises mandarin `zh`/`cmn`. La validation finale reste à effectuer sur
l’appareil qui a reproduit le défaut.
