# iOS — Travel Translator AI

SwiftUI app, Swift 6, iOS 17 deployment target (target features iOS 18).

## Phase 0 setup (next steps)

This folder is intentionally empty. To bootstrap:

1. Open Xcode 16 → File → New → Project → iOS → App.
2. Product Name: `TravelTranslator`. Interface: SwiftUI. Language: Swift. Storage: SwiftData. Include Tests checked.
3. Save into this `ios/` directory so the structure becomes `ios/TravelTranslator.xcodeproj`, `ios/TravelTranslator/...`, etc.
4. In project settings:
   - Set deployment target to iOS 17.0.
   - Enable Swift 6 language mode (Build Settings → Swift Language Version).
   - Enable strict concurrency checking (Build Settings → Strict Concurrency Checking → Complete).
5. Add SwiftPM packages: `firebase-ios-sdk` (FirebaseAuth product), `GoogleSignIn-iOS`.
6. Add Info.plist usage descriptions: `NSMicrophoneUsageDescription`, `NSSpeechRecognitionUsageDescription`.
7. Add a token codegen Run Script Build Phase that reads `../shared/design-tokens.json` and writes `TravelTranslator/DesignSystem/Tokens.swift`.

After scaffolding, the folder layout should match `MOBILE_BUILD_PLAN.md` §3 (Features/, DesignSystem/, Services/, Persistence/).

Track progress in `../MOBILE_BUILD_TRACKER.md`.
