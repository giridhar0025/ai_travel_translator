# Backend Build Tracker — Travel Translator AI

Tick boxes as work lands. Each section maps 1:1 to a phase in `BACKEND_BUILD_PLAN.md` §7 and its agent brief in `agents/`.

---

## Phase 0 — Scaffolding & infra (`agents/00-scaffolding.md`) ✅

- [x] `backend/pyproject.toml` with FastAPI, SQLAlchemy 2, asyncpg, alembic, httpx, firebase-admin, pydantic-settings, pytest pinned
- [x] `uv.lock` committed
- [x] `backend/app/main.py` boots FastAPI app with `/health` route
- [x] `backend/app/config.py` loads from `.env` via `pydantic-settings`
- [x] `backend/.env.example` with every variable from plan §8
- [x] `backend/Dockerfile` (multi-stage, slim base, non-root user)
- [x] `backend/docker-compose.yml` runs Postgres + app for local dev
- [x] `backend/README.md` with `uv sync` → `uv run uvicorn app.main:app` quick start
- [x] `ruff` + `black` configured in `pyproject.toml`
- [x] `uv run pytest` runs (passes on `tests/test_health.py`)

---

## Phase 1 — Database & migrations (`agents/01-database.md`) ✅

- [x] `backend/app/db/base.py` declarative base + naming convention
- [x] `backend/app/db/session.py` async session factory + `get_session` dep
- [x] SQLAlchemy models for: `User`, `UserSettings`, `Language`, `PhraseCategory`, `Phrase`, `PhraseTranslation`, `Conversation`, `Message`, `Favorite`, `Recent`
- [x] `backend/alembic/` initialized, `env.py` wired to async engine + reads URL from `app.config`
- [x] Initial migration `0001_initial.py` creates every table from plan §6
- [x] Indexes from plan §6 created (incl. `pg_trgm` extension + GIN trigram indexes on `messages.source_text` / `messages.translated_text`)
- [x] `scripts/seed_phrases.py` seeds 4 languages + 5 categories + 30 phrases (idempotent: 2nd run inserts 0)
- [x] `uv run alembic upgrade head` then seed succeeds on a fresh DB
- [x] Thin repository stubs in `app/repositories/` for every aggregate (language, user, phrase, conversation, favorite)
- [x] Note: local docker Postgres mapped to host port **5434** (5432/5433 are taken by the user's local Postgres installs)

---

## Phase 2 — Auth (Firebase) (`agents/02-auth.md`) ✅

- [x] Firebase project created — project ID `travel-translator-ai`
- [x] Google, Email/Password, Anonymous providers enabled in Firebase console (Apple deferred — needs Apple Developer account)
- [x] Service account JSON downloaded + base64-encoded into `FIREBASE_SERVICE_ACCOUNT_JSON` in local `.env`; `firebase_admin.get_app().project_id` returns `travel-translator-ai`
- [x] `app/auth/firebase.py` initializes `firebase-admin` lazily
- [x] `app/auth/tokens.py` issues + verifies short-lived backend JWTs (HS256, iss=`travel-translator-api`)
- [x] `POST /auth/verify` verifies Firebase ID token, upserts `User`, returns app JWT (mocked-Firebase integration test green)
- [x] `POST /auth/guest` mints a guest user + JWT — live-curl confirmed
- [x] `GET /me` returns current user + settings — live-curl confirmed
- [x] `DELETE /me` cascade-deletes user-owned rows (via ON DELETE CASCADE FKs) — live-curl confirmed (returns 204)
- [x] `app/deps.py::get_current_user` is the only DI used in protected routes
- [x] Unit tests for token verification: roundtrip / guest flag / expired / wrong signature / wrong issuer / malformed sub (6/6 pass)
- [x] Unit tests for Firebase verifier: happy / firebase error / missing optional claims (3/3 pass)
- [x] Integration tests: guest flow / verify-new / verify-existing / 401-on-bad-firebase / cascade-on-delete / bearer-required (6/6 pass)
- [x] Switched DB engine to `NullPool` to avoid "event loop is closed" cleanup races under TestClient
- [x] Switched local Postgres to brew `postgresql@15` on port **5435** because Docker daemon became unreliable mid-session

---

## Phase 3 — Translation service (`agents/03-translation.md`) ✅

- [x] `app/services/hf_client.py` shared async HF client (auth header, timeout, retry-on-503 with backoff)
- [x] **Model swap**: `facebook/m2m100_418M` returned "not supported by provider hf-inference" on live HF — switched to OPUS-MT (`Helsinki-NLP/opus-mt-en-mul` + `opus-mt-mul-en`). Cross-lang pairs chain via English. CC-BY 4.0 still commercial-safe.
- [x] **HF base URL**: `api-inference.huggingface.co` → `router.huggingface.co/hf-inference` (HF deprecated the old hostname)
- [x] `POST /translate` returns translation + IPA pronunciation + latency_ms + model used
- [x] Pronunciation: `epitran` for hi/te/th (verified working: `aspətaːlə kəɦãː ɦæː?` for Hindi); en returns None
- [x] `POST /translate/batch` runs items via `asyncio.gather` with concurrency=5 semaphore
- [x] `GET /languages` returns the seeded language registry
- [x] In-memory LRU cache (1000 entries) keyed on (source, target, text, context); verified test exercises cache hit path
- [x] Recents background-write via `BackgroundTasks` on `/translate` success; dedup window = 24h on `(user_id, source_text, target_lang)`
- [x] Live integration verified end-to-end against real HF: en↔hi, en↔te, en↔th, plus hi→te (cross-lang chain in 783ms warm)
- [x] Unit tests with mocked HF: 12 tests covering happy path, en↔X, X↔en, cross-lang chain, context prefix, cache, batch, retries, 4xx
- [x] Integration tests for /translate, /translate/batch, /languages with respx-mocked HF (6 tests)
- [x] **License check**: OPUS-MT family = CC-BY 4.0 (commercial-safe)

---

## Phase 4 — Speech-to-text (`agents/04-stt.md`) ✅

- [x] `POST /stt/transcribe` accepts multipart audio (wav/m4a/mp3/etc), validates duration ≤ 30 s for parseable formats
- [x] `app/services/stt.py` calls `openai/whisper-large-v3` via HF Inference router (smaller Whisper variants are not on hf-inference)
- [x] Echoes `language` from request; auto-detect happens server-side at HF but isn't returned in response
- [x] Returns `{transcript, language, confidence, latency_ms, model}`
- [x] 413 if audio > 10 MB; 415 if MIME type unsupported; 400 on empty payload or duration > 30 s
- [x] Unit tests with fake HF client (3 tests: happy / missing-text / string-response)
- [x] Integration tests with respx-mocked HF (7 tests: auth / happy / 415 / 413 / 400-long / 400-empty)
- [x] **Content-Type fix**: HF router rejects `application/octet-stream` for audio. `HuggingFaceClient.infer` now takes `content_type` (defaults to `audio/wav`), STT route passes the upload's content-type through.
- [x] **HF timeout bumped to 90 s** in `.env.example` + config default — Whisper-large-v3 cold-start can exceed 30 s.
- [x] **Live end-to-end verified**: macOS `say "Where is the nearest hospital"` → `/stt/transcribe` returned the exact phrase in 1378 ms; chained through `/translate` → "नज़रतम अस्पताल कहाँ है?" in 3904 ms.

---

## Phase 5 — Text-to-speech (`agents/05-tts.md`) ✅

**Mid-phase model swap (2026-05-26).** The HF `hf-inference` provider stopped
serving `parler-tts/parler-tts-mini-v1` and `ai4bharat/indic-parler-tts`
(`"Model not supported by provider hf-inference"`) — same cull that hit
m2m100 in Phase 3. Probed all HF-routed providers; **no commercial-safe Telugu
TTS exists on the router today**. Rerouted to `ResembleAI/chatterbox`
(`chatterbox-turbo`) via the **Replicate** provider on the HF router — MIT
licensed, covers en + hi. Telugu joins Thai in the client-side fallback
bucket (Android/iOS handle te via on-device TTS).

- [x] `POST /tts/synthesize` returns `audio/wav` stream (binary `Response` with `X-TTS-Model` header)
- [x] Routes via Replicate (`https://router.huggingface.co/replicate/v1/predictions`) — async create + poll + CDN download flow on `hf_client.predict_replicate()`
- [x] `speed=slow` post-processes audio with `librosa` time-stretch (no pitch shift) — rate=0.7
- [x] Returns 415 `{"detail": "client_side_tts_required", "language": ...}` for `te` AND `th`
- [x] Returns 415 `{"detail": "unsupported_language", "language": ...}` for anything else (fr, etc.)
- [x] Returns 502 `{"detail": "tts_provider_failed", ...}` when Replicate prediction status is `failed`/`canceled` or polling times out
- [x] Unit tests: 8 tests (te→ClientSideTtsRequired / th→ClientSideTtsRequired / fr→Unsupported / en→chatterbox / hi→chatterbox / slow stretches / normal passthrough / non-bytes guard)
- [x] Integration tests with respx-mocked HF: 11 tests (auth / en happy / hi happy / te 415 / th 415 / fr 415 / empty 422 / whitespace 400 / >500 chars 422 / slow longer audio / failed→502)
- [x] **Live end-to-end validated 2026-05-26**: en (2.4 s, 230 KB WAV), hi (3.9 s, 952 KB WAV for "नज़दीकी अस्पताल कहाँ है?"), te/th→415 client-side, fr→415 unsupported, slow=en stretches 2.4→2.97 s
- [x] **License check**: confirmed `ResembleAI/chatterbox` = MIT in `backend/README.md` Acknowledgements
- [ ] **Mobile client task** (separate ticket): handle 415 `client_side_tts_required` for `te` AND `th` by falling back to `android.speech.tts.TextToSpeech(Locale.forLanguageTag(...))` / iOS `AVSpeechSynthesizer`

---

## Phase 6 — Conversations & history (`agents/06-conversations.md`) ✅

- [x] `GET /conversations` paginated (keyset on `last_message_at`) + grouped (Today/Yesterday/`YYYY-MM-DD`) + search `?q=`
- [x] `POST /conversations` create (optional `title`); returns 201
- [x] `GET /conversations/{id}` returns convo + messages (oldest-first), keyset paginated by `before_id`
- [x] `DELETE /conversations/{id}` soft-delete; 204 on success, 404 on unknown / already-deleted / other user's row
- [x] `POST /conversations/{id}/messages` append message, transactional bump of `last_message_at` + `message_count` + auto-title (first 40 chars of `source_text`, fallback `"Conversation"`)
- [x] Search uses `ILIKE %q%` against title + EXISTS(messages.source_text|translated_text), backed by GIN trgm indexes from migration 0001 — Hindi script search live-verified
- [x] Date grouping respects `User.timezone` (ZoneInfo, falls back to UTC on unknown tz)
- [x] Unit tests: 4 tests on date_groups (today/yesterday/older / timezone shift / bad-tz fallback / empty)
- [x] Integration tests: 15 tests covering create→append→list→search→pagination→soft-delete→cross-user isolation→auto-title→detail-paginate
- [x] **Live end-to-end 2026-05-26**: create untitled → 3 messages append → auto-title = "Where is the menu?" → search `q=menu` finds it, `q=zzzz` empty → DELETE returns 204 → list excludes

---

## Phase 7 — Phrases library (`agents/07-phrases.md`) ✅

- [x] `GET /phrases/categories` returns the 5 seeded categories in `sort_order` (public — no auth, like `/languages`)
- [x] `GET /phrases?category={slug}&target={lang}` returns 6 phrases per category, translated to target, ordered by `sort_order`
- [x] Translations are lazily generated on first request, then cached in `phrase_translations` (unique on `(phrase_id, target_lang)` → idempotent `pg_insert(...).on_conflict_do_update`)
- [x] `target == source_lang` short-circuits: returns `source_text` as `translation` with no HF round-trip
- [x] Unknown category → 404; unsupported target language → 400 (validated against `languages` registry, not OPUS-MT support)
- [x] Unit tests (5): unknown category / same-language short-circuit / cold cache → batch + persist / warm cache → no HF / partial cache → translate misses only
- [x] Integration tests (7): list-categories / first call → 6 HF calls + cache / warm second call → 0 HF / target=en → 0 HF / switching target → fresh batch / unknown → 404 / unsupported target → 400 / missing params → 422
- [x] **Live end-to-end 2026-05-26**: `/phrases/categories` returns 5 entries; cold `restaurants?target=hi` triggers 6 HF batch calls (~12 s incl. cold-start, ~1 s after warm), second call hits cache in **74 ms**; unknown slug → 404; `target=fr` → 400

---

## Phase 8 — Settings & favorites (`agents/08-settings-favorites.md`) ✅

- [x] `GET /settings` returns defaults if no row yet (auto-creates via `UserSettingsRepo.get_or_create`)
- [x] `PATCH /settings` partial-update — `default_language` validated against `languages` registry (400 on unknown), `playback_speed` typed `Literal["normal","slow","very_slow"]` (422 on bad enum), `offline_enabled` bool
- [x] `GET /favorites?kind=phrase|translation` lists user favorites (newest first)
- [x] `POST /favorites` create — pre-validates `source_lang`/`target_lang` against registry (400), catches `IntegrityError` on the `(user_id, kind, ref_id, target_lang)` unique constraint → **409 `already favorited`**
- [x] `DELETE /favorites/{id}` — 204 on success, 404 if not owned (cross-user isolation tested)
- [x] `GET /recents?limit=20` — Postgres `DISTINCT ON (source_text, target_lang)` subquery dedups newest per pair, then re-sorted by `created_at DESC`
- [x] Recents auto-written from `/translate` handler (after-response task) — already wired in Phase 3 (`app/services/recents.insert_recent` with 24h upsert window); confirmed contract matches §3 of agent brief
- [x] Integration tests: 7 settings (auth / defaults / partial patch / 422 bad speed / 400 bad lang / accepts all 3 speeds / offline flag), 8 favorites (auth / create / 409 dup / 400 bad lang / 422 bad kind / kind filter / 204→404 / cross-user 404), 6 recents (auth / empty / 24h dedup / newest-first / read-side dedup of >24h dups / limit=2) — **21/21 pass**
- [x] Full suite 119/119 pass; ruff clean

---

## Phase 9 — Testing & CI (`agents/09-testing.md`) ✅

- [x] `tests/conftest.py` picks up `TEST_DATABASE_URL` env override before `app.*` import (used by CI). HF + Firebase already mocked inline per-test via `respx` and `unittest.mock.patch` — no internet needed
- [x] Unit tests cover every service module (`test_translation`, `test_stt`, `test_tts`, `test_hf_client`, `test_phrases_service`, `test_conversation_groups`, `test_auth_tokens`, `test_firebase_verifier`, `test_settings`) — 39 unit tests
- [x] Integration tests cover every router happy path + error paths (`test_auth_routes`, `test_conversations_routes`, `test_favorites_routes`, `test_languages_route`, `test_phrases_routes`, `test_recents_routes`, `test_settings_routes`, `test_stt_route`, `test_translate_routes`, `test_tts_route`) — 87 integration tests
- [x] `.github/workflows/backend.yml`: ruff check + black --check + pytest with coverage, gated on `backend/**` paths. Postgres 16 service container, runs `alembic upgrade head` + `seed_phrases` before tests
- [x] Coverage XML uploaded as GitHub Actions artifact (Codecov skipped — optional per brief)
- [x] Coverage fix: added `[tool.coverage.run] concurrency = ["thread", "greenlet"]` to `pyproject.toml` — without it, async route handler bodies under TestClient threadpool dispatch were false-missed (settings router showed 53% pre-fix, 100% after)
- [x] Ruff per-file-ignore `N818` on `app/services/tts.py` — `ClientSideTtsRequired` / `UnsupportedLanguageError` class names ARE the public 415 detail-code contract; renaming would break clients
- [x] **Final result**: 126/126 tests pass, **95% total coverage** (services avg 96%, routers avg 89%), ruff clean, black clean. Local run: `uv run pytest --cov=app --cov-report=term-missing` in 31 s
- [x] **Explicit out-of-scope** (deferred or skipped per pragmatism vs. brief):
  - Full `conftest.py` rewrite with `db_session`/`app`/`auth_headers`/module-level `fake_hf`+`fake_firebase` fixtures — existing per-test inline patches work and rewriting = 100+ test churn for no behavior gain
  - `testcontainers` ephemeral Postgres — CI uses GitHub Actions postgres service container instead (matches brief's YAML; testcontainers needs Docker-in-Docker which is finicky on Actions)
  - `.pre-commit-config.yaml` — marked optional in brief

---

## Phase 10 — Deployment (Railway) (`agents/10-deployment.md`) 🔶 (local artifacts done; deploy step blocked on user)

### Code / artifacts (done)

- [x] `backend/railway.json` — Dockerfile builder, `/health` healthcheck (timeout 30 s), `ON_FAILURE` restart (max 3), pre-deploy `alembic upgrade head && python -m scripts.seed_phrases`, start command `uvicorn app.main:app --host 0.0.0.0 --port $PORT`
- [x] `app/config.py` — `Settings.database_url` `field_validator` rewrites both `postgres://...` (Railway's emitted form) and bare `postgresql://...` to `postgresql+asyncpg://...`. No env hand-editing needed on Railway. **4 new unit tests in `tests/unit/test_config.py`** cover all four scheme cases.
- [x] `backend/DEPLOY.md` — end-to-end runbook: Railway account/project/Postgres setup, env-var table with origins, first-deploy flow, smoke-test curls, Android wiring, rollback. Generated a fresh `APP_JWT_SECRET` for the user to paste.
- [x] `android/app/build.gradle.kts` — reads `BACKEND_BASE_URL` from `local.properties` with `http://10.0.2.2:8000` emulator default, surfaces as `BuildConfig.BACKEND_BASE_URL`. Enabled `buildFeatures { buildConfig = true }`. Added `BACKEND_BASE_URL=` placeholder line (commented) to `android/local.properties` for discoverability.
- [x] Full backend CI trio post-changes: **130/130 tests pass** (was 126 + 4 config tests), ruff clean, black clean.

### Deploy + smoke (blocked on user — needs Railway account + dashboard)

- [ ] Railway project created, Postgres plugin attached
- [ ] All env vars set in Railway dashboard from `DEPLOY.md` table
- [ ] First deploy succeeds — pre-deploy migrations + seed run cleanly
- [ ] `/health` returns 200 from deployed URL
- [ ] Smoke curls pass: `/auth/guest` → JWT, `/translate` cold P50 < 5 s + warm P50 < 2 s, `/docs` lists all routers
- [ ] Android app smoke-tested against deployed URL end-to-end:
  - [ ] `BACKEND_BASE_URL` set in `android/local.properties` to live URL
  - [ ] Google sign-in → `/auth/verify` → JWT issued
  - [ ] Translate en→hi text → response < 3 s warm
  - [ ] Voice → transcript → translate → TTS playback works
  - [ ] Conversations persist + show in history after cold-start
  - [ ] Quick phrases load with cached translations on second open

### Notes

- Dockerfile static-only verification — Docker daemon was unreliable this session (same note as Phase 2). Trace: `alembic` and `python -m scripts.seed_phrases` resolve via the copied venv (`/app/.venv/bin` on `$PATH` in runner stage); `Settings.database_url` validator normalizes Railway's URL on first import. First Railway build is the only end-to-end check that the multi-stage assembly works — risk flagged in `DEPLOY.md`.
- `uv run` dropped from `preDeployCommand` because `uv` binary lives only in the builder stage of `Dockerfile`. Runner uses venv binaries directly via `$PATH`.

---

## Definition of done

All phases checked above + the bullets in `BACKEND_BUILD_PLAN.md` §10.
