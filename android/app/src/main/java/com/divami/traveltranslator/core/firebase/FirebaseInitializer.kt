package com.divami.traveltranslator.core.firebase

import android.content.Context
import com.divami.traveltranslator.BuildConfig
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Initialises the default [FirebaseApp] from `BuildConfig.FIREBASE_*` values
 * instead of a `google-services.json` resource. This keeps the build green on a
 * fresh checkout with no Firebase credentials — [isConfigured] then reports
 * `false` and callers fall back to guest auth.
 *
 * Single responsibility: turn build config into (at most) one initialised
 * FirebaseApp. It does not know about auth, tokens, or UI.
 */
@Singleton
class FirebaseInitializer @Inject constructor() {

    /** True once [initialize] has run with non-blank credentials. */
    @Volatile
    var isConfigured: Boolean = false
        private set

    /**
     * Idempotently initialises Firebase. Returns `true` if a real project is
     * configured and the default app is now available, `false` otherwise.
     */
    fun initialize(context: Context): Boolean {
        if (isConfigured) return true
        if (!hasCredentials()) return false
        if (FirebaseApp.getApps(context).isNotEmpty()) {
            isConfigured = true
            return true
        }
        val options = FirebaseOptions.Builder()
            .setApiKey(BuildConfig.FIREBASE_API_KEY)
            .setApplicationId(BuildConfig.FIREBASE_APP_ID)
            .setProjectId(BuildConfig.FIREBASE_PROJECT_ID)
            .build()
        FirebaseApp.initializeApp(context, options)
        isConfigured = true
        return true
    }

    private fun hasCredentials(): Boolean =
        BuildConfig.FIREBASE_API_KEY.isNotBlank() &&
            BuildConfig.FIREBASE_APP_ID.isNotBlank() &&
            BuildConfig.FIREBASE_PROJECT_ID.isNotBlank()
}
