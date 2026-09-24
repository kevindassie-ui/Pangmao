# Pangmao Web « 我学法语 » — cadrage du MVP

Dernière mise à jour: 2026-09-24.

Statut: candidat Web 0.3.3 en validation; boucle dictionnaire et Reader testée
sur Android et iOS. Le prochain jalon reste la qualification des voix féminine
et masculine réellement utilisées par la PWA installée sur l'iPhone pilote.
La version Android 0.12.2 « J'apprends le chinois » est conservée comme
checkpoint signé, avec une régression manuscrite bloquante désormais suivie.

## Finalité

Le premier utilisateur est une sinophone vivant en France et apprenant le
français. Le MVP doit devenir rapidement utile dans des situations réelles:
comprendre un mot rencontré, retrouver comment exprimer une idée en français,
lire un court texte et entendre la bonne prononciation.

La première livraison est une Progressive Web App installable depuis Safari sur
l'iPhone de l'utilisatrice pilote. Elle permet de tester l'utilité du produit
sans Apple Developer Program. Elle ne doit être présentée ni comme une
application iOS native, ni comme une publication App Store.

La tranche actuelle est servie à l'adresse
<https://kevindassie-ui.github.io/Pangmao-Web/>. L'ouverture et la boucle
dictionnaire ont fonctionné sur Android et iOS le 23 septembre 2026. Plusieurs
jours d'usage sur l'iPhone pilote restent nécessaires avant de la qualifier de
stable.

## Promesse produit

Pangmao est un dictionnaire bilingue **bidirectionnel** dont l'expérience
pédagogique est orientée vers la langue apprise.

Dans le profil « J'apprends le français »:

- `avocat` retrouve ses sens chinois distincts, notamment `律师` et `牛油果`;
- `律师` retrouve le ou les mots français correspondants;
- la langue de la requête est détectée sans imposer un sélecteur de direction;
- le français reste au premier plan: forme, prononciation, genre, catégorie,
  variantes et exemples;
- les explications et aides de navigation sont en chinois simplifié par défaut.

Le sens de recherche ne détermine donc pas la langue enseignée. Ce principe
s'appliquera également au futur profil « J'apprends le chinois ».

## Positionnement initial

Le MVP ne cherche pas encore à devenir un « Pleco universel ». Il résout
d'abord un problème plus précis: aider un sinophone en France à comprendre et
employer le français avec un outil dense, rapide, sobre et explicable dans sa
langue.

Ses différences recherchées sont:

- une vraie recherche bidirectionnelle dans un seul champ;
- une fiche pensée pour apprendre le français, pas une simple traduction;
- un Reader, l'OCR et le TTS réunis dans une continuité d'usage;
- un cœur utile hors ligne et sans compte obligatoire;
- une séparation visible entre données humaines, traductions automatiques et
  générations IA.

La monétisation n'est pas un critère d'entrée du prototype familial. Elle ne
sera définie qu'après observation d'une valeur répétée, d'un public identifiable
et des coûts réels de données, d'IA, de distribution et de support.

## Séquence de produit validée

1. Livrer une PWA Safari fonctionnelle à l'utilisatrice pilote.
2. Observer son usage réel, recueillir ses retours, corriger et stabiliser.
3. Étendre la bêta à quelques apprenants chinois en France.
4. Développer le même versant sur Android, puis tester les canaux adaptés à la
   France et à la Chine.
5. Financer ou justifier l'application iOS native après revenus ou validation
   d'usage suffisante; l'App Store reste l'objectif de distribution iOS.
6. Reprendre le versant « J'apprends le chinois » depuis le socle Android
   existant et réconcilier progressivement les deux profils.

Le passage à l'étape suivante dépend de l'usage constaté, pas seulement de la
présence des fonctions prévues.

## Validation utilisateur et bêta

La première phase repose sur un journal de retours simple avec l'utilisatrice
pilote: tâche tentée, résultat obtenu, difficulté, mot ou texte non compris et
fonction manquante. Le produit doit être utilisé dans la vie quotidienne pendant
plusieurs jours avant une décision de stabilisation.

