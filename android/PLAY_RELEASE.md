# Play Store release — Travel Translator AI

From zero → **Internal testing**. Steps marked 🧑 are yours (account/legal/console
UI — can't be automated); 🤖 are done/preparable by tooling.

## Artifact (ready)
- 🤖 Signed AAB: `android/app/build/outputs/bundle/release/app-release.aab` (13 MB)
- Signed with upload key `~/.keystores/travel-translator-upload.jks`
  (CN=Travel Translator AI). **Back this up.**
- `versionCode = 1`, `versionName = "0.1.0"`.
  ⚠️ Every subsequent upload must **increment `versionCode`** in
  `app/build.gradle.kts` (Play rejects duplicates).
- Points at the deployed backend `https://api-production-4cd7.up.railway.app`.
- ⚠️ **Auth caveat:** this build is **guest-only** (client `FIREBASE_*` keys are
  blank in `local.properties`). Email/Google sign-in won't work in the shipped
  build until you add those keys and rebuild. Fine for internal testing.

## Step 1 — 🧑 Developer account
1. https://play.google.com/console → register ($25 one-time).
2. Choose an **organization** or personal account; complete identity verification
   (Google now requires this before you can publish — can take 1–2 days).

## Step 2 — 🧑 Create the app
Play Console → **Create app**:
- App name: **Travel Translator AI**
- Default language: English (US)
- App or game: **App**
- Free or paid: **Free**
- Accept the declarations.

## Step 3 — 🧑 Set up → App content (compliance)
All under **Policy → App content**. Suggested answers for *this* app:

| Section | Answer |
| --- | --- |
| **Privacy policy** | URL where you host `PRIVACY_POLICY.md` (see Step 4) |
| **App access** | "All functionality available without special access" — guest mode needs no login. (If you later ship Firebase login, add test credentials here.) |
| **Ads** | **No**, app has no ads |
| **Content rating** | Fill the questionnaire → category **Utility/Productivity**; no violence/sexual/gambling content → will rate **Everyone** |
| **Target audience** | 13+ (not designed for children) → answer "No" to "appeals to children" |
| **Data safety** | See table below |
| **Government app** | No |

### Data safety answers (accurate to the code)
Data **collected** and sent off-device:
- **Personal info → Email address, Name** — *only if* the user signs in with
  email/Google (guest mode collects none). Purpose: account management.
- **Messages / other user content** — the text and voice audio the user
  translates. Purpose: app functionality. Processed by a third party
  (Hugging Face Inference API) for translation/speech.
- **App activity** — conversation history & recents, stored server-side per user.

Declarations:
- **Encrypted in transit:** Yes (HTTPS/TLS).
- **Data deletion:** Yes — the app exposes account deletion (`DELETE /me`);
  provide your support email / the in-app Settings path.
- **Shared with third parties:** Yes — text/audio is sent to Hugging Face for
  processing (disclose this).

## Step 4 — 🧑 Host the privacy policy
`PRIVACY_POLICY.md` is drafted (edit the contact + company details). Host it at a
public URL, e.g.:
- GitHub Pages (enable Pages on the repo, drop it as `privacy.html`), or
- a Notion/Google Sites page, or
- Railway static route.
Paste that URL into App content → Privacy policy.

## Step 5 — 🧑 Store listing (Grow → Store presence → Main store listing)
Copy is drafted in `store-listing.md`. Required graphics (you must produce):
- **App icon** 512×512 PNG (32-bit, ≤1 MB)
- **Feature graphic** 1024×500 PNG/JPG
- **Phone screenshots** ≥2 (16:9 or 9:16, 320–3840 px)
The brand palette is coral `#FF6B4A`; screenshots can come from the emulator/device
once you can run the app.

## Step 6 — 🧑 Play App Signing
On the first release, Play offers **Play App Signing** — accept it. Your
`upload-keystore.jks` becomes the *upload* key; Google holds the app signing key.
Losing the upload key is then recoverable (reset via support); losing it *without*
Play App Signing is not — another reason to keep the enrollment.

## Step 7 — Upload to Internal testing
🧑 **Testing → Internal testing → Create new release**:
1. Upload `app-release.aab`.
2. Release name: `0.1.0 (1)`; release notes: see `store-listing.md`.
3. Add testers (an email list); save & roll out.
4. Share the opt-in link with testers — available within minutes.

### 🤖 Automating uploads later
Once the app exists in Console and you create a **service account** (Play Console →
Users & permissions → invite the GCP service account, grant "Release to testing
tracks"; enable the *Google Play Android Developer API* in that GCP project and
download its JSON key), I can wire the **Gradle Play Publisher** plugin so
`./gradlew publishReleaseBundle` uploads straight to a track. Ping me then — it's
a ~15-line build change + the JSON key path in `local.properties`.

## Reality check before Production
- Run the app on a real device/emulator first (device E2E was blocked here by disk).
- Add client `FIREBASE_*` keys if you want social sign-in.
- Consider enabling R8/minify for a smaller production build (currently off).
