package com.divami.traveltranslator.feature.auth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.divami.traveltranslator.core.network.ApiResult
import com.divami.traveltranslator.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** One-shot navigation/side effects the auth screens react to. */
sealed interface AuthEffect {
    /** Sign-in/sign-up succeeded and a session is now stored. */
    data object Authenticated : AuthEffect

    /** A password-reset email was sent. */
    data object PasswordResetSent : AuthEffect
}

data class AuthUiState(
    val submitting: Boolean = false,
    val error: String? = null,
)

/**
 * Drives every auth screen (login, email login, sign-up, forgot-password).
 * Each screen gets its own instance for transient form state; the durable
 * session lives in [AuthRepository]. Actions run in [viewModelScope] so they
 * survive configuration changes, and outcomes are delivered as [AuthEffect]s.
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    private val _effects = Channel<AuthEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    val isFirebaseConfigured: Boolean get() = authRepository.isFirebaseConfigured

    fun signInWithEmail(email: String, password: String) =
        authenticate { authRepository.signInWithEmail(email, password) }

    fun signUpWithEmail(name: String, email: String, password: String) =
        authenticate { authRepository.signUpWithEmail(name.ifBlank { null }, email, password) }

    fun signInWithGoogle(context: Context) =
        authenticate { authRepository.signInWithGoogle(context) }

    fun continueAsGuest() =
        authenticate { authRepository.continueAsGuest() }

    fun sendPasswordReset(email: String) {
        viewModelScope.launch {
            _state.update { it.copy(submitting = true, error = null) }
            when (val result = authRepository.sendPasswordReset(email)) {
                is ApiResult.Success -> _effects.send(AuthEffect.PasswordResetSent)
                is ApiResult.Failure -> _state.update { it.copy(error = result.error.message) }
            }
            _state.update { it.copy(submitting = false) }
        }
    }

    fun clearError() = _state.update { it.copy(error = null) }

    /** Shared submit pipeline: toggles [AuthUiState.submitting], routes success to
     *  an [AuthEffect.Authenticated] effect and failure to an inline error. */
    private fun authenticate(action: suspend () -> ApiResult<Unit>) {
        viewModelScope.launch {
            _state.update { it.copy(submitting = true, error = null) }
            when (val result = action()) {
                is ApiResult.Success -> _effects.send(AuthEffect.Authenticated)
                is ApiResult.Failure -> _state.update { it.copy(error = result.error.message) }
            }
            _state.update { it.copy(submitting = false) }
        }
    }
}
