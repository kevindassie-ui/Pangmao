# Problèmes connus et retours appareil

Dernière mise à jour: 2026-09-14.

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

## Validation appareil de la v0.6.0

La CI et les signatures sont validées. Les nouveaux profils doivent encore être
testés sur l'appareil de référence après mise à jour depuis v0.5.1:

- persistance du profil indépendamment de la langue de l'interface;
- recherches françaises `être`, `etre` et `chat`;
- recherche anglaise `learn`;
- recherche inverse avec un sens chinois;
- ouverture d'un équivalent chinois depuis une fiche française ou anglaise;
- absence de régression dans le profil chinois et le Reader.

## Retour appareil sur la v0.7.0

Confirmé:

- la lecture TTS est claire aux vitesses proposées;
- l'ajout direct d'un mot aux cartes depuis la mini-fiche du Reader fonctionne.

Implémenté dans la v0.8.0 publiée, à valider sur l'appareil:

- pause/reprise au dernier offset fourni par ColorOS, avec repli au début de la
  phrase plutôt qu'au début du texte;
- commandes distinctes pour reprendre, recommencer et arrêter;
- lecture directe depuis une phrase touchée et surlignage de la phrase active;
- ajout de `0,6×`; `1,2×` reste disponible pour les moteurs lents et les
  utilisateurs avancés;
- ouverture d'une mini-fiche ou des blocs logiques depuis une sélection longue
  dans le texte éditable;
- copie du pinyin, des traductions et des éléments de la lecture segmentée;
- exactitude des informations moteur/voix/locale/réseau affichées dans le panneau
  TTS secondaire.

Spécification: [V0.8_RELEASE_PLAN.md](V0.8_RELEASE_PLAN.md).

## Validé sur appareil

Le TTS sur ColorOS et la séparation des blocs logiques ont été confirmés sur
l'appareil le 2026-09-13. Les vitesses et l'ajout Reader → cartes de la v0.7.0
sont confirmés; le défaut de position de reprise reste suivi ci-dessus.