Après correction des principaux retours, le projet préparera une petite cohorte
de sinophones apprenant le français. L'assistant prend en charge la recherche de
canaux pertinents, la présélection, le questionnaire, les messages proposés et
la synthèse des retours. Toute prise de contact externe ou publication reste
soumise à validation humaine préalable et au consentement des testeurs.

Les signaux recherchés sont notamment la répétition spontanée de l'usage, le
taux de recherches menant à une réponse utile, les abandons, les mots absents,
la compréhension des fiches et le retour dans l'application sans relance.

## Tranche verticale initiale

Le premier livrable doit couvrir une boucle complète et fiable:

1. ouvrir la PWA dans Safari et l'ajouter à l'écran d'accueil;
2. saisir un mot français ou un mot chinois dans le même champ;
3. obtenir des résultats exacts puis par préfixe, classés de façon stable;
4. ouvrir une fiche affichant les sens sans fusionner les homonymes;
5. voir les formes disponibles, l'IPA, le genre et la catégorie grammaticale;
6. écouter le mot français par une action explicite;
7. réutiliser le dictionnaire hors ligne après le premier chargement automatique.

La fiche affiche d'abord les équivalents et explications chinoises. Une
explication humaine chinoise enrichie est montrée lorsqu'elle existe; la
définition française d'origine reste consultable mais repliée. Un mot chinois
absent du pack principal déclenche au besoin un seul fragment du dictionnaire
complémentaire. Une correspondance indirecte est toujours intitulée « sens
possible », accompagnée de sa base de rapprochement, et ne devient jamais une
définition attestée par simple effet d'interface.

La recherche française porte uniquement sur les formes lexicales: correspondance
exacte, flexion connue ou préfixe. Le texte narratif d'une définition n'est pas
un champ de recherche, car une occurrence telle que « affiches » dans la
définition de `punaise` ne constitue pas une traduction de `affiche`. Une lacune
confirmée peut recevoir un complément revu et attribué; elle ne doit pas être
masquée par un résultat approximatif.

Requêtes témoins minimales: `avocat`, `律师`, `être`, `etre` et un mot absent.
Une erreur de chargement ou un manque d'espace doit produire un message
compréhensible et une reprise possible, jamais un écran vide.

## Évolutions fonctionnelles

### Reader français

- saisie et collage d'un texte français;
- import initial d'un fichier texte `.txt`;
- découpage en phrases et en mots français;
- toucher un mot pour afficher sa fiche dans le contexte de la phrase;
- traduction ou explication de la phrase, distincte de la définition du mot;
- lecture TTS depuis une phrase, puis depuis le mot précis touché;
- historique local des textes, désactivable et effaçable.

La tranche 0.3 couvre déjà la saisie, le collage, l'import `.txt` UTF-8 jusqu'à
1 Mio, la segmentation locale, le TTS par phrase, l'ouverture de la fiche d'un
mot touché et un brouillon local. La traduction de phrase, la reprise à un mot
sans ouvrir la fiche et l'historique de plusieurs textes restent à livrer après
recette de cette boucle minimale.

PDF et EPUB ne seront annoncés comme formats supportés qu'après un prototype
qui conserve correctement texte, paragraphes et encodage. Ils ne font pas
partie de la première tranche verticale.

### OCR et import d'images

- importer une photo ou une capture d'écran;
- reconnaître plusieurs zones de texte;
- permettre de recadrer ou sélectionner une zone particulière;
- « figer » la zone choisie, puis passer à la traduction et au découpage en
  mots sans que la sélection change;
- conserver le texte reconnu seulement sur demande, sans conserver l'image par
  défaut.

Le parcours doit fonctionner pour les captures de conversations et de contenus
courts. Le traitement local est préféré; tout service distant éventuel exige un
consentement explicite et décrit les données envoyées.

### Partage depuis une autre application

- offrir d'abord un parcours fiable par collage, import de texte ou image et
  ouverture de la PWA;
- exploiter une cible de partage Web seulement lorsqu'elle est réellement
  supportée et validée sur les appareils concernés;
- proposer plus tard `Traduire avec Pangmao` depuis la sélection de texte et
  depuis une capture d'écran dans les applications Android et iOS natives.

La PWA ne doit pas promettre une intégration système qu'iOS Safari n'expose pas
de manière fiable.

