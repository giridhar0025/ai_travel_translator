# tools

## `gen-tokens.mjs`

Reads `../shared/design-tokens.json` and emits Swift + Kotlin token files.

```bash
node tools/gen-tokens.mjs
```

Default outputs:
- `shared/generated/Tokens.swift`
- `shared/generated/Tokens.kt`

Override paths once the IDE projects exist:

```bash
TOKENS_IOS_OUT=ios/TravelTranslator/DesignSystem/Tokens.swift \
TOKENS_ANDROID_OUT=android/app/src/main/java/com/divami/traveltranslator/designsystem/Tokens.kt \
  node tools/gen-tokens.mjs
```

### Wiring into the IDE builds (Phase 0)

**Xcode:** add a Run Script Build Phase before "Compile Sources":

```bash
cd "$SRCROOT/.."
TOKENS_IOS_OUT="$SRCROOT/TravelTranslator/DesignSystem/Tokens.swift" \
  node tools/gen-tokens.mjs
```

**Gradle:** add a task in `android/app/build.gradle.kts`:

```kotlin
tasks.register<Exec>("generateDesignTokens") {
    workingDir = rootProject.projectDir.parentFile
    commandLine("node", "tools/gen-tokens.mjs")
    environment(
        "TOKENS_ANDROID_OUT",
        file("src/main/java/com/divami/traveltranslator/designsystem/Tokens.kt").absolutePath
    )
}
tasks.named("preBuild") { dependsOn("generateDesignTokens") }
```

### What's NOT codegen'd

`languages`, `context`, and `playbackSpeed` in the JSON are app config (not visual tokens). Hand-code those as enums in each project and load translations from JSON resources at runtime.
