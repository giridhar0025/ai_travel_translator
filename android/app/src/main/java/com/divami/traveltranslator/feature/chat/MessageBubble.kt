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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.OpenInFull
import androidx.compose.material.icons.outlined.Speed
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.divami.traveltranslator.R
import com.divami.traveltranslator.designsystem.AppTheme
import com.divami.traveltranslator.designsystem.DesignTokens
import com.divami.traveltranslator.designsystem.minimumTouchTarget
import com.divami.traveltranslator.designsystem.rememberReducedMotion
import com.divami.traveltranslator.designsystem.components.AppLanguage
import com.divami.traveltranslator.designsystem.components.LanguageFlag

/** Playback state of a message's translation audio, as seen by its bubble. */
enum class PlaybackState { Idle, Preparing, Playing }

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
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
    onShow: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AppTheme.colors
    val reducedMotion = rememberReducedMotion()
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
            .padding(horizontal = DesignTokens.Spacing.S6),
        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start,
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 360.dp)
                .clip(bubbleShape)
                .background(if (isMine) colors.primaryLight else colors.card)
                .border(
                    width = 1.dp,
                    color = if (isMine) colors.primary.copy(alpha = 0.45f) else colors.borderStrong,
                    shape = bubbleShape,
                ),
        ) {
            Column(
                modifier = Modifier.padding(
                    horizontal = DesignTokens.Spacing.S3,
                    vertical = DesignTokens.Spacing.S2,
                ),
            ) {
                val topTransition = if (reducedMotion) {
                    fadeIn(tween(0)) togetherWith fadeOut(tween(0))
                } else {
                    (slideInVertically(tween(220)) { -it / 3 } + fadeIn(tween(220))) togetherWith
                        (slideOutVertically(tween(220)) { it / 3 } + fadeOut(tween(220)))
                }
                val bottomTransition = if (reducedMotion) {
                    fadeIn(tween(0)) togetherWith fadeOut(tween(0))
                } else {
                    (slideInVertically(tween(220)) { it / 3 } + fadeIn(tween(220))) togetherWith
                        (slideOutVertically(tween(220)) { -it / 3 } + fadeOut(tween(220)))
                }

                AnimatedContent(
                    targetState = flipped,
                    transitionSpec = { topTransition },
                    label = "bubble-top",
                ) { isFlipped ->
                    if (!isFlipped) {
                        LanguageSection(
                            lang = message.originalLanguage,
                            text = message.originalText,
                            isTranslation = false,
                            pronunciation = message.pronunciation,
                            showTyping = false,
                            isOffline = false,
                        )
                    } else {
                        LanguageSection(
                            lang = message.translatedLanguage,
                            text = message.translatedText,
                            isTranslation = true,
                            pronunciation = null,
                            showTyping = showTyping,
                            isOffline = message.isOffline,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(DesignTokens.Spacing.S1))

                AnimatedContent(
                    targetState = flipped,
                    transitionSpec = { bottomTransition },
                    label = "bubble-bottom",
                ) { isFlipped ->
                    if (!isFlipped) {
                        LanguageSection(
                            lang = message.translatedLanguage,
                            text = message.translatedText,
                            isTranslation = true,
                            pronunciation = null,
                            showTyping = showTyping,
                            isOffline = message.isOffline,
                        )
                    } else {
                        LanguageSection(
                            lang = message.originalLanguage,
                            text = message.originalText,
                            isTranslation = false,
                            pronunciation = message.pronunciation,
                            showTyping = false,
                            isOffline = false,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(DesignTokens.Spacing.S1))

                // Every action is single-tap and at least 48dp. Slow playback is
                // explicit (long-press remains a shortcut) so switch-access users
                // can discover and perform it.
                // Two deliberate rows keep the primary playback actions from
                // being split awkwardly when the bubble is narrow or text is
                // scaled up. Every control remains single-tap and 48dp.
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.S1),
                ) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.S1),
                        verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.S1),
                    ) {
                        PlayChip(
                            playback = playback,
                            onClick = onPlayToggle,
                            onSlow = onSlow,
                            enabled = message.translatedText.isNotBlank(),
                        )
                        SlowChip(
                            onClick = onSlow,
                            enabled = message.translatedText.isNotBlank(),
                        )
                    }
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.S1),
                        verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.S1),
                    ) {
                        BubbleActionIcon(
                            icon = Icons.Outlined.ContentCopy,
                            contentDescription = stringResource(R.string.chat_copy_translation),
                            onClick = onCopy,
                            enabled = message.translatedText.isNotBlank(),
                        )
                        if (isOwnLatest) {
                            BubbleActionIcon(
                                icon = Icons.Outlined.Edit,
                                contentDescription = stringResource(R.string.chat_edit_message),
                                onClick = onEdit,
                            )
                        }
                        if (message.translatedText.isNotBlank()) {
                            BubbleActionIcon(
                                icon = Icons.Outlined.OpenInFull,
                                contentDescription = stringResource(R.string.chat_show_to_local),
                                onClick = onShow,
                            )
                        }
                        BubbleActionIcon(
                            icon = Icons.Outlined.SwapVert,
                            contentDescription = stringResource(R.string.chat_flip_languages),
                            onClick = onFlip,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PlayChip(
    playback: PlaybackState,
    onClick: () -> Unit,
    onSlow: () -> Unit,
    enabled: Boolean,
) {
    val colors = AppTheme.colors
    val isBusy = playback != PlaybackState.Idle
    val shape = RoundedCornerShape(DesignTokens.Radius.Sm)
    val label = when (playback) {
        PlaybackState.Playing -> stringResource(R.string.chat_stop)
        PlaybackState.Preparing -> stringResource(R.string.chat_loading)
        PlaybackState.Idle -> stringResource(R.string.chat_play)
    }
    Row(
        modifier = Modifier
            .minimumTouchTarget()
            .heightIn(min = DesignTokens.TouchTarget.Min)
            .clip(shape)
            .background(if (isBusy) colors.primaryDark else colors.primary)
            .combinedClickable(
                enabled = enabled,
                onClick = onClick,
                onLongClick = onSlow,
            )
            .semantics { role = Role.Button }
            .padding(horizontal = DesignTokens.Spacing.S3, vertical = DesignTokens.Spacing.S2),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.S1),
    ) {
        when (playback) {
            PlaybackState.Preparing -> CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
                color = colors.onPrimary,
            )
            PlaybackState.Playing -> Icon(
                imageVector = Icons.Outlined.Stop,
                contentDescription = null,
                tint = colors.onPrimary,
                modifier = Modifier.size(16.dp),
            )
            PlaybackState.Idle -> Icon(
                imageVector = Icons.AutoMirrored.Outlined.VolumeUp,
                contentDescription = null,
                tint = colors.onPrimary,
                modifier = Modifier.size(16.dp),
            )
        }
        Text(
            text = label,
            color = colors.onPrimary,
            fontSize = DesignTokens.Typography.Size.Sm,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun SlowChip(onClick: () -> Unit, enabled: Boolean) {
    val colors = AppTheme.colors
    val shape = RoundedCornerShape(DesignTokens.Radius.Sm)
    Row(
        modifier = Modifier
            .minimumTouchTarget()
            .heightIn(min = DesignTokens.TouchTarget.Min)
            .clip(shape)
            .background(colors.card)
            .border(1.dp, colors.borderStrong, shape)
            .clickable(enabled = enabled, onClick = onClick)
            .semantics { role = Role.Button }
            .padding(horizontal = DesignTokens.Spacing.S3, vertical = DesignTokens.Spacing.S2),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.S1),
    ) {
        Icon(
            imageVector = Icons.Outlined.Speed,
            contentDescription = null,
            tint = colors.textSecondary,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = stringResource(R.string.chat_slow),
            color = colors.textSecondary,
            fontSize = DesignTokens.Typography.Size.Sm,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun BubbleActionIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    val colors = AppTheme.colors
    Box(
        modifier = Modifier
            .minimumTouchTarget()
            .size(DesignTokens.TouchTarget.Min)
            .clip(CircleShape)
            .background(colors.muted)
            .clickable(enabled = enabled, onClick = onClick)
            .semantics { role = Role.Button },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (enabled) colors.textSecondary else colors.textSecondary.copy(alpha = 0.5f),
            modifier = Modifier.size(20.dp),
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
    isOffline: Boolean,
) {
    val colors = AppTheme.colors
    val translatingLabel = stringResource(R.string.chat_translating)
    val offlineLabel = stringResource(R.string.chat_offline_badge)
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
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (isTranslation && isOffline) {
                Text(
                    text = offlineLabel,
                    color = colors.primary,
                    fontSize = DesignTokens.Typography.Size.Xs,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .clip(RoundedCornerShape(DesignTokens.Radius.Sm))
                        .background(colors.primary.copy(alpha = 0.12f))
                        .padding(horizontal = DesignTokens.Spacing.S1, vertical = 2.dp),
                )
            }
        }
        if (showTyping) {
            TypingDots(
                modifier = Modifier.padding(top = DesignTokens.Spacing.S2),
                label = translatingLabel,
            )
        } else {
            Text(
                text = text,
                color = colors.foreground,
                fontSize = if (isTranslation) DesignTokens.Typography.Size.Xl else DesignTokens.Typography.Size.Base,
                lineHeight = if (isTranslation) 27.sp else 22.sp,
                fontWeight = if (isTranslation) FontWeight.Medium else FontWeight.Normal,
                modifier = Modifier
                    .padding(top = DesignTokens.Spacing.S1)
                    .semantics {
                        if (isTranslation && isOffline) {
                            stateDescription = offlineLabel
                        }
                    },
            )
        }
        if (!pronunciation.isNullOrBlank()) {
            Text(
                text = pronunciation,
                color = colors.textSecondary,
                fontSize = DesignTokens.Typography.Size.Xs,
                fontStyle = FontStyle.Italic,
                lineHeight = 17.sp,
                modifier = Modifier.padding(top = DesignTokens.Spacing.S1),
            )
        }
        if (isTranslation && isOffline) {
            Text(
                text = stringResource(R.string.chat_offline_note),
                color = colors.textSecondary,
                fontSize = DesignTokens.Typography.Size.Xs,
                lineHeight = 17.sp,
                modifier = Modifier.padding(top = DesignTokens.Spacing.S1),
            )
        }
    }
}

/** Three dots pulsing in sequence — or a static label when motion is reduced. */
@Composable
private fun TypingDots(modifier: Modifier = Modifier, label: String) {
    val colors = AppTheme.colors
    val reducedMotion = rememberReducedMotion()
    if (reducedMotion) {
        Text(
            text = label,
            modifier = modifier,
            color = colors.textSecondary,
            fontSize = DesignTokens.Typography.Size.Base,
        )
        return
    }
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
