# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Travel Translator AI — native iOS (SwiftUI) and Android (Jetpack Compose) apps being built from a Figma Make UI prototype. Repo layout:

```
.
├── ios/              # SwiftUI app (Swift 6, iOS 17+)
├── android/          # Jetpack Compose app (Kotlin 2.x, minSdk 26)
├── shared/
│   └── design-tokens.json
├── reference_app/    # Original Vite + React 18 + TS prototype — READ-ONLY reference
├── MOBILE_BUILD_PLcantAN.md
├── MOBILE_BUILD_TRACKER.md
└── README.md
```

**The native apps are the product.** `reference_app/` is the visual + behavioral spec; do not modify it.

## Where to look first

- **`MOBILE_BUILD_PLAN.md`** — strategy, stack decisions, architecture, open §6 decisions (Telugu support is the big one), 10 build phases.
- **`MOBILE_BUILD_TRACKER.md`** — per-task checklist; tick boxes as work lands.
- **`reference_app/FEATURES_AND_FLOWS.md`** — full UX spec, every screen, brand colors, supported languages.
- **`reference_app/NEW_FEATURES_SUMMARY.md`** — context toggle + dual-layer bubbles + favorites/recent.
- **`shared/design-tokens.json`** — single source of truth for colors/radii/spacing/typography. Already extracted from `reference_app/src/styles/theme.css`.
- **`reference_app/src/app/App.tsx`** — screen state machine to mirror.
- **`reference_app/src/app/components/*.tsx`** — every screen component to be ported.

## Reference app commands

Only run when comparing visuals against the prototype while building:

```bash
cd reference_app
npm install
npm run dev
```

There is no test runner, linter, formatter, or `tsconfig.json` in `reference_app/`. TypeScript is type-checked only by Vite/esbuild during build. Treat it as read-only — bugs there are not in scope.

## Native build commands

The `ios/` and `android/` projects are not yet bootstrapped (Phase 0 of the tracker). Setup steps live in `ios/README.md` and `android/README.md`.

## Architecture (high level)

- **State machine parity:** the prototype's `App.tsx` is a single state machine — `currentScreen` (12 values) plus 4 boolean overlay flags. Mirror this on both platforms:
  - iOS: `AppRouter` (`@Observable`) + `NavigationStack` + `.sheet` / `.fullScreenCover` for the 4 overlays.
  - Android: typed `@Serializable` Navigation-Compose 2.8+ destinations + `ModalBottomSheet` for overlays.
- **Lifted chat state:** the prototype lifts `chatMessages` and `chatInput` into `App.tsx`. Mirror with a `ChatViewModel` that owns chat state; child views read/write via bindings/state hoisting.
- **Design tokens:** never hardcode brand colors. `shared/design-tokens.json` compiles into `Tokens.swift` and `Tokens.kt` via codegen (Phase 0).
- **Service contracts:** see `MOBILE_BUILD_PLAN.md` §5 for matching `TranslationService` / `SpeechService` / `TtsService` / `AuthService` interfaces.

## Brand identity

- Primary: `#FF6B4A` coral
- Primary Dark: `#E85533`
- Primary Light: `#FFF0ED`
- Accent: `#F59E0B` amber
- Success: `#22C55E`
- Languages: English, Hindi, Telugu, Thai (Telugu support is platform-dependent — see `MOBILE_BUILD_PLAN.md` §6)

## Doing work here

1. Pick the next unchecked item from `MOBILE_BUILD_TRACKER.md`.
2. Reference the matching screen in `reference_app/src/app/components/` for visual + behavioral truth.
3. Use `shared/design-tokens.json` (or its compiled `Tokens.swift` / `Tokens.kt`). Never hardcode tokens.
4. Tick the tracker row when done.
