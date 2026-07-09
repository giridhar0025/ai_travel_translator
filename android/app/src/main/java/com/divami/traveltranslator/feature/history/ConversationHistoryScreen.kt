package com.divami.traveltranslator.feature.history

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.divami.traveltranslator.designsystem.AppTheme
import com.divami.traveltranslator.designsystem.DesignTokens
import com.divami.traveltranslator.designsystem.components.SearchBar

@Composable
fun ConversationHistoryScreen(
    onConversationOpen: (String) -> Unit,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val colors = AppTheme.colors
    val state by viewModel.state.collectAsState()
    var pendingDelete by remember { mutableStateOf<HistoryItem?>(null) }

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
                .padding(horizontal = DesignTokens.Spacing.S6)
                .padding(top = DesignTokens.Spacing.S6, bottom = DesignTokens.Spacing.S4),
            verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.S4),
        ) {
            Text(
                text = "History",
                color = colors.foreground,
                fontSize = DesignTokens.Typography.Size.Xl,
                fontWeight = FontWeight.SemiBold,
            )
            SearchBar(
                value = state.query,
                onValueChange = viewModel::onQueryChange,
                placeholder = "Search conversations…",
            )
        }

        when {
            state.items.isEmpty() && !state.loading ->
                EmptyState(query = state.query, error = state.error)

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = contentPadding.calculateBottomPadding() + DesignTokens.Spacing.S6),
            ) {
                val grouped = state.items.groupBy { it.groupLabel }
                grouped.forEach { (label, items) ->
                    item(key = "header-$label") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(colors.background)
                                .padding(horizontal = DesignTokens.Spacing.S6, vertical = DesignTokens.Spacing.S2),
                        ) {
                            Text(
                                text = label,
                                color = colors.textSecondary,
                                fontSize = DesignTokens.Typography.Size.Sm,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                    items.forEach { conv ->
                        item(key = conv.id) {
                            ConversationRow(
                                conv = conv,
                                onClick = { onConversationOpen(conv.id) },
                                onLongClick = { pendingDelete = conv },
                                modifier = Modifier
                                    .padding(horizontal = DesignTokens.Spacing.S6)
                                    .padding(bottom = DesignTokens.Spacing.S2),
                            )
                        }
                    }
                }
            }
        }
    }

    pendingDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete conversation?", color = colors.foreground, fontWeight = FontWeight.SemiBold) },
            text = { Text("\"${target.title}\" will be permanently removed.", color = colors.textSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(target.id)
                    pendingDelete = null
                }) {
                    Text("Delete", color = colors.destructive, fontWeight = FontWeight.Medium)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text("Cancel", color = colors.textSecondary)
                }
            },
            containerColor = colors.card,
        )
    }
}

@Composable
private fun EmptyState(query: String, error: String?) {
    val colors = AppTheme.colors
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(DesignTokens.Spacing.S10),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.S4),
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(colors.primaryLight),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.Chat,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(40.dp),
                )
            }
            val title = when {
                error != null -> "Couldn't load history"
                query.isNotBlank() -> "No matches"
                else -> "No conversations yet"
            }
            val subtitle = when {
                error != null -> error
                query.isNotBlank() -> "No conversations match \"$query\""
                else -> "Start chatting to see your translation history here."
            }
            Text(
                text = title,
                color = colors.foreground,
                fontSize = DesignTokens.Typography.Size.Lg,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
            Text(
                text = subtitle,
                color = colors.textSecondary,
                fontSize = DesignTokens.Typography.Size.Sm,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ConversationRow(
    conv: HistoryItem,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
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
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(DesignTokens.Spacing.S4),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.S3),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(DesignTokens.Radius.Lg))
                .background(colors.primaryLight),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.Chat,
                contentDescription = null,
                tint = colors.primary,
                modifier = Modifier.size(20.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Text(
                    text = conv.title,
                    color = colors.foreground,
                    fontSize = DesignTokens.Typography.Size.Base,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Text(
                    text = conv.timeLabel,
                    color = colors.textSecondary,
                    fontSize = DesignTokens.Typography.Size.Xs,
                    modifier = Modifier.padding(start = DesignTokens.Spacing.S2),
                )
            }
            Text(
                text = "${conv.messageCount} messages",
                color = colors.textSecondary,
                fontSize = DesignTokens.Typography.Size.Xs,
                modifier = Modifier.padding(top = DesignTokens.Spacing.S1),
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            contentDescription = null,
            tint = colors.textSecondary,
            modifier = Modifier
                .align(Alignment.CenterVertically)
                .size(20.dp),
        )
    }
}
