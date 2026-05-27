import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

// BACKEND_BASE_URL flows local.properties → BuildConfig.BACKEND_BASE_URL.
// Default targets the emulator host loopback (10.0.2.2) so a fresh checkout
// works against `uvicorn` on the dev machine. Override in local.properties
// to point at the Railway URL — see backend/DEPLOY.md.
val backendBaseUrl: String = run {
    val props = Properties()
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { props.load(it) }
    props.getProperty("BACKEND_BASE_URL") ?: "http://10.0.2.2:8000"
}

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
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
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
    debugImplementation(libs.androidx.ui.tooling)
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
