# Travel Translator AI — Native Mobile Build Plan

**Source of truth:** the React/TS prototype in `reference_app/src/`, plus `reference_app/FEATURES_AND_FLOWS.md` and `reference_app/NEW_FEATURES_SUMMARY.md`.
**Goal:** ship native iOS (SwiftUI/Swift 6) and Android (Jetpack Compose/Kotlin 2.x) apps that match the prototype 1:1 — every screen, every state, every interaction.
**Companion file:** `MOBILE_BUILD_TRACKER.md` is the per-task checklist. This file explains *what* and *why*; the tracker is *do this next*.

---

## 1. Goals & non-goals

### Goals
- Two native apps with identical feature parity to the web prototype.
- On-device STT, on-device TTS, on-device translation where possible.
- Firebase Auth (Email, Google, Apple/Sign-in-with-Google).
- Local persistence for history, favorites, recents, settings.
- Pixel-faithful design system shared between platforms (tokens generated from `theme.css`).

### Non-goals (v1)
- No backend service of our own.
- No offline mode (the "Download phrases" toggle in settings is UI-only / disabled).
- No real-time conversation, OCR, AR, or features in the "Future Enhancements" section of `reference_app/FEATURES_AND_FLOWS.md` §12.
- No cross-device sync.

---

## 2. Stack decisions (Context7-verified)

### iOS
| Layer | Choice | Min | Notes |
|---|---|---|---|
| Language | Swift 6 (strict concurrency) | — | |
| UI | SwiftUI | iOS 17 (target iOS 18) | |
| State | Observation framework (`@Observable`) | iOS 17 | Drop `ObservableObject`/`@Published`. |
| Navigation | `NavigationStack` + `.sheet` / `.fullScreenCover` | iOS 16 | Sheets for the 4 overlays (voice, phrases, history, settings). |
| Persistence | SwiftData | iOS 17 | History, favorites, recents, prefs. |
| Auth | Firebase iOS SDK (FirebaseAuth) + Sign in with Apple (`AuthenticationServices`) + GoogleSignIn-iOS | — | SwiftPM. |
| STT | `Speech` framework, `SFSpeechRecognizer` with `requiresOnDeviceRecognition = true` | iOS 13+ | Confirmed via Apple docs. iOS 17 adds `customizedLanguageModel`. |
| TTS | `AVSpeechSynthesizer` | — | Speak translated text + slow rate for "Slow" button. |
| Translation | Apple Translation framework (`LanguageAvailability`, `TranslationSession.Configuration`, `.translationTask`, `session.prepareTranslation()`) | iOS 17.4 | Confirmed via Apple docs. Hindi & Thai supported on iOS 18; **Telugu is not — see §6**. |
| Audio | `AVAudioPlayer` for the audio-player component's playback states | — | |
| Permissions | Microphone, Speech recognition | — | Add `NSMicrophoneUsageDescription`, `NSSpeechRecognitionUsageDescription` to Info.plist. |
| Testing | Swift Testing + XCUITest | — | |
| Build | Xcode 16, SwiftPM | — | No CocoaPods. |

### Android
| Layer | Choice | Min | Notes |
|---|---|---|---|
| Language | Kotlin 2.x | — | |
| UI | Jetpack Compose + Material 3 | minSdk 26, target/compile 35 | |
| State | ViewModel + `StateFlow` + `collectAsStateWithLifecycle` | — | Stateless composables + state hoisting (verified pattern). |
| Navigation | Navigation-Compose 2.8+ with **type-safe `@Serializable` routes** | — | Verified via Context7 — `kotlinx-serialization` plugin required. |
| Persistence | Room (history, favorites, recents) + DataStore (prefs) | — | |
| DI | Hilt | — | |
| Auth | Firebase Auth Android + Credential Manager (Google, passkeys) + Sign in with Apple via Firebase | — | |
| STT | `SpeechRecognizer` with `RecognizerIntent.EXTRA_PREFER_OFFLINE` (and `OnDeviceRecognizer` API 31+) | API 26+ | |
| TTS | `TextToSpeech` | — | `setSpeechRate(0.6f)` for "Slow". |
| Translation | **ML Kit Translation** (on-device, 50+ langs, downloadable packs) | — | Confirmed via Context7. **Telugu not supported — see §6**. |
| Audio | `MediaPlayer` (or ExoPlayer if needed) | — | |
| Permissions | `RECORD_AUDIO` runtime permission | — | |
| Testing | JUnit5 + Compose UI Test + Espresso | — | |
| Build | Gradle KTS, Kotlin DSL, version catalogs (`libs.versions.toml`) | — | |

