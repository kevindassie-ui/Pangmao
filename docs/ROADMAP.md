# Roadmap produit Pangmao

Dernière mise à jour: 2026-10-03.

## Principes

- Fiabilité avant accumulation de fonctions.
- Fonctionnement local après les téléchargements de modèles choisis par
  l'utilisateur.
- Interface sobre: les options avancées restent accessibles sans encombrer le
  parcours principal.
- Données humaines conservées avec leur source; tout enrichissement automatique
  est identifié comme tel.
- Langue de l'interface et langue étudiée sont deux réglages indépendants.

## Priorité actuelle — Pangmao Web « 我学法语 »

La version Android 0.12.2 « J'apprends le chinois » est figée comme checkpoint
signé, mais elle n'est plus qualifiée de stable depuis la confirmation d'une
régression bloquante de la reconnaissance manuscrite. Son développement
fonctionnel est mis en pause, sans abandon ni migration risquée, pendant la
validation du versant « J'apprends le français ». Le défaut manuscrit devra être
corrigé avant toute nouvelle diffusion Android issue de cette base.

Le premier client de ce nouveau versant est une PWA installable depuis Safari.
Elle répond d'abord au besoin réel d'une utilisatrice sinophone vivant en France
et évite une dépense Apple avant preuve d'utilité. Le cadrage et les critères
d'acceptation sont détaillés dans [WEB_FRENCH_MVP.md](WEB_FRENCH_MVP.md).

### W0 — fondation sans régression Android

- conserver le tag et la release Android `v0.12.2` comme point de reprise;
- créer `webApp/` dans le même dépôt, avec build et tests indépendants;
- ne pas ajouter la PWA au build Gradle ni refactoriser Android pour la lancer;
- publier uniquement les fichiers statiques validés dans le miroir public
  `Pangmao-Web`, sans code Android ni historique privé;
- produire un pack Web français–chinois compact, versionné, attribué et
  reproductible à partir des sources validées;
- utiliser des identifiants namespacés stables avant d'ajouter favoris et cartes.

### W1 — boucle dictionnaire familiale

- interface chinoise simplifiée et installation depuis Safari;
- champ unique avec détection de la requête et recherche bidirectionnelle
  français ↔ chinois;
- recherche exacte, par préfixe et insensible aux accents côté français;
- fiches conservant homonymes et sens distincts, avec IPA, genre, catégorie et
  formes lorsqu'ils sont disponibles;
- TTS français d'un mot par action explicite;
- chargement automatique du pack au premier démarrage, annoncé avec sa taille,
  puis réutilisation hors ligne;
- recette sur l'iPhone de l'utilisatrice pilote avec `avocat`, `律师`, `être` et
  `etre` comme requêtes témoins.

État au 23 septembre 2026: le premier essai fonctionne sur Android et iOS. Le
lot correctif 0.2 ajoute une hiérarchie chinoise dans les fiches, 2 393
explications chinoises attribuées, un repli chinois fragmenté avec hypothèses
explicitement signalées, et une variante visuelle rouge au `鹿` réservée à
l'utilisatrice pilote. La marque globale verte reste inchangée.

Correctif validé le 24 septembre: une requête française ne parcourt plus le
texte libre des définitions ni les sous-chaînes internes. `affiche` dispose
désormais d'une entrée revue issue de CFDICT et ne peut plus remonter `gigue`
ou `punaise`. Les résultats saisis en chinois n'affichent plus de pinyin; l'IPA
du français étudié reste conservé.

Correctif 0.3.1 lancé le 24 septembre: les fichiers et le paquet de données sont
liés à une même version afin d'empêcher tout assemblage de caches anciens et
nouveaux. Le contrôle de traduction parcourt tout le paquet et protège 23
témoins revus. Le TTS du Reader sélectionne exclusivement une voix française,
avec choix, essai et diagnostic visibles dans l'application.

Correctif Web 0.3.2 lancé le 24 septembre: la détection TTS est relancée depuis
le geste utilisateur et au retour dans l'application; l'aide indique comment
installer une voix française sur Android ou iPhone et rappelle que les
navigateurs intégrés peuvent masquer les voix système. La recherche chinoise
préfère désormais un mot exact du complément à une sous-chaîne fortuite. Une
couche éditoriale traçable ajoute `péter`, `lâcher une caisse` et `bananer`, et
remplace le sens `香蕉人` de `banane` par l'insulte légère réellement utile en
France. La variante épouse reçoit des motifs intérieurs `桂花` et `月饼`, sans
modifier les icônes ni le thème global.

