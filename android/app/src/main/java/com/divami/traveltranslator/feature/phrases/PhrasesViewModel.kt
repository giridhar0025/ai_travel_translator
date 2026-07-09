package com.divami.traveltranslator.feature.phrases

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.divami.traveltranslator.core.network.ApiResult
import com.divami.traveltranslator.data.repository.FavoriteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val RecentCap = 5

data class PhrasesUiState(
    val favorites: Set<String> = emptySet(),
    val recent: List<String> = emptyList(),
)

/**
 * Backs the Quick Phrases screen. The curated category catalog is a fixed
 * client-side list (its Material icons/colors), but the **Recent** section is
 * real: it's seeded from the backend `/recents` history (which the translate
 * flow auto-populates) and updated optimistically as phrases are tapped.
 */
@HiltViewModel
class PhrasesViewModel @Inject constructor(
    private val favoriteRepository: FavoriteRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(PhrasesUiState())
    val state: StateFlow<PhrasesUiState> = _state.asStateFlow()

    init {
        loadRecents()
    }

    fun loadRecents() {
        viewModelScope.launch {
            when (val result = favoriteRepository.recents(limit = RecentCap)) {
                is ApiResult.Success -> _state.update { current ->
                    // Show the original phrases the user translated, newest first.
                    current.copy(recent = result.data.map { it.sourceText }.distinct().take(RecentCap))
                }
                is ApiResult.Failure -> Unit // Keep whatever we have; recents are non-critical.
            }
        }
    }

    fun toggleFavorite(phrase: String) {
        _state.update {
            val next = it.favorites.toMutableSet().apply { if (!add(phrase)) remove(phrase) }
            it.copy(favorites = next)
        }
    }

    /** Optimistically surface a just-used phrase; the backend record is created
     *  once the phrase is actually translated in Chat. */
    fun pushRecent(phrase: String) {
        _state.update {
            val deduped = listOf(phrase) + it.recent.filterNot { existing -> existing == phrase }
            it.copy(recent = deduped.take(RecentCap))
        }
    }
}
