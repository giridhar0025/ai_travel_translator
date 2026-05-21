# Travel Translator AI — Native Build Tracker

Companion to `MOBILE_BUILD_PLAN.md`. Tick boxes as work lands; each ticked row should map to a commit on the iOS or Android repo.

**Status legend:** `[ ]` not started · `[~]` in progress · `[x]` done · `[!]` blocked (add note) · `[-]` won't do (v1)

---

## Phase 0 — Bootstrap

### Repos & tooling
- [x] Create `ios/` Xcode project (SwiftUI App, iOS 17 deployment target, Swift 6 mode) — XcodeGen via `ios/project.yml`; `xcodegen generate` produces `ios/TravelTranslator.xcodeproj`. Build verified with `xcodebuild`.
- [x] Create `android/` Gradle project (Compose, Kotlin 2.x, minSdk 26, target/compile 35) — hand-written `settings.gradle.kts`, `build.gradle.kts`, `app/build.gradle.kts`; Gradle wrapper 8.10.2 generated; `./gradlew assembleDebug` BUILD SUCCESSFUL
- [x] Add `gradle/libs.versions.toml` with Compose BOM, Hilt, Navigation-Compose 2.8+, kotlinx-serialization, ML Kit, Firebase BoM, Room, DataStore
- [-] Add SwiftPM packages: FirebaseAuth, GoogleSignIn-iOS — deferred to Phase 9
- [x] `.gitignore` for both platforms
- [x] GitHub Actions: iOS `xcodebuild test`, Android `./gradlew testDebug`
- [x] Add `shared/design-tokens.json` at repo root (extracted from `reference_app/src/styles/theme.css`)
- [x] Token codegen script → `ios/.../DesignSystem/Tokens.swift` and `android/.../designsystem/Tokens.kt` (`tools/gen-tokens.mjs` — outputs default to `shared/generated/`; override paths once IDE projects exist; see `tools/README.md`)
- [x] `README.md` updates documenting `ios/`, `android/`, and the token pipeline

### Smoke test
- [x] iOS app launches on simulator with blank "Hello" screen using a token color — installed and launched on booted iPhone 17 sim; renders "Travel Translator AI" / "Speak the world" on `DesignTokens.Brand.coral`
- [~] Android app launches on emulator with blank "Hello" screen using a token color — `app-debug.apk` (9.4 MB) builds clean; emulator system-image not yet installed (~1.5 GB download — defer until needed)

---

## Phase 1 — Design system

### Tokens (both platforms, generated)
- [x] Colors: `primary` `#FF6B4A`, `primaryDark` `#E85533`, `primaryLight` `#FFF0ED`, `accent` `#F59E0B`, `success` `#22C55E`, plus neutral grays — `DesignTokens.Brand.*` / `DesignTokens.Semantic.*` / `DesignTokens.Neutral.*` generated from `shared/design-tokens.json`
- [x] Typography scale (xs / sm / base / lg / xl), weights 400/500 — `DesignTokens.Typography.Size.{xs,sm,base,lg,xl,xxl}` + `weightNormal/weightMedium`
- [x] Radii: 12 / 16 / 24 — `DesignTokens.Radius.{sm:8,md:10,lg:12,xl:16,modal:24}`
- [x] Spacing: 4-px grid — `DesignTokens.Spacing.s{1..10}`
- [x] Dark mode variants — `DesignTokens.Dark.*` (oklch → sRGB hex)

### iOS components (`DesignSystem/Components/`)
- [x] `CoralButton` (primary/secondary/text variants, disabled, loading)
- [x] `CoralTextField` (text/email/password/secure)
- [x] `CoralCard`
- [x] `BottomSheet` modifier — `View.appBottomSheet` wrapper over `.sheet` with token-aligned detents
- [x] `Toast` (replaces sonner) — `ToastCenter` `@Observable` + `ToastOverlay`
- [x] `LanguageFlag` view
- [x] `WaveformView` (parameterized: bar count, animated bool) — `Waveform.swift`, supports 8-bar live + 40-bar player + idle
- [x] `BottomNavBar` (4 tabs)
- [x] `SearchBar`
- [x] `RadioCard` (used in language selection)

### Android components (`designsystem/components/`)
- [x] `CoralButton`
- [x] `CoralTextField`
- [x] `CoralCard`
- [x] `ModalBottomSheet` wrapper — `AppBottomSheet`
- [x] `Toast` / Snackbar host — `ToastHost` + `rememberToastState`
- [x] `LanguageFlag`
- [x] `Waveform`
- [x] `BottomNavBar`
- [x] `SearchBar`
- [x] `RadioCard`