### Shared
| Concern | Choice |
|---|---|
| Design tokens | Single source of truth: `shared/design-tokens.json` (already extracted from `reference_app/src/styles/theme.css`: coral `#FF6B4A`, dark coral `#E85533`, light coral `#FFF0ED`, amber `#F59E0B`, success `#22C55E`). Compiled to `Tokens.swift` and Compose `Tokens.kt`. |
| Languages | English, Hindi, Telugu, Thai (per `reference_app/FEATURES_AND_FLOWS.md` §1.4). |
| Repository layout | `ios/`, `android/`, `shared/`, and `reference_app/` siblings at repo root. |
| CI | GitHub Actions: iOS `xcodebuild test` matrix, Android `./gradlew testDebug connectedAndroidTest`. |

---

## 3. Architecture per platform

### iOS
```
ios/                                 # repo root, opened in Xcode
  TravelTranslator.xcodeproj
  TravelTranslator/
    App/
      TravelTranslatorApp.swift      // @main, root NavigationStack
      AppRouter.swift                // @Observable, mirrors AppScreen union
    Features/
      Welcome/
      Splash/
      Auth/                          // Login, EmailLogin, Signup, ForgotPassword
      Onboarding/                    // 3 slides + LanguageSelection + MicPermission
      Chat/
        ChatView.swift
        ChatViewModel.swift          // @Observable, owns messages + chatInput
        MessageBubble.swift          // Dual-layer with flip toggle
        ContextToggleBar.swift       // Formal / Casual / Tourist
        LanguageSelectorBar.swift
      Voice/
        VoiceInputView.swift         // Recording + Review states
        WaveformView.swift           // 8-bar live, 40-bar player
      Phrases/
        QuickPhrasesView.swift       // Favorites + Recent + Categories
      History/
      Settings/
      AudioPlayer/
    DesignSystem/
      Tokens.swift                   // Generated
      Theme.swift                    // ColorScheme, typography
      Components/                    // Button, Card, Modal, Toast
    Services/
      TranslationService.swift       // Apple Translation framework wrapper
      SpeechService.swift            // SFSpeechRecognizer wrapper
      TTSService.swift               // AVSpeechSynthesizer wrapper
      AuthService.swift              // FirebaseAuth wrapper
    Persistence/
      Models/                        // SwiftData @Model classes
      Repositories/
    Resources/
      Assets.xcassets
      Localizable.xcstrings
  TravelTranslatorTests/
  TravelTranslatorUITests/
```

### Android
```
android/                             # repo root, opened in Android Studio
  app/
    src/main/java/com/divami/traveltranslator/
      MainActivity.kt
      TravelTranslatorApp.kt          // @HiltAndroidApp
      navigation/
        AppNavGraph.kt                // @Serializable destinations
        Routes.kt
      features/
        welcome/
        splash/
        auth/
        onboarding/
        chat/
          ChatScreen.kt
          ChatViewModel.kt
          MessageBubble.kt
          ContextToggleBar.kt
          LanguageSelectorBar.kt
        voice/
        phrases/
        history/
        settings/
        audioplayer/
      designsystem/
        Tokens.kt                     // Generated
        Theme.kt
        Color.kt
        Type.kt
        components/
      services/
        TranslationService.kt         // ML Kit wrapper
        SpeechService.kt
        TtsService.kt
        AuthService.kt
      persistence/
        AppDatabase.kt                // Room
        dao/
        entities/
        prefs/                        // DataStore
      di/                             // Hilt modules
    src/main/res/
  build.gradle.kts                    // Version catalog
  gradle/libs.versions.toml
```

