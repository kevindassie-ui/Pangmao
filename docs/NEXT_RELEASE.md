# Pangmao — prochaine reprise dans Codex

Checkpoint de développement: 2026-10-08 (Europe/Paris). Retour d'écoute retrouvé
et nouveaux défauts indépendants de la vitesse. Comparaison v3 publiée puis
non acceptée: aucune amélioration, « médecin »→« meudecin » en plus.
Diagnostic A/B/C d1 préparé pour isoler codec et entrées phonétiques.
État et preuves: [STATUS.md](STATUS.md).

## Point de départ

Lire [../AGENTS.md](../AGENTS.md), [STATUS.md](STATUS.md),
[PRODUCT_DECISIONS.md](PRODUCT_DECISIONS.md), puis
[KNOWN_ISSUES.md](KNOWN_ISSUES.md). La roadmap séquence les lots; les anciens
plans `V0.*` décrivent des versions Android historiques.

- Base publiée: `main`, Web 0.3.3; Android signé `v0.12.2` gelé.
- Candidat sauvegardé: `feat/web-missed-searches-20261003`,
  head vérifié avant le lot de comparaison v3:
  `7b61890692571377813f240b0db0063470a571c5`,
  [PR #14 en brouillon](https://github.com/kevindassie-ui/Pangmao/pull/14).
- Son plan [WEB_MISSED_SEARCHES_PLAN.md](WEB_MISSED_SEARCHES_PLAN.md) existe
  sur cette branche et inclut maintenant le petit lot vocal. Le lire sur place,
  sans en créer une copie. `b6095fd` est le candidat historique du 3 octobre,
  antérieur à cet essai; revérifier le head de la PR avant toute modification.

Avant de reprendre le candidat, vérifier l'état Git, récupérer `main` et la
branche, puis rapprocher les références documentaires de `main` avec celles de
la branche. Conserver le travail existant et les derniers workflows CI; ne pas
réécrire ni forcer l'historique distant. La PR reste en brouillon, sans fusion
ni promotion de l'application stable pendant la comparaison des voix.
Un essai statique isolé peut servir à la recette du petit lot, distinct de la
publication de l'application 0.3.4.

## Première priorité — W1.6, voix naturelles intégrées

Le choix Amélie/Thomas et l'amélioration de l'accent sont confirmés. Le naturel
reste insatisfaisant; persistance et Reader ne sont pas encore explicitement
acceptés. La décision [D-042](PRODUCT_DECISIONS.md#d-042--voix-naturelles-intégrées-sans-configuration-système)
demande une solution dans Pangmao, avec voix femme/homme et débit, sans parcours
de réglage système complexe. Aucun moteur de production n'est encore choisi.

Le petit lot est maintenant implémenté dans `webApp/voice-trial/`: deux voix
UPMC Jessica/Pierre et candidats SIWIS / MLS 1840, six textes identiques.
La révision 3 conserve les fichiers UPMC v2 et ajoute douze extraits candidats:
24 fichiers, 835 500 octets. Les heures et « je voudrais acheter » gardent la
préparation de v2, sans nouvelles substitutions ciblées.
La [page d'essai publiée](https://kevindassie-ui.github.io/Pangmao-Web/voice-trial/)
sert à la recette; l'application stable reste en 0.3.3. La CI Web du head
`b727fa7` est réussie
([run 37775162253](https://github.com/kevindassie-ui/Pangmao/actions/runs/37775162253)).
Tester la phrase quotidienne et la phrase longue à 1×, puis les liaisons et
nombres avec les deux voix. Évaluer qualité sur iPhone/Android,
licences de redistribution, poids, délai, coût, confidentialité et hors-ligne.
Les pistes déjà cadrées sont l'audio de dictionnaire produit en amont et mis en
cache à la demande, puis un moteur Reader local ou serveur à comparer. Une
piste n'est ni un fournisseur validé ni un engagement de dépense. Un éventuel
appel distant reste soumis au consentement explicite de D-016.

Le naturel de Jessica/Pierre est favorable globalement, mais la recette v2
signale encore « baguette », « avocat », « fixé » et le « et » avant « prendre »,
aux différentes vitesses. Ne pas traiter ce retour par un simple réglage de débit.
L'essai v3 conserve les fichiers v2 et permet une comparaison avec SIWIS / MLS
1840, aux mêmes textes et vitesses. Le titre affiche « ESSAI DES VOIX 3 ».
Le profil MLS reste non assigné; ne pas présenter l'ID comme une preuve de genre.
Le dernier retour constate aucune amélioration et ajoute « médecin »→« meudecin »
avec la voix masculine. **La recette v3 a échoué.** Ne pas demander à nouveau
de modifier la vitesse ni considérer la bonne livraison des fichiers comme
une preuve de bonne prononciation. Pierre est inchangé depuis v1 sur médecin;
MLS est nouveau, et le retour ne distingue pas ces locuteurs.

La reprise a comparé la conversion texte→phonèmes avec l'ancienne chaîne
Piper 2023.11.14-4: mêmes séquences sur six témoins (le CLI historique aplatit
les groupes de phrases). Le modèle ne précise pas la version exacte de son
phonémiseur d'entraînement. SIWIS et MLS reçoivent
`medəsˈɛ̃`, avec un schwa après d absent des IPA du pack; leur première voyelle
reste `e`, donc la cause exacte du « meu » entendu n'est pas établie. Les
modèles comparés partagent encore eSpeak via Piper. Pour baguette et fixé,
les consonnes attendues sont déjà présentes dans l'entrée: un audit du texte
seul ne suffira pas. Conserver les extraits actuels comme témoins de défauts;
ne pas ajouter aveuglément des substitutions à chaque mot ou généraliser ces
voix.

Le diagnostic d1 est implémenté dans `webApp/voice-trial/diagnostic/`:
Jessica/Pierre, quatre passages, 20 fichiers / 1 366 558 octets. Comparer d'abord
A (MP3) et B (WAV issu exactement de la même synthèse) sur médecin et la phrase
d'achat, puis C quand disponible. C change uniquement l'entrée phonétique de
médecin ou avocat, d'après les IPA du pack. Les contrôles ont été régénérés pour
ce diagnostic; ils ne remplacent pas les fichiers v3. Le lecteur reste à 1×,
sans réglages système. La page exige une connexion, sans cache hors ligne
promis ni envoi de texte ou d'avis. Les 60 tests Node et validations du lot
passent; la qualification auditive sur téléphone reste ouverte.

Interpréter séparément A/B (codec/conteneur et lecture) et B/C (deux hypothèses
phonétiques). Un résultat sur ces deux mots ne valide ni baguette/fixé ni le
Reader libre. Consigner le retour avant de choisir un autre moteur ou de
généraliser ces voix. Reader libre et corpus complet restent à intégrer séparément.

## Travail préparé — W1.5, journal local 0.3.4

Réutiliser l'implémentation de la PR #14; ne pas recréer le journal. La recette
de promotion reste celle de son plan: validation vocale 0.3.3, vérification du
candidat et de ses deux variantes, puis recette iPhone du journal lors de sa
future publication (activation, compteurs, complément chinois, copie/export,
réouverture, hors-ligne et préservation des autres données locales).

La traduction de phrase, la reprise au mot, l'OCR, les cartes Web et les clients
natifs restent les lots suivants de [ROADMAP.md](ROADMAP.md). La reconnaissance
manuscrite Android devra être réparée avant une nouvelle diffusion Android.

## Message de démarrage à copier

> Reprends Pangmao en lisant AGENTS.md, docs/STATUS.md, docs/NEXT_RELEASE.md et
> docs/PRODUCT_DECISIONS.md. Vérifie main et la PR brouillon #14
> (feat/web-missed-searches-20261003, candidat Web 0.3.4); conserve le travail
> existant. Priorité: voix françaises naturelles intégrées, sans réglages système
> complexes. Le petit lot vocal est implémenté: reprendre ses preuves et le
> retour d'écoute avant de produire le dictionnaire complet ou choisir un moteur
> Reader. La PR reste en brouillon tant que les portes de recette sont ouvertes.

## Ancienne cible de ce fichier

La stabilisation Android v0.2.1 du 12 septembre est publiée et ses corrections
manuscrite/`zh-Hans` avaient été confirmées sur appareil. Son historique reste
dans [STATUS.md](STATUS.md#corrective-release-021) et
[../CHANGELOG.md](../CHANGELOG.md#021--2026-09-12), ainsi que dans Git. Elle ne
constitue plus la prochaine release; la régression manuscrite v0.12.2 est un
défaut ultérieur distinct.
