# Web 0.3.4 — journal local des recherches manquées

Préparation: 3 octobre 2026. Première tranche du lot W1.5, après la correction
vocale Web 0.3.3. La recette auditive W1.6 n'est pas encore effectuée.

## Comportement

Dans « À propos / 关于与数据来源 », la section secondaire « 缺词记录 » permet
d'activer le journal. Il est désactivé par défaut. Après activation, seule une
requête confirmée par la touche « Rechercher / 搜索 » du clavier et sans aucun
résultat est retenue. La recherche instantanée et les ouvertures automatiques
depuis des exemples ou des fiches ne remplissent pas le journal.

Pour le chinois, la vérification attend le complément exact. Un résultat direct
ou une hypothèse affichée compte comme résultat; un téléchargement impossible,
une requête remplacée ou un effacement pendant l'attente ne compte pas comme
recherche manquée. Le journal mesure l'absence de résultat affiché, pas la
qualité pédagogique ni l'exactitude d'une traduction.

Le journal conserve les 100 dernières requêtes distinctes, jusqu'à 120
caractères chacune, avec langue détectée, compteur et première/dernière date.
Espaces, casse et apostrophes sont harmonisés pour les répétitions; les accents
restent conservés pour analyser les requêtes réellement saisies. Les valeurs
vides, purement numériques ou sans lettres et les textes trop longs sont exclus.

Les données restent dans une clé locale distincte des favoris, du Reader et des
voix. Désactiver arrête les nouveaux enregistrements sans effacer les anciens.
Le bouton d'effacement demande confirmation et ne modifie que le journal. Un
échec de stockage affiche un message et ne prétend pas avoir sauvegardé ou
effacé les données. Aucun service, compte, collecte automatique ou envoi réseau
n'est ajouté.

Chaque requête peut être relancée depuis la liste. Copier produit un JSON
lisible; exporter produit un fichier JSON daté avec schéma, version de
l'application et date d'export. Si le presse-papiers est indisponible,
l'application indique l'export fichier comme solution de repli.

## Validation

- Tests automatisés: consentement initial, persistance et effacement isolés,
  déduplication, plafonds, données corrompues, stockage bloqué, complément
  chinois, erreurs et résultats tardifs, export et unicité d'un enregistrement.
- Vérification des deux sites statiques, de la présence hors ligne du nouveau
  module, de tous les couples français–chinois et des 26 témoins existants.
- Le corpus lexical et ses identifiants restent inchangés. Tous les fichiers
  d'application et le paquet sont liés à la version candidate 0.3.4 afin de
  préserver la cohérence des caches lors de la future publication.

## Recette iPhone et promotion

1. Sur la version publique **0.3.3**, depuis l'icône de l'application, ouvrir
   « À propos », écouter puis enregistrer les profils 女声 et 男声 avec la même
   phrase entièrement française. Confirmer le naturel de chaque voix.
2. Vérifier le mot dans le dictionnaire, une phrase du Reader et la conservation
   de chaque choix après fermeture/réouverture. Garder W1.6 ouvert en cas de
   prononciation insatisfaisante; partager alors l'inventaire vocal copiable.
3. Une fois cette porte acceptée, promouvoir le candidat validé par la CI:
   fusion dans Pangmao, construction du pilote cerf, puis publication de ses
   seuls fichiers statiques dans Pangmao-Web. Pas de rebuild Android, d'APK
   temporaire ni de modification de budget dans ce lot.
4. Sur **0.3.4**, confirmer que le journal est initialement désactivé. Activer,
   soumettre un mot sans résultat deux fois, vérifier une seule ligne avec
   compteur 2; taper sans soumettre et vérifier l'absence de ligne.
5. Soumettre `放屁` et vérifier que le résultat du complément ne crée aucune
   ligne. Vérifier qu'une recherche chinoise dont le complément est inaccessible
   hors ligne n'est pas comptée comme absence lexicale.
6. Fermer/réouvrir, copier et exporter sur Safari/PWA, relancer une requête,
   désactiver puis effacer. Vérifier le maintien des favoris, du brouillon Reader
   et des deux choix vocaux, puis le fonctionnement hors ligne après chargement.

Les fonctions de presse-papiers, de téléchargement et la voix réelle exigent
une recette sur l'appareil; les tests automatisés ne les déclarent pas validées.
