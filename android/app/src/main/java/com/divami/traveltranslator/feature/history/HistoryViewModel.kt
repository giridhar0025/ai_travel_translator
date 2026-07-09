package com.divami.traveltranslator.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.divami.traveltranslator.core.network.ApiResult
import com.divami.traveltranslator.data.repository.ConversationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/** A conversation as the History list renders it. */
data class HistoryItem(
    val id: String,
    val title: String,
    val timeLabel: String,
    val groupLabel: String,
    val messageCount: Int,
)

data class HistoryUiState(
    val items: List<HistoryItem> = emptyList(),
    val query: String = "",
    val loading: Boolean = true,
    val error: String? = null,
)

/**
 * Loads the user's conversation history from the backend, with debounced search
 * and optimistic delete. Grouping/time labels are derived here so the screen
 * stays purely presentational.
 */
@OptIn(FlowPreview::class)
@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val conversationRepository: ConversationRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(HistoryUiState())
    val state: StateFlow<HistoryUiState> = _state.asStateFlow()

    private val timeFormatter = DateTimeFormatter.ofPattern("h:mm a").withZone(ZoneId.systemDefault())
    private val dateFormatter = DateTimeFormatter.ofPattern("MMM d").withZone(ZoneId.systemDefault())

    init {
        // Reload whenever the (debounced) query changes, including the initial "".
        viewModelScope.launch {
            _state
                .map { it.query }
                .distinctUntilChanged()
                .debounce(300L)
                .collect { load(it) }
        }
    }

    fun onQueryChange(query: String) = _state.update { it.copy(query = query) }

    fun refresh() = load(_state.value.query)

    fun delete(id: String) {
        val snapshot = _state.value.items
        _state.update { it.copy(items = it.items.filterNot { c -> c.id == id }) }
        viewModelScope.launch {
            if (conversationRepository.delete(id) is ApiResult.Failure) {
                _state.update { it.copy(items = snapshot) } // revert on failure
            }
        }
    }

    private fun load(query: String) {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            when (val result = conversationRepository.list(query = query.ifBlank { null }, limit = 50)) {
                is ApiResult.Success -> _state.update {
                    it.copy(items = result.data.items.map(::toItem), loading = false)
                }
                is ApiResult.Failure -> _state.update {
                    it.copy(loading = false, error = result.error.message)
                }
            }
        }
    }

    private fun toItem(c: com.divami.traveltranslator.domain.model.Conversation) = HistoryItem(
        id = c.id,
        title = c.title?.takeIf { it.isNotBlank() } ?: "Conversation",
        timeLabel = timeFormatter.format(c.lastMessageAt),
        groupLabel = groupLabel(c.lastMessageAt),
        messageCount = c.messageCount,
    )

    private fun groupLabel(instant: Instant): String {
        val today = LocalDate.now()
        val date = instant.atZone(ZoneId.systemDefault()).toLocalDate()
        return when (date) {
            today -> "Today"
            today.minusDays(1) -> "Yesterday"
            else -> dateFormatter.format(instant)
        }
    }
}
