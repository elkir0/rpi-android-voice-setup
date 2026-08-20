# v0.1.0-test1

Première prerelease de Raspberry Voice Setup, commune aux Raspberry Pi 4 et
Pi 5 sous Android 16.

## Fonctionnalités

- diagnostic modèle, build, locale, Google/Gemini et hashes connus ;
- test réel du micro USB à 48 kHz mono avec vumètre et erreurs ;
- reconnaissance vocale Google réelle ;
- guides correctif système, langue et Voice Match ;
- rapport texte sans audio ni compte ;
- compagnon ADB avec confirmation, revalidation du PID et contrôle HOTWORD
  source 1999 sur USB.

## Statut

Compilation, lint, tests de sécurité et CI sont validés. La prerelease restera
marquée expérimentale jusqu’à l’essai de l’APK distribuée sur Pi 4 et Pi 5,
incluant un test « Hey Google » après redémarrage à froid.

L’APK test1 est signée avec une clé de développement et ne possède aucun
privilège système. Les ZIP TWRP sont publiés séparément dans les dépôts propres
à chaque modèle.
