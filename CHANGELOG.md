# Journal des versions

Ce fichier consigne les changements effectivement livrés. Les évolutions
envisagées restent dans [docs/ROADMAP.md](docs/ROADMAP.md).

Le format suit [Keep a Changelog](https://keepachangelog.com/fr/1.1.0/) et les
numéros suivent le versionnage sémantique.

## Non publié

### Ajouté

- Fondation locale de la v0.9 pour marquer séparément un mot `À apprendre` ou
  `Connu`; l'absence de marquage reste un troisième état et ne dépend ni des
  favoris ni des cartes SRS.
- Migration additive de la base personnelle de la version 2 à la version 3,
  sans modification du dictionnaire embarqué ni des données existantes.
- Calcul pur de couverture distinguant occurrences, mots uniques, blocs
  inconnus et complétude du profil; aucun niveau de difficulté n'est annoncé
  tant qu'une part suffisante du vocabulaire n'a pas été classée.
- Choix local `Non marqué / À apprendre / Connu` dans la fiche dictionnaire et
  dans la mini-fiche du Reader, sans effet automatique sur les cartes SRS.

## 0.8.1 — 2026-09-14

### Modifié

- Le sélecteur TTS horizontal est remplacé par un curseur compact à quatre
  crans (`0,6×`, `0,8×`, `1×`, `1,2×`); l'essai vocal et les détails du moteur
  restent accessibles dans un menu secondaire sans masquer le texte du Reader.

## 0.8.0 — 2026-09-14

### Ajouté

- Vitesse vocale `0,6×`, navigation tactile par phrase et mise en évidence de la
  phrase active; la plage précise est accentuée lorsque le moteur Android la
  fournit.
- Commande `Recommencer` distincte de `Pause / Reprendre` et d’`Arrêter`, avec
  reprise au dernier passage suivi ou, à défaut, au début de la phrase courante.
- Définition d’un caractère, mot ou passage sélectionné dans le champ éditable:
  une entrée exacte ouvre sa mini-fiche, sinon les blocs logiques reconnus sont
  proposés.
- Copie explicite du texte complet, du pinyin avec marques de ton, de chaque
  traduction et des données visibles d’un mot segmenté ou d’une mini-fiche.
- Essai vocal court et panneau indiquant le moteur TTS, la voix, la locale et la
  dépendance éventuelle au réseau signalés par l’appareil.

### Modifié

- Les quatre vitesses `0,6×`, `0,8×`, `1×` et `1,2×` utilisent un sélecteur
  défilable et restent persistantes.
- Le champ du Reader conserve l’édition, le curseur et la sélection Android
  natives pendant le suivi vocal.

### Qualité

- Les offsets TTS restent reliés au texte source en UTF-16, y compris avec les
  caractères hors BMP; les callbacks anciens ne peuvent plus déplacer une
  nouvelle lecture.
- Chaque lot A1, A2, B, C et D a passé séparément la reconstruction du corpus,
  les tests JVM, le lint Android et la compilation d’un APK debug.

## 0.7.0 — 2026-09-14

### Ajouté

- Commandes pause, reprise et arrêt pour la lecture vocale du Reader.
- Vitesses `0,8×`, `1×` et `1,2×`, avec choix persistant appliqué à toutes les
  lectures chinoises de l’application.
- Ajout ou retrait d’un mot dans les cartes directement depuis sa mini-fiche du
  Reader, sans ouvrir la fiche complète.

### Modifié

- Les textes longs sont découpés en phrases courtes; reprendre recommence la
  phrase interrompue plutôt que l’ensemble du texte.
- Modifier le texte arrête immédiatement l’ancienne lecture.
- Ajouter une carte déjà existante est désormais sans effet et ne réinitialise
  jamais sa progression de répétition espacée.

### Qualité

- Le corpus, les identifiants du dictionnaire et le schéma utilisateur restent
  inchangés; les nouveaux états sont exposés en français, anglais et chinois.

## 0.6.0 — 2026-09-14

### Ajouté

- Profil étudié `chinois`, `français` ou `anglais`, persistant et indépendant
  de la langue de l’interface et de celle des définitions.
- Dictionnaires directs français–chinois et anglais–chinois issus des versions
  FreeDict/WikDict 2025.11.23, filtrées et attribuées sous CC BY-SA 3.0.
- 10 923 fiches françaises et 26 549 fiches anglaises conservant séparément
  formes, prononciations, informations grammaticales et sens chinois.
- Recherche exacte, par préfixe et plein texte dans la langue étudiée, recherche
  française insensible aux accents et recherche inverse par sens chinois.
- Fiches lexicales dédiées permettant d’ouvrir les équivalents chinois présents
  dans le dictionnaire principal.

### Modifié

- L’accueil adapte ses libellés et ses méthodes de saisie au profil choisi;
  OCR et écriture restent propres au profil chinois.
- Le dictionnaire embarqué passe au schéma v4 sans modifier les 132 342 entrées
  chinoises ni leurs identifiants.
- Le Reader, les favoris et les cartes chinoises conservent leur comportement
  de la v0.5.1.

### Qualité

- Les entrées sans sens chinois exploitable, les valeurs non chinoises et les
  variantes de prononciation excédentaires sont rejetées déterministiquement.
- La reconstruction vérifie six sources épinglées, leurs empreintes, les clés
  étrangères, les index plein texte et les entrées témoins FR/EN.

## 0.5.1 — 2026-09-13

### Corrigé

- Les blocs logiques de l’analyse restent visibles par défaut; seul le résumé
  linéaire mot à mot est désormais repliable.
- La construction signalée `我习惯每天晚上喝咖啡才去打球` utilise une traduction
  française et anglaise relue dans le dictionnaire comme dans le Reader.
- Android 11 et versions ultérieures peuvent découvrir les services TTS grâce à
  la déclaration de visibilité requise dans le manifeste.
- Le TTS essaie successivement le moteur par défaut puis les autres moteurs
  installés, accepte les balises `zh` et `cmn`, et ne dépend plus d’une liste de
  voix OEM complète pour reconnaître le mandarin.

### Qualité

- Deux tests couvrent les niveaux de disponibilité TTS et les balises de langue
  chinoise; les traductions relues sont protégées contre tout appel au modèle.

## 0.5.0 — 2026-09-13

### Ajouté

- Audit déterministe de la couverture bilingue, des doublons, de la provenance
  et des anomalies candidates, archivé par la CI.
- Schéma de dictionnaire v3 avec provenance compacte par définition et
  remplacement fiable de l’ancienne base lors d’une mise à jour.
- 77 définitions françaises relues pour 46 entrées fréquentes qui en étaient
  dépourvues.
- 26 exemples bilingues supplémentaires: huit alignements éditoriaux et 18
  relations directes Tatoeba relues, portant le total de 12 à 38.
- Évaluateurs reproductibles pour Wiktionnaire/Kaikki et les relations directes
  Tatoeba mandarin–français.

### Modifié

- Les imports candidats sont désormais bloqués en cas d’identifiant, graphie,
  pinyin, source ou seuil d’avis incohérent.
- Les exemples conservent une provenance distincte pour l’anglais et le
  français; les données Tatoeba non relues restent hors de la base.

### Qualité

- Aucun import automatique de l’extraction Wiktionnaire bruitée ni des 808
  relations Tatoeba restant à examiner.
- La base conserve 132 342 entrées et 64 912 phrases chinoises uniques pour
  68,29 Mo.

## 0.4.1 — 2026-09-12

### Corrigé

- Les longues lignes de hanzi colorés disposent d’un interligne explicite et
  ne se chevauchent plus dans l’analyse de texte.
- Le TTS réutilise en priorité une voix chinoise déjà exposée par Android,
  gère les moteurs dont l’initialisation répond immédiatement, puis se relance
  au retour des paramètres; le diagnostic propose aussi une nouvelle tentative.
- `肉夹馍` et `肉夾饃` ont une interprétation bilingue revue et restent
  « roujiamo » dans les phrases envoyées au traducteur local.

## 0.4.0 — 2026-09-12

### Ajouté

- Fiches organisées en onglets Définitions, Exemples et Caractères/Mots.
- Navigation vers la fiche de chaque caractère composant un mot.
- Explorateur filtrable des mots contenant, commençant ou finissant par un
  caractère, avec tri par fréquence ou pinyin.
- Petit corpus éditorial français–anglais revu pour les lacunes et erreurs
  documentées, notamment `奶茶婊`, `肉夹馍`, `爸比` et `你很像你哥哥`.

### Corrigé

- Les exemples identiques ne sont plus répétés sous des traductions différentes.
- `肉夹馍` est protégé contre les traductions littérales « pince à viande » et
  « meat clamp »; les phrases signalées disposent de traductions naturelles.
- Réactiver le pinyin continu replace le lecteur en haut pour le rendre visible.
- Une voix chinoise manquante ou indisponible est maintenant signalée avec une
  action d’installation, au lieu d’un échec silencieux du TTS.

### Modifié

- L’analyse mot à mot de l’accueil devient facultative et masquée par défaut.
- L’OCR rejoint les actions rapides de l’accueil; la navigation basse est réduite
  à Dictionnaire, Lecteur et Cartes.
- Le stockage des exemples accepte désormais une traduction française et une
  provenance propres à chaque langue.

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

[0.7.0]: https://github.com/kevindassie-ui/Pangmao/releases/tag/v0.7.0
[0.6.0]: https://github.com/kevindassie-ui/Pangmao/releases/tag/v0.6.0
[0.5.1]: https://github.com/kevindassie-ui/Pangmao/releases/tag/v0.5.1
[0.5.0]: https://github.com/kevindassie-ui/Pangmao/releases/tag/v0.5.0
[0.4.1]: https://github.com/kevindassie-ui/Pangmao/releases/tag/v0.4.1
[0.4.0]: https://github.com/kevindassie-ui/Pangmao/releases/tag/v0.4.0
[0.3.1]: https://github.com/kevindassie-ui/Pangmao/releases/tag/v0.3.1
[0.3.0]: https://github.com/kevindassie-ui/Pangmao/releases/tag/v0.3.0
[0.2.1]: https://github.com/kevindassie-ui/Pangmao/releases/tag/v0.2.1
[0.2.0]: https://github.com/kevindassie-ui/Pangmao/releases/tag/v0.2.0
[0.1.0-mvp]: https://github.com/kevindassie-ui/Pangmao/releases/tag/v0.1.0-mvp
