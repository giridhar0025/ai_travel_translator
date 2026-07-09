import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

// All build-time config flows local.properties → BuildConfig. Firebase is
// initialised at runtime from these fields (see TravelTranslatorApp), so the
// google-services Gradle plugin — and a checked-in google-services.json — are
// deliberately NOT required to build. A fresh checkout compiles and runs the
// guest-auth path immediately; drop the four FIREBASE_* values into
// local.properties to light up email/Google sign-in.
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
fun prop(key: String, default: String = ""): String =
    (localProps.getProperty(key) ?: System.getenv(key) ?: default)

// BACKEND_BASE_URL — default targets the emulator host loopback (10.0.2.2) so a
// fresh checkout works against `uvicorn` on the dev machine. Override in
// local.properties to point at the Railway URL — see backend/DEPLOY.md.
val backendBaseUrl: String = prop("BACKEND_BASE_URL", "http://10.0.2.2:8000/")

android {
    namespace = "com.divami.traveltranslator"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.divami.traveltranslator"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "BACKEND_BASE_URL", "\"$backendBaseUrl\"")

        // Firebase config — sourced from local.properties (see the Firebase
        // console → Project settings → your Android app). Empty by default;
        // FirebaseAuthGateway treats blank values as "Firebase not configured"
        // and only the guest path stays available.
        buildConfigField("String", "FIREBASE_API_KEY", "\"${prop("FIREBASE_API_KEY")}\"")
        buildConfigField("String", "FIREBASE_APP_ID", "\"${prop("FIREBASE_APP_ID")}\"")
        buildConfigField("String", "FIREBASE_PROJECT_ID", "\"${prop("FIREBASE_PROJECT_ID")}\"")
        // OAuth 2.0 Web client ID (Firebase → Authentication → Google → Web SDK
        // config). Required for the Credential Manager Google sign-in flow.
        buildConfigField("String", "FIREBASE_WEB_CLIENT_ID", "\"${prop("FIREBASE_WEB_CLIENT_ID")}\"")
    }

    // Release signing — credentials come from local.properties (gitignored).
    // When RELEASE_STORE_FILE is blank the release build stays unsigned, so a
    // fresh checkout without the keystore still configures cleanly.
    val releaseStoreFile = prop("RELEASE_STORE_FILE")
    signingConfigs {
        if (releaseStoreFile.isNotBlank()) {
            create("release") {
                storeFile = file(releaseStoreFile)
                storePassword = prop("RELEASE_STORE_PASSWORD")
                keyAlias = prop("RELEASE_KEY_ALIAS")
                keyPassword = prop("RELEASE_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.findByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.ui.text.google.fonts)

    // Dependency injection
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    // Networking
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)

    // Local persistence (auth token + preferences)
    implementation(libs.androidx.datastore.preferences)

    // Firebase auth + Google sign-in via Credential Manager
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)

    debugImplementation(libs.androidx.ui.tooling)

    // Unit tests
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}

// Token codegen — runs before compile
tasks.register<Exec>("generateDesignTokens") {
    workingDir = rootProject.projectDir.parentFile
    commandLine("node", "tools/gen-tokens.mjs")
    environment(
        "TOKENS_ANDROID_OUT",
        file("src/main/java/com/divami/traveltranslator/designsystem/Tokens.kt").absolutePath
    )
}
tasks.named("preBuild") { dependsOn("generateDesignTokens") }