### Verification
- [x] Component gallery screen on each platform (debug only) showing all components in all states — iOS `ComponentGallery.swift` (mounted via `ContentView` in DEBUG); Android `ComponentGallery.kt` (mounted via `MainActivity`). Both build clean: iOS `xcodebuild` BUILD SUCCEEDED; Android `./gradlew assembleDebug` BUILD SUCCESSFUL.
- [ ] Side-by-side screenshots vs web prototype reviewed

---

## Phase 2 — Pre-auth flow

### Screen 1 · Welcome Overview
- [ ] iOS: `WelcomeView` with feature showcase + "Explore" CTA
- [ ] iOS: First-launch user-guide modal (UserDefaults flag)
- [ ] Android: `WelcomeScreen`
- [ ] Android: First-launch user-guide modal (DataStore flag)
- [ ] Navigate to Splash on tap

### Screen 2 · Splash
- [ ] iOS: `SplashView`, coral gradient, app name, tagline, 2-second auto-advance
- [ ] Android: `SplashScreen`, same
- [ ] Navigate to Login

### Screen 3 · Login
- [ ] iOS: Google / Apple / Email / Skip buttons, privacy notice
- [ ] Android: same
- [ ] Mock callbacks → Onboarding

### Screen 4 · Email Login
- [ ] iOS: email + password fields, validation, forgot/signup links, back to Login
- [ ] Android: same

### Screen 5 · Sign Up
- [ ] iOS: full name + email + password (with strength indicator), terms text
- [ ] Android: same

### Screen 6 · Forgot Password
- [ ] iOS: state 1 (email entry → Send Reset Link)
- [ ] iOS: state 2 (success confirmation → Back to Login)
- [ ] Android: state 1
- [ ] Android: state 2

### Screen 7 · Onboarding Flow (4 sub-screens)
- [ ] iOS: TabView paged — Slide 1 "Speak Instantly"
- [ ] iOS: Slide 2 "Translate Naturally"
- [ ] iOS: Slide 3 "Play Audio to Locals"
- [ ] iOS: Language Selection (4 radio cards: 🇬🇧 EN / 🇮🇳 HI / 🇮🇳 TE / 🇹🇭 TH)
- [ ] iOS: Microphone Permission screen with "Enable" + "Maybe Later"
- [ ] iOS: Skip button
- [ ] Android: HorizontalPager — 3 slides
- [ ] Android: Language Selection
- [ ] Android: Microphone Permission (`RECORD_AUDIO` runtime request)
- [ ] Android: Skip button

### Wiring
- [ ] iOS: `AppRouter` enum + bindings drives the linear flow
- [ ] Android: type-safe `@Serializable` routes for each step

### Verification
- [ ] Full happy path: Welcome → Splash → Login (all 4 paths) → Onboarding → Chat (placeholder) on both platforms
- [ ] Unit tests for AppRouter / NavGraph state transitions

---

## Phase 3 — Chat shell

### Bottom navigation
- [ ] iOS: `TabView` with 4 tabs (Chat / Phrases / History / Settings)
- [ ] Android: M3 `NavigationBar` with 4 destinations
- [ ] Active state coral, inactive gray, safe-area padding handled

### Chat screen (no real translation yet)
- [ ] iOS: `ChatView` with header "Translation Chat" + status indicator
- [ ] iOS: `LanguageSelectorBar` — source / swap / target with flags + bottom-sheet picker
- [ ] iOS: `ContextToggleBar` — Formal 👔 / Casual 😊 (default) / Tourist 🧳
- [ ] iOS: `MessageBubble` — dual-layer with flip toggle (⇅), persists per message
- [ ] iOS: User vs AI bubble styles
- [ ] iOS: Per-message actions (Play / Slow / Edit) — buttons present, no-op for now
- [ ] iOS: Voice button + text input + send button (disabled when empty)
- [ ] iOS: Auto-scroll to latest, seed message present
- [ ] iOS: `ChatViewModel` (`@Observable`) owns `messages`, `chatInput`, `contextMode`, `flipReversed`, `sourceLang`, `targetLang`
- [ ] Android: `ChatScreen` + `ChatViewModel` mirror of all the above

### Verification
- [ ] Sending a message inserts a bubble; mock translation echoes back as AI message
- [ ] Flip toggle works per message
- [ ] Context toggle persists across sends
- [ ] Language swap button reverses source/target

---

## Phase 4 — Translation engine

### iOS — Apple Translation framework
- [ ] `TranslationService` protocol + `AppleTranslationService` implementation using `LanguageAvailability` + `TranslationSession.Configuration` + `.translationTask`
- [ ] First-launch language-pack pre-download for the user's selected pair (`session.prepareTranslation()`)
- [ ] Language-availability check on chat mount; surface unsupported pairs as a soft warning
- [ ] Wire into `ChatViewModel.send()` — replace mock with real translate

