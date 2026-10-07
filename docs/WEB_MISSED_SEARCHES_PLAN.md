# Web 0.3.4 — journal local des recherches manquées

Préparation: 3 octobre 2026. Première tranche du lot W1.5, après la correction
vocale Web 0.3.3. Le dernier retour du 3 octobre confirme une sélection
fonctionnelle d'Amélie et Thomas et un accent français nettement amélioré.
Le timbre reste robotique: W1.6 est partiellement validé, avec naturel et
persistance/Reader encore à vérifier. À 17 h 29, l'utilisateur précise que la
solution produit doit être intégrée à Pangmao, sans réglages système complexes.
Le téléchargement manuel de voix iOS ne constitue donc pas la solution cible.

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

## Direction vocale intégrée — décision utilisateur du 3 octobre 2026

Exigence: un nouvel utilisateur peut écouter une voix française naturelle dès
le premier usage, avec choix femme/homme et débit dans Pangmao. Aucun réglage
iOS/Android, installation manuelle de voix, compte tiers ou clé API utilisateur
ne doit faire partie du parcours normal. « Intégré » décrit l'expérience; il
n'impose pas une migration vers une application iOS native.

La synthèse Web actuelle dépend des voix exposées par l'appareil et ne garantit
pas ce résultat. Son réglage de débit seul ne remplace pas un meilleur moteur.
Source technique: <https://developer.mozilla.org/en-US/docs/Web/API/SpeechSynthesis/getVoices>.

Direction à évaluer avant implémentation:
- Dictionnaire: deux voix françaises de référence avec fichiers audio produits
  en amont, téléchargés à la demande et mis en cache avec une limite d'espace.
  Ne pas embarquer toutes les prononciations dans le téléchargement initial.
- Reader: synthèse intégrée pour le texte libre. Comparer un moteur local
  téléchargeable automatiquement et une synthèse côté serveur avec cache;
  mesurer qualité, poids, délai, coût et confidentialité sur iPhone/Android.
- Hors ligne: lire les audios déjà en cache; garder la voix système comme
  secours explicite lorsqu'aucun audio intégré n'est disponible. Ne pas
  présenter le secours comme de même qualité ni déclarer tout le Reader
  naturel hors ligne sans moteur local validé.

Validation: comparer un petit lot identique (mots isolés, liaisons, nombres,
phrases longues), avec les deux voix, avant de produire un corpus complet ou
retenir un service. Vérifier les licences de redistribution et les deux voix
françaises réelles. Aucun fournisseur ni coût récurrent n'est choisi dans ce
cadrage. Les pages publiques restent sur 0.3.3; aucun moteur n'est encore intégré.

## Petit lot vocal implémenté — 7 octobre 2026

`webApp/voice-trial/` fournit six textes originaux fixes: avocat, médecin,
liaisons, date/heure/prix, achat et train, puis récit à plusieurs propositions.
Les douze MP3 sont préparés avec Piper 1.4.1 et ONNX Runtime 1.30.0,
voix UPMC Jessica (femme) et Pierre (homme), français de France, à débit 1.
Le modèle est épinglé à `c10ece1aade47bb51c153c893d14e5bf8e5b7117` et ses
hashes vérifiés avant exécution; les sorties et leurs hashes sont consignés
dans `manifest.json`. Voir les licences et sources humaines dans
[NOTICE de l'essai](../webApp/voice-trial/NOTICE.md).

Mesures de ce lot: **385 252 octets** pour les douze MP3 mono, 22 050 Hz,
64 kbit/s; 2,373 secondes de synthèse CPU pour les douze textes, hors
chargement initial et téléchargement du modèle. Cette mesure sur l'environnement
de développement ne prédit pas les délais du Reader sur iPhone. Le modèle
(environ 74 Mo) et le moteur ne sont pas distribués dans le site.

L'écoute utilise un unique élément audio HTML, sans Web Speech ni voix système.
Un nouvel extrait arrête le précédent; le débit peut être 0,85×, 1× ou 1,15×.
Les fichiers sont chargés seulement à l'écoute et placés dans un cache séparé,
borné à ces douze extraits. Les réponses HTTP partielles servent Safari.
Le formulaire d'avis reste local et fournit une copie manuelle; il n'envoie
aucun avis ni texte personnel. Le téléchargement des extraits fixes n'est
pas un traitement distant des textes de l'utilisateur.

Pour reproduire manuellement, installer `piper-tts==1.4.1` et
`onnxruntime==1.30.0` dans un environnement Python isolé avec `ffmpeg`, puis:

```bash
ORT_DISABLE_TELEMETRY=1 python tools/generate_web_voice_trial.py
python tools/verify_web_app.py
```

Le script désactive aussi la télémétrie avant import/initialisation ONNX.
La génération n'appartient pas à la CI: celle-ci vérifie les fichiers MP3
commis, leurs tailles et leurs hashes sans télécharger le modèle.

Recette téléphone: comparer les deux voix sur « Bonjour, je voudrais… » puis
sur le récit long à 1×; vérifier médecin/avocat, les liaisons et nombres.
Basculer femme/homme pendant l'écoute; régler le débit; réécouter hors ligne
un extrait déjà lu et vérifier qu'un extrait non téléchargé échoue clairement.
Le cache peut être évincé par le téléphone. Accepter séparément naturel et
prononciation pour chaque voix avant production du corpus. Le texte libre
Reader reste une étape distincte; le journal n'est pas promu avec cet essai.
