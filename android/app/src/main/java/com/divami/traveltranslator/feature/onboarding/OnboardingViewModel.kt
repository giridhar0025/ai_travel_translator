package com.divami.traveltranslator.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.divami.traveltranslator.data.local.SessionStore
import com.divami.traveltranslator.data.repository.SettingsRepository
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
        val code = LANGUAGE_CODES[languageDisplayName] ?: DEFAULT_CODE
        viewModelScope.launch {
            // Persist the preference; ignore failures (guest offline, etc.) — the
            // onboarding flag below is what gates the flow.
            settingsRepository.update(defaultLanguage = code)
            sessionStore.setHasOnboarded(true)
            onDone()
        }
    }

    private companion object {
        const val DEFAULT_CODE = "en"
        val LANGUAGE_CODES = mapOf(
            "English" to "en",
            "Hindi" to "hi",
            "Telugu" to "te",
            "Thai" to "th",
        )
    }
}
