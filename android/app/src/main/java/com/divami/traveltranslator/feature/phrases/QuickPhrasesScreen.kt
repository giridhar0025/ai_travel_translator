package com.divami.traveltranslator.feature.phrases

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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.divami.traveltranslator.designsystem.AppTheme
import com.divami.traveltranslator.designsystem.DesignTokens
import com.divami.traveltranslator.designsystem.components.SearchBar

@Composable
fun QuickPhrasesScreen(
    onPhraseSelect: (String) -> Unit,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    viewModel: PhrasesViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()
    var openCategory by rememberSaveable { mutableStateOf<String?>(null) }

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
    ) { categoryId ->
        if (categoryId == null) {
            PhrasesList(
                state = state,
                contentPadding = contentPadding,
                query = query,
                onQueryChange = { query = it },
                onCategoryOpen = { openCategory = it.id },
                onPhraseTap = handlePhrase,
                onFavoriteToggle = viewModel::toggleFavorite,
            )
        } else {
            val category = PhraseCategory.entries.first { it.id == categoryId }
            PhraseCategoryScreen(
                category = category,
                favorites = state.favorites,
                contentPadding = contentPadding,
                onBack = { openCategory = null },
                onPhraseTap = handlePhrase,
                onFavoriteToggle = viewModel::toggleFavorite,
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
    onFavoriteToggle: (String) -> Unit,
) {
    val colors = AppTheme.colors

    // All phrases paired with their category, used for search
    val allPhrases: List<Pair<PhraseCategory, String>> = PhraseCategory.entries
        .flatMap { cat -> cat.phrases.map { cat to it } }

    val searchResults = if (query.isBlank()) emptyList()
    else allPhrases.filter { (_, phrase) -> phrase.contains(query, ignoreCase = true) }

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
                text = "Quick Phrases",
                color = colors.foreground,
                fontSize = DesignTokens.Typography.Size.Xl,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "Common phrases for travelers",
                color = colors.textSecondary,
                fontSize = DesignTokens.Typography.Size.Sm,
            )
            SearchBar(
                value = query,
                onValueChange = onQueryChange,
                placeholder = "Search phrases…",
            )
        }

        if (query.isNotBlank()) {
            // Search mode: flat list of matched phrases or empty state
            if (searchResults.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(DesignTokens.Spacing.S10),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "No phrases match \"$query\"",
                        color = colors.textSecondary,
                        fontSize = DesignTokens.Typography.Size.Sm,
                        textAlign = TextAlign.Center,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = DesignTokens.Spacing.S6,
                        end = DesignTokens.Spacing.S6,
                        top = DesignTokens.Spacing.S4,
                        bottom = contentPadding.calculateBottomPadding() + DesignTokens.Spacing.S6,
                    ),
                    verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.S2),
                ) {
                    items(searchResults, key = { (cat, phrase) -> "search-${cat.id}-$phrase" }) { (cat, phrase) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.S3),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(DesignTokens.Radius.Md))
                                    .background(cat.colors.bg),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = cat.icon,
                                    contentDescription = null,
                                    tint = cat.colors.fg,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                            PhraseRow(
                                phrase = phrase,
                                isFavorite = state.favorites.contains(phrase),
                                onTap = { onPhraseTap(phrase) },
                                onFavoriteToggle = { onFavoriteToggle(phrase) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        } else {
            // Default mode: favorites + recent + categories
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = contentPadding.calculateBottomPadding() + DesignTokens.Spacing.S6),
            ) {
                if (state.favorites.isNotEmpty()) {
                    item("favorites-header") {
                        SectionHeader(
                            title = "Favorites",
                            icon = Icons.Filled.Star,
                            iconTint = colors.accent,
                        )
                    }
                    items(state.favorites.toList(), key = { "fav-$it" }) { phrase ->
                        PhraseRow(
                            phrase = phrase,
                            isFavorite = true,
                            onTap = { onPhraseTap(phrase) },
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
                            title = "Recent",
                            icon = Icons.Outlined.AccessTime,
                            iconTint = colors.textSecondary,
                        )
                    }
                    items(state.recent, key = { "rec-$it" }) { phrase ->
                        PhraseRow(
                            phrase = phrase,
                            isFavorite = state.favorites.contains(phrase),
                            onTap = { onPhraseTap(phrase) },
                            onFavoriteToggle = { onFavoriteToggle(phrase) },
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
                            text = "ALL CATEGORIES",
                            color = colors.textSecondary,
                            fontSize = DesignTokens.Typography.Size.Xs,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }

                items(PhraseCategory.entries, key = { it.id }) { category ->
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

@Composable
private fun SectionHeader(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: androidx.compose.ui.graphics.Color,
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
fun PhraseRow(
    phrase: String,
    isFavorite: Boolean,
    onTap: () -> Unit,
    onFavoriteToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AppTheme.colors
    val shape = RoundedCornerShape(DesignTokens.Radius.Lg)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.card)
            .border(width = 1.dp, color = colors.border, shape = shape),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = phrase,
            color = colors.foreground,
            fontSize = DesignTokens.Typography.Size.Sm,
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onTap)
                .padding(horizontal = DesignTokens.Spacing.S5, vertical = DesignTokens.Spacing.S3),
        )
        Box(
            modifier = Modifier
                .clickable(onClick = onFavoriteToggle)
                .padding(horizontal = DesignTokens.Spacing.S4, vertical = DesignTokens.Spacing.S3),
        ) {
            Icon(
                imageVector = if (isFavorite) Icons.Filled.Star else Icons.Outlined.StarOutline,
                contentDescription = if (isFavorite) "Unfavorite" else "Favorite",
                tint = if (isFavorite) colors.accent else colors.textSecondary,
                modifier = Modifier.size(20.dp),
            )
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
    val shape = RoundedCornerShape(DesignTokens.Radius.Lg)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.card)
            .border(width = 1.dp, color = colors.border, shape = shape)
            .clickable(onClick = onClick)
            .padding(horizontal = DesignTokens.Spacing.S5, vertical = DesignTokens.Spacing.S4),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.S4),
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(DesignTokens.Radius.Lg))
                .background(category.colors.bg),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = category.icon,
                contentDescription = null,
                tint = category.colors.fg,
                modifier = Modifier.size(24.dp),
            )
        }
        Text(
            text = category.displayName,
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
    category: PhraseCategory,
    favorites: Set<String>,
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onPhraseTap: (String) -> Unit,
    onFavoriteToggle: (String) -> Unit,
) {
    val colors = AppTheme.colors
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(top = contentPadding.calculateTopPadding()),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.card)
                .padding(horizontal = DesignTokens.Spacing.S4)
                .padding(top = DesignTokens.Spacing.S4, bottom = DesignTokens.Spacing.S4),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = "Back",
                tint = colors.foreground,
                modifier = Modifier
                    .clip(RoundedCornerShape(DesignTokens.Radius.Md))
                    .clickable(onClick = onBack)
                    .padding(DesignTokens.Spacing.S2)
                    .size(24.dp),
            )
            Text(
                text = category.displayName,
                color = colors.foreground,
                fontSize = DesignTokens.Typography.Size.Xl,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = DesignTokens.Spacing.S3, start = DesignTokens.Spacing.S2),
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.S3),
            contentPadding = PaddingValues(
                start = DesignTokens.Spacing.S6,
                end = DesignTokens.Spacing.S6,
                top = DesignTokens.Spacing.S6,
                bottom = contentPadding.calculateBottomPadding() + DesignTokens.Spacing.S6,
            ),
        ) {
            items(category.phrases, key = { it }) { phrase ->
                PhraseRow(
                    phrase = phrase,
                    isFavorite = favorites.contains(phrase),
                    onTap = { onPhraseTap(phrase) },
                    onFavoriteToggle = { onFavoriteToggle(phrase) },
                )
            }
        }
    }
}
