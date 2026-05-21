# Android — Travel Translator AI

Jetpack Compose app, Kotlin 2.x, minSdk 26, target/compile 35.

## Phase 0 setup (next steps)

This folder is intentionally empty. To bootstrap:

1. Open Android Studio → New Project → Empty Activity (Compose).
2. Name: `Travel Translator`. Package: `com.divami.traveltranslator`. Language: Kotlin. Min SDK: 26.
3. Save into this `android/` directory.
4. In `gradle/libs.versions.toml`, declare versions for: Compose BOM (latest stable), `androidx.navigation:navigation-compose:2.8+`, `org.jetbrains.kotlinx:kotlinx-serialization-json`, Hilt, Room, DataStore, ML Kit translate (`com.google.mlkit:translate`), Firebase BoM, Coroutines, Lifecycle.
5. Apply Gradle plugins: `kotlin("android")`, `kotlin("plugin.serialization")`, `com.google.devtools.ksp`, `dagger.hilt.android.plugin`, `com.google.gms.google-services`.
6. Add `RECORD_AUDIO` permission and Firebase `google-services.json`.
7. Add a Gradle task that reads `../shared/design-tokens.json` and writes `app/src/main/java/.../designsystem/Tokens.kt`.

After scaffolding, the folder layout should match `MOBILE_BUILD_PLAN.md` §3 (features/, designsystem/, services/, persistence/, di/).

Track progress in `../MOBILE_BUILD_TRACKER.md`.