### State machine — both platforms
The reference prototype's `reference_app/src/app/App.tsx` is a single state machine with:
- `currentScreen: AppScreen` (welcome, splash, login, email-login, signup, forgot-password, onboarding, chat)
- 4 boolean overlays: `showVoiceInput`, `showQuickPhrases`, `showHistory`, `showSettings`
- chat state: `chatMessages`, `chatInput`

**iOS:** `AppRouter` (`@Observable`) holds the equivalent enum + sheet bindings. ChatViewModel owns chat state; child views read/write via bindings (mirrors the lifted-state pattern).

**Android:** `AppViewModel` (Hilt singleton) + `NavController` for the linear screens. Overlays are `ModalBottomSheet`s controlled by booleans in `ChatViewModel`.

---

## 4. Complete screen & feature inventory

(See `MOBILE_BUILD_TRACKER.md` for the per-task checklist. This is the canonical list.)

### 4.1 Pre-auth screens
1. **Welcome Overview** — feature showcase + first-launch user-guide modal + "Explore" CTA → splash.
2. **Splash** — 2-second auto-advance, coral gradient, app name "Travel Translator AI", tagline "Speak the world".
3. **Login** — Google, Apple, Email, Skip; privacy notice.
4. **Email Login** — email + password; forgot password link; sign-up link.
5. **Sign Up** — full name, email, password with strength indicator; terms acceptance.
6. **Forgot Password** — 2 states: entry + success confirmation.
7. **Onboarding Flow** (4 sub-screens):
   - 7a. Slide 1 — "Speak Instantly"
   - 7b. Slide 2 — "Translate Naturally"
   - 7c. Slide 3 — "Play Audio to Locals"
   - 7d. Language Selection — 4 languages with flag radio cards
   - 7e. Microphone Permission — runtime permission request

### 4.2 Main app (post-auth)
8. **Chat Interface** (home, default tab):
   - Header with title "Translation Chat" + status indicator
   - Language selector bar: source / swap / target with flags
   - **Context-awareness toggle row** — Formal 👔 / Casual 😊 / Tourist 🧳 (default Casual)
   - **Dual-layer message bubbles** with flip toggle (⇅) — both source + translated text in one bubble
   - User vs AI bubble styles per `reference_app/FEATURES_AND_FLOWS.md` §3.1
   - Per-message actions: Play Audio, Slow Playback, Edit Translation
   - Voice input button + text input + send button
   - Auto-scroll to latest
9. **Voice Input** (full-screen modal, 2 states):
   - 9a. Recording — pulsing mic, ping animation, 8-bar live waveform, "Listening…" text, Stop button
   - 9b. Review — green check, editable transcript, Translate / Record Again
10. **Quick Phrases**:
    - **Favorites section** ⭐ (star toggle, persisted)
    - **Recent section** 🕐 (last 5, auto-tracked)
    - **All Categories**: Restaurants 🍴 (orange), Directions 🧭 (blue), Transport 🚗 (green), Shopping 🛍️ (purple), Emergencies ⚠️ (red) — 6 phrases each, 30 total
11. **Conversation History**:
    - Search bar with real-time filtering
    - Date-grouped cards (Today / Yesterday / specific date), sticky headers
    - Tap → open conversation in chat
12. **Settings**:
    - Default Language (modal selector)
    - Playback Speed (Normal / Slow / Very Slow)
    - Download Phrases toggle (UI present, disabled in v1)
    - Privacy Settings (data management, clear history, export — at minimum "Clear history" working)
    - Log Out (confirmation dialog → returns to Login)

