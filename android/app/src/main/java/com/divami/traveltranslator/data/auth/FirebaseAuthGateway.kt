package com.divami.traveltranslator.data.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.divami.traveltranslator.BuildConfig
import com.divami.traveltranslator.core.firebase.FirebaseInitializer
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/** Raised when a Firebase-backed action is attempted without configuration. */
class FirebaseNotConfiguredException :
    Exception("Sign-in isn't configured in this build. Continue as guest instead.")

/**
 * Thin wrapper over the Firebase Auth + Google Credential Manager SDKs. Its only
 * job is to authenticate the user with the identity provider and return a fresh
 * **Firebase ID token** — the token the backend `/auth/verify` endpoint
 * exchanges for an app JWT. It knows nothing about the backend or app session.
 */
interface FirebaseAuthGateway {
    val isConfigured: Boolean

    suspend fun signInWithEmail(email: String, password: String): String
    suspend fun signUpWithEmail(email: String, password: String, displayName: String?): String
    suspend fun sendPasswordReset(email: String)
    suspend fun signInWithGoogle(context: Context): String
    fun signOut()
}

@Singleton
class FirebaseAuthGatewayImpl @Inject constructor(
    private val initializer: FirebaseInitializer,
) : FirebaseAuthGateway {

    override val isConfigured: Boolean get() = initializer.isConfigured

    private val auth: FirebaseAuth
        get() {
            if (!initializer.isConfigured) throw FirebaseNotConfiguredException()
            return FirebaseAuth.getInstance()
        }

    override suspend fun signInWithEmail(email: String, password: String): String {
        val result = auth.signInWithEmailAndPassword(email.trim(), password).await()
        return result.user.freshIdToken()
    }

    override suspend fun signUpWithEmail(email: String, password: String, displayName: String?): String {
        val result = auth.createUserWithEmailAndPassword(email.trim(), password).await()
        if (!displayName.isNullOrBlank()) {
            result.user?.updateProfile(
                UserProfileChangeRequest.Builder().setDisplayName(displayName.trim()).build(),
            )?.await()
        }
        return result.user.freshIdToken()
    }

    override suspend fun sendPasswordReset(email: String) {
        auth.sendPasswordResetEmail(email.trim()).await()
    }

    override suspend fun signInWithGoogle(context: Context): String {
        if (!initializer.isConfigured || BuildConfig.FIREBASE_WEB_CLIENT_ID.isBlank()) {
            throw FirebaseNotConfiguredException()
        }
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(BuildConfig.FIREBASE_WEB_CLIENT_ID)
            .setAutoSelectEnabled(false)
            .build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        val response = CredentialManager.create(context).getCredential(context, request)
        val googleIdToken = GoogleIdTokenCredential.createFrom(response.credential.data).idToken
        val firebaseCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
        val result = auth.signInWithCredential(firebaseCredential).await()
        return result.user.freshIdToken()
    }

    override fun signOut() {
        if (initializer.isConfigured) FirebaseAuth.getInstance().signOut()
    }

    private suspend fun com.google.firebase.auth.FirebaseUser?.freshIdToken(): String {
        val user = this ?: error("Firebase returned no user")
        return user.getIdToken(false).await().token
            ?: error("Firebase returned no ID token")
    }
}