### Android — ML Kit Translation
- [ ] `TranslationService` interface + `MlKitTranslationService` implementation
- [ ] Download language pack on demand via `RemoteModelManager`; show progress
- [ ] Wi-Fi-only / metered-network handling per ML Kit guidance
- [ ] Wire into `ChatViewModel`

### §6 decisions
- [ ] **§6a:** Confirm Telugu unavailable in ML Kit list — add notice in language picker on Android
- [ ] **§6b:** Run `LanguageAvailability().supportedLanguages` on iOS device; confirm or rule out Telugu
- [ ] Document final decision in `MOBILE_BUILD_PLAN.md` §6

---

## Phase 5 — Voice input

### Permissions
- [ ] iOS: Info.plist entries `NSMicrophoneUsageDescription`, `NSSpeechRecognitionUsageDescription`
- [ ] iOS: Request both, gate UI on grant
- [ ] Android: `RECORD_AUDIO` runtime permission
- [ ] Both: from-settings deep-link if denied

### Recording state (full-screen modal)
- [ ] iOS: `VoiceInputView` — pulsing mic icon, coral circular gradient, ping animation, 8-bar live waveform driven by audio levels
- [ ] iOS: "Listening…" text + instruction
- [ ] iOS: Stop Recording button (red)
- [ ] iOS: `SpeechService` using `SFSpeechRecognizer` with `requiresOnDeviceRecognition = true`
- [ ] iOS: `AsyncThrowingStream<String, Error>` driving partial transcripts
- [ ] Android: full-screen Compose route with same visual elements
- [ ] Android: `SpeechService` using `SpeechRecognizer` with `EXTRA_PREFER_OFFLINE` (and `OnDeviceRecognizer` API 31+)

### Review state
- [ ] iOS: green check + "Recording complete" text
- [ ] iOS: editable `TextEditor` with the transcript
- [ ] iOS: "Translate" (primary) + "Record Again" (secondary)
- [ ] iOS: Tapping Translate populates `chatInput` and dismisses
- [ ] Android: same set