Retour appareil du 24 septembre: la PWA installée sur l'iPhone produit bien du
son, mais la voix française automatiquement retenue donne une prononciation
perçue comme fortement non native. Sur le téléphone Android testé dans le
navigateur Web, aucune voix n'est exposée. Le premier cas devient le défaut
prioritaire: une locale `fr-FR` ne suffit pas à garantir une voix pédagogique
acceptable. Le choix doit être qualifié sur l'iPhone réel et partagé par le
dictionnaire et le Reader.

### Ordre de reprise après Web 0.3.3

| Priorité | Lot | Résultat attendu |
| --- | --- | --- |
| 1 | **W1.6 — voix et stabilisation appareil** | Qualifier le choix système déjà livré puis comparer une solution de voix naturelles intégrées à Pangmao selon D-042, avec femme/homme et débit sans réglages système complexes. Vérifier dictionnaire, Reader, persistance, cache, mise à jour et hors-ligne disponible sur l'iPhone. La recette utilise des phrases françaises communes, pas un nom propre isolé. |
| 2 | **W1.5 — profondeur lexicale** | Journal local exportable des recherches manquées, extension par lots du français courant, oral, familier, argotique et des locutions, formes fléchies, registres, traductions chinoises revues et premiers exemples authentiques attribués. |
| 3 | **W2 — Reader avancé** | Traduction ou explication de phrase, reprise TTS depuis le mot choisi, historique de plusieurs textes et amélioration progressive des imports. |
| 4 | **W3 — image et OCR** | Import de photo ou capture, sélection et gel d'une zone, OCR puis envoi vers traduction, segmentation et dictionnaire. |
| 5 | **W4 — apprentissage** | Conjugaisons, collocations, statuts lexicaux, cartes/SRS, exemples IA à la demande clairement marqués, puis images mnémotechniques facultatives et STT/prononciation. |
| 6 | **Validation élargie et clients natifs** | Bêta sinophone en France, portage Android léger avec packs téléchargeables, Google Play, puis iOS natif/App Store lorsque l'usage ou les revenus le justifient. |

Cet ordre reste piloté par les retours d'usage. Un défaut bloquant de la boucle
actuelle passe avant une nouvelle fonction; les lots 2 à 5 ne doivent pas être
ouverts simultanément.

### W1.6 — voix française et stabilisation appareil

Les contrôles suivants portent sur la synthèse système déjà livrée. La cible
intégrée définie le 3 octobre est précisée après ce checkpoint; elle ne se
limite pas à installer ou sélectionner une voix dans les réglages iOS.

- inventorier sur l'iPhone pilote le nom, l'identifiant, la locale et le type
  local ou distant de chaque voix déclarée française;
- permettre d'écouter une même phrase témoin avec chaque voix, puis conserver
  explicitement le choix approuvé sur cet appareil;
- ne plus choisir une voix par la seule étiquette `fr-FR`: utiliser le choix
  validé lorsqu'il existe et laisser l'utilisatrice le remplacer;
- appliquer la même voix, le même débit et les mêmes règles au mot isolé et au
  Reader;
- conserver un diagnostic local exportable pour pouvoir distinguer un défaut
  Pangmao d'une limite Safari/iOS, sans télémétrie implicite;
- valider sur l'iPhone installé: ouverture depuis l'icône, dictionnaire, Reader,
  redémarrage, mise à jour, fonctionnement hors ligne et persistance du choix;
- traiter l'absence de voix dans un navigateur Android comme une capacité
  système distincte: elle ne doit ni provoquer une voix chinoise de repli ni
  masquer la réussite du parcours iPhone.

État Web 0.3.3: deux profils locaux `女声` et `男声` sont implémentés. Chaque
profil peut recevoir une voix française distincte après écoute de la même phrase
témoin; le choix actif est partagé par les fiches et le Reader. L'inventaire
français et les choix enregistrés peuvent être copiés localement pour le
diagnostic. Le retour du 3 octobre confirme le choix Amélie/Thomas et un accent
nettement amélioré; le timbre reste robotique. Persistance et Reader ne sont
pas encore explicitement acceptés. W1.6 reste partiellement validé.

