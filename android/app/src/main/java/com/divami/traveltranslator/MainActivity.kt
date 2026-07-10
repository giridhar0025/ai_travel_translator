package com.divami.traveltranslator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.divami.traveltranslator.designsystem.TravelTranslatorTheme
import com.divami.traveltranslator.nav.AppNavHost
import dagger.hilt.android.AndroidEntryPoint

/** Whether the soft keyboard is currently visible. Read once at the root (above
 *  imePadding, so there's no recomposition feedback) and provided to the tree so
 *  screens can react (e.g. hide the bottom nav bar while typing). */
val LocalImeVisible = staticCompositionLocalOf { false }

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalLayoutApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            // Android 15 forces edge-to-edge and never resizes the window for the
            // keyboard. Lift the whole app above the keyboard with a single root-level
            // imePadding (consistent, no per-screen double-count), and surface IME
            // visibility to the tree so the bottom nav can hide while typing.
            val imeVisible = WindowInsets.isImeVisible
            Box(Modifier.fillMaxSize().imePadding()) {
                CompositionLocalProvider(LocalImeVisible provides imeVisible) {
                    TravelTranslatorTheme { AppNavHost() }
                }
            }
        }
    }
}
