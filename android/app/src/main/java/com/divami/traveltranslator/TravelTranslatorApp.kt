package com.divami.traveltranslator

import android.app.Application
import android.util.Log
import com.divami.traveltranslator.core.firebase.FirebaseInitializer
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Application entry point. Hilt owns the object graph; Firebase is initialised
 * here (rather than via the google-services Gradle plugin) so the project
 * builds without a checked-in `google-services.json`. See [FirebaseInitializer].
 */
@HiltAndroidApp
class TravelTranslatorApp : Application() {

    @Inject lateinit var firebaseInitializer: FirebaseInitializer

    override fun onCreate() {
        super.onCreate()
        val initialised = firebaseInitializer.initialize(this)
        if (!initialised) {
            Log.i(
                TAG,
                "Firebase not configured (blank FIREBASE_* BuildConfig). " +
                    "Email/Google sign-in disabled; guest mode still available.",
            )
        }
    }

    private companion object {
        const val TAG = "TravelTranslatorApp"
    }
}
