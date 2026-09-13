# Problèmes connus et retours appareil

Dernière mise à jour: 2026-09-13.

Ce fichier suit les défauts reproduits jusqu’à leur validation sur l’appareil.
Ils restent séparés des idées produit de la roadmap et quittent cette liste une
fois le correctif publié puis confirmé.

## Validation restante après la v0.5.1

### Traduction contextuelle avec `才`

La phrase signalée `我习惯每天晚上喝咖啡才去打球` produisait des formulations
maladroites équivalentes à « boire du café tous les soirs pour jouer ».

Sens désormais fourni selon le contexte: « Chaque soir, je ne vais jouer au ballon
qu’après avoir bu un café » / “Every evening, I only go play ball after having
coffee.” Cette phrase relue contourne le modèle local dans le Reader et dans
l’analyse du dictionnaire. Le résultat sur appareil reste à confirmer.

## Validé sur appareil

Le TTS sur ColorOS et la séparation des blocs logiques ont été confirmés sur
l'appareil le 2026-09-13. Aucun nouveau défaut n'a été signalé pour ces deux
fonctions.
