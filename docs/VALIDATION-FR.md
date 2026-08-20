# Matrice de validation

## Validation automatisée

| Contrôle | État |
|---|---|
| Compilation APK Android 28–35 | OK |
| Android Lint debug | OK |
| Refus de zéro/plusieurs/mauvais processus HOTWORD | OK |
| Refus si le PID change après confirmation | OK |
| Détection d’un APEX connu sur le mauvais modèle | OK |
| Confirmation HOTWORD source 1999 sur USB | OK, fixture |
| CI GitHub | OK |

## Validation matérielle requise avant release stable

Pour **chaque** modèle :

- installation et ouverture sur l’écran réel ;
- détection exacte du modèle et des hashes ;
- test USB 48 kHz mono avec signal et zéro erreur ;
- reconnaissance Google d’une phrase ;
- parcours Voice Match avec le compagnon ;
- `mPerformingSoftwareHotwordDetection=true` et source 1999 sur USB ;
- déclenchement « Hey Google » puis réponse Gemini/Assistant ;
- second déclenchement après redémarrage à froid.

La release reste une prerelease tant que toute cette séquence n’a pas été
rejouée avec la version exacte de l’APK distribuée sur Pi 4 et Pi 5.
