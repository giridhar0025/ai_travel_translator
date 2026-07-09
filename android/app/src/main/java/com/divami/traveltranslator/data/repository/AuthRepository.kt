package com.divami.traveltranslator.data.repository

import android.content.Context
import com.divami.traveltranslator.core.network.ApiError
import com.divami.traveltranslator.core.network.ApiResult
import com.divami.traveltranslator.core.network.safeApiCall
import com.divami.traveltranslator.data.auth.FirebaseAuthGateway
import com.divami.traveltranslator.data.auth.FirebaseNotConfiguredException
import com.divami.traveltranslator.data.local.Session
import com.divami.traveltranslator.data.local.SessionStore
import com.divami.traveltranslator.data.remote.TravelTranslatorApi
import com.divami.traveltranslator.data.remote.dto.AuthResponseDto
import com.divami.traveltranslator.data.remote.dto.VerifyTokenRequestDto
import com.divami.traveltranslator.data.remote.dto.toDomain
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Owns the authentication lifecycle: identity-provider sign-in (Firebase),
 * token exchange with the backend, and session persistence. Everything returns
 * an [ApiResult] so ViewModels never deal with exceptions.
 */
interface AuthRepository {
    val session: Flow<Session>
    val isFirebaseConfigured: Boolean

    suspend fun signInWithEmail(email: String, password: String): ApiResult<Unit>
    suspend fun signUpWithEmail(displayName: String?, email: String, password: String): ApiResult<Unit>
    suspend fun sendPasswordReset(email: String): ApiResult<Unit>
    suspend fun signInWithGoogle(context: Context): ApiResult<Unit>
    suspend fun continueAsGuest(): ApiResult<Unit>
    suspend fun deleteAccount(): ApiResult<Unit>
    suspend fun logout()
}

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val api: TravelTranslatorApi,
    private val gateway: FirebaseAuthGateway,
    private val sessionStore: SessionStore,
    private val json: Json,
) : AuthRepository {

    override val session: Flow<Session> = sessionStore.session
    override val isFirebaseConfigured: Boolean get() = gateway.isConfigured

    override suspend fun signInWithEmail(email: String, password: String): ApiResult<Unit> =
        withFirebaseToken { gateway.signInWithEmail(email, password) }

    override suspend fun signUpWithEmail(
        displayName: String?,
        email: String,
        password: String,
    ): ApiResult<Unit> =
        withFirebaseToken { gateway.signUpWithEmail(email, password, displayName) }

    override suspend fun signInWithGoogle(context: Context): ApiResult<Unit> =
        withFirebaseToken { gateway.signInWithGoogle(context) }

    override suspend fun sendPasswordReset(email: String): ApiResult<Unit> =
        runFirebase { gateway.sendPasswordReset(email) }

    override suspend fun continueAsGuest(): ApiResult<Unit> =
        safeApiCall(json) { api.guest() }.persist()

    override suspend fun deleteAccount(): ApiResult<Unit> {
        val result = safeApiCall(json) { api.deleteAccount() }
        if (result is ApiResult.Success) logout()
        return result
    }

    override suspend fun logout() {
        gateway.signOut()
        sessionStore.clearAuth()
    }

    // --- helpers -------------------------------------------------------------

    /** Firebase sign-in → backend `/auth/verify` → persist. */
    private suspend fun withFirebaseToken(getIdToken: suspend () -> String): ApiResult<Unit> {
        val tokenResult = runFirebaseForToken(getIdToken)
        val idToken = when (tokenResult) {
            is ApiResult.Success -> tokenResult.data
            is ApiResult.Failure -> return tokenResult
        }
        return safeApiCall(json) { api.verify(VerifyTokenRequestDto(idToken)) }.persist()
    }

    /** Save an [AuthResponseDto] into the session, collapsing to `Unit`. */
    private suspend fun ApiResult<AuthResponseDto>.persist(): ApiResult<Unit> = when (this) {
        is ApiResult.Success -> {
            sessionStore.saveAuth(data.token, data.user.toDomain())
            ApiResult.Success(Unit)
        }
        is ApiResult.Failure -> this
    }

    private suspend fun runFirebaseForToken(block: suspend () -> String): ApiResult<String> = try {
        ApiResult.Success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        ApiResult.Failure(e.toAuthError())
    }

    private suspend fun runFirebase(block: suspend () -> Unit): ApiResult<Unit> = try {
        block()
        ApiResult.Success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        ApiResult.Failure(e.toAuthError())
    }

    private fun Exception.toAuthError(): ApiError {
        val (kind, message) = when (this) {
            is FirebaseNotConfiguredException ->
                ApiError.Kind.Client to (message ?: "Sign-in isn't available.")
            is FirebaseAuthInvalidUserException ->
                ApiError.Kind.Client to "No account found for that email."
            is FirebaseAuthInvalidCredentialsException ->
                ApiError.Kind.Client to "Incorrect email or password."
            is FirebaseAuthWeakPasswordException ->
                ApiError.Kind.Client to "Password is too weak — use at least 6 characters."
            is FirebaseAuthUserCollisionException ->
                ApiError.Kind.Client to "An account with that email already exists."
            is FirebaseNetworkException ->
                ApiError.Kind.Network to "No internet connection. Check your network and retry."
            is GetCredentialCancellationException, is NoCredentialException ->
                ApiError.Kind.Client to "Google sign-in was cancelled."
            is GetCredentialException ->
                ApiError.Kind.Client to "Google sign-in failed. Please try again."
            else -> ApiError.Kind.Unknown to (message ?: "Sign-in failed.")
        }
        return ApiError(kind, message)
    }
}
