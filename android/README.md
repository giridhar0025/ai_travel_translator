# Android — Travel Translator AI

Jetpack Compose app, Kotlin 2.x, minSdk 26, target/compile 35.
Package `com.divami.traveltranslator`. Current version `0.1.2` (versionCode 3).

## Build

```bash
# Point at a backend first (see below), then:
./gradlew assembleDebug      # debug APK
./gradlew testDebugUnitTest  # unit tests
./gradlew bundleRelease      # signed AAB for Play
```

Release signing reads `RELEASE_STORE_FILE` / `RELEASE_KEY_ALIAS` and the
passwords from `local.properties` (gitignored). The upload keystore lives at
`~/.keystores/travel-translator-upload.jks` — **back it up**.

## Backend URL

`local.properties` drives `BuildConfig.BACKEND_BASE_URL`:

```properties
BACKEND_BASE_URL=https://your-backend.example.com
```

Leave it commented to use the emulator default `http://10.0.2.2:8000`.
Cleartext HTTP is permitted only for `10.0.2.2`, `localhost` and `127.0.0.1`
(see `res/xml/network_security_config.xml`), so a physical device needs an
HTTPS URL — e.g. a Cloudflare quick tunnel in front of the local backend.

## Firebase

`FIREBASE_API_KEY`, `FIREBASE_APP_ID`, `FIREBASE_PROJECT_ID` and
`FIREBASE_WEB_CLIENT_ID` in `local.properties` enable email/Google sign-in.
Left blank, the app still runs on the guest-auth path.

## Offline translation

`data/local/OfflineTranslator.kt` falls back to on-device ML Kit models when the
backend is unreachable. Models download per language pair on Wi-Fi/Ethernet only
and are warmed up after each successful online translation.

## Layout

```
app/src/main/java/com/divami/traveltranslator/
├── core/           # network, Firebase bootstrap
├── data/           # api, dto, repositories, local (session, offline translator)
├── designsystem/   # components + generated Tokens.kt / Theme.kt
├── di/             # Hilt modules
├── domain/model/   # Language, Translation, Conversation, ...
├── feature/        # auth, onboarding, chat, voice, phrases, history, settings
└── nav/            # routes + NavHost
```

Track progress in `../MOBILE_BUILD_TRACKER.md`.