### Verification
- [ ] Full happy path on real iPhone (simulator can't STT well)
- [ ] Full happy path on real Android device
- [ ] Permission-denied path shows correct UI

---

## Phase 6 — Quick Phrases

### Data
- [ ] Author 30 phrases × 4 (or 3 if Telugu dropped) language translations as a JSON resource bundled in both apps
- [ ] iOS: `PhrasesRepository` reads bundled JSON; `FavoritesRepository` (SwiftData)
- [ ] Android: same with Room for favorites/recents and assets/raw for the bundled phrases

### Categories
- [ ] Restaurants 🍴 (orange) — 6 phrases
- [ ] Directions 🧭 (blue) — 6 phrases
- [ ] Transport 🚗 (green) — 6 phrases
- [ ] Shopping 🛍️ (purple) — 6 phrases
- [ ] Emergencies ⚠️ (red) — 6 phrases

### Screen
- [ ] iOS: `QuickPhrasesView` — Favorites section ⭐ at top (only if any), Recent 🕐 (only if any), All Categories
- [ ] iOS: Star toggle on every phrase row in any context
- [ ] iOS: Tapping a phrase pushes onto `recentPhrases` (cap 5, dedupe), populates `chatInput`, dismisses
- [ ] Android: `QuickPhrasesScreen` — same structure
- [ ] Android: same star/recent behavior

### Persistence
- [ ] iOS: SwiftData entity `FavoritePhrase` + `RecentPhrase`
- [ ] Android: Room `FavoritePhraseEntity` + `RecentPhraseEntity` + DAO

### Verification
- [ ] Star/unstar reflects across reopens of the app
- [ ] Recent list caps at 5, newest first, no dupes

---

## Phase 7 — History + Settings

### History
- [ ] iOS: `Conversation` SwiftData entity (id, title, lastMessagePreview, timestamp, messageCount, messages relationship)
- [ ] Android: `ConversationEntity` + Room DAO
- [ ] Persist conversations from chat sends
- [ ] iOS: `ConversationHistoryView` — search bar with debounced filtering, date-grouped sticky sections (Today / Yesterday / specific dates), card UI per `reference_app/FEATURES_AND_FLOWS.md` §6.1
- [ ] Android: `ConversationHistoryScreen` mirror
- [ ] Tap conversation → opens in Chat with messages loaded

### Settings
- [ ] iOS: `SettingsView` — Translation section (Default Language modal selector, Playback Speed Normal/Slow/Very Slow), Offline section (Download Phrases toggle disabled in v1), Privacy (Clear History action), Account (Log Out red button with confirmation)
- [ ] iOS: Persist selections in SwiftData/AppStorage
- [ ] Android: `SettingsScreen` mirror, persist via DataStore
- [ ] Logout returns to Login on both platforms

---

## Phase 8 — Audio (TTS) + Audio Player

### TTS
- [ ] iOS: `TTSService` using `AVSpeechSynthesizer`, locale-aware voice selection, `rate` mapped to Normal/Slow/Very Slow
- [ ] Android: `TtsService` using `TextToSpeech`, `setSpeechRate` mapping
- [ ] Wire in-message Play / Slow buttons → speak translated text

### Audio Player component
- [ ] iOS: `AudioPlayerView` — large translated text + pronunciation guide, 40-bar animated waveform, large play/pause coral gradient, slow toggle
- [ ] iOS: idle / playing / paused visual states
- [ ] Android: `AudioPlayer` composable, same

### Verification
- [ ] Play / Slow buttons in chat speak with correct rate
- [ ] Standalone Audio Player animates only while playing

---

## Phase 9 — Firebase Auth

### Setup
- [ ] Firebase project; add iOS app + Android app; download `GoogleService-Info.plist` and `google-services.json`
- [ ] Enable Email/Password, Google, Apple providers in Firebase console

### iOS
- [ ] `AuthService` wrapping FirebaseAuth + Sign in with Apple (`AuthenticationServices`) + GoogleSignIn-iOS
- [ ] Email Login → real sign-in
- [ ] Sign Up → real account creation
- [ ] Forgot Password → real password-reset email
- [ ] Login → real Google / Apple / Email / Skip (Skip stays as guest)
- [ ] Persist session; auto-skip auth flow if signed in
- [ ] Logout → Firebase sign-out

### Android
- [ ] `AuthService` with FirebaseAuth + Credential Manager (Google + passkeys) + Apple via Firebase OAuthProvider web flow
- [ ] All four sign-in paths
- [ ] Session persistence
- [ ] Logout

### Verification
- [ ] Real account create + login + logout on both platforms
- [ ] Forgot-password email arrives

---

## Phase 10 — Polish + ship

### Accessibility
- [ ] iOS: VoiceOver labels on every interactive element; Dynamic Type tested at xxxLarge
- [ ] iOS: Reduce Motion respects waveform/ping animations
- [ ] Android: TalkBack labels; `fontScale` 0.85–1.5 tested
- [ ] Both: WCAG AA contrast verified, 44 pt / 48 dp tap targets

### UX polish
- [ ] iOS: Haptics on tab switch, send, voice stop
- [ ] Android: Haptics equivalent (`HapticFeedbackConstants`)
- [ ] Loading states (skeleton screens) for chat, phrases, history
- [ ] Empty states for history, favorites, recent

### Theming
- [ ] Dark mode pass — every screen
- [ ] App icons (all sizes) imported from Figma export
- [ ] Launch screens

### Localization scaffolding
- [ ] All UI strings extracted (`Localizable.xcstrings` / `strings.xml`); v1 ships English only

### Store readiness
- [ ] iOS: App Store Connect listing, screenshots, privacy nutrition labels (mic, speech, audio)
- [ ] Android: Play Console listing, screenshots, data safety form
- [ ] iOS: TestFlight build distributed
- [ ] Android: Play Internal Testing track

### Release
- [ ] iOS submission
- [ ] Android submission

---

## Cross-cutting tasks (run alongside any phase)

- [ ] Crash reporting (Firebase Crashlytics on both platforms)
- [ ] Analytics events (Firebase Analytics) — at minimum: screen_view, send_message, voice_used, phrase_selected, favorite_toggled, language_changed, logout
- [ ] Privacy policy + terms hosted (URLs referenced in Login screen)
- [ ] App icons sourced from Figma (export 1024×1024 + adaptive icon variants for Android)

---

## Decision log

Track open §6 questions and their resolutions here as they're answered.

| Date | Decision | Outcome |
|------|----------|---------|
| _yyyy-mm-dd_ | §6a Telugu on Android | _pending_ |
| _yyyy-mm-dd_ | §6b Telugu on iOS | _pending_ |
| _yyyy-mm-dd_ | §6c Sign-in providers | _pending_ |
| _yyyy-mm-dd_ | §6d Bundle IDs / branding | _pending_ |

---

## Per-screen DoD reference

A screen is "done" only when (per `MOBILE_BUILD_PLAN.md` §8):
1. Pixel-faithful to `reference_app/` at 360–428 px
2. All states (default / press / active / disabled / loading / error / empty)
3. Accessibility labels + Dynamic Type / fontScale 0.85–1.5
4. Dark mode
5. Strings extracted
6. ViewModel unit test + 1 UI happy-path test
7. Tracker row ticked with date + commit SHA
