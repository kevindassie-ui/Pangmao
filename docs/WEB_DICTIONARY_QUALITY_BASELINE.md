# Référence qualité du dictionnaire Web français–chinois

Mesure du 24 septembre 2026, produite par
[`tools/audit_web_dictionary_quality.py`](../tools/audit_web_dictionary_quality.py)
sur le paquet Web 0.3.1.

## Périmètre contrôlé

Le contrôle parcourt les **10 924 entrées**, **11 558 sens** et **15 176 couples
français–chinois** effectivement livrés. Il ne se limite donc pas au témoin
`affiche`.

Deux niveaux sont volontairement séparés:

1. vingt-trois mots usuels ou polysémiques revus servent de tests bloquants;
   ils couvrent notamment `affiche`, `avocat`, `médecin`, `bonjour`, `chat`,
   `chien`, `manger`, `boire`, `livre`, `école`, `être` et `voler`;
2. chaque couple livré est comparé, lorsqu'une entrée existe, aux définitions
   françaises du dictionnaire chinois→français indépendant déjà embarqué dans
   Pangmao.

Le lot passe les 23 témoins sans échec. Parmi les 15 176 couples:

| Résultat du contrôle croisé | Couples |
|---|---:|
| Définition inverse disponible | 7 119 |
| Accord lexical exact après normalisation | 4 962 |
| Candidats à une revue humaine | 2 157 |
| Pas de donnée inverse exploitable | 8 057 |

Ces catégories ne constituent pas une note de justesse. Une absence d'accord
exact peut venir d'un synonyme, d'une paraphrase, d'une flexion ou d'un sens
contextuel parfaitement valable. Elle crée une file de revue; elle ne déclenche
jamais une suppression ou une « correction » automatique.

## Régression `affiche`

Le résultat `affiche → gigue / punaise` ne venait pas de ces mots comme
traductions, mais de la présence de la chaîne « affiche(s) » dans leurs
définitions françaises. La recherche ne parcourt plus ce texte narratif et le
complément revu `affiche → 海报 / 告示` est protégé par les tests.

La capture du 24 septembre montrait simultanément l'interface Web 0.3 et
l'ancien paquet de 10 923 entrées: l'ancien service worker avait assemblé des
fichiers de deux versions. Web 0.3.1 versionne désormais le code et les données,
refuse un paquet dont la version diffère, purge les anciens caches et recharge
une seule fois après l'activation du nouveau service worker.

## Politique de correction

- une source ne remplace pas aveuglément une autre;
- un candidat est corrigé seulement après examen du mot, du sens et de la
  provenance;
- les sens distincts restent distincts, notamment `avocat` (律师 / 牛油果) et
  `voler` (飞 / 偷);
- le rapport est déterministe, exécuté en CI et ne modifie jamais le corpus.

Cette référence réduit les régressions et priorise la revue, mais ne permet pas
d'affirmer que 15 176 équivalents ont tous été validés manuellement. Une telle
promesse serait inexacte.
