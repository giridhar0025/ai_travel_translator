package com.divami.traveltranslator.feature.phrases

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.divami.traveltranslator.core.network.ApiResult
import com.divami.traveltranslator.data.repository.FavoriteRepository
import com.divami.traveltranslator.data.repository.PhraseRepository
import com.divami.traveltranslator.domain.model.Favorite
import com.divami.traveltranslator.domain.model.Phrase
import com.divami.traveltranslator.domain.model.PhraseCategory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

private const val RecentCap = 5

/** The phrase API is English-source today, so English cannot be a target. */
private const val FallbackTargetLanguage = "hi"

data class PhrasesUiState(
    val categories: List<PhraseCategory> = emptyList(),
    val phrasesByCategory: Map<String, List<Phrase>> = emptyMap(),
    val favorites: List<Favorite> = emptyList(),
    val recent: List<String> = emptyList(),
    val targetLanguage: String = FallbackTargetLanguage,
    val loading: Boolean = true,
    val error: String? = null,
) {
    val favoritePhraseIds: Set<String>
        get() = favorites.map { it.refId }.toSet()

    val allPhrases: List<Phrase>
        get() = phrasesByCategory.values.flatten()

    /** Favorites that belong to the currently loaded target language. */
    val favoritePhrases: List<Phrase>
        get() {
            val byId = allPhrases.associateBy { it.id }
            return favorites.mapNotNull { byId[it.refId] }
        }

    val phraseBySourceText: Map<String, Phrase>
        get() = allPhrases.associateBy { it.sourceText }
}

/**
 * Backs the Quick Phrases screen entirely from the API:
 *
 * - `/phrases/categories` supplies the category list
 * - `/phrases?category=…&target=…` supplies source text + cached translation
 * - `/favorites?kind=phrase` supplies the user's saved phrase ids
 * - `/recents` supplies the recently used phrase list
 *
 * No phrase content is baked into the APK anymore.
 */
@HiltViewModel
class PhrasesViewModel @Inject constructor(
    private val phraseRepository: PhraseRepository,
    private val favoriteRepository: FavoriteRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(PhrasesUiState())
    val state: StateFlow<PhrasesUiState> = _state.asStateFlow()

    private var loadJob: Job? = null

    init {
        loadRecents()
    }

    /**
     * Loads the phrasebook for the chat's active target language. If the chat
     * target is English, fall back to Hindi because the seeded phrase catalog
     * is English-source and same-language translations are rejected by the API.
     */
    fun setTargetLanguage(code: String) {
        if (code.isBlank()) return
        val target = if (code == "en") FallbackTargetLanguage else code
        if (target == _state.value.targetLanguage && _state.value.categories.isNotEmpty()) return

        loadJob?.cancel()
        loadJob = viewModelScope.launch { loadCatalog(target) }
    }

    /** Retry after a network/API failure. */
    fun refresh() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch { loadCatalog(_state.value.targetLanguage) }
        loadRecents()
    }

    fun loadRecents() {
        viewModelScope.launch {
            when (val result = favoriteRepository.recents(limit = RecentCap)) {
                is ApiResult.Success -> _state.update { current ->
                    current.copy(
                        recent = result.data.map { it.sourceText }.distinct().take(RecentCap),
                    )
                }
                is ApiResult.Failure -> Unit // Recents are non-critical.
            }
        }
    }

    fun toggleFavorite(phrase: Phrase) {
        val existing = _state.value.favorites.firstOrNull { it.refId == phrase.id }

        if (existing == null) {
            val optimistic = Favorite(
                id = "pending-${phrase.id}",
                kind = "phrase",
                refId = phrase.id,
                sourceLang = phrase.sourceLang,
                targetLang = phrase.targetLang,
                createdAt = Instant.EPOCH,
            )
            _state.update { it.copy(favorites = it.favorites + optimistic, error = null) }
            viewModelScope.launch {
                when (val result = favoriteRepository.addFavorite(
                    kind = "phrase",
                    refId = phrase.id,
                    sourceLang = phrase.sourceLang,
                    targetLang = phrase.targetLang,
                )) {
                    is ApiResult.Success -> _state.update { current ->
                        current.copy(
                            favorites = current.favorites.map {
                                if (it.id == optimistic.id) result.data else it
                            },
                        )
                    }
                    is ApiResult.Failure -> _state.update { current ->
                        current.copy(
                            favorites = current.favorites.filterNot { it.id == optimistic.id },
                            error = result.error.message,
                        )
                    }
                }
            }
        } else {
            _state.update { it.copy(favorites = it.favorites.filterNot { f -> f.id == existing.id }, error = null) }
            viewModelScope.launch {
                when (val result = favoriteRepository.removeFavorite(existing.id)) {
                    is ApiResult.Success -> Unit
                    is ApiResult.Failure -> _state.update { current ->
                        current.copy(
                            favorites = current.favorites + existing,
                            error = result.error.message,
                        )
                    }
                }
            }
        }
    }

    /** Optimistically surface a just-used phrase; the backend records it after translation. */
    fun pushRecent(phrase: String) {
        _state.update {
            val deduped = listOf(phrase) + it.recent.filterNot { existing -> existing == phrase }
            it.copy(recent = deduped.take(RecentCap))
        }
    }

    fun dismissError() = _state.update { it.copy(error = null) }

    private suspend fun loadCatalog(target: String) {
        val targetChanged = target != _state.value.targetLanguage
        _state.update {
            it.copy(
                targetLanguage = target,
                loading = true,
                error = null,
                // Never show translations/favorites from the previous direction.
                phrasesByCategory = if (targetChanged) emptyMap() else it.phrasesByCategory,
                favorites = if (targetChanged) emptyList() else it.favorites,
            )
        }

        val categoriesResult = phraseRepository.categories()
        if (categoriesResult is ApiResult.Failure) {
            _state.update { it.copy(loading = false, error = categoriesResult.error.message) }
            return
        }
        val categories = (categoriesResult as ApiResult.Success).data.sortedBy { it.sortOrder }
        // Show the category chrome immediately; phrases fill in below.
        _state.update { it.copy(categories = categories) }

        // Fetch each category in parallel; one failed category should not hide
        // the others. The backend caches generated translations after the first
        // request, so warm loads are cheap.
        val rendered = coroutineScope {
            categories.map { category ->
                async { category.slug to phraseRepository.phrases(category.slug, target) }
            }.awaitAll()
        }

        val phrasesByCategory = linkedMapOf<String, List<Phrase>>()
        var firstError: String? = null
        for ((slug, result) in rendered) {
            when (result) {
                is ApiResult.Success -> phrasesByCategory[slug] = result.data.sortedBy { it.sortOrder }
                is ApiResult.Failure -> if (firstError == null) firstError = result.error.message
            }
        }

        val favorites = when (val result = favoriteRepository.favorites(kind = "phrase")) {
            is ApiResult.Success -> result.data
            is ApiResult.Failure -> {
                if (firstError == null) firstError = result.error.message
                emptyList()
            }
        }

        _state.update {
            it.copy(
                categories = categories,
                phrasesByCategory = phrasesByCategory,
                favorites = favorites,
                loading = false,
                error = firstError,
            )
        }
    }
}
