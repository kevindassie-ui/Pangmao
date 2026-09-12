# Pangmao v0.2.1 — stabilisation

Checkpoint: 2026-09-12, après test réel de v0.2.0.

La direction graphique est validée et gelée. Cette version corrective doit
rester courte afin de remettre rapidement une base fiable entre les mains de
l'utilisateur.

## 1. Écriture manuscrite

Constat: crash généralement après le premier trait, au premier appel
automatique à ML Kit Digital Ink.

- sérialiser les reconnaissances et ignorer les résultats obsolètes;
- protéger les erreurs synchrones et asynchrones du moteur;
- conserver le dessin et afficher une erreur exploitable en cas d'échec;
- rapprocher la capture des traits de l'implémentation de référence Google;
- prévoir un diagnostic local partageable si un crash natif subsiste.

Acceptation: dix caractères successifs, traits rapides et lents, annulation et
effacement, sans fermeture de l'application.

## 2. Interface chinoise

Constat: `简体中文` est sélectionnable mais l'interface reste dans la langue du
système.

- aligner strictement la balise `zh-Hans`, la configuration Android et le
  répertoire de ressources BCP-47;
- tester le changement à chaud, la fermeture complète et le redémarrage.

Acceptation: tous les écrans principaux passent en chinois et le choix persiste.

## 3. Distribution

- conserver l'APK universel signé pour garantir les mises à jour;
- produire aussi un APK ARM64 plus léger pour le téléphone actuel;
- retester le téléchargement depuis ChatGPT, sans présenter comme un correctif
  Pangmao ce qui dépend du gestionnaire de téléchargement externe;
- CI, lint, tests, vérification de signature et release `v0.2.1`.

Les nouvelles fonctions de lecture et de dictionnaire commencent en `v0.3.0`:
voir [ROADMAP.md](ROADMAP.md).

