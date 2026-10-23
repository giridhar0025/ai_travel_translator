# Mobile Build Tracker — Travel Translator AI

Status of the native apps and the backend they talk to. Replaces the
`MOBILE_BUILD_PLAN.md` / `MOBILE_BUILD_TRACKER.md` referenced by earlier docs —
those files were never committed, so this is the authoritative list.

**Last updated:** 2026-10-23

---

## Where things stand

| Area | State | Notes |
| --- | --- | --- |
| Backend (FastAPI) | ✅ Complete | 146 tests, 95%+ coverage, ruff/black clean |
| Android (Compose) | ✅ Feature-complete | `0.1.2` (versionCode 3), signed APK/AAB |
| iOS (SwiftUI) | 🟡 Design system only | No screens, no networking, no view models |
| Hosting | 🔶 Local + Cloudflare tunnel | Railway trial expired; needs a paid plan |
| Play Store | 🟡 Artifacts + docs ready | Console steps not done |

---

## Backend

Phases 0–9 of `backend/BACKEND_BUILD_TRACKER.md` are complete. Phase 10
(deployment) is blocked on billing, not code.

- 26 routes under `/api/v1`; 20 seeded languages; phrases, conversations,
  search, favorites, recents, settings.
- **Translation** now runs through an interchangeable chat-completions client:
  - `LLM_PROVIDER=auto` → **Groq** when `GROQ_API_KEY` is set, else Hugging Face.
  - Groq model: `openai/gpt-oss-120b` (free tier). HF's `llama-3.3-70b` is not
    available on current Groq keys — do not set it.
  - Provider failures map to **502**, so clients can fall back cleanly.
- HF Inference credits are exhausted (402). HF stays wired as a fallback but is
  not usable until credits are added.
- Pronunciation: IPA via `epitran` for hi/te/ta/bn/mr/th/ko/ru; `None` elsewhere.
- TTS: en/hi via Chatterbox (Replicate); te/th fall back to on-device TTS.

## Android

Package `com.divami.traveltranslator`, minSdk 26, target 35.

Screens: splash → welcome → login/signup/forgot → onboarding → chat
(streaming bubbles, context toggle, language selector) + quick phrases,
history, settings, voice input, audio player.

**Offline translation (0.1.1+, hardened in 0.1.2)**

- `data/local/OfflineTranslator.kt` wraps ML Kit (`com.google.mlkit:translate`).
- Fallback triggers on `Network`, `Timeout`, `Server`, `Unknown` failures only.
  4xx/auth errors are surfaced, not masked.
- `core/network/NetworkStatus.kt` short-circuits the request when the device has
  no network. Without it the SSE call hung on its socket timeout (~45 s of
  typing dots) before the fallback ever ran.
- Models download per language pair **on Wi-Fi/Ethernet only**, warmed up in the
  background after each successful online translation.
- Offline results are tagged `model = "mlkit-offline"` and carry no IPA.
- Limitation: a pair can only translate offline after its model has been
  downloaded once. Language coverage is 59 (all 20 app languages are included);
  quality is literal.

**Verified on device (SM-S938B, Android 17, 2026-10-23)**

- Online: `Where are you → तुम कहाँ हो?`, `Good morning → सुप्रभात`,
  `Good night → शुभ रात्रि` — streamed, with IPA pronunciation.
- Offline (Wi-Fi + data disabled): `Hello → नमस्ते` via ML Kit, no network calls.
- 20/20 Android unit tests pass (7 SafeApiCall, 7 Repository, 6 Serialization).

## iOS

`ios/TravelTranslator/` contains the design system (`Theme.swift`, `Tokens.swift`,
11 components) and a placeholder `ContentView.swift` that shows the component
gallery in debug. There is no feature UI, no `TranslationService`, no networking.

---

## Open items

### Blocking
- [ ] **Backend hosting** — Railway trial expired. Either subscribe to Hobby
      ($5/mo, then `railway up`) or keep using a Cloudflare quick tunnel.
      Quick-tunnel URLs change on restart, which breaks installed builds.
- [ ] Rebuild the APK whenever `BACKEND_BASE_URL` changes
      (`android/local.properties`).

### Play Store
- [ ] Host `android/PRIVACY_POLICY.md` at a public URL
- [ ] App icon 512×512, feature graphic 1024×500, ≥2 phone screenshots
- [ ] Create app + internal testing track in Play Console
- [ ] Upload `app-release.aab` (bump `versionCode` every upload)

### Product / quality
- [ ] Real-device end-to-end pass (translate, voice, TTS, history, offline)
- [ ] Replace placeholder Terms/Privacy URLs in login/signup
      (`TODO(audit-#26)`)
- [ ] Enable R8/minify for production builds
- [ ] Decide whether iOS ships in v1; if yes, port screens + services
- [ ] Optional: pre-download offline models on first launch for the default pair

### Housekeeping
- [ ] `android/README.md` still describes an empty folder — stale
- [ ] Commit `MOBILE_BUILD_TRACKER.md`; the old plan/tracker files are gone
- [ ] Rotate secrets that have been in local files (HF token, Groq key, JWT
      secret, keystore passwords) before any public release

---

## Run it

```bash
# Backend (needs Postgres on 5435 and a Groq key in backend/.env)
cd backend && .venv/bin/python -m uvicorn app.main:app --host 0.0.0.0 --port 8000

# Expose it publicly for a device build
cloudflared tunnel --url http://127.0.0.1:8000

# Point the app at it, then build + install
#   android/local.properties -> BACKEND_BASE_URL=https://<tunnel>.trycloudflare.com
cd android && ./gradlew assembleRelease
adb install -r app/build/outputs/apk/release/app-release.apk
```