### Enrichissements pédagogiques

- favoris, historique et statut `À apprendre / Connu` avec identifiants stables;
- exemples humains plus longs, naturels, contextualisés et attribués;
- génération à la demande d'exemples ou d'expressions par IA lorsqu'une donnée
  validée ne répond pas au besoin;
- étiquette visible `Généré par IA`, sans confusion avec un exemple authentique;
- images mnémotechniques générées par IA dans une phase ultérieure, facultatives
  et elles aussi signalées;
- cartes et répétition espacée après validation du parcours dictionnaire et
  Reader.

## Données, provenance et licences

La première base Web s'appuie sur le corpus français–chinois filtré de
FreeDict/WikDict `fra-zho`, complété par CFDICT et la couche éditoriale Pangmao,
actuellement constitué de 10 926 entrées, 11 562 sens et 15 187 équivalents
chinois. Un
export dédié ne doit contenir que les données nécessaires au profil français,
au lieu de distribuer la base Android complète de 92,34 Mio.

Le pack ajoute 2 393 fiches avec une explication chinoise issue d'entrées
françaises exactement appariées du Wiktionnaire chinois. Le fichier amont
complet n'est pas livré. Le repli chinois–français est découpé en 32 fragments:
le téléphone n'en télécharge qu'un, d'environ 0,5 Mio au maximum, lorsqu'une
recherche absente l'exige.

Les compléments `affiche`, `banane`, `bananer` et `péter` sont isolés,
versionnés et attribués à leur source ou à la révision Pangmao. Ils constituent
des correctifs éditoriaux traçables, pas une autorisation d'inverser
automatiquement toutes les phrases d'un dictionnaire chinois–français.

Un audit de livraison parcourt les 10 926 entrées, 11 562 sens et 15 187
équivalents, protège 26 mots témoins revus et compare chaque couple aux
définitions inverses disponibles. Les désaccords restent une file de revue et
ne provoquent aucune correction automatique. La méthode et la référence
chiffrée sont décrites dans
[WEB_DICTIONARY_QUALITY_BASELINE.md](WEB_DICTIONARY_QUALITY_BASELINE.md).

Le pack doit:

- préserver les sens, homonymes, formes, prononciations et informations
  grammaticales disponibles;
- fournir un index français normalisé et un index inverse chinois;
- utiliser des identifiants stables et namespacés, indépendants des identifiants
  numériques du dictionnaire chinois;
- inclure version, empreinte, source, révision et licence;
- afficher dans l'application les attributions imposées par chaque source;
- être reconstruit et vérifié de manière déterministe.

Aucune nouvelle source ni aucun contenu extrait automatiquement ne rejoint un
produit public ou commercial avant vérification de la provenance, de la licence,
de l'attribution et des obligations de partage à l'identique. Les exemples
générés par IA restent séparés des corpus humains.

## Architecture et dépôt

Pangmao reste dans un dépôt unique:

```text
Pangmao/
├── app/       application Android 0.12.2 conservée
├── webApp/    PWA Safari isolée
│   └── brands/wife/  identité rouge au cerf, appliquée uniquement au build pilote
├── tools/     construction et contrôles des données
└── docs/      décisions, état et feuilles de route
```

La PWA possède son propre outillage, ses tests et son cycle de publication. Elle
n'est pas ajoutée au build Gradle Android. Les deux clients partagent en premier
lieu un contrat de données, des règles fonctionnelles et des cas de test. Une
extraction de code multiplateforme ne sera envisagée qu'après preuve qu'elle
réduit réellement le coût sans mettre en danger Android.

La version Android 0.12.2, son identifiant d'application, sa signature et ses
bases personnelles ne sont pas modifiés pour construire le prototype Web.

Le dépôt public `kevindassie-ui/Pangmao-Web` est uniquement un miroir de
publication des fichiers statiques validés. Pendant le test familial, il reçoit
la variante construite `wife`; `webApp/` conserve la marque globale verte. Le
miroir ne contient ni code
Android, ni outillage interne, ni historique privé et ne devient pas une seconde
source de vérité. Le code canonique, les tests et la construction du pack
restent dans le dépôt privé Pangmao.

