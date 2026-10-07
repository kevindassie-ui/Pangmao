# Instructions pour Codex — Pangmao

## Lire avant d'agir

- `docs/STATUS.md`: checkpoint, versions, branches et preuves de validation.
- `docs/NEXT_RELEASE.md`: reprise immédiate et message de démarrage.
- `docs/PRODUCT_DECISIONS.md`: décisions acceptées; D-042 est la direction
  vocale intégrée du 3 octobre 2026.
- `docs/KNOWN_ISSUES.md`: défauts et recette appareil encore ouverte.
- `docs/ROADMAP.md` et `docs/WEB_FRENCH_MVP.md`: ordre des lots et périmètre.
- Pour les données: `tools/SOURCES.md`, `NOTICE.md`,
  `docs/WEB_DICTIONARY_QUALITY_BASELINE.md` et `docs/WEB_LEXICAL_DEPTH_PLAN.md`.

Actualiser ces fichiers à leur place. Ne pas créer un second README, une seconde
roadmap, un nouveau registre de décisions ou un document de reprise parallèle.
Garder les plans `V0.*` et mesures datées comme historiques. Distinguer code
implémenté, candidat sauvegardé, publication et validation sur appareil.

## Dépôt et travail en cours

Le dépôt canonique est `kevindassie-ui/Pangmao`, redevenu public et vérifié le
7 octobre 2026. `app/` est le client
Android Kotlin/Compose; `webApp/` est une PWA statique JavaScript avec ses tests
Node; `tools/` contient les constructions et audits Python. Le Web n'est pas un
module Gradle. Les sorties `build/` et bases SQLite générées sont ignorées.

À ce checkpoint, `main` et le miroir public contiennent Web 0.3.3. Le journal
facultatif Web 0.3.4 existe déjà sur `feat/web-missed-searches-20261003`, PR #14
en brouillon. Son plan `docs/WEB_MISSED_SEARCHES_PLAN.md` vit sur cette branche;
le lire là où il existe, sans le recréer sur `main`. Le SHA sauvegardé et les
preuves sont dans `docs/STATUS.md`; revérifier les références avant de travailler.

Commencer par `git status --short --branch`, les remotes et l'historique.
Conserver tous les changements existants. Lors de la reprise du candidat,
rapprocher les documents actualisés et les workflows de `main` avec sa branche;
ne pas perdre la décision vocale ni recréer les fonctions déjà présentes.
Ne pas forcer les pushes ou réécrire l'historique distant pour une sauvegarde.

## Contraintes produit

- Priorité à la PWA « 我学法语 » pour sinophones; recherche français/chinois,
  pédagogie française, interface chinoise et sens distincts.
- Android `v0.12.2` est gelé, signé, avec une régression manuscrite bloquante.
  Garder application ID, version et signature; pas de refactorisation Android
  pour avancer sur le Web. Corriger/recetter ce défaut avant une nouvelle
  diffusion Android. Ne jamais utiliser les tags Android `v*` pour le Web.
- Audio: voix françaises uniquement, profils femme/homme sans inférence de
  genre par le nom système. D-042 demande une voix naturelle intégrée sans
  configuration système complexe; aucun nouveau moteur n'est encore choisi.
  Ne pas confondre amélioration de l'accent et validation du naturel/Reader.
- Hors ligne pour les capacités locales disponibles; consentement explicite
  et nature des données envoyées avant tout traitement distant (D-016).
- Préserver identifiants lexicaux, favoris, brouillon Reader et choix vocaux.
  Le journal candidat est local et désactivé par défaut, sans télémétrie.
- Sources humaines attribuées et licences vérifiées; hypothèses et générations
  IA restent visibles et distinctes, sans import automatique dans le corpus.
- La marque canonique reste verte. Le pilote rouge au cerf et les motifs
  saisonniers proviennent de `webApp/brands/wife/` via le build de variante.

## Vérification proportionnée

Pour une mise à jour documentaire: vérifier les faits contre le code, les liens
locaux, `git diff --check` et le périmètre du diff. Ne pas lancer un build
Android, une release, un nettoyage d'artefacts ou un déploiement pour ce seul lot.

Pour un lot Web, les commandes de la CI depuis la racine sont:

```bash
npm --prefix webApp test
python3 -m unittest discover -s tools/tests -p 'test_web*.py'
python3 -m unittest tools.tests.test_export_web_dictionary
python3 tools/verify_web_app.py
python3 tools/audit_web_dictionary_quality.py --strict --json-out build/reports/web-dictionary-quality.json
python3 tools/build_web_variant.py --brand wife --output build/web-wife
python3 tools/verify_web_app.py build/web-wife
```

Il n'y a pas de dépendances npm à installer ni de bundler. Pour prévisualiser:
`python3 -m http.server 8000 --directory webApp`; servir `build/web-wife` pour
la variante. Garder version du pack, imports, HTML et service worker alignés.
Les tests protègent notamment `affiche`, `avocat`, `banane`, `放屁` et `臭屁`.
La baseline distingue revue humaine et comparaison inverse automatique.

Le candidat contient `webApp/voice-trial/`: six textes fixes, deux voix et un
cache audio séparé chargé à la demande. Le générateur est manuel, hors CI;
il désactive la télémétrie ONNX avant initialisation. Ne pas distribuer les
poids du modèle, ni présenter cet essai comme un moteur Reader validé.

À la reprise Android seulement: JDK 17 et SDK Android 35, reconstruction ou
restauration vérifiée des bases avec `tools/fetch_and_build_dictionary.sh` et
`tools/fetch_and_build_strokes.sh`, validateurs/audit et tests Python, puis
`./gradlew --no-daemon testDebugUnitTest lintDebug assembleDebug`.
Les clés de signature restent dans les secrets GitHub Actions; ne jamais les
copier dans le dépôt ni en afficher les valeurs.

## CI, sauvegarde et publication

Les workflows filtrent les chemins. Android CI ne reconstruit pas un lot limité
au Web ou à la documentation; un changement dans les tests/outils partagés peut
en revanche la déclencher. L'APK debug est uploadé sur lancement manuel seulement
et expire après trois jours. Rapports d'échec et paquets Web expirent après un
jour. Les releases signées sont des actifs distincts. Le workflow de suppression
des artefacts est limité à un manifeste revu; ne pas le relancer pour une reprise.

`Pangmao-Web` est uniquement un miroir public des fichiers statiques validés:
modifier/tester dans le dépôt canonique, construire le pilote, puis publier
seulement à l'étape de promotion prévue. Ne pas y développer les fonctionnalités.
Une sauvegarde documentaire ne fusionne pas la PR #14, ne change pas le miroir,
ne crée pas de tag et ne souscrit aucun service. Les consignes explicites de
l'utilisateur fixent le périmètre de chaque étape.