### 4.3 Reusable components
- Bottom Navigation Bar (4 tabs: Chat, Phrases, History, Settings)
- Language Selector Modal (bottom sheet, 4 languages with flags + checkmark)
- Audio Player (40-bar animated waveform, play/pause, slow toggle)
- Toast/snackbar (replaces sonner Toaster from web)
- Reusable: Button (primary/secondary/text), Input (text/email/password), Card, Modal, Search Bar, Toggle, Radio group, Message bubble, Icon button, Language flag selector, Waveform visualizer

---

## 5. Cross-platform service contracts

Define the same protocol/interface on both platforms so feature code reads the same.

```swift
// iOS
protocol TranslationServicing {
    func isSupported(from: Locale.Language, to: Locale.Language) async -> Bool
    func translate(_ text: String, from: Locale.Language, to: Locale.Language) async throws -> String
    func prepare(from: Locale.Language, to: Locale.Language) async throws
}

protocol SpeechServicing {
    func startStreaming(locale: Locale) -> AsyncThrowingStream<String, Error>
    func stop()
}

protocol TTSServicing {
    func speak(_ text: String, locale: Locale, rate: Float)
    func stop()
}
```

```kotlin
// Android
interface TranslationService {
    suspend fun isSupported(source: String, target: String): Boolean
    suspend fun translate(text: String, source: String, target: String): String
    suspend fun prepare(source: String, target: String)
}

interface SpeechService {
    fun startStreaming(localeTag: String): Flow<String>
    fun stop()
}

interface TtsService {
    fun speak(text: String, localeTag: String, rate: Float)
    fun stop()
}
```

---

## 6. Open decisions — must resolve before phase 4

### 6a. Telugu translation on Android
**Problem:** ML Kit Translation (the chosen on-device engine) does not list Telugu in its supported languages. Confirmed via Context7 — ML Kit covers 50+ languages, primarily majors. Hindi ✅, Thai ✅, Telugu ✗.

**Options (rank-ordered):**
1. **Drop Telugu from Android v1.** Show 3 languages on Android, 4 on iOS (if iOS supports it — see 6b). Simplest, ships fastest.
2. **Cloud fallback for TE↔X only.** Re-introduce a tiny backend or call a translation API directly from the client (Google Cloud Translation API, DeepL). Violates "no backend" but is pragmatic.
3. **Bundle a TFLite Telugu model** (e.g., NLLB-200-distilled or a MarianMT pair). Significant work; large APK.

**Recommendation:** option 1 for v1, option 2 in v1.1 if Telugu demand justifies it.

### 6b. Telugu translation on iOS
**Problem:** Apple Translation framework supports a curated language set, expanded each iOS release. As of iOS 18, Hindi is supported; Telugu coverage is not yet confirmed in our Context7 lookup.

**Action:** at iOS scaffold time, run `LanguageAvailability().supportedLanguages` on a real device and confirm. If Telugu isn't supported, apply the same fallback decision as Android.

### 6c. Apple/Google Sign-In
- iOS: Sign in with Apple is required by App Store guidelines if Google sign-in is offered. Plan to support both.
- Android: Apple sign-in goes through Firebase OAuth provider (web flow). Google sign-in via Credential Manager.

### 6d. App identifiers / branding
- Need: bundle id (e.g., `com.divami.traveltranslator`), app icons (export from Figma), launch screen, App Store/Play Store metadata. Defer until phase 9.

---

## 7. Build phases (10 phases, ~2 weeks each as a rough planning frame)

