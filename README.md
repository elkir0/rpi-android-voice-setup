# Raspberry Voice Setup

Assistant Android libre pour configurer et diagnostiquer un microphone USB,
la reconnaissance Google, Voice Match et le wakeword sur Raspberry Pi 4 et
Raspberry Pi 5.

Le projet transforme une procédure technique validée sur du matériel réel en
un parcours guidé et vérifiable. Il ne prétend pas qu’une application Android
ordinaire peut remplacer sans risque un APEX système.

## État du projet

**MVP 0.1 en développement actif.** L’application compile, s’installe et
propose déjà :

- détection Pi 4 / Pi 5, Android, build et locale ;
- identification des APEX audio stock/corrigés connus par SHA-256 ;
- inventaire des entrées audio et du microphone USB ;
- test réel `AudioRecord` mono PCM 16 bits à 48 kHz ;
- vumètre, frames lues, niveau maximal, route et erreurs de lecture ;
- test de reconnaissance Google avec conservation du texte reconnu ;
- raccourcis vers la langue Android et les réglages Assistant ;
- guide Voice Match adapté aux microphones USB à capture unique ;
- rapport texte partageable, sans audio ni identifiant de compte ;
- compagnon ADB avec garde-fous stricts pour l’enrôlement.

## Installation rapide

Prérequis sur l’ordinateur : Android Platform Tools, Java 17 et une connexion
ADB autorisée.

```sh
git clone https://github.com/elkir0/rpi-android-voice-setup.git
cd rpi-android-voice-setup
./tools/rpi-voice-setup install --serial IP:PORT
```

Ou construire uniquement l’APK :

```sh
./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Parcours utilisateur

```text
DIAGNOSTIC DU PI
  → SIGNAL MICRO USB 48 kHz MONO
  → RECONNAISSANCE GOOGLE RÉELLE
  → LANGUES ANDROID / ASSISTANT
  → PRÉPARATION VOICE MATCH
  → ENRÔLEMENT
  → RÉARMEMENT HOTWORD
  → TEST « HEY GOOGLE »
  → SECOND TEST APRÈS REDÉMARRAGE
```

Chaque étape montre une preuve mesurée. La présence d’un périphérique USB ne
suffit jamais à déclarer le microphone fonctionnel.

## Compagnon ADB

```sh
./tools/rpi-voice-setup doctor --serial IP:PORT
./tools/rpi-voice-setup set-language --serial IP:PORT --locale fr-FR
./tools/rpi-voice-setup open-voice-match --serial IP:PORT
./tools/rpi-voice-setup prepare-enrollment --serial IP:PORT
./tools/rpi-voice-setup finish-enrollment --serial IP:PORT
```

`prepare-enrollment` est la seule commande qui termine un processus. Elle
refuse toute action sauf si **une seule** ligne satisfait simultanément ces
conditions :

1. utilisateur Android isolé commençant par `u0_i` ;
2. commande contenant `com.google.android.googlequicksearchbox` ;
3. commande contenant `gsa.hot` ;
4. confirmation explicite `OUI` juste avant l’action.

Il n’utilise jamais `pkill`, ne tue jamais `:interactor` et ne force jamais
l’arrêt complet de l’application Google.

## Correctifs système compatibles

L’application est commune aux deux cartes ; les binaires système restent
séparés et strictement liés à leur build :

- [correctif Raspberry Pi 4](https://github.com/elkir0/rpi4-android-usb-wakeword-fix) ;
- [correctif Raspberry Pi 5](https://github.com/elkir0/rpi5-android-usb-wakeword-fix).

Ne copiez jamais un APEX Pi 4 sur un Pi 5, ni un APEX vers une build dont le
SHA-256 n’est pas explicitement accepté par son installateur TWRP.

## Limites de sécurité

Sans privilèges, l’application Android ne peut pas :

- écrire sous `/vendor` ;
- remplacer ou signer un APEX ;
- exécuter tous les diagnostics `dumpsys` ;
- arrêter le listener HOTWORD appartenant à Google.

Ces opérations restent volontairement séparées dans les paquets TWRP des
dépôts matériels ou dans le compagnon ADB auditable.

## Développement

- Java 17 ;
- Android Gradle Plugin 8.9.1 ;
- `minSdk 28`, `targetSdk 35` ;
- aucune bibliothèque applicative externe ;
- interface paysage, boutons larges adaptés aux petits écrans tactiles.

```sh
./gradlew clean :app:assembleDebug :app:lintDebug
```

## Confidentialité

Le test du micro analyse uniquement des niveaux PCM en mémoire. Il n’écrit
aucun fichier audio. Le rapport exporté contient les caractéristiques de la
build et de l’audio, jamais l’audio ni l’identité du compte Google.

## Licence

Apache License 2.0. Les marques Google, Gemini, Raspberry Pi et les logiciels
tiers appartiennent à leurs propriétaires respectifs. Ce projet n’est pas une
distribution officielle de Google, Raspberry Pi ou KonstaKANG.