## Stockage, confidentialité et limites

- aucun compte obligatoire, aucune publicité et aucune collecte analytique dans
  le prototype familial;
- recherches, historique et favoris locaux par défaut;
- chargement automatique du pack linguistique au premier démarrage, avec un
  message visible et l'indication de sa taille, sans bouton préalable;
- fonctionnement hors ligne vérifié après ce premier chargement;
- possibilité de retélécharger les données si Safari les évince sous pression
  de stockage;
- aucune synchronisation implicite entre la PWA et l'application Android.

La synthèse vocale Web dépend des voix installées et exposées par l'appareil.
Elle doit toujours être déclenchée par une action utilisateur et signaler
clairement son indisponibilité. Web 0.3.2 attend aussi le chargement retardé des
voix après le geste utilisateur et au retour dans l'application, ne retient que
les locales françaises, privilégie `fr-FR` et offre un choix persistant ainsi
qu'un essai dans « À propos ». Si le navigateur n'expose aucune voix française,
les chemins d'installation Android/iPhone et la limite des navigateurs intégrés
sont affichés. Une voix chinoise n'est jamais utilisée comme repli silencieux
pour lire du français.

Le test appareil du 24 septembre précise cette limite: l'iPhone installé produit
du son, contrairement au navigateur Web Android testé, mais la voix
automatiquement sélectionnée est perçue comme fortement non native. Le prochain
lot doit donc qualifier la **qualité** et non plus seulement la disponibilité:
comparaison des voix françaises sur une phrase entièrement française,
persistance du choix approuvé, identité technique visible et utilisation
identique dans les fiches et le Reader. La prononciation d'un nom chinois par
une voix française reste un test séparé et ne doit pas servir seule à déterminer
l'accent de la voix.

Web 0.3.3 fournit deux profils configurables, `女声` et `男声`. Safari ne donnant
pas le genre dans son objet Web de voix, Pangmao présente toutes les voix
françaises disponibles avec leur nom, locale et type local/distant. Pour chaque
profil, l'utilisatrice écoute la phrase témoin, confirme la voix retenue et la
retrouve ensuite dans le dictionnaire comme dans le Reader. Les deux choix et
l'inventaire peuvent être copiés localement pour la recette, sans télémétrie.

## Ordre des prochaines tranches

1. voix française et stabilisation réelle de la PWA installée;
2. profondeur lexicale, expressions et exemples authentiques;
3. Reader: traduction de phrase, reprise au mot et historique;
4. import d'images, sélection de zone et OCR;
5. apprentissage: conjugaisons, collocations, statuts, cartes/SRS, contenus IA
   marqués et STT ultérieur;
6. bêta élargie, Android avec packs légers, puis iOS natif si l'usage ou les
   revenus le justifient.

La description exécutable et les portes de validation de ces lots vivent dans
[ROADMAP.md](ROADMAP.md); le plan de données lexicales reste dans
[WEB_LEXICAL_DEPTH_PLAN.md](WEB_LEXICAL_DEPTH_PLAN.md).

## Portes de validation

Avant de qualifier le prototype de stable:

- tests unitaires de normalisation, classement, homonymes et recherche inverse;
- construction reproductible du pack et contrôle de son intégrité;
- démarrage, mise à jour et reprise hors ligne sans perte des données locales;
- absence de défaut bloquant connu et reproductible dans recherche, fiche et
  prononciation;
- recette réelle sur l'iPhone de l'utilisatrice pilote, depuis Safari puis
  depuis l'icône d'écran d'accueil;
- lisibilité en chinois, tailles de texte agrandies, mode sombre et VoiceOver;
- validation explicite des attributions et licences visibles.

Le MVP est validé par plusieurs jours d'usage réel et des tâches accomplies,
pas par le seul succès d'une démonstration technique.

## Hors périmètre initial

- publication App Store et application iOS native;
- parité fonctionnelle avec Pangmao Android;
- OCR vidéo en direct;
- PDF ou EPUB avec restitution complète de la mise en page;
- compte, synchronisation cloud, paiement et abonnement;
- images IA, génération automatique non sollicitée et assistant conversationnel;
- reprise immédiate du développement du versant « J'apprends le chinois ».
