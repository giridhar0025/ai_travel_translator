package com.divami.traveltranslator.feature.history

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.divami.traveltranslator.designsystem.AppTheme
import com.divami.traveltranslator.designsystem.DesignTokens
import com.divami.traveltranslator.designsystem.components.SearchBar
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class Conversation(
    val id: String,
    val title: String,
    val preview: String,
    val date: String,
    val time: String,
    val messageCount: Int,
    // NOTE(audit-#24): time field still static; will be derived from real timestamp when DB lands
    val timestamp: Long = 0L,
)

private val now = System.currentTimeMillis()
private val day = 86_400_000L

// TODO(audit-#23): toggle SeedConversations to emptyList() to preview empty state
private val SeedConversations = listOf(
    Conversation("1", "Restaurant Conversation", "Table for two, please…", "Today", "2:30 PM", 8, now - 0 * day),
    Conversation("2", "Directions to Museum", "How do I get to the museum?", "Today", "10:15 AM", 5, now - 0 * day),
    Conversation("3", "Hotel Check-in", "I have a reservation…", "Yesterday", "6:45 PM", 12, now - 1 * day),
    Conversation("4", "Shopping at Market", "How much does this cost?", "Yesterday", "3:20 PM", 15, now - 1 * day),
    Conversation("5", "Taxi to Airport", "How much to the airport?", "Feb 16", "7:00 AM", 4, now - 6 * day),
)

private fun groupLabel(timestamp: Long): String {
    if (timestamp == 0L) return "Other"
    val today = LocalDate.now()
    val date = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
    return when {
        date == today -> "Today"
        date == today.minusDays(1) -> "Yesterday"
        else -> date.format(DateTimeFormatter.ofPattern("MMM d"))
    }
}

@Composable
fun ConversationHistoryScreen(
    onConversationOpen: (String) -> Unit,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    val colors = AppTheme.colors
    var query by remember { mutableStateOf("") }

    val filtered = remember(query) {
        if (query.isBlank()) SeedConversations
        else SeedConversations.filter {
            it.title.contains(query, ignoreCase = true) ||
                it.preview.contains(query, ignoreCase = true)
        }
    }

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
                value = query,
                onValueChange = { query = it },
                placeholder = "Search conversations…",
            )
        }

        // #23: true empty state (no history at all) — rendered outside LazyColumn
        if (SeedConversations.isEmpty()) {
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
                    Text(
                        text = "No conversations yet",
                        color = colors.foreground,
                        fontSize = DesignTokens.Typography.Size.Lg,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = "Start chatting to see your translation history here.",
                        color = colors.textSecondary,
                        fontSize = DesignTokens.Typography.Size.Sm,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = contentPadding.calculateBottomPadding() + DesignTokens.Spacing.S6),
            ) {
                // #24: group by computed groupLabel(timestamp) instead of static date field
                val grouped = filtered.groupBy { groupLabel(it.timestamp) }
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
                                modifier = Modifier
                                    .padding(horizontal = DesignTokens.Spacing.S6)
                                    .padding(bottom = DesignTokens.Spacing.S2),
                            )
                        }
                    }
                }

                // #23: search-empty state (has history, but query matched nothing)
                if (filtered.isEmpty()) {
                    item(key = "empty") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(DesignTokens.Spacing.S10),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "No conversations match \"$query\"",
                                color = colors.textSecondary,
                                fontSize = DesignTokens.Typography.Size.Sm,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ConversationRow(
    conv: Conversation,
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
                    text = conv.time,
                    color = colors.textSecondary,
                    fontSize = DesignTokens.Typography.Size.Xs,
                    modifier = Modifier.padding(start = DesignTokens.Spacing.S2),
                )
            }
            Text(
                text = conv.preview,
                color = colors.textSecondary,
                fontSize = DesignTokens.Typography.Size.Sm,
                maxLines = 1,
                modifier = Modifier.padding(top = DesignTokens.Spacing.S1),
            )
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
