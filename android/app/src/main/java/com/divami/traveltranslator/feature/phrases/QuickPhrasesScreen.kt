package com.divami.traveltranslator.feature.phrases

import android.graphics.Color as AndroidColor
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.divami.traveltranslator.R
import com.divami.traveltranslator.designsystem.AppTheme
import com.divami.traveltranslator.designsystem.DesignTokens
import com.divami.traveltranslator.designsystem.components.SearchBar
import com.divami.traveltranslator.designsystem.minimumTouchTarget
import com.divami.traveltranslator.domain.model.Phrase
import com.divami.traveltranslator.domain.model.PhraseCategory

@Composable
fun QuickPhrasesScreen(
    targetLanguageCode: String,
    onPhraseSelect: (String) -> Unit,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    viewModel: PhrasesViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    var openCategory by rememberSaveable { mutableStateOf<String?>(null) }

    // Keep the phrasebook in sync with the active chat direction.
    LaunchedEffect(targetLanguageCode) {
        viewModel.setTargetLanguage(targetLanguageCode)
    }

    val handlePhrase: (String) -> Unit = { phrase ->
        viewModel.pushRecent(phrase)
        openCategory = null
        onPhraseSelect(phrase)
    }

    var query by rememberSaveable { mutableStateOf("") }

    AnimatedContent(
        targetState = openCategory,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "phrasesContent",
    ) { categorySlug ->
        if (categorySlug == null) {
            PhrasesList(
                state = state,
                contentPadding = contentPadding,
                query = query,
                onQueryChange = { query = it },
                onCategoryOpen = { openCategory = it.slug },
                onPhraseTap = handlePhrase,
                onFavoriteToggle = viewModel::toggleFavorite,
                onRetry = viewModel::refresh,
            )
        } else {
            val category = state.categories.firstOrNull { it.slug == categorySlug }
            PhraseCategoryScreen(
                category = category,
                phrases = category?.let { state.phrasesByCategory[it.slug] }.orEmpty(),
                loading = state.loading,
                error = state.error,
                favorites = state.favoritePhraseIds,
                contentPadding = contentPadding,
                onBack = { openCategory = null },
                onPhraseTap = handlePhrase,
                onFavoriteToggle = viewModel::toggleFavorite,
                onRetry = viewModel::refresh,
            )
        }
    }
}

