# Roadmap produit Pangmao

Dernière mise à jour: 2026-09-12.

## Principes

- Fiabilité avant accumulation de fonctions.
- Fonctionnement local après les téléchargements de modèles choisis par
  l'utilisateur.
- Interface sobre: les options avancées restent accessibles sans encombrer le
  parcours principal.
- Données humaines conservées avec leur source; tout enrichissement automatique
  est identifié comme tel.
- Langue de l'interface et langue étudiée sont deux réglages indépendants.

## v0.2.1 — stabiliser · publiée

- Corriger le crash de l'écriture manuscrite.
- Corriger l'interface `zh-Hans`.
- Publier un APK universel et un APK ARM64 plus léger.

Spécification détaillée: [NEXT_RELEASE.md](NEXT_RELEASE.md).

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

## v0.4.1 — stabiliser les retours appareil · en préparation

- Empêcher le chevauchement des hanzi colorés sur plusieurs lignes.
- Fiabiliser l’initialisation TTS avec les moteurs Android qui répondent avant
  l’affectation de leur instance, sélectionner d’abord une voix chinoise
  disponible et relancer la détection au retour des paramètres.
- Protéger `肉夹馍`/`肉夾饃` avant la traduction automatique, dans le lecteur
  comme dans l’analyse de texte du dictionnaire.

## v0.5.0 — profondeur et qualité du corpus

- Importer un corpus chinois–français direct, versionné et attribué; mesurer la
  couverture commune avec le corpus chinois–anglais.
- Harmoniser les lacunes français/anglais par lots révisables, sans présenter
  une sortie automatique comme donnée authentique.
- Ajouter davantage d’exemples dans les deux langues et des tests de naturel,
  registre, contresens et duplication.
- Ajouter une vue détaillée par source, registre et catégorie grammaticale quand
  la donnée est assez fiable.
- Ajouter pause, vitesse, test de voix et suivi de lecture au TTS.
- Étendre l’explorateur: favoris, niveau HSK, longueur, ordre alphabétique avancé
  et relations caractère–mot–expression.

## v0.6.0 — modes d'apprentissage

Ajouter un réglage distinct de la langue d'interface:

- `J'apprends le chinois`;
- `J'apprends le français`;
- `J'apprends l'anglais`.

Un sélecteur compact sur l'accueil permettra de changer rapidement de profil.
Le mode chinois conserve hanzi, pinyin, tons et caractères au premier plan. Les
modes français et anglais inversent la hiérarchie: mot cible, prononciation,
grammaire, formes, exemples et sens chinois.

Le mode français nécessitera notamment genre, pluriel, IPA et conjugaisons. Le
mode anglais nécessitera formes, variantes UK/US, collocations et prononciations
distinctes. Ces modes ne seront publiés qu'avec des données libres suffisamment
riches: la base actuelle permet une consultation élémentaire, pas encore un vrai
dictionnaire d'apprentissage pour sinophones.

## v0.7.0 — apprendre et mémoriser

- Ordre des traits animé et entraînement manuscrit évalué.
- Cartes par sens, modes reconnaissance/écoute/écriture et statistiques utiles.
- Ajout aux cartes depuis un mot du lecteur en un geste.
- Liste de mots connus et estimation de difficulté d'un texte.
- Radicaux, composants et vue arborescente caractère → mots → expressions.
- Notes, étiquettes personnelles, export et sauvegarde locale chiffrée.

## Idées à évaluer, sans engagement

- Bascule simplifié/traditionnel et zhuyin.
- Recherche floue pour pinyin fautif et variantes orthographiques.
- Reprise rapide via partage Android, presse-papiers ou raccourci système.
- OCR avec traduction superposée et conservation facultative du texte reconnu.
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