| # | Phase | Output |
|---|---|---|
| 0 | **Bootstrap** | Repos, CI, design tokens generated, empty iOS + Android projects build & run. |
| 1 | **Design system** | Theme + tokens + reusable components on both platforms (Button, Input, Card, Modal, Toast, Bottom Nav). |
| 2 | **Pre-auth flow** | Welcome → Splash → Login → Email Login → Signup → Forgot Password → Onboarding (3 slides + Lang + Mic Perm). Mock auth. |
| 3 | **Chat shell** | Bottom nav, empty Chat screen, language selector bar, context toggle row, dual-layer bubble component. Mock translation. |
| 4 | **Translation engine** | Real Apple Translation (iOS) + ML Kit (Android), language-pack download UX, language-availability check on launch. Resolve §6 decisions. |
| 5 | **Voice input** | STT permission flow, Recording state with live waveform, Review state, integration with chat. |
| 6 | **Quick phrases** | Categories + 30 phrases, Favorites with star toggle, Recent (last 5), persistence. |
| 7 | **History + Settings** | Conversation persistence, search, date grouping; settings screen incl. default language, playback speed, clear history, logout. |
| 8 | **Audio (TTS) + Audio Player** | TTS via `AVSpeechSynthesizer` / `TextToSpeech`; in-message Play/Slow buttons; standalone Audio Player with 40-bar animated waveform. |
| 9 | **Firebase Auth** | Email/Password, Google, Apple. Replace mock auth. Persist session. |
| 10 | **Polish + ship** | Haptics, accessibility (VoiceOver/TalkBack, Dynamic Type, font scaling), dark mode, RTL where applicable, app icons, store listings, TestFlight + Play Internal Testing. |

Each phase should end with: build green on both platforms, screenshots captured, tracker rows ticked.

---

## 8. Definition of done (per screen)

A screen is "done" only when **all** of:
- [ ] Pixel-faithful to the web prototype at 360–428 px width (mobile sizes).
- [ ] All interactive states implemented (default, hover/press, active, disabled, loading, error, empty).
- [ ] Accessibility: VoiceOver/TalkBack labels, Dynamic Type / `fontScale` 0.85–1.5 doesn't break layout, contrast WCAG AA, 44 pt / 48 dp min touch targets.
- [ ] Dark mode renders correctly.
- [ ] Localized strings extracted to `Localizable.xcstrings` / `strings.xml` (UI is English-only in v1, but strings are extracted so future locales are cheap).
- [ ] Unit test for the ViewModel (state transitions) + 1 UI test for the happy path.
- [ ] Tracker row ticked with date + commit SHA.

---

## 9. Risks

| Risk | Likelihood | Mitigation |
|---|---|---|
| Telugu not supported on either platform | High | §6 decision: drop or add tiny cloud fallback. |
| Apple Translation language pack download UX surprises users | Medium | Prefetch packs at first chat-screen mount; show clear progress UI. |
| On-device STT accuracy for Hindi/Telugu/Thai varies by device | Medium | Provide "Edit transcript" before translate (already in design — Voice Review state). |
| Firebase Auth + Sign in with Apple cert/profile issues | Medium | Tackle in phase 9 with a real device early. |
| Pixel-faithful port drifts from prototype as Figma changes | Low–Med | Lock the prototype version; treat further Figma edits as separate change requests. |

---

## 10. References

- `reference_app/src/app/App.tsx` — screen state machine.
- `reference_app/src/app/components/*.tsx` — every screen component.
- `reference_app/src/styles/theme.css` — original design tokens.
- `shared/design-tokens.json` — extracted tokens (source of truth for native).
- `reference_app/FEATURES_AND_FLOWS.md` — full UX spec.
- `reference_app/NEW_FEATURES_SUMMARY.md` — context toggle + dual-layer bubbles + favorites/recent.
- Apple Translation framework: https://developer.apple.com/documentation/Translation
- Apple Speech framework: https://developer.apple.com/documentation/Speech
- ML Kit Translation: https://developers.google.com/ml-kit/language/translation
- Jetpack Compose Navigation 2.8 (type-safe routes): https://developer.android.com/develop/ui/compose/navigation
