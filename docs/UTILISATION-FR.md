# Utiliser Raspberry Voice Setup

## 1. Installer l’application

Télécharger l’APK et `SHA256SUMS` depuis la même release, vérifier le hash,
puis :

```sh
adb -s IP:PORT install -r raspberry-voice-setup-v0.1.0-test1.apk
adb -s IP:PORT shell am start \
  -n io.github.elkir0.rpivoicesetup/.MainActivity
```

L’APK de la prerelease est signée pour le développement et n’est ni une
application Google ni une application système privilégiée.

## 2. Suivre le tableau de bord

1. **Correctif système** : le modèle et le hash doivent correspondre au paquet
   Pi 4 ou Pi 5 exact. L’application ne flashe rien elle-même.
2. **Microphone USB** : parler plusieurs secondes. Le test valide au moins une
   seconde de frames, un niveau supérieur à `-55 dBFS` et zéro erreur.
3. **Reconnaissance Google** : la phrase reconnue confirme le chemin Android →
   service Google ; aucun fichier audio n’est conservé.
4. **Langue** : aligner Android, Google et l’Assistant sur la même variante.
5. **Voice Match** : utiliser le compagnon ADB au moment indiqué.

## 3. Enrôler Voice Match

```sh
./tools/rpi-voice-setup doctor --serial IP:PORT
./tools/rpi-voice-setup set-language --serial IP:PORT --locale fr-FR
./tools/rpi-voice-setup open-voice-match --serial IP:PORT
./tools/rpi-voice-setup prepare-enrollment --serial IP:PORT
# Prononcer immédiatement les phrases affichées, puis :
./tools/rpi-voice-setup finish-enrollment --serial IP:PORT
```

`prepare-enrollment` revalide après confirmation qu’il existe exactement un
processus isolé `u0_i*` du paquet Google contenant `gsa.hot`. Toute ambiguïté
annule l’action. `finish-enrollment` attend et exige HOTWORD source 1999 sur le
micro USB.

## 4. Validation finale

Prononcer « Hey Google », vérifier l’ouverture de Gemini/Assistant, effectuer
un redémarrage à froid et recommencer. La simple présence d’un micro ou la
réussite de Voice Match ne suffisent pas à valider le wakeword persistant.

## Confidentialité

Le vumètre traite les échantillons PCM uniquement en mémoire. Le rapport ne
contient ni audio, ni adresse de compte, ni jeton Google. Il peut contenir le
fingerprint Android et les hashes système : relire le texte avant partage.
