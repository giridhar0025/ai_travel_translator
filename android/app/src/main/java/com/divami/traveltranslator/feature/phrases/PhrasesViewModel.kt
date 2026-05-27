package com.divami.traveltranslator.feature.phrases

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

private const val RecentCap = 5

data class PhrasesUiState(
    val favorites: Set<String> = emptySet(),
    val recent: List<String> = emptyList(),
)

class PhrasesViewModel : ViewModel() {
    private val _state = MutableStateFlow(PhrasesUiState())
    val state: StateFlow<PhrasesUiState> = _state.asStateFlow()

    fun toggleFavorite(phrase: String) {
        _state.update {
            val next = it.favorites.toMutableSet().apply {
                if (!add(phrase)) remove(phrase)
            }
            it.copy(favorites = next)
        }
    }

    fun pushRecent(phrase: String) {
        _state.update {
            val deduped = listOf(phrase) + it.recent.filterNot { existing -> existing == phrase }
            it.copy(recent = deduped.take(RecentCap))
        }
    }
}
