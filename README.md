# Travel Translator AI

Native iOS + Android apps for the Travel Translator AI product.

## Layout

```
.
├── ios/              # SwiftUI app (Swift 6, iOS 17+)
├── android/          # Jetpack Compose app (Kotlin 2.x, minSdk 26)
├── shared/
│   └── design-tokens.json   # Single source of truth for colors, radii, spacing, typography
├── reference_app/    # Original Figma Make Vite + React + TS prototype (read-only reference)
├── MOBILE_BUILD_PLAN.md      # Strategy + stack + architecture
├── MOBILE_BUILD_TRACKER.md   # Per-task checklist
└── CLAUDE.md
```

## Reference app

`reference_app/` is the original UI prototype. **Treat it as read-only design reference** — every screen, interaction, and visual detail in the native apps must match what is in there. Run it to compare visuals while building:

```bash
cd reference_app
npm install
npm run dev
```

## Native apps

- **iOS**: open `ios/` in Xcode 16. See `MOBILE_BUILD_PLAN.md` §3 for architecture.
- **Android**: open `android/` in Android Studio. See `MOBILE_BUILD_PLAN.md` §3 for architecture.

Work order is in `MOBILE_BUILD_TRACKER.md` — phase 0 → phase 10.

## Design tokens

`shared/design-tokens.json` is the source of truth. A small codegen step (added in phase 0) compiles it into `Tokens.swift` and `Tokens.kt`. Never hardcode brand colors in either app.
