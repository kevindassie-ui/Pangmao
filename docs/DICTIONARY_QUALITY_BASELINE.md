# Référence de qualité du dictionnaire

Mesure du 2026-09-13, produite par
[`tools/audit_dictionary_quality.py`](../tools/audit_dictionary_quality.py) à
partir des sources épinglées de la v0.4.1.

## Méthode

- Une « entrée » est une ligne lexicale, donc une combinaison de graphies et de
  prononciation; ce n’est pas nécessairement un mot chinois unique.
- La fréquence mesure les occurrences issues du corpus Tatoeba embarqué. Elle
  sert à prioriser les données actuellement rencontrées par Pangmao, mais ne
  constitue pas une fréquence générale du chinois.
- « Bilingue » signifie que la ligne possède au moins une définition française
  et une définition anglaise. Cela ne garantit ni l’alignement sens par sens ni
  le naturel de chaque formulation.
- Les candidats d’anomalie sont signalés sans modifier la base et doivent être
  confirmés avant correction.

Sources mesurées: CC-CEDICT
`3e29e175d6186f76a6978d8716d0976a2016923f`, CFDICT téléchargé le
2026-09-11, Tatoeba 2026-05-20 et Unihan 17.0.0.

## Couverture des entrées

| Périmètre | Total | Bilingues | Anglais seul | Français seul | Sans définition |
|---|---:|---:|---:|---:|---:|
| Toutes les entrées | 132 342 | 47 071 (35,57 %) | 76 058 (57,47 %) | 9 210 (6,96 %) | 3 |
| Observées au moins une fois | 22 788 | 15 947 (69,98 %) | 5 369 (23,56 %) | 1 472 (6,46 %) | 0 |
| Observées au moins cinq fois | 6 513 | 5 318 (81,65 %) | 665 (10,21 %) | 530 (8,14 %) | 0 |

### Détail par fréquence interne

| Occurrences Tatoeba | Total | Bilingues | Anglais seul | Français seul |
|---|---:|---:|---:|---:|
| 100 et plus | 561 | 435 (77,54 %) | 80 | 46 |
| 20 à 99 | 1 764 | 1 423 (80,67 %) | 169 | 172 |
| 5 à 19 | 4 188 | 3 460 (82,62 %) | 416 | 312 |
| 1 à 4 | 16 275 | 10 629 (65,31 %) | 4 704 | 942 |
| 0 | 109 554 | 31 124 (28,41 %) | 70 689 | 7 738 |

La base contient 198 808 lignes de définition anglaises et 101 211 françaises.
Ce volume ne doit pas être interprété comme 50,91 % de sens alignés: une ligne
française peut regrouper ou découper les sens différemment de l’anglais.

## Couverture des exemples

| Total | Bilingues | Anglais seul | Français seul | Sans traduction |
|---:|---:|---:|---:|---:|
| 64 912 | 12 (0,02 %) | 64 900 | 0 | 0 |

Les 64 900 phrases anglaises proviennent de Tatoeba. Les douze exemples
bilingues sont actuellement le petit supplément éditorial Pangmao. Le déficit
français des exemples est donc la priorité de données la plus nette, mais il ne
sera pas comblé en présentant une traduction automatique comme authentique.

## Provenance

| Source | Entrées associées |
|---|---:|
| CC-CEDICT | 123 131 |
| CFDICT | 56 278 |
| Pangmao | 4 |

Une entrée peut compter dans plusieurs sources; les nombres ne sont donc pas
additifs.

## Candidats de revue

- 12 définitions sont répétées dans une même langue après normalisation de la
  casse et des espaces.
- 3 entrées CC-CEDICT n’ont aucune définition: le proverbe
  `不管白猫黑猫，捉住老鼠就是好猫`, le caractère `宊` et `石咀山市`.
- 16 563 définitions contiennent une référence pinyin CC-CEDICT du type
  `[ni3]`. Ce ne sont pas des erreurs; elles constituent un inventaire utile
  pour une future présentation structurée des renvois.
- L’heuristique volontairement conservatrice n’a détecté ni résidu HTML réel,
  ni traduction manifestement anglaise dans un champ français. Cela ne mesure
  pas encore l’idiomaticité ou les contresens, qui nécessitent le corpus de
  régression du lot C.

## Décisions pour les lots suivants

1. Prioriser d’abord les 665 entrées anglaises seules et les 530 entrées
   françaises seules observées au moins cinq fois, avant la longue traîne.
2. Traiter l’enrichissement des exemples comme un flux distinct des
   définitions, avec traduction française directe ou révision humaine et
   provenance par langue.
3. Examiner puis nettoyer les quinze cas structurels relevés dans un petit changement isolé,
   sans attendre un import massif.
4. Utiliser cette référence pour comparer toute source candidate sur le gain
   réel de couverture, le bruit introduit et la taille ajoutée.
