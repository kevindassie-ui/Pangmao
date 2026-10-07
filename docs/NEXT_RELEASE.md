# Pangmao — prochaine reprise dans Codex

Checkpoint de développement: 2026-10-07. Reprise autorisée par l'utilisateur,
à partir du candidat sauvegardé. État et preuves: [STATUS.md](STATUS.md).

## Point de départ

Lire [../AGENTS.md](../AGENTS.md), [STATUS.md](STATUS.md),
[PRODUCT_DECISIONS.md](PRODUCT_DECISIONS.md), puis
[KNOWN_ISSUES.md](KNOWN_ISSUES.md). La roadmap séquence les lots; les anciens
plans `V0.*` décrivent des versions Android historiques.

- Base publiée: `main`, Web 0.3.3; Android signé `v0.12.2` gelé.
- Candidat sauvegardé: `feat/web-missed-searches-20261003`,
  `b6095fd6bd69cf69e6cba77177742502fbf01491`,
  [PR #14 en brouillon](https://github.com/kevindassie-ui/Pangmao/pull/14).
- Son plan [WEB_MISSED_SEARCHES_PLAN.md](https://github.com/kevindassie-ui/Pangmao/blob/b6095fd6bd69cf69e6cba77177742502fbf01491/docs/WEB_MISSED_SEARCHES_PLAN.md)
  existe sur cette branche: le lire sur place, sans en créer une copie.

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
de réglage système complexe. Aucun moteur n'est encore choisi.

Le petit lot est maintenant implémenté dans `webApp/voice-trial/`: deux voix
UPMC Jessica/Pierre, six textes identiques et 385 252 octets d'audio au total.
La [page d'essai publiée](https://kevindassie-ui.github.io/Pangmao-Web/voice-trial/)
sert à la recette; l'application stable reste en 0.3.3. La CI Web du candidat
`0315a79` est réussie (run 37658786608).
Tester la phrase quotidienne et la phrase longue à 1×, puis les liaisons et
nombres avec les deux voix. Évaluer qualité sur iPhone/Android,
licences de redistribution, poids, délai, coût, confidentialité et hors-ligne.
Les pistes déjà cadrées sont l'audio de dictionnaire produit en amont et mis en
cache à la demande, puis un moteur Reader local ou serveur à comparer. Une
piste n'est ni un fournisseur validé ni un engagement de dépense. Un éventuel
appel distant reste soumis au consentement explicite de D-016.

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
