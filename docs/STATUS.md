# Pangmao — release status

Checkpoint date: 2026-10-08 (Europe/Paris)

## Reconnexion — 2026-10-08

- Checkout propre et aligné sur la PR #14, head distant vérifié
  `54078f51cfd0e6cf15ce690da5183c3e60c41757` avant ce lot documentaire.
  La PR reste ouverte et en brouillon. Le journal et l'essai vocal sont conservés.
- [Web CI 37659560197](https://github.com/kevindassie-ui/Pangmao/actions/runs/37659560197)
  terminée avec succès sur ce head; aucun besoin de relancer les contrôles
  fonctionnels pour corriger uniquement les références de reprise.
- Miroir au commit `c96e6404a6ae363af88468d39660ca85a2b6e16e`.
  La page d'essai répond HTTP 200; manifeste `2026-10-07-v1`, six textes,
  douze extraits et 385 252 octets. Le package stable distant reste en 0.3.3.
- `NEXT_RELEASE.md` pointait encore vers `b6095fd`, antérieur au petit lot
  vocal. La référence de départ et la preuve CI sont corrigées à leur place.
- Aucun nouveau retour d'écoute dans la demande de reconnexion. Le naturel
  reste à évaluer sur téléphone avant production du dictionnaire complet ou
  sélection d'un moteur Reader. Ce lot ne change que la documentation.

## Reprise du développement — 2026-10-07

- Le dépôt canonique `kevindassie-ui/Pangmao` est désormais public. La reprise
  demandée par l'utilisateur récupère le candidat de la PR #14 et rapproche
  `main` (`d0b1466`, nettoyage approuvé du 6 octobre), sans réécrire l'historique.
- Le journal local 0.3.4 existant est conservé. La nouvelle étape W1.6 est
  `webApp/voice-trial/`: six textes communs, douze extraits MP3, voix UPMC
  Jessica/Pierre (français de France), produits avec Piper 1.4.1. L'essai
  permet de comparer femme/homme, débit, liaisons, nombres et phrase longue
  sans installer de voix ni fournir de compte ou de clé sur le téléphone.
- Poids total des douze extraits: **385 252 octets**. Aucun audio préchargé;
  seuls les fichiers écoutés sont téléchargés et mis en cache à la demande.
  Le lecteur unique arrête l'extrait précédent; le cache audio gère les requêtes
  partielles nécessaires à Safari et reste séparé du cache de l'application.
- Modèle épinglé et vérifié par SHA-256, sources humaines attribuées,
  CC BY-SA 4.0. Modèle et moteur restent hors du produit. Génération locale
  sans service payant; télémétrie ONNX désactivée avant initialisation.
- Vérification locale: 52 tests Node, 6 tests Python Web et 5 tests Python
  d'export réussis; validateurs global/cerf et audit strict des 26 témoins
  réussis (10 926 entrées, 11 562 sens, 15 187 couples).
- L'essai est une étape de recette, pas une intégration de ces voix à tout le
  dictionnaire ou au Reader libre. Le naturel exige une écoute iPhone/Android;
  W1.6 reste ouvert, la PR #14 reste en brouillon et Web 0.3.3 reste la base
  stable. Aucun service, coût récurrent, tag ou build Android n'est ajouté.
- Le téléchargement des navigateurs Playwright n'a pas abouti dans cet
  environnement; les tests Node de plages HTTP ne sont pas une recette Safari.
  Les mesures de génération et le protocole d'écoute sont dans
  [WEB_MISSED_SEARCHES_PLAN.md](WEB_MISSED_SEARCHES_PLAN.md).

### Sauvegarde et page de recette

- Candidat sauvegardé dans la PR #14 au commit
  `0315a7903df7ed1ee24a1f3feeae20bc4d25e6d9`, avec deux parents conservant
  le candidat antérieur et le `main` courant. Aucun push forcé.
- [Web CI 37658786608](https://github.com/kevindassie-ui/Pangmao/actions/runs/37658786608)
  réussie sur ce commit: tests Node/Python, validation globale, variante cerf
  et audit strict. Aucun build Android ni upload d'APK lancé pour ce lot.
- Page de recette publiée seule dans le miroir au commit
  `c96e6404a6ae363af88468d39660ca85a2b6e16e`:
  [essai vocal](https://kevindassie-ui.github.io/Pangmao-Web/voice-trial/).
  Le diff du miroir ajoute uniquement `voice-trial/`; le site stable garde
  Web 0.3.3. Le lien d'essai du panneau À propos appartient au candidat.
  Les douze audios publiés ont été relus par HTTP et leurs tailles/hashes
  correspondent au manifeste; le package stable distant indique 0.3.3.
- Vérification du site publié dans Chrome: les deux récits MP3 sont décodés
  et atteignent leur fin, le débit passe à 0,85×, Arrêter retire la source,
  le panneau signale le cache actif. [Capture](evidence/voice-trial-20261007.jpg).
  Ceci ne valide ni la qualité à l'oreille, ni Safari/iPhone, ni une lecture
  réellement hors ligne; ces points restent dans la recette téléphone.

## Checkpoint historique du 3 octobre


## Codex handover checkpoint

- Canonical repository: private `kevindassie-ui/Pangmao`. The inspected `main`
  checkpoint is `d5252f3`; the checkout has no pre-existing uncommitted changes.
- `main` and the public `Pangmao-Web` mirror both contain Web **0.3.3**.
  The mirror version was checked in its `package.json`; this is not a fresh
  device acceptance test.
- Work in progress is already saved in
  [draft PR #14](https://github.com/kevindassie-ui/Pangmao/pull/14), branch
  `feat/web-missed-searches-20261003`, head
  `b6095fd6bd69cf69e6cba77177742502fbf01491`. Its Web **0.3.4** candidate adds
  an optional local journal of explicitly submitted searches without results:
  disabled by default, 100 queries of up to 120 characters, counters, retry,
  copy/export JSON and isolated deletion. Chinese fallback failures and stale
  results are excluded. The lexical corpus and identifiers are unchanged.
- The existing candidate plan is
  [WEB_MISSED_SEARCHES_PLAN.md on the saved branch](https://github.com/kevindassie-ui/Pangmao/blob/b6095fd6bd69cf69e6cba77177742502fbf01491/docs/WEB_MISSED_SEARCHES_PLAN.md).
  Do not recreate it on `main` or implement a second journal. The PR records
  46 passing Web tests and Web CI
  [37126331821](https://github.com/kevindassie-ui/Pangmao/actions/runs/37126331821)
  on `164089a7`; this is historical evidence, not a new check of the later head.
- User feedback recorded on 3 October confirms Amélie/Thomas selection and a
  much improved French accent on iPhone; the timbre remains robotic. Amélie is
  labelled `fr-CA`. Naturalness, persistence and Reader acceptance remain open;
  W1.6 is only partially accepted. See [KNOWN_ISSUES.md](KNOWN_ISSUES.md).
- The later accepted direction requires natural voices integrated into
  Pangmao without complex iOS/Android setup. System voice installation is a
  diagnostic option, not the target product flow. No new engine, provider or
  recurring expense has been approved or implemented. See
  [D-042](PRODUCT_DECISIONS.md#d-042--voix-naturelles-intégrées-sans-configuration-système).
- Since PR #13, Android CI is filtered by paths and uploads an APK only for a
  manual run (three days); Web packages and failure reports expire after one
  day. The current cleanup workflow has a fixed manifest of 47 legacy artifacts
  with rollback safeguards. Its presence is verified in code; this handover
  neither reruns deletions nor certifies the current quota or cleanup outcome.
- This handover updates documentation and agent instructions only. No feature,
  release tag, Android build, draft promotion or public deployment is performed.
  Resume with [NEXT_RELEASE.md](NEXT_RELEASE.md) and [../AGENTS.md](../AGENTS.md).
- Local verification on the inspected Web 0.3.3 code: Node tests pass across
  seven test files; static/pack validation passes for 10,926 entries, 11,562
  senses and 15,187 pairs; all 26 strict audit witnesses pass. Documentation
  links and whitespace checks pass. No new iPhone acceptance or full CI result
  is claimed for the 0.3.4 candidate.

## User-approved Actions cleanup — 2026-10-06

- User selected removal of only the older of the two historical universal APKs.
  Artifact `10379113716`, originating in Android CI run `34925963711`,
  was deleted: 129,221,473 compressed bytes.
- [Cleanup run 37431024963](https://github.com/kevindassie-ui/Pangmao/actions/runs/37431024963)
  completed successfully. Its paginated before/after inventory confirms exactly
  one removal; previous cleanup manifests removed zero additional artifacts.
  The source-run artifact API separately confirms the target is absent.
- The newer historical APK `10380370700` and latest ARM64 CI APK
  `10830387526` remain present and explicitly protected. Their metadata was
  checked before deletion and their presence checked again afterward.
  Signed Releases, reports, newer Web packages, caches and run history are unchanged.
- Pangmao now retains 71 artifacts totalling 240,841,242 bytes (240.841 MB).
  This is current Pangmao artifact storage, not an account billing-dashboard
  reading or a reset of monthly accrued GB-hours.
- The maintenance workflow uses the single fixed ID approved on 6 October.
  Retained artifact and Release policy otherwise remains as documented.
  No Android build, product change, tag, draft promotion or deployment occurred.

## Actions storage audit — 2026-10-05

- Account inventory covered all four repositories exposed by the all-repository
  GitHub installation and 182 existing runs: 143 Pangmao, 31 meeting recorder,
  eight public Pages runs and none in Home-Menu.
- Verified historical cleanup run
  [37148237510](https://github.com/kevindassie-ui/Pangmao/actions/runs/37148237510)
  deleted 47 legacy `Pangmao-MVP-APK` archives on 3 October:
  5,510,076,604 bytes. This was already complete and was not repeated.
- Current Pangmao artifacts total 370,062,715 bytes after deleting only the
  superseded first Web package `10768362883` (816,548 bytes).
  [Cleanup run 37340862843](https://github.com/kevindassie-ui/Pangmao/actions/runs/37340862843)
  verified that both newer Web packages and all three protected APK archives
  remain. There are 72 retained artifacts, including 67 dictionary reports
  totalling only 142,196 bytes.
- Protected APKs remain pending any further explicit user decision:
  `10380370700` and `10379113716` (legacy universal test/rollback builds,
  258,443,210 bytes combined, expiry 15 October), and `10830387526`
  (latest ARM64 CI APK, 99,534,026 bytes, expiry 24 October).
  Signed Releases are distinct and were not deleted. Android regression/device
  acceptance status is unchanged.
- Account current artifacts total 370,231,079 bytes after this targeted cleanup.
  meeting-transcription-app retained its 168,364-byte report; its duplicate
  `alpha.3.2` archive was removed after all four file hashes matched its Release.
  Public Pangmao-Web and Home-Menu have no current artifacts.
- Current `main` and active PR #14 Android workflows already upload APKs only
  on manual runs (three days). Web packages and failure reports retain one day.
  Twelve other legacy Pangmao branches still have the old Android workflow
  (30-day APK/quality report and 14-day failure report retention). They are not
  current producers; synchronize workflow corrections before resuming them or
  rerunning their old jobs. Do not merge or delete these branches for housekeeping.
- Older archives keep their original expiry; shortening current workflow
  retention does not retroactively shorten them. Three-day retention also does
  not cap the number of concurrent manual APK archives.
- GitHub's monthly accrued artifact storage is measured in GB-hours. Removing
  stored archives does not erase already accrued usage; the 3 October alert is
  consistent with the multi-GB legacy backlog surviving into October.
  Account billing/Packages totals have not been read; the artifact inventory is
  exact for the accessible account repositories, not a billing-dashboard reading.
- No feature, tag, APK build, PR #14 promotion or public deployment was performed.

## Reprise Web — 2026-10-03

- Le blocage de stockage GitHub Actions est levé: le packaging Web du commit
  `3589e159` a réussi lors de la reprise ciblée du 2026-10-02. Les artefacts
  temporaires Web expirent après un jour; aucune dépense ni runner personnel.
- Retour utilisateur du 2026-10-03 vers 16 h 50 (Paris): le choix des voix
  fonctionne sur l'iPhone. Amélie et Thomas donnent un véritable accent français,
  nettement meilleur qu'avant, mais un rendu encore robotique et peu fluide.
  La capture indique Amélie `fr-CA` (français canadien), pas `fr-FR`.
  W1.6 est partiellement validé pour la sélection et l'accent; le naturel reste
  à améliorer. La conservation après réouverture et le Reader ne sont pas
  explicitement confirmés par ce retour.
- Première piste de qualité: télécharger dans les réglages d'accessibilité iOS
  une voix français (France) en qualité améliorée ou premium, puis vérifier
  qu'elle apparaît dans Pangmao et comparer sur la même phrase. Les voix
  disponibles dans Safari ne sont pas garanties par leur disponibilité dans
  les réglages système. Références Apple:
  <https://support.apple.com/fr-fr/111798> et
  <https://support.apple.com/fr-fr/guide/iphone/iph96b214f0/ios>.
- Le code impose actuellement `rate = 0.82`. Un débit réglable avec comparaison
  au débit 1.0 est une piste complémentaire, pas un correctif du moteur vocal.
  La visibilité des réglages (au bas de « À propos ») doit aussi être améliorée.
- Le candidat Web 0.3.4 prépare la première tranche W1.5: journal local des
  recherches explicitement soumises sans résultat, désactivé par défaut,
  borné à 100 requêtes de 120 caractères, avec compteurs, reprise, copie,
  export JSON et effacement. Les erreurs de complément chinois, les recherches
  en cours de saisie et les résultats obsolètes ne sont pas enregistrés.
- Le corpus reste à 10 926 entrées; aucun équivalent chinois ni exemple nouveau
  n'est ajouté dans ce lot. Les sources, l'audit et les 26 témoins sont conservés.
- Le candidat reste sur une branche de préparation. Aucune fusion ni mise à
  jour du miroir public avant la recette des voix 0.3.3 sur l'iPhone. Recette et
  promotion: [WEB_MISSED_SEARCHES_PLAN.md](WEB_MISSED_SEARCHES_PLAN.md).

## Current direction

- Android `v0.12.2` is the frozen, signed checkpoint for the « J'apprends le
  chinois » client. Its feature development is paused, but the release is not
  considered fully stable because automatic handwriting recognition has a
  confirmed blocking regression on the reference device.
- The active product target is Pangmao Web « 我学法语 », a Safari-installable
  PWA for real-life testing by a Chinese-speaking French learner.
- The Web dictionary is bidirectional: French and Chinese queries reach the
  same French-learning entries, while French pronunciation, grammar and usage
  remain pedagogically primary.
- Android « J'apprends le français » follows only after Web feedback and
  stabilization. Native iOS and App Store distribution follow revenues or
  sufficient usage validation.
- Both canonical clients remain in one private repository, but `webApp/` has an
  isolated build; no Android 0.12.2 code, application identifier or signing
  material is changed for the Web prototype. The deployable static files are
  mirrored separately in the public `Pangmao-Web` repository.
- Scope and acceptance gates: [WEB_FRENCH_MVP.md](WEB_FRENCH_MVP.md).

The first Web build is available at
<https://kevindassie-ui.github.io/Pangmao-Web/>. Its initial technical trial
passed on both Android and iOS on 2026-09-23. This validates cross-device
opening and the core dictionary loop, not yet stability: sustained daily use,
accessibility checks and French voice quality still require device acceptance.

## Web French pilot 0.3.3 — current implementation

- Chinese meanings now dominate the entry sheet; the original French source
  paragraph is retained behind an explicit disclosure.
- 2,393 exact French headwords receive attributed Chinese explanations from
  Chinese Wiktionary. Existing FreeDict Chinese equivalents remain available;
  reviewed CFDICT and Pangmao editorial layers bring the pack to 10,926 entries.
- An additional Chinese dictionary is split into 32 lazy shards. It exposes
  direct French meanings first and only 3,480 conservative semantic bridges;
  each inferred result is visibly labelled and names its dictionary basis.
  `臭屁` is a required regression witness.
- The canonical application keeps its green Pangmao identity. A separate
  build-only pilot variant uses a warm red palette and a cute `鹿` mascot for
  the first user, without turning that personal treatment into the global brand.
  The same private variant now includes transparent, cute `桂花` and `月饼`
  ornaments inside the application only; neither app icon is changed.
- A first attributed authentic-example batch now belongs to lexical depth W1.5.
  Broader contextual coverage and on-demand AI examples remain in W4; generated
  text will always be labelled separately from a validated corpus example.
- French search now indexes lexical forms only. The pilot regression
  `affiche → gigue / punaise`, caused by incidental prose in source definitions,
  is covered by tests; `affiche` returns reviewed equivalents including `海报`,
  `招贴` and `告示`.
- Chinese fallback cards no longer display numbered pinyin beneath the queried
  characters. French IPA remains visible because French is the studied language.
- The first Reader slice accepts pasted text and `.txt` files up to 1 MiB,
  segments locally, reads a selected sentence aloud and opens an exact lexical
  entry from a touched word. Its draft stays on the device.
- Every release now audits all 10,926 entries and 15,187 French/Chinese pairs,
  with 26 reviewed blocking witnesses and a conservative reverse-dictionary
  comparison that queues disagreements without changing them automatically.
- Static code and dictionary URLs carry the same release identifier. A pack
  from another release is rejected and fetched again, preventing the observed
  Web 0.3 interface / 10,923-entry cache mixture.
- Word and Reader speech only use voices advertised as French. The user can
  inspect, choose, persist and test the French voice; a Chinese voice is never
  accepted as an implicit fallback. Version 0.3.2 also retries voice discovery
  after the user action and after returning to the app, leaves the detection
  action enabled when the first inventory is empty, and shows Android/iPhone
  installation paths plus the limitation of embedded browsers.
- Device feedback on 2026-09-24 confirms audible speech from the installed
  iPhone PWA, but the automatically selected French-labelled voice sounds
  strongly non-native. The Android Web browser tested still exposes no voice.
  This describes the September report; the partial improvement confirmed on
  3 October is recorded above. Locale matching alone remains insufficient.
- Web 0.3.3 implements separate `女声` and `男声` profiles. Each profile stores
  a user-confirmed French voice, uses the same French-only comparison sentence,
  and applies the active choice to word and Reader playback. A copy action
  exports the local French voice inventory and both saved identifiers without
  analytics. Naturalness and complete iPhone acceptance remain open as recorded
  above; selection and improved accent have been confirmed.
- Exact Chinese fallback results outrank substring-only base matches. `放屁`
  reaches `péter` / `lâcher un pet` instead of stopping at `coussin péteur`.
  The editorial review layer also adds `bananer` and replaces the obscure
  `香蕉人` sense of `banane` with the common light insult `傻瓜 / 笨蛋 / 呆瓜`.
- Broader slang, colloquial expressions and regional vocabulary are a measured
  follow-up rather than a list of one-off patches; the source and acceptance
  plan is recorded in [WEB_LEXICAL_DEPTH_PLAN.md](WEB_LEXICAL_DEPTH_PLAN.md).

## Implemented on Android

- Offline Chinese dictionary search in Hanzi, pinyin, French and English.
- Simplified/traditional forms, tone-coloured hanzi and tone-marked pinyin.
- Entry details, authentic examples, character metadata and Android TTS.
- Context-aware search and reader segmentation with global and block-by-block
  interpretation, pinyin and definitions.
- Optional on-device sentence translation after explicit model download.
- Camera and gallery OCR using the bundled ML Kit Chinese model, with tappable
  text-region selection.
- Handwriting canvas and automatic offline recognition code after the optional,
  one-time Chinese model download; recognition is currently blocked on the
  reference device by the confirmed v0.12.2 regression.
- Separate local histories for searches and viewed entries, favourites and SRS
  flashcards.
- Rounded Material 3 / Compose interface with Porcelaine light and Sceau de nuit
  dark palettes.
- App language selection (system, French, English or Simplified Chinese) and
  theme selection (system, light or dark), stored locally.
- Persistent choice of French, English or both definition languages.
- Consistent reader actions, including a compact clear control inside the text
  field.
- Reproducible dictionary builder and validator.
- GitHub Actions workflows for build, unit tests, lint and APK artifacts.

## Historical Android data checkpoints

- Dictionary integrity: 132,342 entries, 64,912 unique Chinese examples and
  14,622 character records.
- Thirty-eight reviewed examples currently provide aligned French and English;
  this is the published v0.5 checkpoint, not a pending release target.

## Verified on GitHub Actions

- `main` is published at `kevindassie-ui/Pangmao`.
- Run `34635287544` passed dictionary validation, all eight unit tests, Android
  lint and debug APK assembly on commit `1a7a681`.
- Run `34636124452` passed the same checks after adding the release workflow on
  commit `b48d749`.
- A persistent RSA-4096 Android release key is stored only in encrypted GitHub
  Actions secrets, allowing later APKs to update the first installation.
- Phase 2 run `34659129708` passed dictionary validation, all eleven unit tests,
  Android lint and debug APK assembly on commit `1c92f89`.
- Final Phase 2 CI run `34659579861` passed the same checks and uploaded the
  debug APK on release commit `caaf6c1`.

## Previous release

- Tag: `v0.1.0-mvp` at commit `b48d749`.
- Release run `34636691630` rebuilt the dictionary, ran tests and lint, assembled
  the release APK, and verified its Android signature successfully.
- APK: `Pangmao-v0.1.0-mvp.apk` (88,575,070 bytes).
- SHA-256: `07b8b2aeed103072a3a02f0c63c0fbb4091cc9e66cfa07ff85f1eb6511638081`.
- Download: <https://github.com/kevindassie-ui/Pangmao/releases/download/v0.1.0-mvp/Pangmao-v0.1.0-mvp.apk>

## Phase 2 publication

- Tag: `v0.2.0` at commit `caaf6c1`.
- Release run `34659758215` rebuilt the dictionary, ran all tests and lint,
  assembled the release APK, and verified its Android signature successfully.
- APK: `Pangmao-v0.2.0.apk` (118,716,908 bytes).
- SHA-256: `d64365f5208ad146adb1c39106a90a45b6b5e4d20d6e7791b7c090c6f14c65ee`.
- Download: <https://github.com/kevindassie-ui/Pangmao/releases/download/v0.2.0/Pangmao-v0.2.0.apk>

## Corrective release 0.2.1

- Tag: `v0.2.1` at commit `eda8183`.
- Implementation CI run `34679865764` and final CI run `34680165813` passed
  dictionary validation, unit tests, Android lint and debug APK assembly.
- Release run `34680206681` repeated validation and tests, built both release
  variants, and verified their Android signatures successfully.
- ARM64 APK: `Pangmao-v0.2.1-arm64.apk` (66,857,764 bytes), SHA-256
  `a5aa63a8c49be59419554a26ea4d728e88f99a388366a4cea388fb7caf0c03b9`.
- ARM64 download: <https://github.com/kevindassie-ui/Pangmao/releases/download/v0.2.1/Pangmao-v0.2.1-arm64.apk>
- Universal APK: `Pangmao-v0.2.1-universal.apk` (119,370,092 bytes), SHA-256
  `f282b83e398dc328271a597b322e32d7d8a3a08a14acf5f7c507445bb59fbf4f`.
- Universal download: <https://github.com/kevindassie-ui/Pangmao/releases/download/v0.2.1/Pangmao-v0.2.1-universal.apk>
- Device acceptance completed: the handwriting crash is resolved and the
  `zh-Hans` interface switches correctly.

## Text understanding release 0.3.0

- Tag: `v0.3.0` at commit `3a39d88`.
- CI run `34687560717` passed dictionary validation, unit tests, Android lint
  and debug APK assembly.
- Release run `34687861761` repeated validation and tests, built both release
  variants, and verified their Android signatures successfully.
- ARM64 APK: `Pangmao-v0.3.0-arm64.apk` (83,416,414 bytes), SHA-256
  `8ea26d99791256e63c7d9023a97c760cfa5642918381f926a498b673125a28a6`.
- ARM64 download: <https://github.com/kevindassie-ui/Pangmao/releases/download/v0.3.0/Pangmao-v0.3.0-arm64.apk>
- Universal APK: `Pangmao-v0.3.0-universal.apk` (182,192,068 bytes), SHA-256
  `088b4e92af8b556b8dcfd500daea8a0efb7575977f7ddb510ccac643e371e069`.
- Universal download: <https://github.com/kevindassie-ui/Pangmao/releases/download/v0.3.0/Pangmao-v0.3.0-universal.apk>
- Device acceptance remains to be completed for automatic handwriting,
  segmentation, model download and the new reader workflow.

## Reader quality release 0.3.1

- Tag: `v0.3.1` at commit `fe7874f`.
- CI run `34689603981` passed dictionary validation, unit tests, Android lint
  and debug APK assembly.
- Release run `34689854701` repeated validation and tests, built both variants,
  and verified their Android signatures successfully.
- ARM64 APK: `Pangmao-v0.3.1-arm64.apk` (83,416,402 bytes), SHA-256
  `c4880e964c76a4ded198688efebd1811cf39465e0c7c8e430205cd0bfd29956a`.
- ARM64 download: <https://github.com/kevindassie-ui/Pangmao/releases/download/v0.3.1/Pangmao-v0.3.1-arm64.apk>
- Universal APK: `Pangmao-v0.3.1-universal.apk` (182,192,056 bytes), SHA-256
  `bd461e6ab4ea1df4a914fef53c51afcd2c4f376515a0475d6949e4dd885c44b6`.
- Universal download: <https://github.com/kevindassie-ui/Pangmao/releases/download/v0.3.1/Pangmao-v0.3.1-universal.apk>
- Device acceptance confirms OCR, handwriting, general dictionary, FR/EN/both
  definitions, multiword search and the compact reader are functional.
- Follow-up observations are recorded for v0.4.0: re-centre the reader when
  pinyin is re-enabled, make word-by-word analysis optional, continue improving
  French phrasing, move OCR to the home actions and expose TTS failures.

## Dictionary depth release 0.4.0

- Tag: `v0.4.0` at commit `9eaafa9`.
- Implementation CI run `34702542913` and final CI run `34702712090` passed
  full dictionary validation, unit tests, Android lint and debug APK assembly.
- Release run `34703031162` repeated validation and tests, built both release
  variants, verified their Android v2 signatures and published the assets.
- ARM64 APK: `Pangmao-v0.4.0-arm64.apk` (84,475,458 bytes), SHA-256
  `440e93a9caa1d5e1bfdd4c992463f663c94021a2c12086e5c972342507f7c3da`.
- ARM64 download: <https://github.com/kevindassie-ui/Pangmao/releases/download/v0.4.0/Pangmao-v0.4.0-arm64.apk>
- Universal APK: `Pangmao-v0.4.0-universal.apk` (183,251,112 bytes), SHA-256
  `713d736b26eddfbf14e5f1f0c5cbd2fe869efc9448a94499e925ab029e1ed160`.
- Universal download: <https://github.com/kevindassie-ui/Pangmao/releases/download/v0.4.0/Pangmao-v0.4.0-universal.apk>
- Device acceptance remains to be completed for the new tabs, character
  navigation, word explorer, OCR relocation, pinyin repositioning and TTS
  diagnostic.

## Corrective release 0.4.1

- Tag: `v0.4.1` at commit `295a709`.
- Implementation CI run `34725277909` and final CI run `34725441325` passed
  full dictionary validation, unit tests, Android lint and debug APK assembly.
- Release run `34725716490` repeated validation and tests, built both release
  variants, verified their Android v2 signatures and published the assets.
- ARM64 APK: `Pangmao-v0.4.1-arm64.apk` (84,475,982 bytes), SHA-256
  `e9e5cde8998453dffb5f1783e60ab0e6174583a82db9bfc28de83000e2927306`.
- ARM64 download: <https://github.com/kevindassie-ui/Pangmao/releases/download/v0.4.1/Pangmao-v0.4.1-arm64.apk>
- Universal APK: `Pangmao-v0.4.1-universal.apk` (183,251,636 bytes), SHA-256
  `19933c7588bb8e321264710aed0816f8b8dff368c47b579424858aadc4713e46`.
- Universal download: <https://github.com/kevindassie-ui/Pangmao/releases/download/v0.4.1/Pangmao-v0.4.1-universal.apk>
- Device acceptance remains to be completed for multiline hanzi, the existing
  Chinese TTS voice and `肉夹馍` in both translation surfaces.

## Bilingual quality baseline — v0.5 lot A

- Commit: `d016d31`.
- CI run `34727407033` passed the deterministic bilingual audit and its tests,
  full dictionary reconstruction and validation, Android unit tests, lint and
  debug APK assembly.
- Baseline: 47,071 of 132,342 lexical entries are bilingual; coverage rises to
  81.65% among entries observed at least five times in the bundled corpus.
- Examples are the main measured gap: the C2 supplement raised the baseline
  from 12 to 20 of 64,912 unique Chinese sentences with both French and English.
- This checkpoint changes neither the generated dictionary nor the APK. Lot B
  will evaluate candidate direct Chinese-French sources before any import.

## Bilingual source and data checkpoints — v0.5 lots B–D0

- Kaikki / Wiktionnaire français was limited to reviewed candidates; the full
  extraction is not safe for automatic ingestion.
- Schema 3 records compact per-definition provenance without increasing the
  database size. CI run `34742487247` passed on commit `13fff9b`.
- C2 added 77 reviewed French definitions for 46 frequent entries and eight
  aligned examples. CI run `34742814129` passed on commit `7c198dc`.
- D0 inspected the complete official Tatoeba link graph. Of 14,600 direct
  Mandarin–French alignments, 826 pass the identity and sentence-review gate,
  but they still require relation-level editorial review before import.
- D1 accepted 18 of the 23 strongest candidates after relation-level review.
  The other five remain excluded for register, intensity, fidelity or
  near-duplication. The bilingual example count is now 38 of 64,912. CI run
  `34755490616` passed on commit `83246fc`.

## Corpus quality release 0.5.0

- Tag: `v0.5.0` at commit `234576f`.
- Final CI run `34755749553` passed dictionary reconstruction and validation,
  all 16 Python tests, Android unit tests, lint and debug APK assembly.
- Release run `34756006883` repeated the complete validation, restored the
  persistent signing key, built both variants and verified their Android
  signatures before publication.
- ARM64 APK: `Pangmao-v0.5.0-arm64.apk` (84,483,074 bytes), SHA-256
  `9ef08ae5cc9cd6916d0a70b2cc6067206c66ae6cec05c53fe9bfdb41b7480a29`.
- ARM64 download: <https://github.com/kevindassie-ui/Pangmao/releases/download/v0.5.0/Pangmao-v0.5.0-arm64.apk>
- Universal APK: `Pangmao-v0.5.0-universal.apk` (183,258,728 bytes), SHA-256
  `cf8706f17c358f2c1035a230544ed695104fed316e468859087b528abb082873`.
- Universal download: <https://github.com/kevindassie-ui/Pangmao/releases/download/v0.5.0/Pangmao-v0.5.0-universal.apk>

## Corrective release 0.5.1

- Tag: `v0.5.1` at commit `a22c095`.
- Implementation CI run `34761953644` and final CI run `34762205792` passed
  dictionary validation, Python and Android unit tests, lint and debug APK
  assembly.
- Release run `34762555388` repeated the complete validation, restored the
  persistent signing key, built both variants and verified their Android
  signatures before publication.
- ARM64 APK: `Pangmao-v0.5.1-arm64.apk` (84,483,134 bytes), SHA-256
  `06f9b7af79239c0f080658611af406fe6279123956e29a4994167fadf7cfe40d`.
- ARM64 download: <https://github.com/kevindassie-ui/Pangmao/releases/download/v0.5.1/Pangmao-v0.5.1-arm64.apk>
- Universal APK: `Pangmao-v0.5.1-universal.apk` (183,258,788 bytes), SHA-256
  `90c4ccf9b6dbf4b790c52daa81fd0913522cb48b4a87a42c0ca64edfc0bf856c`.
- Universal download: <https://github.com/kevindassie-ui/Pangmao/releases/download/v0.5.1/Pangmao-v0.5.1-universal.apk>
- Device acceptance confirms that TTS now works on the reported ColorOS device
  and that logical blocks remain visible independently of the optional linear
  word gloss. Acceptance of the reviewed `才` sentence remains to be recorded.

## Learning profiles — v0.6 foundation

- Commit `19e667e` adds a persistent learning-language axis independent from
  the app locale and definition preference. Existing installations migrate to
  `CHINESE`; CI run `34770974051` passed migration tests, lint and assembly.
- Commits `e78fada` and `15a3f2c` add the translated home selector and honest
  bilingual-preview labels. CI run `34771566996` passed all checks.
- Commit `da94620` adds a non-mutating evaluation of direct FreeDict/WikDict
  sources. The measured snapshots contain 10,947 French-to-Chinese and 26,660
  English-to-Chinese entries; they qualify for a filtered pilot, not a raw
  import. CI run `34772064048` passed 18 data tests, Android tests, lint and
  debug APK assembly.
- Schema v4 and the profile-driven Android screens are published in v0.6.0;
  device acceptance remains to be recorded.

## Learning dictionary schema — v0.6 lot D1

- Schema v4 adds isolated, normalized tables for French/English lexical entries,
  forms, pronunciations, senses, Chinese equivalents and per-sense sources.
- The pinned filtered build imports 10,923 French and 26,549 English entries;
  166 source entries without a usable Chinese sense and 144 non-Han values are
  rejected deterministically.
- The existing 132,342 Chinese entries and stable identifiers are unchanged.
- The generated database grows from about 69 MiB to 92.34 MiB. Profile-driven
  Android search and entry rendering are now published; device QA remains.
- Commit `ccd7549` passed the full GitHub Actions run `34781393394`, including
  pinned-source reconstruction, 21 Python tests, Android tests, lint and debug
  APK assembly.
- Detailed measurements and invariants are recorded in
  [V0.6_SCHEMA_V4_REPORT.md](V0.6_SCHEMA_V4_REPORT.md).
- Commit `0f77fba` adds the Android learning-entry models and exact/prefix/FTS
  repository queries without changing visible behavior. Accented and Hanzi
  reverse queries are covered; CI run `34781815388` passed all data and Android
  checks.
- Commit `20db749` activates real offline French/English searches and lexical
  entry screens. It keeps the Chinese Reader unchanged and deliberately leaves
  favorites/cards disabled for the new entries until identifiers are safely
  namespaced. CI run `34786545793` passed corpus reconstruction, Android tests,
  lint and debug APK assembly.

## Learning profiles release 0.6.0

- Tag: `v0.6.0` at commit `b4a6eb0`.
- Final CI run `34812967331` passed reconstruction of all six pinned sources,
  dictionary validation, 21 Python tests, Android unit tests, lint and debug APK
  assembly.
- Release run `34813530300` repeated the complete validation, restored the
  persistent signing key, built both variants and verified their Android
  signatures before publication.
- ARM64 APK: `Pangmao-v0.6.0-arm64.apk` (95,229,198 bytes), SHA-256
  `f6e2532ff4bc378e19c68d73e1b3f83c300679a0b96a6b626c0ed72a85252334`.
- ARM64 download: <https://github.com/kevindassie-ui/Pangmao/releases/download/v0.6.0/Pangmao-v0.6.0-arm64.apk>
- Universal APK: `Pangmao-v0.6.0-universal.apk` (194,004,852 bytes), SHA-256
  `5e14e097d2c74009c34f9da85226e508d9c5884846d99754773f0da3eaefd02e`.
- Universal download: <https://github.com/kevindassie-ui/Pangmao/releases/download/v0.6.0/Pangmao-v0.6.0-universal.apk>
- Device acceptance remains to cover upgrade from v0.5.1, profile persistence,
  accent-insensitive French lookup, English lookup and Hanzi reverse lookup.

## Resumable TTS controls — v0.7 lot A1

- Commit `734f0ce` adds sentence-based playback for long Reader texts, with
  pause/resume from the current sentence, explicit stop and persistent speeds
  of `0.8×`, `1×` and `1.2×`.
- CI run `34816025784` passed full corpus reconstruction and audit, Android unit
  tests, lint and debug APK assembly without changing dictionary data.
- Device acceptance is required before adding visual tracking, because OEM TTS
  engines can differ in progress callbacks; v0.7.0 provides the signed test build.

## Learning and listening release 0.7.0

- Tag: `v0.7.0` at commit `207bb08`.
- Lot B1 CI run `34817468421` and final CI run `34817879563` passed full corpus
  reconstruction, tests, Android lint and debug APK assembly.
- Release run `34818400857` repeated the complete validation, restored the
  persistent signing key, built both variants and verified their Android
  signatures before publication.
- ARM64 APK: `Pangmao-v0.7.0-arm64.apk` (95,247,506 bytes), SHA-256
  `e5a676b68a0b203692287009deb1439e9cb6bc66915c9e77522118eb1016088d`.
- ARM64 download: <https://github.com/kevindassie-ui/Pangmao/releases/download/v0.7.0/Pangmao-v0.7.0-arm64.apk>
- Universal APK: `Pangmao-v0.7.0-universal.apk` (194,023,160 bytes), SHA-256
  `ecd18dbbb14a3bd07ef146a84e53e695404021ae5be11e386503a759be35f464`.
- Universal download: <https://github.com/kevindassie-ui/Pangmao/releases/download/v0.7.0/Pangmao-v0.7.0-universal.apk>
- Device acceptance remains to cover long-text pause/resume/stop, all three
  speech speeds and direct Reader-to-card actions.

## Interactive Reader implementation 0.8.0

- A0/A1 establish source-preserving UTF-16 sentence offsets, tracked Android
  ranges, stale-callback protection, precise resume with sentence fallback and
  the persistent `0.6×` speed. CI runs `34840167767` and `34842064354` passed.
- A2 adds sentence navigation and highlighting, distinct restart/stop controls
  and an independent voice test. CI run `34842943581` passed.
- B preserves native selection and defines an exact selection or exposes its
  recognized logical blocks. CI run `34843566276` passed.
- C copies the full text, tone-marked pinyin, separate translations and visible
  word data with a uniform confirmation. CI run `34844255209` passed.
- D reports the Android engine package, selected voice, locale and network
  requirement. CI run `34844833919` passed.
- ColorOS acceptance is tracked in [KNOWN_ISSUES.md](KNOWN_ISSUES.md).

## Interactive Reader release 0.8.0

- Tag: `v0.8.0` at commit `0b27dcd`.
- Final CI run `34845407781` passed corpus reconstruction, tests, Android lint
  and debug APK assembly.
- Release run `34846019637` repeated the complete validation, restored the
  persistent signing key, built both variants and verified their Android
  signatures before publication.
- ARM64 APK: `Pangmao-v0.8.0-arm64.apk` (95,293,686 bytes), SHA-256
  `e83be29d2d35767f6db0a819bb4e9d0e39b6882ef80075081f0add4cea624cf8`.
- ARM64 download: <https://github.com/kevindassie-ui/Pangmao/releases/download/v0.8.0/Pangmao-v0.8.0-arm64.apk>
- Universal APK: `Pangmao-v0.8.0-universal.apk` (194,069,340 bytes), SHA-256
  `88ecf5feff9f26a549bf29a6afbbf21f3b790a7089845076f18169c99f3fb969`.
- Universal download: <https://github.com/kevindassie-ui/Pangmao/releases/download/v0.8.0/Pangmao-v0.8.0-universal.apk>
- Device acceptance remains to cover ColorOS range callbacks, the selection
  workflow, all copy targets, sentence navigation and reported voice details.

## Reader ergonomics release 0.8.1

- Tag: `v0.8.1` at commit `b3024cf`.
- Implementation CI run `34895404292` and final CI run `34895946814` passed
  corpus reconstruction, tests, Android lint and debug APK assembly.
- Release run `34896620819` repeated the complete validation, restored the
  persistent signing key, built both variants and verified their Android
  signatures before publication.
- ARM64 APK: `Pangmao-v0.8.1-arm64.apk` (95,294,190 bytes), SHA-256
  `7bf8e075e88243f5273ece9eb211a0f45dd015669891550835a71a3d8166d6de`.
- ARM64 download: <https://github.com/kevindassie-ui/Pangmao/releases/download/v0.8.1/Pangmao-v0.8.1-arm64.apk>
- Universal APK: `Pangmao-v0.8.1-universal.apk` (194,069,844 bytes), SHA-256
  `48c131fa0244225189dad958e01e2ea2cae2b0ceaac8cacc2cb4570ce9d19e98`.
- Universal download: <https://github.com/kevindassie-ui/Pangmao/releases/download/v0.8.1/Pangmao-v0.8.1-universal.apk>

## Vocabulary mastery release 0.9.0

- Local three-state word knowledge and the additive user-database migration
  passed CI run `34897181176`.
- Reader coverage modeling passed run `34897902607`; dictionary and Reader
  status controls passed run `34898627436`.
- Reader coverage UI and optional review highlighting passed run `34899421460`;
  compact sentence navigation passed run `34899946543`.
- Learning, known and favorite vocabulary lists, including 500-entry SQLite
  batching, passed run `34925963711`.
- Tag: `v0.9.0` at commit `7f3d8c3`.
- Final CI run `34926379223` passed corpus reconstruction, migrations, tests,
  Android lint and debug APK assembly.
- Release run `34926881772` repeated the complete validation, restored the
  persistent signing key, built both variants and verified their Android
  signatures before publication.
- ARM64 APK: `Pangmao-v0.9.0-arm64.apk` (95,341,810 bytes), SHA-256
  `cd41400352f9d1d2bbbede9b225c40a41feb5542a1b63df57e93bd6cf9a48f03`.
- ARM64 download: <https://github.com/kevindassie-ui/Pangmao/releases/download/v0.9.0/Pangmao-v0.9.0-arm64.apk>
- Universal APK: `Pangmao-v0.9.0-universal.apk` (194,117,464 bytes), SHA-256
  `4d8f7549397dbe693244553bc2731bd691e2ecf85d72f6594a0a685568f59aa1`.
- Universal download: <https://github.com/kevindassie-ui/Pangmao/releases/download/v0.9.0/Pangmao-v0.9.0-universal.apk>
- A real update test from v0.8.1 remains to be recorded.

## Interactive learning-flow release 0.10.0

- Explicit dictionary card actions and their independence from vocabulary status
  passed CI run `34941032837`.
- The compact Reader hierarchy and reordered vocabulary coverage passed CI run
  `34941965884`.
- Reading/editing modes, sentence touch selection, Unicode-safe word long-press
  lookup and their 24 JVM tests passed CI run `34942952495`.
- Tag: `v0.10.0` at commit `c870fc9`.
- Release run `34943610317` reused the verified dictionary cache, repeated tests
  and Android lint, restored the persistent signing key, built the ARM64 release
  and verified its Android signature before publication.
- ARM64 APK: `Pangmao-v0.10.0-arm64.apk` (95,360,894 bytes), SHA-256
  `a77adb41c9827b512edefcc5a657a5c0d239d18ad568ac7406104f7b7adced6e`.
- ARM64 download: <https://github.com/kevindassie-ui/Pangmao/releases/download/v0.10.0/Pangmao-v0.10.0-arm64.apk>
- No universal APK is produced during private development; a future Android App
  Bundle will restore store-managed architecture delivery.
- Device acceptance remains to cover the v0.9.0 → v0.10.0 update, narrow-screen
  Reader layout, sentence touch selection and long-press dictionary lookup.

## Stroke-order release 0.11.0

- Tag: `v0.11.0` at commit `b705f50`.
- Release run `35142476420` rebuilt and verified the dictionary and separate
  stroke database, passed tests and Android lint, restored the persistent
  signing key, built the ARM64 release and verified its signature.
- The release adds local animated stroke order and guided practice for 9,574
  simplified and traditional characters without changing vocabulary or SRS
  state.
- Detailed implementation and remaining device checks are recorded in
  [V0.11_RELEASE_PLAN.md](V0.11_RELEASE_PLAN.md).

## Short speech-input release 0.12.2

- `v0.12.0` was published from commit `00fafc2` by signed release run
  `35249765429`.
- Corrective release `v0.12.2` was published from commit `162c300` by signed
  release run `35319016439`; tests, lint, offline databases, APK signature and
  SHA-256 artifact verification passed.
- The release adds explicit short Chinese speech input and keeps four
  single-character entry tabs visible. The corrective flow delegates to the
  provider-owned voice activity when ColorOS rejects embedded recognition.
- Scope and validation history are recorded in
  [V0.12_RELEASE_PLAN.md](V0.12_RELEASE_PLAN.md).
- Device evidence received on 2026-09-23 shows a systematic failure of
  automatic handwriting recognition across six drawings, including simple
  characters. The release therefore remains signed and reproducible, but not
  fully stable. The blocking regression is tracked in
  [KNOWN_ISSUES.md](KNOWN_ISSUES.md#régression-bloquante-confirmée--v0122).

Exhaustive corpus alignment, richer authentic examples, visual audio tracking
and interactive source details remain incomplete. The Web MVP must not present
generated examples as authentic or silently inherit data whose redistribution
conditions have not been checked.

Keep the Android application ID, GitHub signing secrets and `v0.12.2` release
unchanged so a later native version can update the existing installation. Web
versions use an independent lifecycle and must not use Android's `v*` tag
namespace.

Longer-term work is tracked in [ROADMAP.md](ROADMAP.md), accepted trade-offs in
[PRODUCT_DECISIONS.md](PRODUCT_DECISIONS.md), and confirmed regressions in
[KNOWN_ISSUES.md](KNOWN_ISSUES.md). Older release plans remain historical
records rather than the current priority.

The generated 92.34 MiB SQLite database is intentionally not committed. CI
rebuilds it from pinned, hash-verified CC-CEDICT, CFDICT, Tatoeba, Unihan and
FreeDict/WikDict sources.
