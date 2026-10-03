# Problèmes connus et retours appareil

Dernière mise à jour: 2026-10-03.

Ce fichier suit les défauts reproduits jusqu’à leur validation sur l’appareil.
Ils restent séparés des idées produit de la roadmap et quittent cette liste une
fois le correctif publié puis confirmé.

## Priorité Web confirmée — qualité de la voix française

**Statut:** Web 0.3.3, choix Amélie/Thomas et accent amélioré confirmés le
3 octobre; naturel insatisfaisant, persistance et Reader encore à confirmer.
L'absence de voix dans le navigateur Web Android est le constat du 24 septembre,
sans nouveau résultat appareil.

Le retour du 3 octobre décrit un rendu toujours robotique et peu fluide.
Amélie est déclarée `fr-CA`, pas `fr-FR`; la locale ne certifie pas le naturel.
La demande ultérieure exige une solution intégrée à Pangmao, sans réglages
système complexes: [D-042](PRODUCT_DECISIONS.md#d-042--voix-naturelles-intégrées-sans-configuration-système).
L'installation manuelle de voix iOS n'est plus la solution produit cible.
Les options de moteur restent à évaluer; aucune n'est encore implémentée.

Le 2026-09-24, la lecture du dictionnaire et du Reader fonctionne depuis
l'application ajoutée à l'écran d'accueil de l'iPhone. La voix retenue est
toutefois perçue comme celle d'un sinophone parlant français plutôt que comme
une voix française native. Le nom chinois de l'utilisatrice est, lui, prononcé
à la française; ce comportement sur un nom propre ne permet pas de conclure sur
la qualité des phonèmes français.

La version 0.3.2 filtre correctement les locales françaises, mais une étiquette
`fr-FR` fournie par le système ne certifie ni le moteur ni la qualité de la
voix. Avant de poursuivre les fonctions suivantes, il faut:

- relever toutes les voix françaises réellement exposées par l'iPhone, avec
  nom, identifiant, locale et caractère local/distant;
- comparer la même phrase entièrement française avec chacune d'elles;
- mémoriser la voix explicitement approuvée et l'appliquer au dictionnaire comme
  au Reader;
- vérifier que le choix persiste après fermeture, redémarrage et mise à jour;
- conserver l'absence de repli vers une voix chinoise;
- fournir un diagnostic exportable localement, sans collecte automatique.

Ce défaut est la première porte de la section W1.6 de
[ROADMAP.md](ROADMAP.md).

La version publiée 0.3.3 ajoute les profils `女声` et `男声`, une phrase de comparaison
commune, une confirmation persistante par profil et la copie locale de
l'inventaire vocal. Le problème ne quittera cette liste qu'après validation
du naturel des deux profils, de leur persistance et de leur usage dans le
Reader sur l'iPhone réel. Le retour du 3 octobre est une validation partielle.

## Régression bloquante confirmée — v0.12.2

### Reconnaissance automatique de l'écriture manuscrite

**Statut:** reproduite sur l'appareil de référence; aucun correctif publié.

Le 2026-09-23, six essais successifs ont tous conservé le dessin puis affiché
`Recognition failed. Your drawing was kept: try again or clear it.` sans aucune
suggestion. Le lot comprend notamment des caractères très simples comme `人`,
`日` et `水`; l'échec ne peut donc pas être attribué à un seul caractère
complexe ou à un tracé isolé.

**Impact:** le mode de saisie manuscrite de la v0.12.2 est inutilisable sur cet
appareil. L'application ne se ferme pas et le dessin reste récupérable, mais le
parcours ne produit aucun caractère à envoyer à la recherche.

Cette régression Android ne bloque pas la validation prioritaire de Pangmao Web.
Elle devient en revanche une porte bloquante avant toute nouvelle diffusion
Android issue de cette base. À la reprise du client Android, il faudra:

- reproduire avec et sans réseau, après vérification explicite de l'état du
  modèle Digital Ink;
- relever le diagnostic applicatif et `logcat` au moment de l'échec afin de
  distinguer modèle absent, initialisation, téléchargement et reconnaissance;
- vérifier au minimum dix caractères successifs, dont `人`, `日`, `水`, annuler
  et effacer, sans échec systémique;
- ajouter une couverture de non-régression au niveau le plus bas permettant de
  simuler les états du modèle et les retours du moteur.

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

## Retour appareil sur la v0.8.0

Confirmé:

- lecture, découpage et lancement depuis une phrase fonctionnels;
- les quatre vitesses et l'essai de voix fonctionnent;
- les cartes peuvent être ajoutées directement depuis le Reader.

Correctif publié dans la v0.8.1, à confirmer sur l'appareil:

- la rangée TTS horizontalement défilable masque alternativement certaines
  vitesses ou les actions `Tester/Détails` sur un écran étroit; elle est
  remplacée par un curseur à quatre crans et un menu d'actions compact.

Amélioration ergonomique publiée dans la v0.9.0:

- remplacer la rangée de phrases tronquées par une navigation compacte
  précédent/suivant avec compteur et extrait de la phrase active.

## Validation appareil de la v0.9.0

- mise à jour depuis la v0.8.1 sans perte des cartes, favoris et historiques;
- persistance des états `À apprendre / Connu / Non marqué` après redémarrage;
- cohérence des listes Cartes, À apprendre, Connus et Favoris;
- couverture non évaluée tant que le profil lexical est insuffisamment rempli;
- surlignage facultatif des mots à revoir;
- navigation précédente/suivante et choix direct d'une phrase sur écran étroit.

## Validation appareil de la v0.10.0

- mise à jour directe depuis la v0.9.0 sans perte de données personnelles;
- espace de lecture utile sur écrans étroits et défilement interne d’un long
  texte chinois;
- sélection et surlignage d’une phrase au toucher, à l’arrêt et pendant le TTS;
- appui long sur un mot vers la bonne mini-fiche, y compris après ponctuation;
- passage `Modifier / Terminer`, sélection Android native et analyse actualisée;
- ajout/retrait d’une carte visible dans la fiche sans couplage involontaire au
  statut lexical.

## Validé sur appareil

Le TTS sur ColorOS et la séparation des blocs logiques ont été confirmés sur
l'appareil le 2026-09-13. Les vitesses et l'ajout Reader → cartes de la v0.7.0
sont confirmés; le défaut de position de reprise reste suivi ci-dessus.
