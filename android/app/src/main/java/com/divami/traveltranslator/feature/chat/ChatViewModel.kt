package com.divami.traveltranslator.feature.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.divami.traveltranslator.core.network.ApiResult
import com.divami.traveltranslator.data.local.SessionStore
import com.divami.traveltranslator.data.repository.ConversationRepository
import com.divami.traveltranslator.data.repository.TranslationRepository
import com.divami.traveltranslator.designsystem.components.AppLanguage
import com.divami.traveltranslator.domain.model.MessageRole
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val input: String = "",
    val sourceLanguage: AppLanguage = AppLanguage.English,
    val targetLanguage: AppLanguage = AppLanguage.Hindi,
    val contextMode: ContextMode = ContextMode.Casual,
    val flippedIds: Set<String> = emptySet(),
    val isSending: Boolean = false,
    val error: String? = null,
)

/**
 * Owns chat state and drives real translations through [TranslationRepository].
 * Each sent message is translated by the backend and, best-effort, persisted to
 * the user's conversation via [ConversationRepository] so it appears in History.
 */
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val translationRepository: TranslationRepository,
    private val conversationRepository: ConversationRepository,
    private val sessionStore: SessionStore,
) : ViewModel() {

    private val _state = MutableStateFlow(ChatUiState())
    val state: StateFlow<ChatUiState> = _state.asStateFlow()

    /** Lazily-created conversation id; persistence is a nice-to-have, never blocks translation. */
    private var conversationId: String? = null

    /**
     * The language spoken by "me" (this side of the conversation). Anchored to the
     * source language of the first message in a conversation. Messages spoken in
     * this language render on the right (You); the other language renders on the
     * left (OtherPerson) — so swapping the direction flips the bubble's side.
     */
    private var homeLanguageCode: String? = null

    init {
        // Honor the language chosen during onboarding / in Settings so the chat
        // opens with the user's preferred target language rather than the default.
        viewModelScope.launch {
            sessionStore.currentSession().defaultLanguage?.let(::applyPreferredLanguage)
        }
    }

    fun onInputChange(text: String) = _state.update { it.copy(input = text) }
    fun onContextChange(mode: ContextMode) = _state.update { it.copy(contextMode = mode) }
    fun onSourceLanguageChange(lang: AppLanguage) = _state.update { it.copy(sourceLanguage = lang) }
    fun onTargetLanguageChange(lang: AppLanguage) = _state.update { it.copy(targetLanguage = lang) }
    fun swapLanguages() = _state.update {
        it.copy(sourceLanguage = it.targetLanguage, targetLanguage = it.sourceLanguage)
    }
    fun dismissError() = _state.update { it.copy(error = null) }

    /** Clears the current conversation and starts fresh, keeping the selected
     *  languages. The next message re-anchors which side is "me". */
    fun newConversation() {
        conversationId = null
        homeLanguageCode = null
        _state.update {
            it.copy(
                messages = emptyList(),
                flippedIds = emptySet(),
                input = "",
                error = null,
                isSending = false,
            )
        }
    }

    fun toggleFlip(messageId: String) = _state.update {
        val next = it.flippedIds.toMutableSet().apply { if (!add(messageId)) remove(messageId) }
        it.copy(flippedIds = next)
    }

    /** Sets the target language from a language code (e.g. from onboarding), before any chatting. */
    fun applyPreferredLanguage(code: String) {
        AppLanguage.Supported.firstOrNull { it.code == code }?.let { lang ->
            _state.update { if (it.messages.isEmpty()) it.copy(targetLanguage = lang) else it }
        }
    }

    fun onSend() {
        val text = _state.value.input.trim()
        if (text.isEmpty() || _state.value.isSending) return
        val src = _state.value.sourceLanguage
        val tgt = _state.value.targetLanguage
        val contextHint = _state.value.contextMode.label

        // Anchor "my" language on the first message, then place each message by
        // its source language so swapping the direction flips it to the other side.
        if (homeLanguageCode == null) homeLanguageCode = src.code
        val sender = if (src.code == homeLanguageCode) Sender.You else Sender.OtherPerson

        _state.update { it.copy(input = "", isSending = true, error = null) }

        viewModelScope.launch {
            when (val result = translationRepository.translate(src.code, tgt.code, text, contextHint)) {
                is ApiResult.Success -> {
                    val message = ChatMessage(
                        id = "msg-${text.hashCode()}-${_state.value.messages.size}",
                        sender = sender,
                        originalText = text,
                        originalLanguage = src,
                        translatedText = result.data.translation,
                        translatedLanguage = tgt,
                        pronunciation = result.data.pronunciation,
                    )
                    _state.update { it.copy(messages = it.messages + message, isSending = false) }
                    persist(text, src, tgt, result.data.translation, result.data.pronunciation)
                }
                is ApiResult.Failure -> {
                    // Restore the text so the user doesn't lose it — but not if
                    // they've already started typing something new.
                    _state.update {
                        it.copy(
                            input = if (it.input.isBlank()) text else it.input,
                            isSending = false,
                            error = result.error.message,
                        )
                    }
                }
            }
        }
    }

    /** Best-effort persistence — failures are swallowed so History lag never breaks chat. */
    private suspend fun persist(
        text: String,
        src: AppLanguage,
        tgt: AppLanguage,
        translation: String,
        pronunciation: String?,
    ) {
        val id = ensureConversation() ?: return
        conversationRepository.appendMessage(
            conversationId = id,
            role = MessageRole.User,
            sourceText = text,
            sourceLang = src.code,
            translatedText = translation,
            targetLang = tgt.code,
            pronunciation = pronunciation,
        )
    }

    private suspend fun ensureConversation(): String? {
        conversationId?.let { return it }
        return when (val created = conversationRepository.create()) {
            is ApiResult.Success -> created.data.id.also { conversationId = it }
            is ApiResult.Failure -> null
        }
    }
}
