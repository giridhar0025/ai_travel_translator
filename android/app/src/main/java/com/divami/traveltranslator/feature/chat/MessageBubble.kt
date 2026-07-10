package com.divami.traveltranslator.feature.chat

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.divami.traveltranslator.designsystem.AppTheme
import com.divami.traveltranslator.designsystem.DesignTokens
import com.divami.traveltranslator.designsystem.components.AppLanguage
import com.divami.traveltranslator.designsystem.components.LanguageFlag

/** Playback state of a message's translation audio, as seen by its bubble. */
enum class PlaybackState { Idle, Preparing, Playing }

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubble(
    message: ChatMessage,
    flipped: Boolean,
    playback: PlaybackState,
    isOwnLatest: Boolean,
    onFlip: () -> Unit,
    onPlayToggle: () -> Unit,
    onSlow: () -> Unit,
    onEdit: () -> Unit,
    onCopy: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AppTheme.colors
    val isMine = message.sender == Sender.You
    val showTyping = message.isStreaming && message.translatedText.isBlank()

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
        Box(
            modifier = Modifier
                .widthIn(max = 320.dp)
                .clip(bubbleShape)
                // My side is coral-tinted, the other person's is neutral — a clear
                // two-sided conversation.
                .background(if (isMine) colors.primaryLight else colors.card)
                .border(
                    width = 1.dp,
                    color = if (isMine) colors.primary.copy(alpha = 0.25f) else colors.border,
                    shape = bubbleShape,
                ),
        ) {
          Column(
            modifier = Modifier.padding(
                horizontal = DesignTokens.Spacing.S3,
                vertical = DesignTokens.Spacing.S2,
            ),
        ) {
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
                        showTyping = false,
                    )
                } else {
                    LanguageSection(
                        lang = message.translatedLanguage,
                        text = message.translatedText,
                        isTranslation = true,
                        pronunciation = null,
                        showTyping = showTyping,
                    )
                }
            }

            Spacer(modifier = Modifier.height(DesignTokens.Spacing.S1))

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
                        showTyping = showTyping,
                    )
                } else {
                    LanguageSection(
                        lang = message.originalLanguage,
                        text = message.originalText,
                        isTranslation = false,
                        pronunciation = message.pronunciation,
                        showTyping = false,
                    )
                }
            }

            Spacer(modifier = Modifier.height(DesignTokens.Spacing.S1))

            // Action row: inline Play/Stop (long-press = slow), one-tap Copy, and
            // Edit only on your own most-recent message. No overflow menu.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.S2),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Play chip — toggles speak/stop; long-press speaks slowly.
                val isBusy = playback != PlaybackState.Idle
                val playLabel = when (playback) {
                    PlaybackState.Playing -> "Stop"
                    PlaybackState.Preparing -> "Loading"
                    PlaybackState.Idle -> "Play"
                }
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(DesignTokens.Radius.Sm))
                        .background(if (isBusy) colors.primary.copy(alpha = 0.18f) else colors.primaryLight)
                        .combinedClickable(
                            onClick = onPlayToggle,
                            onLongClick = onSlow,
                        )
                        .padding(horizontal = DesignTokens.Spacing.S3, vertical = DesignTokens.Spacing.S2),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.S1),
                ) {
                    when (playback) {
                        PlaybackState.Preparing -> CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp,
                            color = colors.primary,
                        )
                        PlaybackState.Playing -> Icon(
                            imageVector = Icons.Outlined.Stop,
                            contentDescription = null,
                            tint = colors.primary,
                            modifier = Modifier.size(14.dp),
                        )
                        PlaybackState.Idle -> Icon(
                            imageVector = Icons.AutoMirrored.Outlined.VolumeUp,
                            contentDescription = null,
                            tint = colors.primary,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                    Text(
                        text = playLabel,
                        color = colors.primary,
                        fontSize = DesignTokens.Typography.Size.Xs,
                        fontWeight = FontWeight.Medium,
                    )
                }

                // One-tap Copy
                BubbleActionIcon(
                    icon = Icons.Outlined.ContentCopy,
                    contentDescription = "Copy translation",
                    onClick = onCopy,
                )

                // Edit — only meaningful on your own latest message
                if (isOwnLatest) {
                    BubbleActionIcon(
                        icon = Icons.Outlined.Edit,
                        contentDescription = "Edit message",
                        onClick = onEdit,
                    )
                }
            }
          }

          // Flip control — explicit, labelled corner button, kept for accessibility.
          Box(
              modifier = Modifier
                  .align(Alignment.TopEnd)
                  .padding(DesignTokens.Spacing.S1)
                  .size(28.dp)
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
    }
}

@Composable
private fun BubbleActionIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    val colors = AppTheme.colors
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(colors.muted)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = colors.textSecondary,
            modifier = Modifier.size(16.dp),
        )
    }
}

@Composable
private fun LanguageSection(
    lang: AppLanguage,
    text: String,
    isTranslation: Boolean,
    pronunciation: String?,
    showTyping: Boolean,
) {
    val colors = AppTheme.colors
    Column(modifier = Modifier.padding(top = DesignTokens.Spacing.S1)) {
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
        if (showTyping) {
            TypingDots(modifier = Modifier.padding(top = DesignTokens.Spacing.S2))
        } else {
            Text(
                text = text,
                color = colors.foreground,
                fontSize = DesignTokens.Typography.Size.Base,
                modifier = Modifier.padding(top = DesignTokens.Spacing.S1),
            )
        }
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

/** Three dots pulsing in sequence — the "translating…" indicator while streaming. */
@Composable
private fun TypingDots(modifier: Modifier = Modifier) {
    val colors = AppTheme.colors
    val transition = rememberInfiniteTransition(label = "typing")
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(3) { index ->
            val alpha by transition.animateFloat(
                initialValue = 0.25f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 600, delayMillis = index * 150),
                    repeatMode = RepeatMode.Reverse,
                ),
                label = "dot-$index",
            )
            Box(
                modifier = Modifier
                    .alpha(alpha)
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(colors.primary),
            )
        }
    }
}
