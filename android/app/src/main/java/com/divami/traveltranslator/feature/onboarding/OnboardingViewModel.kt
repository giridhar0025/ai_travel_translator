package com.divami.traveltranslator.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.divami.traveltranslator.data.local.SessionStore
import com.divami.traveltranslator.data.repository.SettingsRepository
import com.divami.traveltranslator.designsystem.components.AppLanguage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Persists the onboarding outcome: stores the chosen default language on the
 * backend (best-effort) and flips the local `hasOnboarded` flag so the user
 * skips onboarding on the next launch.
 */
@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val sessionStore: SessionStore,
) : ViewModel() {

    /** Maps an onboarding display name (e.g. "Hindi") to its backend code. */
    fun complete(languageDisplayName: String, onDone: () -> Unit) {
        val code = AppLanguage.Supported
            .firstOrNull { it.displayName == languageDisplayName }?.code ?: DEFAULT_CODE
        viewModelScope.launch {
            // Persist the choice locally and flip the onboarding flag, then
            // navigate immediately. These are fast local writes, so the user
            // never sits on a frozen screen waiting for the network.
            sessionStore.setDefaultLanguage(code)
            sessionStore.setHasOnboarded(true)
            // Best-effort backend sync — must not block navigation. Failures
            // (guest offline, etc.) are ignored; the local value is what the
            // app reads.
            launch { runCatching { settingsRepository.update(defaultLanguage = code) } }
            onDone()
        }
    }

    private companion object {
        const val DEFAULT_CODE = "en"
    }
}
