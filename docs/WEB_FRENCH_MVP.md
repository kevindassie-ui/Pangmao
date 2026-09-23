# Pangmao Web « 我学法语 » — cadrage du MVP

Dernière mise à jour: 2026-09-23.

Statut: orientation validée; développement prioritaire. La version Android
0.12.2 « J'apprends le chinois » est conservée comme point de reprise stable.

## Finalité

Le premier utilisateur est une sinophone vivant en France et apprenant le
français. Le MVP doit devenir rapidement utile dans des situations réelles:
comprendre un mot rencontré, retrouver comment exprimer une idée en français,
lire un court texte et entendre la bonne prononciation.

La première livraison est une Progressive Web App installable depuis Safari sur
l'iPhone de l'utilisatrice pilote. Elle permet de tester l'utilité du produit
sans Apple Developer Program. Elle ne doit être présentée ni comme une
application iOS native, ni comme une publication App Store.

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
FreeDict/WikDict `fra-zho`, actuellement constitué de 10 923 entrées, 11 556
sens et 15 168 équivalents chinois. Un export dédié ne doit contenir que les
données nécessaires au profil français, au lieu de distribuer la base Android
complète de 92,34 Mio.

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
clairement son indisponibilité.

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