@Composable
private fun PhrasesList(
    state: PhrasesUiState,
    contentPadding: PaddingValues,
    query: String,
    onQueryChange: (String) -> Unit,
    onCategoryOpen: (PhraseCategory) -> Unit,
    onPhraseTap: (String) -> Unit,
    onFavoriteToggle: (Phrase) -> Unit,
    onRetry: () -> Unit,
) {
    val colors = AppTheme.colors

    val allPhrases: List<Pair<PhraseCategory, Phrase>> = state.categories.flatMap { category ->
        state.phrasesByCategory[category.slug].orEmpty().map { category to it }
    }

    val searchResults = if (query.isBlank()) {
        emptyList()
    } else {
        allPhrases.filter { (category, phrase) ->
            phrase.sourceText.contains(query, ignoreCase = true) ||
                phrase.translation.contains(query, ignoreCase = true) ||
                category.name.contains(query, ignoreCase = true)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(top = contentPadding.calculateTopPadding()),
    ) {
        // Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.card)
                .padding(horizontal = DesignTokens.Spacing.S6)
                .padding(top = DesignTokens.Spacing.S6, bottom = DesignTokens.Spacing.S4),
            verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.S3),
        ) {
            Text(
                text = stringResource(R.string.phrases_title),
                color = colors.foreground,
                fontSize = DesignTokens.Typography.Size.Xl,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = stringResource(R.string.phrases_subtitle),
                color = colors.textSecondary,
                fontSize = DesignTokens.Typography.Size.Sm,
            )
            SearchBar(
                value = query,
                onValueChange = onQueryChange,
                placeholder = stringResource(R.string.phrases_search),
            )
        }
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(colors.border))

        if (query.isNotBlank()) {
            when {
                state.loading && allPhrases.isEmpty() -> LoadingBlock()
                state.error != null && allPhrases.isEmpty() -> ErrorBlock(state.error, onRetry)
                searchResults.isEmpty() -> Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(DesignTokens.Spacing.S10),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.phrases_no_match, query),
                        color = colors.textSecondary,
                        fontSize = DesignTokens.Typography.Size.Sm,
                        textAlign = TextAlign.Center,
                    )
                }
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = DesignTokens.Spacing.S6,
                        end = DesignTokens.Spacing.S6,
                        top = DesignTokens.Spacing.S4,
                        bottom = contentPadding.calculateBottomPadding() + DesignTokens.Spacing.S6,
                    ),
                    verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.S2),
                ) {
                    items(searchResults, key = { (_, phrase) -> "search-${phrase.id}" }) { (category, phrase) ->
                        SearchResultRow(
                            category = category,
                            phrase = phrase,
                            isFavorite = phrase.id in state.favoritePhraseIds,
                            onPhraseTap = { onPhraseTap(phrase.sourceText) },
                            onFavoriteToggle = { onFavoriteToggle(phrase) },
                        )
                    }
                }
            }
        } else {
            // Default mode: favorites + recent + categories
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = contentPadding.calculateBottomPadding() + DesignTokens.Spacing.S6),
            ) {
                if (state.loading && state.categories.isEmpty()) {
                    item("loading") { LoadingBlock() }
                } else if (state.error != null && state.categories.isEmpty()) {
                    item("error") { ErrorBlock(state.error, onRetry) }
                } else {
                    if (state.favoritePhrases.isNotEmpty()) {
                        item("favorites-header") {
                            SectionHeader(
                                title = stringResource(R.string.phrases_favorites),
                                icon = Icons.Filled.Star,
                                iconTint = colors.accent,
                            )
                        }
                        items(state.favoritePhrases, key = { "fav-${it.id}" }) { phrase ->
                            PhraseRow(
                                phraseText = phrase.sourceText,
                                translatedText = phrase.translation,
                                isFavorite = true,
                                onTap = { onPhraseTap(phrase.sourceText) },
                                onFavoriteToggle = { onFavoriteToggle(phrase) },
                                modifier = Modifier
                                    .padding(horizontal = DesignTokens.Spacing.S6)
                                    .padding(top = DesignTokens.Spacing.S2),
                            )
                        }
                        item("favorites-spacer") { Spacer16() }
                    }

                    if (state.recent.isNotEmpty()) {
                        item("recent-header") {
                            SectionHeader(
                                title = stringResource(R.string.phrases_recent),
                                icon = Icons.Outlined.AccessTime,
                                iconTint = colors.textSecondary,
                            )
                        }
                        items(state.recent, key = { "rec-$it" }) { text ->
                            val phrase = state.phraseBySourceText[text]
                            PhraseRow(
                                phraseText = text,
                                translatedText = phrase?.translation,
                                isFavorite = phrase != null && phrase.id in state.favoritePhraseIds,
                                canFavorite = phrase != null,
                                onTap = { onPhraseTap(text) },
                                onFavoriteToggle = { phrase?.let(onFavoriteToggle) },
                                modifier = Modifier
                                    .padding(horizontal = DesignTokens.Spacing.S6)
                                    .padding(top = DesignTokens.Spacing.S2),
                            )
                        }
                        item("recent-spacer") { Spacer16() }
                    }

                    item("categories-header") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = DesignTokens.Spacing.S6)
                                .padding(top = DesignTokens.Spacing.S6, bottom = DesignTokens.Spacing.S3),
                        ) {
                            Text(
                                text = stringResource(R.string.phrases_all_categories),
                                color = colors.textSecondary,
                                fontSize = DesignTokens.Typography.Size.Xs,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }

                    if (state.categories.isEmpty()) {
                        item("empty") {
                            Text(
                                text = stringResource(R.string.phrases_empty),
                                color = colors.textSecondary,
                                fontSize = DesignTokens.Typography.Size.Sm,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(DesignTokens.Spacing.S6),
                                textAlign = TextAlign.Center,
                            )
                        }
                    } else {
                        items(state.categories, key = { it.slug }) { category ->
                            CategoryRow(
                                category = category,
                                onClick = { onCategoryOpen(category) },
                                modifier = Modifier
                                    .padding(horizontal = DesignTokens.Spacing.S6)
                                    .padding(top = DesignTokens.Spacing.S3),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResultRow(
    category: PhraseCategory,
    phrase: Phrase,
    isFavorite: Boolean,
    onPhraseTap: () -> Unit,
    onFavoriteToggle: () -> Unit,
) {
    val (accent, background) = categoryColors(category)
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.S3),
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(DesignTokens.Radius.Md))
                .background(background),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = category.icon.ifBlank { "•" },
                color = accent,
                fontSize = 18.sp,
            )
        }
        PhraseRow(
            phraseText = phrase.sourceText,
            translatedText = phrase.translation,
            isFavorite = isFavorite,
            onTap = onPhraseTap,
            onFavoriteToggle = onFavoriteToggle,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun SectionHeader(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
) {
    val colors = AppTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = DesignTokens.Spacing.S6)
            .padding(top = DesignTokens.Spacing.S6, bottom = DesignTokens.Spacing.S3),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.S2),
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
        Text(
            text = title.uppercase(),
            color = colors.foreground,
            fontSize = DesignTokens.Typography.Size.Xs,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun Spacer16() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = DesignTokens.Spacing.S4),
    )
}

@Composable
private fun LoadingBlock() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(DesignTokens.Spacing.S10),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = AppTheme.colors.primary)
    }
}

