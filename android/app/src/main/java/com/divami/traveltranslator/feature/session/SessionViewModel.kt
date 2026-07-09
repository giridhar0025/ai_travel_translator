package com.divami.traveltranslator.feature.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.divami.traveltranslator.data.local.Session
import com.divami.traveltranslator.data.local.SessionStore
import com.divami.traveltranslator.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Where the app should start once the persisted session is known. */
enum class StartRoute { Welcome, Login, Onboarding, Home }

sealed interface SessionUiState {
    data object Loading : SessionUiState
    data class Ready(val start: StartRoute) : SessionUiState
}

/**
 * App-level session state used to pick the start destination and to log out.
 * The durable session lives in [SessionStore]; this just projects it into a
 * routing decision the nav host can act on once (avoiding a Login flash for
 * already-authenticated users).
 */
@HiltViewModel
class SessionViewModel @Inject constructor(
    private val sessionStore: SessionStore,
    private val authRepository: AuthRepository,
) : ViewModel() {

    val uiState: StateFlow<SessionUiState> = sessionStore.session
        .map { SessionUiState.Ready(it.toStartRoute()) as SessionUiState }
        .stateIn(viewModelScope, SharingStarted.Eagerly, SessionUiState.Loading)

    fun markWelcomeSeen() = viewModelScope.launch { sessionStore.setHasSeenWelcome(true) }

    fun logout() = viewModelScope.launch { authRepository.logout() }

    private fun Session.toStartRoute(): StartRoute = when {
        isAuthenticated && hasOnboarded -> StartRoute.Home
        isAuthenticated -> StartRoute.Onboarding
        hasSeenWelcome -> StartRoute.Login
        else -> StartRoute.Welcome
    }
}