La décision ultérieure [D-042](PRODUCT_DECISIONS.md#d-042--voix-naturelles-intégrées-sans-configuration-système)
fixe la cible: voix naturelles, femme/homme et débit dans Pangmao, sans parcours
de réglage iOS/Android complexe. Le choix système existant reste un secours;
l'amélioration du débit seule ne remplace pas un moteur de meilleure qualité.
Comparer d'abord un petit lot avec deux voix: audio de dictionnaire préparé en
amont et mis en cache à la demande, puis moteur local ou serveur pour le Reader.
Ces pistes exigent évaluation des licences, qualité, poids, délai, coût et
confidentialité; aucun fournisseur ni coût récurrent n'est choisi.

### W1.5 — profondeur lexicale et registres

- mesurer les recherches sans résultat à partir d'une liste exportable locale,
  sans collecte analytique implicite;
- étendre l'inventaire avec des sources compatibles couvrant locutions,
  français familier, argot, vulgarismes et variantes régionales;
- conserver obligatoirement le registre, le sens, la provenance et une
  traduction chinoise revue; une proposition automatique reste explicitement
  marquée et ne devient jamais silencieusement une entrée directe;
- contrôler la recherche bidirectionnelle, les faux positifs de sous-chaîne et
  les sens socioculturels peu pertinents pour une apprenante vivant en France;
- ajouter un premier lot d'exemples authentiques attribués aux entrées
  prioritaires, sans attendre l'ensemble des outils pédagogiques;
- intégrer par lots mesurés plutôt que corriger uniquement les mots signalés.

Plan de sources et portes qualité:
[WEB_LEXICAL_DEPTH_PLAN.md](WEB_LEXICAL_DEPTH_PLAN.md).

La première tranche de journal est déjà préparée en Web 0.3.4 dans la
[PR brouillon #14](https://github.com/kevindassie-ui/Pangmao/pull/14), sur
`feat/web-missed-searches-20261003`. Elle n'est ni fusionnée ni publiée et
n'ajoute pas de nouvelles entrées. Réutiliser son
[plan existant](https://github.com/kevindassie-ui/Pangmao/blob/b6095fd6bd69cf69e6cba77177742502fbf01491/docs/WEB_MISSED_SEARCHES_PLAN.md);
la reprise et les portes de promotion sont dans [NEXT_RELEASE.md](NEXT_RELEASE.md).

### W2 — Reader français

- saisie, collage et import `.txt`;
- découpage en phrases et mots français, avec fiche contextuelle au toucher;
- traduction de phrase séparée de la définition lexicale;
- lecture depuis une phrase puis reprise depuis le mot précisément touché;
- historique local, favoris et statut lexical après stabilisation des
  identifiants.

État au 24 septembre 2026: la première tranche Web est implémentée avec saisie,
collage, import `.txt` jusqu'à 1 Mio, segmentation locale en phrases et mots,
TTS explicite par phrase, ouverture de la fiche au toucher et conservation
locale du brouillon. La traduction de phrase, la reprise audio au milieu d'une
phrase et l'historique de plusieurs textes restent dans les tranches suivantes;
aucune traduction de phrase factice n'est affichée pour remplir l'interface.

### W3 — images, OCR et continuité entre applications

- import d'une photo ou d'une capture d'écran;
- sélection, recadrage et gel d'une zone de texte avant analyse;
- passage de la zone choisie vers traduction, découpage et fiches;
- partage vers Pangmao lorsque le navigateur ou la plateforme le permet;
- solution de repli claire par collage ou import;
- extension native ultérieure `Traduire avec Pangmao` pour une sélection de
  texte ou une capture d'écran.

### W4 — profondeur pédagogique

- exemples humains plus longs, naturels, contextualisés et attribués;
- génération à la demande d'exemples ou d'expressions, toujours marquée
  `Généré par IA` lorsqu'elle ne provient pas d'un corpus validé;
- images mnémotechniques générées par IA, facultatives et signalées;
- conjugaisons, flexions, collocations, cartes et répétition espacée seulement
  après mesure de la qualité des sources et validation des parcours principaux.

W1.5 livre les premiers exemples authentiques nécessaires pour comprendre les
nouveaux sens. W4 étend ensuite cette couverture à un parcours pédagogique plus
large et ajoute les générations IA facultatives, toujours étiquetées.

### Après validation du MVP Web

1. Stabiliser le produit à partir des retours d'usage de l'épouse du porteur du
   projet, puis élargir à quelques bêta-testeurs chinois en France.
2. Développer le versant « J'apprends le français » sur Android et le distribuer
   via Google Play en France/international, avec des canaux compatibles avec la
   Chine continentale lorsque le marché le justifie.
3. Développer l'application iOS native et la publier sur l'App Store après
   revenus ou validation d'usage suffisante pour justifier l'Apple Developer
   Program.
4. Reprendre « J'apprends le chinois » sur la structure existante, puis
   rapprocher les deux versants autour des mêmes contrats de données et règles
   produit.

## v0.2.1 — stabiliser · publiée

- Corriger le crash de l'écriture manuscrite.
- Corriger l'interface `zh-Hans`.
- Publier un APK universel et un APK ARM64 plus léger.

Historique: [STATUS.md](STATUS.md#corrective-release-021) et
[CHANGELOG.md](../CHANGELOG.md#021--2026-09-12). `NEXT_RELEASE.md` décrit
désormais la reprise Web actuelle.

## v0.3.0 — comprendre un texte · publiée

- Unifier le moteur de la recherche et du lecteur.
- Afficher une interprétation globale puis des blocs logiques nettement séparés.
- Pour chaque bloc: hanzi, pinyin, définition choisie et accès à la fiche.
- Remplacer le découpage glouton par une segmentation contextuelle; couvrir
  notamment `大 / 笨蛋` et `我 / 喜欢 / 一 / 起床 / 就 / 吃 / 面条`.
- Ajouter le choix persistant `français`, `anglais` ou `les deux`.
- Ajouter traduction de phrase, pinyin et définitions sous forme de couches
  activables plutôt que d'écrans supplémentaires.
- Ajouter la lecture vocale du texte complet. La pause, la vitesse et le suivi
  visuel restent dans l'itération audio dédiée afin de ne pas fragiliser ce lot.
- Ajouter l'historique des requêtes, en distinguant l'historique déjà existant
  des fiches consultées.
- Colorer les hanzi selon leur ton, ajouter le petit chat et alléger le titre en
  `胖猫`.

## v0.3.1 — lisibilité et qualité · publiée

- Ne jamais demander un modèle de traduction lorsqu’une définition de
  dictionnaire répond déjà exactement à la recherche.
- Corriger les traductions automatiques erronées remontées pendant les tests.
- Placer le pinyin continu et la traduction immédiatement sous le texte.
- Supprimer l’aperçu mot à mot redondant tout en conservant les cartes détaillées.

## v0.4.0 — enrichir le dictionnaire français · publiée

- Rendre l’analyse mot à mot repliable et corriger le repositionnement du pinyin.
- Déplacer l’OCR vers les actions rapides de l’accueil.
- Corriger les traductions contextuelles signalées et protéger les termes
  culturels tels que `肉夹馍` contre les calques littéraux.
- Dédupliquer les exemples par phrase chinoise et préparer leur schéma bilingue.
- Ajouter des compléments éditoriaux bilingues revus pour les lacunes observées.
- Organiser les fiches en onglets, rendre leurs caractères cliquables et ajouter
  un explorateur de mots avec filtres combinables.
- Diagnostiquer l’absence de voix chinoise au lieu de laisser le TTS échouer
  silencieusement.

Plan d’exécution et critères d’acceptation: [V0.4_RELEASE_PLAN.md](V0.4_RELEASE_PLAN.md).

## v0.4.1 — stabiliser les retours appareil · publiée

- Empêcher le chevauchement des hanzi colorés sur plusieurs lignes.
- Fiabiliser l’initialisation TTS avec les moteurs Android qui répondent avant
  l’affectation de leur instance, sélectionner d’abord une voix chinoise
  disponible et relancer la détection au retour des paramètres.
- Protéger `肉夹馍`/`肉夾饃` avant la traduction automatique, dans le lecteur
  comme dans l’analyse de texte du dictionnaire.

## v0.5.0 — profondeur et qualité du corpus · publiée

- Importer un corpus chinois–français direct, versionné et attribué; mesurer la
  couverture commune avec le corpus chinois–anglais.
- Harmoniser les lacunes français/anglais par lots révisables, sans présenter
  une sortie automatique comme donnée authentique.
- Ajouter davantage d’exemples dans les deux langues et des tests de naturel,
  registre, contresens et duplication.
- Ajouter une vue détaillée par source, registre et catégorie grammaticale quand
  la donnée est assez fiable.

La v0.5.0 reste volontairement centrée sur les données. La compatibilité TTS
signalée sur l’appareil de test est isolée dans la v0.5.1; les commandes audio
et l’extension de l’explorateur passent ensuite dans un lot distinct afin que
chaque version possède des critères d’acceptation cohérents.

La vue détaillée par définition reste également différée tant que les données
fiables de source, registre et catégorie sont trop rares pour justifier un écran
supplémentaire.

Plan d’exécution et portes de décision: [V0.5_RELEASE_PLAN.md](V0.5_RELEASE_PLAN.md).

## v0.5.1 — compatibilité et corrections · publiée

- Séparer les blocs logiques, visibles par défaut, de l’interprétation mot à mot
  facultative.
- Corriger la traduction contextuelle signalée avec `才` dans les deux surfaces.
- Déclarer les moteurs TTS dans la visibilité Android et essayer les moteurs
  installés en repli lorsque le moteur par défaut échoue.

## v0.6.0 — modes d'apprentissage · publiée

Ajout d'un réglage distinct de la langue d'interface:

- `J'apprends le chinois`;
- `J'apprends le français`;
- `J'apprends l'anglais`.

Un sélecteur compact sur l'accueil permet de changer rapidement de profil.
Le mode chinois conserve hanzi, pinyin, tons et caractères au premier plan. Les
modes français et anglais inversent la hiérarchie: mot cible, prononciation,
grammaire, formes et sens chinois.

La v0.6 livre la consultation directe avec les formes, prononciations, genres et
catégories disponibles dans FreeDict/WikDict. Les conjugaisons françaises, les
variantes UK/US et les collocations anglaises restent différées jusqu'à disposer
de sources libres dont la couverture et la qualité auront été mesurées.

Plan d'exécution et portes de données: [V0.6_RELEASE_PLAN.md](V0.6_RELEASE_PLAN.md).

## Après v0.6 — connectivité, parole et diffusion

- Conserver un mode hors ligne complet et ajouter un mode en ligne explicitement
  activé pour la traduction, le TTS ou le STT améliorés.
- Ajouter le STT comme quatrième saisie de l'accueil et partager ses briques
  techniques avec la future application de transcription longue.
- Développer l'analyse grammaticale contextuelle, le suivi des mots connus, la
  difficulté des textes et le parcours capture → définition → carte.
- Préparer commercialisation, conformité, bêta élargie et portage iOS seulement
  après stabilisation des profils d'apprentissage.

Stratégie détaillée: [POST_V0.6_STRATEGY.md](POST_V0.6_STRATEGY.md).

## v0.7.0 — apprendre et écouter · publiée

- Pause/reprise par phrase, arrêt, vitesse persistante et lecture robuste des
  textes longs dans le Reader.
- Ajout ou retrait d'une carte directement depuis la mini-fiche d'un mot du
  Reader, sans réinitialiser une planification SRS existante.

Plan d'exécution et validation: [V0.7_RELEASE_PLAN.md](V0.7_RELEASE_PLAN.md).

## v0.8.0 — Reader interactif · publiée

- Ajouter `0,6×` tout en conservant `0,8×`, `1×` et `1,2×` dans un sélecteur
  compact.
- Distinguer reprise suivie, recommencement du texte et lecture depuis une phrase
  touchée, avec repli explicite au début de la phrase selon le moteur TTS.
- Surligner la phrase active et exploiter la plage parlée lorsqu'elle est fournie.
- Définir une sélection faite par appui long dans le champ éditable.
- Copier pinyin, traductions et contenu d'un mot segmenté.
- Ajouter un test de voix et les détails du moteur dans une vue secondaire.

Plan borné et critères d'acceptation: [V0.8_RELEASE_PLAN.md](V0.8_RELEASE_PLAN.md).

## v0.8.1 — commandes vocales compactes · publiée

- Remplacer les boutons de vitesse débordants par un curseur discret à quatre
  crans et déplacer l'essai vocal et les détails dans un menu compact.

## v0.9.0 — vocabulaire maîtrisé · publiée

- Marquer indépendamment les mots `À apprendre`, `Connus` ou non marqués.
- Afficher une couverture lexicale honnête dans le Reader, sans faux niveau
  CECR, et proposer un surlignage facultatif des mots à revoir.
- Retrouver les mots à apprendre, connus et favoris dans l'écran Cartes.
- Remplacer le carrousel de phrases par une navigation compacte.

Plan et critères d'acceptation: [V0.9_RELEASE_PLAN.md](V0.9_RELEASE_PLAN.md).

## Apprentissage avancé

### v0.10.0 — Reader et parcours d’apprentissage · publiée

- Rendre l’ajout aux cartes explicite dans la fiche tout en maintenant
  l’indépendance entre statut lexical et répétition espacée.
- Passer le Reader en mode Lecture par défaut avec modification explicite,
  sélection directe d’une phrase et surlignage du point de départ.
- Donner davantage de hauteur au contenu grâce à un en-tête compact persistant.
- Placer la couverture du vocabulaire après la traduction et avant la lecture
  segmentée.
- Préparer une aide contextuelle puis une FAQ recherchable; un assistant en
  ligne ne sera évalué qu’après stabilisation de cette documentation.

### v0.11.0 — tracé des caractères · publiée

- Ajouter l’ordre des traits animé, contrôlable et entièrement hors ligne dans
  les fiches à un caractère.
- Proposer un entraînement manuscrit guidé et tolérant qui vérifie ordre,
  direction et trajectoire générale sans toucher aux cartes SRS.
- Isoler les données graphiques du dictionnaire et conserver leur licence et
  transformation reproductible.

Plan et critères d’acceptation: [V0.11_RELEASE_PLAN.md](V0.11_RELEASE_PLAN.md).

### v0.12.0 — première saisie vocale · publiée

- Commencer le STT court comme quatrième saisie de l’accueil, avec microphone
  toujours explicite et correction avant recherche ou Reader.
- Séparer les contrats de moteur et de capture du parcours UI afin de pouvoir
  réutiliser ces briques dans l’application de transcription longue.

Plan et matrice de validation: [V0.12_RELEASE_PLAN.md](V0.12_RELEASE_PLAN.md).

- Cartes par sens, modes reconnaissance/écoute/écriture et statistiques utiles.
- Radicaux, composants et vue arborescente caractère → mots → expressions.
- Étendre l’explorateur: favoris, niveau HSK, longueur, ordre alphabétique avancé
  et relations caractère–mot–expression.
- Notes, étiquettes personnelles, export et sauvegarde locale chiffrée.

## Idées à évaluer, sans engagement

- Définitions chinoises monolingues pour apprenants avancés, avec source
  visible et reformulation pédagogique optionnelle graduée de B1 à C2.
- Bascule simplifié/traditionnel et zhuyin.
- Recherche floue pour pinyin fautif et variantes orthographiques.
- Grammaire contextuelle et patrons de phrase.
- Cantonais et jyutping.

## Écartées pour l'instant

- Fil d'actualités, cours propriétaires ou bibliothèque éditoriale à maintenir.
- Réseau social, comptes obligatoires ou synchronisation serveur Pangmao.
- Gamification envahissante, publicités et fonctions nécessitant un abonnement.
- Nouveaux onglets principaux pour chaque outil: préférer des vues secondaires.

## Inspirations UX publiques

- [Pleco](https://play.google.com/store/apps/details?id=com.pleco.chinesesystem):
  consultation par toucher, partage Android, OCR, écriture et outils réunis.
- [Du Chinese](https://duchinese.net/): définition instantanée, traduction de
  phrase, couches pinyin/traduction et audio synchronisé.
- [Hanping](https://hanpingchinese.com/): recherche hors ligne, presse-papiers,
  reconnaissance et accès Android rapide.
- [Skritter](https://skritter.com/): ordre des traits et répétition espacée
  centrée sur l'écriture.
- [欧路词典](https://www.eudic.net/): historique, notes, listes personnelles,
  prononciation et consultation transversale pour apprenants sinophones.