@Composable
private fun ErrorBlock(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(DesignTokens.Spacing.S10),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.S3),
    ) {
        Text(
            text = message,
            color = AppTheme.colors.textSecondary,
            fontSize = DesignTokens.Typography.Size.Sm,
            textAlign = TextAlign.Center,
        )
        TextButton(onClick = onRetry) {
            Text(
                text = stringResource(R.string.common_retry),
                color = AppTheme.colors.primary,
                fontSize = DesignTokens.Typography.Size.Sm,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
fun PhraseRow(
    phraseText: String,
    isFavorite: Boolean,
    onTap: () -> Unit,
    onFavoriteToggle: () -> Unit,
    modifier: Modifier = Modifier,
    translatedText: String? = null,
    canFavorite: Boolean = true,
) {
    val colors = AppTheme.colors
    val shape = RoundedCornerShape(DesignTokens.Radius.Lg)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.card)
            .border(width = 1.dp, color = colors.borderStrong, shape = shape),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .heightIn(min = DesignTokens.TouchTarget.Min)
                .clickable(onClick = onTap)
                .semantics { role = Role.Button }
                .padding(horizontal = DesignTokens.Spacing.S5, vertical = DesignTokens.Spacing.S3),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = phraseText,
                color = colors.foreground,
                fontSize = DesignTokens.Typography.Size.Sm,
            )
            if (!translatedText.isNullOrBlank()) {
                Text(
                    text = translatedText,
                    color = colors.textSecondary,
                    fontSize = DesignTokens.Typography.Size.Xs,
                )
            }
        }
        if (canFavorite) {
            Box(
                modifier = Modifier
                    .minimumTouchTarget()
                    .size(DesignTokens.TouchTarget.Min)
                    .clickable(onClick = onFavoriteToggle)
                    .semantics { role = Role.Button },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Filled.Star else Icons.Outlined.StarOutline,
                    contentDescription = stringResource(if (isFavorite) R.string.phrases_unfavorite else R.string.phrases_favorite),
                    tint = if (isFavorite) colors.accent else colors.textSecondary,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
private fun CategoryRow(
    category: PhraseCategory,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AppTheme.colors
    val (accent, background) = categoryColors(category)
    val shape = RoundedCornerShape(DesignTokens.Radius.Lg)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.card)
            .border(width = 1.dp, color = colors.borderStrong, shape = shape)
            .minimumTouchTarget()
            .heightIn(min = DesignTokens.TouchTarget.Min)
            .clickable(onClick = onClick)
            .semantics { role = Role.Button }
            .padding(horizontal = DesignTokens.Spacing.S5, vertical = DesignTokens.Spacing.S4),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.S4),
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(DesignTokens.Radius.Lg))
                .background(background),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = category.icon.ifBlank { "•" },
                color = accent,
                fontSize = 24.sp,
            )
        }
        Text(
            text = category.name,
            color = colors.foreground,
            fontSize = DesignTokens.Typography.Size.Base,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f),
        )
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            contentDescription = null,
            tint = colors.textSecondary,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun PhraseCategoryScreen(
    category: PhraseCategory?,
    phrases: List<Phrase>,
    loading: Boolean,
    error: String?,
    favorites: Set<String>,
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onPhraseTap: (String) -> Unit,
    onFavoriteToggle: (Phrase) -> Unit,
    onRetry: () -> Unit,
) {
    val colors = AppTheme.colors
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(top = contentPadding.calculateTopPadding()),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.card)
                .padding(horizontal = DesignTokens.Spacing.S4, vertical = DesignTokens.Spacing.S3),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.S2),
        ) {
            Box(
                modifier = Modifier
                    .minimumTouchTarget()
                    .size(DesignTokens.TouchTarget.Min)
                    .clip(RoundedCornerShape(DesignTokens.Radius.Md))
                    .clickable(onClick = onBack)
                    .semantics { role = Role.Button },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = stringResource(R.string.common_back),
                    tint = colors.foreground,
                    modifier = Modifier.size(24.dp),
                )
            }
            Text(
                text = category?.name ?: stringResource(R.string.phrases_title),
                color = colors.foreground,
                fontSize = DesignTokens.Typography.Size.Xl,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .weight(1f)
                    .semantics { heading() },
            )
        }

        when {
            loading && phrases.isEmpty() -> LoadingBlock()
            error != null && phrases.isEmpty() -> ErrorBlock(error, onRetry)
            phrases.isEmpty() -> Text(
                text = stringResource(R.string.phrases_empty),
                color = colors.textSecondary,
                fontSize = DesignTokens.Typography.Size.Sm,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(DesignTokens.Spacing.S10),
                textAlign = TextAlign.Center,
            )
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.S3),
                contentPadding = PaddingValues(
                    start = DesignTokens.Spacing.S6,
                    end = DesignTokens.Spacing.S6,
                    top = DesignTokens.Spacing.S6,
                    bottom = contentPadding.calculateBottomPadding() + DesignTokens.Spacing.S6,
                ),
            ) {
                items(phrases, key = { it.id }) { phrase ->
                    PhraseRow(
                        phraseText = phrase.sourceText,
                        translatedText = phrase.translation,
                        isFavorite = phrase.id in favorites,
                        onTap = { onPhraseTap(phrase.sourceText) },
                        onFavoriteToggle = { onFavoriteToggle(phrase) },
                    )
                }
            }
        }
    }
}

@Composable
private fun categoryColors(category: PhraseCategory): Pair<Color, Color> {
    val fallback = AppTheme.colors.primary
    val accent = runCatching { Color(AndroidColor.parseColor(category.color)) }.getOrDefault(fallback)
    return accent to accent.copy(alpha = 0.14f)
}
