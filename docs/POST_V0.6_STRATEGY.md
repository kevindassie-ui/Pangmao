# Pangmao — stratégie après v0.6

## Positionnement

Ne pas chercher à reproduire Pleco fonction par fonction. Pangmao peut sortir du
lot comme assistant d'apprentissage français–chinois fiable, transparent et
local: corpus bilingue relu, grammaire contextuelle et parcours continu depuis
la capture d'un texte jusqu'à sa mémorisation.

Priorités possibles: explication des patrons grammaticaux, mots connus et
difficulté d'un texte, entraînement aux tons, capture → Reader → carte en un
geste, sources et registres visibles, export et sauvegarde chiffrée facultative.

## Architecture locale et en ligne

Le mode hors ligne reste le comportement par défaut et couvre le produit
essentiel. Le mode « En ligne amélioré » sera facultatif, réversible et visible.
Il pourra proposer:

- traduction de phrases plus naturelle, avec fournisseur interchangeable;
- voix TTS distante plus expressive; les voix système et d'éventuels modèles
  neuronaux téléchargés restent possibles hors ligne;
- STT distant lorsque le moteur local est insuffisant.

L'architecture séparera l'intention (`translate`, `speak`, `transcribe`) de ses
implémentations locale et distante. Elle devra gérer consentement,
confidentialité, coût, quota, délai, annulation et repli automatique. Aucun
compte Pangmao ne sera requis pour les fonctions locales.

## STT et réutilisation

Le STT Pangmao traite d'abord des énoncés courts depuis l'accueil. Les contrats
de moteur, tampons audio, segmentation et tests pourront être extraits dans un
module partageable avec l'application de réunions. Cette dernière ajoutera
séparément enregistrement long, service de premier plan, reprise, diarisation,
timeline et travaux persistants.

## Préparation commerciale

Avant diffusion payante: audit des licences et de la marque, tests
multi-appareils, politique de confidentialité et déclarations store, CGU/CGV,
support, sauvegarde des clés, bundle Google Play et bêta fermée. Kairos Solum
pourra porter les comptes d'organisation et la comptabilité. Le modèle achat
unique/freemium/abonnement ne sera choisi qu'après mesure de l'usage et des
coûts éventuels du mode en ligne.

## Portage iOS

Après stabilisation Android, réutiliser le corpus, le schéma, les règles métier,
les tests et éventuellement un cœur Kotlin Multiplatform. Réécrire l'interface
et les intégrations en SwiftUI: caméra, permissions, partage, TTS, stockage et
téléchargements de modèles. Prévoir Xcode/macOS, signature Apple, TestFlight et
une nouvelle matrice de tests. Ordre de grandeur conservé: 50 à 70 % de l'effort
technique Android, avec moins d'incertitude produit.

## Environnement de build

Le cache Gradle local manquant n'affecte pas les livrables. Après v0.6, un runner
persistant ou l'autorisation des dépôts Gradle/Maven pourra raccourcir la boucle;
la CI GitHub reste la source de vérité et évite d'intégrer une lourde distribution
Gradle dans le dépôt.
