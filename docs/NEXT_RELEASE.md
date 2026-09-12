# Pangmao v0.2.1 — stabilisation et analyse de texte

Checkpoint: 2026-09-12, après test réel de v0.2.0.

La direction graphique de v0.2.0 est validée et gelée. La prochaine version
privilégie la fiabilité et une recherche réellement utile sur plusieurs mots.

## Bloc 1 — correctifs bloquants

### Écriture manuscrite

Constat: crash généralement après le premier trait, au moment du premier appel
automatique à ML Kit Digital Ink.

Travail prévu:

- sérialiser les reconnaissances et annuler proprement les résultats obsolètes;
- protéger les erreurs synchrones et asynchrones du moteur;
- conserver le dessin si la reconnaissance échoue et afficher une erreur utile;
- rapprocher la capture des traits de l'implémentation de référence Google;
- ajouter un journal de diagnostic local, partageable depuis l'application si un
  crash natif subsiste.

Acceptation: dix caractères successifs, traits rapides et lents, annulation et
effacement, sans fermeture de l'application.

### Interface chinoise

Constat: `简体中文` est sélectionnable mais l'interface reste dans la langue du
système (anglais dans le test).

Travail prévu: aligner strictement la balise `zh-Hans`, la configuration Android
des langues et le répertoire de ressources BCP-47, puis tester changement à
chaud et redémarrage.

Acceptation: tous les écrans principaux passent immédiatement en chinois et le
choix persiste après fermeture complète.

## Bloc 2 — recherche et lecteur unifiés

Créer un seul moteur d'analyse utilisé par la recherche et le lecteur:

1. afficher d'abord l'entrée exacte lorsqu'elle existe;
2. sinon afficher une traduction/interprétation globale, explicitement marquée
   comme traduction automatique locale;
3. afficher dessous les blocs logiques avec hanzi, pinyin, définition française
   et accès à la fiche complète;
4. proposer une segmentation contextuelle plutôt que le découpage glouton
   actuel.

Cas d'acceptation:

- `大笨蛋`: interprétation globale, puis `大 / 笨蛋`;
- `我喜欢一起床就吃面条`: `我 / 喜欢 / 一 / 起床 / 就 / 吃 / 面条`, sans
  découpage erroné `一起 / 床`;
- une expression présente dans le dictionnaire reste prioritaire sur sa
  décomposition;
- ponctuation et texte mixte restent lisibles et sélectionnables.

La traduction devra rester gratuite, légale, locale après un téléchargement de
modèle déclenché par l'utilisateur. La solution sera validée avant intégration.

## Bloc 3 — finition visuelle ciblée

- colorer aussi chaque caractère selon son ton, en conservant les couleurs du
  pinyin et un contraste accessible dans les deux thèmes;
- ajouter en haut de l'accueil un petit chat issu de l'identité validée;
- remplacer le titre `Pangmao · 胖猫` par le plus léger `胖猫`.

## Bloc 4 — APK et validation

- conserver l'APK universel signé pour les mises à jour;
- produire aussi un APK ARM64 plus léger pour le téléphone actuel;
- vérifier si ce poids réduit permet le téléchargement depuis ChatGPT; le blocage
  du gestionnaire de téléchargement ChatGPT/GitHub est externe à Pangmao;
- tests unitaires de segmentation, langues et coloration, puis CI, lint,
  vérification de signature et release GitHub v0.2.1.

## Ordre d'exécution à la reprise

1. Correctif manuscrit + langue chinoise, tests et APK de stabilisation.
2. Moteur d'analyse multi-mots et nouveau lecteur.
3. Ton sur les hanzi et en-tête `胖猫`.
4. APK universel + ARM64, validation et publication.

