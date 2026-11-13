# Badminton Smart Manager — Android

Native offline badminton club accounts app. **This is a public code repository: do not commit real member payments, migration exports, or signing keys.**

The existing v19 Android Studio source and test package is available separately in the ChatGPT conversation. The public repository will contain only code and synthetic test data.

## Build

GitHub Actions will build a debug APK once the source project is uploaded under the repository root. The workflow will run the migration and accounting tests, compile the app, then upload `app-debug.apk` as an artifact.

This app is a separate native preview and does not replace the existing offline v17 app. Keep your existing phone ledger and export a verified backup before any migration.
