package com.divami.traveltranslator.feature.chat

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.divami.traveltranslator.designsystem.AppTheme
import com.divami.traveltranslator.designsystem.DesignTokens
import com.divami.traveltranslator.designsystem.components.AppLanguage
import com.divami.traveltranslator.designsystem.components.LanguageFlag
import androidx.compose.animation.core.tween

@Composable
fun MessageBubble(
    message: ChatMessage,
    flipped: Boolean,
    onFlip: () -> Unit,
    onPlay: () -> Unit,
    onSlow: () -> Unit,
    onEdit: () -> Unit,
    onCopy: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AppTheme.colors
    val isMine = message.sender == Sender.You

    val bubbleShape = RoundedCornerShape(
        topStart = if (isMine) DesignTokens.Radius.Modal else DesignTokens.Radius.Sm,
        topEnd = if (isMine) DesignTokens.Radius.Sm else DesignTokens.Radius.Modal,
        bottomStart = DesignTokens.Radius.Modal,
        bottomEnd = DesignTokens.Radius.Modal,
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = DesignTokens.Spacing.S4),
        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 320.dp)
                .clip(bubbleShape)
                .background(colors.card)
                .border(
                    width = 1.dp,
                    color = colors.border,
                    shape = bubbleShape,
                )
                .padding(horizontal = DesignTokens.Spacing.S4, vertical = DesignTokens.Spacing.S3),
        ) {
            // Flip control — top-right corner, 24dp circular tap target
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.TopEnd,
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onFlip),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.SwapVert,
                        contentDescription = "Flip languages",
                        tint = colors.primary,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }

            // #39: animate the top section when flipped state changes
            AnimatedContent(
                targetState = flipped,
                transitionSpec = {
                    (slideInVertically(tween(220)) { -it / 3 } + fadeIn(tween(220))) togetherWith
                        (slideOutVertically(tween(220)) { it / 3 } + fadeOut(tween(220)))
                },
                label = "bubble-top",
            ) { isFlipped ->
                if (!isFlipped) {
                    LanguageSection(
                        lang = message.originalLanguage,
                        text = message.originalText,
                        isTranslation = false,
                        pronunciation = message.pronunciation,
                    )
                } else {
                    LanguageSection(
                        lang = message.translatedLanguage,
                        text = message.translatedText,
                        isTranslation = true,
                        pronunciation = null,
                    )
                }
            }

            Spacer(modifier = Modifier.height(DesignTokens.Spacing.S3))

            // #39: animate the bottom section when flipped state changes
            AnimatedContent(
                targetState = flipped,
                transitionSpec = {
                    (slideInVertically(tween(220)) { it / 3 } + fadeIn(tween(220))) togetherWith
                        (slideOutVertically(tween(220)) { -it / 3 } + fadeOut(tween(220)))
                },
                label = "bubble-bottom",
            ) { isFlipped ->
                if (!isFlipped) {
                    LanguageSection(
                        lang = message.translatedLanguage,
                        text = message.translatedText,
                        isTranslation = true,
                        pronunciation = null,
                    )
                } else {
                    LanguageSection(
                        lang = message.originalLanguage,
                        text = message.originalText,
                        isTranslation = false,
                        pronunciation = message.pronunciation,
                    )
                }
            }

            Spacer(modifier = Modifier.height(DesignTokens.Spacing.S3))

            // Action row: Play chip + overflow menu
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.S2),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Primary Play chip
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(DesignTokens.Radius.Sm))
                        .background(colors.primaryLight)
                        .clickable(onClick = onPlay)
                        .padding(horizontal = DesignTokens.Spacing.S3, vertical = DesignTokens.Spacing.S2),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.S1),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.VolumeUp,
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(14.dp),
                    )
                    Text(
                        text = "Play",
                        color = colors.primary,
                        fontSize = DesignTokens.Typography.Size.Xs,
                        fontWeight = FontWeight.Medium,
                    )
                }

                // Overflow menu: Slow / Edit / Copy
                var expanded by remember { mutableStateOf(false) }
                Box {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(colors.muted)
                            .clickable { expanded = true },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.MoreVert,
                            contentDescription = "More actions",
                            tint = colors.textSecondary,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text("Slow playback") },
                            leadingIcon = { Icon(Icons.Outlined.Speed, null) },
                            onClick = { onSlow(); expanded = false },
                        )
                        DropdownMenuItem(
                            text = { Text("Edit") },
                            leadingIcon = { Icon(Icons.Outlined.Edit, null) },
                            onClick = { onEdit(); expanded = false },
                        )
                        DropdownMenuItem(
                            text = { Text("Copy translation") },
                            leadingIcon = { Icon(Icons.Outlined.ContentCopy, null) },
                            onClick = { onCopy(); expanded = false },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LanguageSection(
    lang: AppLanguage,
    text: String,
    isTranslation: Boolean,
    pronunciation: String?,
) {
    val colors = AppTheme.colors
    Column(modifier = Modifier.padding(top = DesignTokens.Spacing.S2)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.S2),
        ) {
            LanguageFlag(language = lang, compact = true)
            Text(
                text = lang.displayName,
                color = if (isTranslation) colors.primary else colors.foreground,
                fontSize = DesignTokens.Typography.Size.Xs,
                fontWeight = FontWeight.Medium,
            )
        }
        Text(
            text = text,
            color = colors.foreground,
            fontSize = DesignTokens.Typography.Size.Base,
            modifier = Modifier.padding(top = DesignTokens.Spacing.S1),
        )
        if (!pronunciation.isNullOrBlank()) {
            Text(
                text = pronunciation,
                color = colors.textSecondary,
                fontSize = DesignTokens.Typography.Size.Xs,
                fontStyle = FontStyle.Italic,
                modifier = Modifier.padding(top = DesignTokens.Spacing.S1),
            )
        }
    }
}
