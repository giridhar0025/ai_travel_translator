package com.divami.traveltranslator.feature.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.divami.traveltranslator.R
import com.divami.traveltranslator.core.network.ApiResult
import com.divami.traveltranslator.data.local.OfflineTranslator
import com.divami.traveltranslator.data.local.SessionStore
import com.divami.traveltranslator.data.repository.SettingsRepository
import com.divami.traveltranslator.designsystem.components.AppLanguage
import com.divami.traveltranslator.domain.model.PlaybackSpeed
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val displayName: String = "Guest User",
    val email: String? = null,
    val isGuest: Boolean = true,
    val defaultLanguageCode: String = "en",
    val playbackSpeed: PlaybackSpeed = PlaybackSpeed.Normal,
    val offlineEnabled: Boolean = false,
    val downloadedPacks: Set<String> = emptySet(),
    val downloadingPacks: Set<String> = emptySet(),
    val packError: String? = null,
    val loading: Boolean = true,
) {
    val defaultLanguageName: String
        get() = AppLanguage.Supported.firstOrNull { it.code == defaultLanguageCode }?.displayName ?: "English"
}

/**
 * Backs the Settings screen: projects the current account, loads server-side
 * preferences, and pushes edits back via PATCH. Edits are optimistic — the UI
 * updates immediately and reverts on failure so it never lies about state.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val sessionStore: SessionStore,
    private val offlineTranslator: OfflineTranslator,
    @ApplicationContext private val appContext: Context,
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    init {
        observeAccount()
        observeOfflinePacks()
        refresh()
    }

    private fun observeOfflinePacks() {
        viewModelScope.launch {
            offlineTranslator.downloaded.collect { packs ->
                _state.update { it.copy(downloadedPacks = packs) }
            }
        }
        viewModelScope.launch {
            offlineTranslator.downloading.collect { packs ->
                _state.update { it.copy(downloadingPacks = packs) }
            }
        }
    }

    private fun observeAccount() {
        viewModelScope.launch {
            sessionStore.session.collect { session ->
                val account = session.account
                _state.update {
                    it.copy(
                        displayName = account?.displayName?.takeIf { n -> n.isNotBlank() }
                            ?: if (account?.isGuest == false) "Traveler" else "Guest User",
                        email = account?.email,
                        isGuest = account?.isGuest ?: true,
                    )
                }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            when (val result = settingsRepository.get()) {
                is ApiResult.Success -> _state.update {
                    it.copy(
                        defaultLanguageCode = result.data.defaultLanguage,
                        playbackSpeed = result.data.playbackSpeed,
                        offlineEnabled = result.data.offlineEnabled,
                        loading = false,
                    )
                }
                is ApiResult.Failure -> _state.update { it.copy(loading = false) }
            }
        }
    }

    fun setDefaultLanguage(code: String) {
        val previous = _state.value.defaultLanguageCode
        _state.update { it.copy(defaultLanguageCode = code) }
        // Mirror locally so the chat picks up the change (SessionStore is the
        // source of truth the ChatViewModel reads).
        viewModelScope.launch { sessionStore.setDefaultLanguage(code) }
        patch(revert = { _state.update { it.copy(defaultLanguageCode = previous) } }) {
            settingsRepository.update(defaultLanguage = code)
        }
    }

    fun setPlaybackSpeed(speed: PlaybackSpeed) {
        val previous = _state.value.playbackSpeed
        _state.update { it.copy(playbackSpeed = speed) }
        patch(revert = { _state.update { it.copy(playbackSpeed = previous) } }) {
            settingsRepository.update(playbackSpeed = speed)
        }
    }

    fun setOffline(enabled: Boolean) {
        val previous = _state.value.offlineEnabled
        _state.update { it.copy(offlineEnabled = enabled) }
        patch(revert = { _state.update { it.copy(offlineEnabled = previous) } }) {
            settingsRepository.update(offlineEnabled = enabled)
        }
    }

    fun downloadPack(languageCode: String) {
        viewModelScope.launch {
            _state.update { it.copy(packError = null) }
            val ready = offlineTranslator.download(languageCode)
            if (!ready) {
                _state.update { it.copy(packError = appContext.getString(R.string.settings_offline_failed)) }
            }
        }
    }

    fun deletePack(languageCode: String) {
        viewModelScope.launch {
            offlineTranslator.delete(languageCode)
        }
    }

    fun clearPackError() = _state.update { it.copy(packError = null) }

    private fun patch(revert: () -> Unit, action: suspend () -> ApiResult<*>) {
        viewModelScope.launch {
            if (action() is ApiResult.Failure) revert()
        }
    }
}
