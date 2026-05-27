package com.divami.traveltranslator.feature.audio

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.divami.traveltranslator.designsystem.AppTheme
import com.divami.traveltranslator.designsystem.DesignTokens
import kotlinx.coroutines.delay
import kotlin.random.Random

private const val BarCount = 40
private const val PlayDurationMs = 2000L
private const val SlowPlayDurationMs = 4000L

@Composable
fun AudioPlayer(
    text: String,
    pronunciation: String?,
    modifier: Modifier = Modifier,
    initialSlow: Boolean = false,
    // NOTE(audit-#10): caller should pass initialPlay = req.slow
    initialPlay: Boolean = false,
) {
    val colors = AppTheme.colors
    var isPlaying by rememberSaveable { mutableStateOf(false) }
    var isSlow by rememberSaveable { mutableStateOf(initialSlow) }

    // #10: auto-start playback when requested (e.g. launched from "Slow" chip)
    LaunchedEffect(Unit) {
        if (initialPlay) isPlaying = true
    }

    LaunchedEffect(isPlaying, isSlow) {
        if (isPlaying) {
            delay(if (isSlow) SlowPlayDurationMs else PlayDurationMs)
            isPlaying = false
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(DesignTokens.Radius.Modal))
            .background(colors.card)
            .padding(DesignTokens.Spacing.S6),
    ) {
        Text(
            text = text,
            color = colors.foreground,
            fontSize = DesignTokens.Typography.Size.Xxl,
            fontWeight = FontWeight.Medium,
        )
        Spacer(modifier = Modifier.height(DesignTokens.Spacing.S2))
        Text(
            text = pronunciation?.takeIf { it.isNotBlank() }
                ?: "Pronunciation guide will appear here",
            color = colors.textSecondary,
            fontSize = DesignTokens.Typography.Size.Sm,
            fontStyle = FontStyle.Italic,
        )

        Spacer(modifier = Modifier.height(DesignTokens.Spacing.S6))
        PlayerWaveform(isPlaying = isPlaying)

        Spacer(modifier = Modifier.height(DesignTokens.Spacing.S6))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.S3),
        ) {
            PlayPauseButton(
                isPlaying = isPlaying,
                onToggle = { isPlaying = !isPlaying },
                modifier = Modifier.weight(1f),
            )
            SlowToggle(
                isSlow = isSlow,
                onToggle = { isSlow = !isSlow },
            )
        }

        if (isSlow) {
            Spacer(modifier = Modifier.height(DesignTokens.Spacing.S3))
            Text(
                text = "Slow mode enabled for clearer pronunciation",
                color = colors.textSecondary,
                fontSize = DesignTokens.Typography.Size.Xs,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}

@Composable
private fun PlayerWaveform(isPlaying: Boolean) {
    val colors = AppTheme.colors

    val seeds = remember { List(BarCount) { Random.nextFloat() * 0.6f + 0.2f } }
    val phaseTransition = rememberInfiniteTransition(label = "playerPhase")
    val phase by phaseTransition.animateFloat(
        initialValue = 0f,
        targetValue = (Math.PI * 2).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "phase",
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .clip(RoundedCornerShape(DesignTokens.Radius.Lg))
            .background(colors.background)
            .padding(horizontal = DesignTokens.Spacing.S4),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        for (i in 0 until BarCount) {
            val targetLevel = if (isPlaying) {
                val wave = (kotlin.math.sin(phase + i * 0.45f) * 0.5f + 0.5f)
                (seeds[i] * wave).coerceIn(0.15f, 0.95f)
            } else {
                0.2f
            }
            val animated by animateFloatAsState(
                targetValue = targetLevel,
                animationSpec = tween(durationMillis = if (isPlaying) 120 else 220),
                label = "bar$i",
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(64.dp * animated)
                    .clip(RoundedCornerShape(50))
                    .background(if (isPlaying) colors.primary else colors.border),
            )
        }
    }
}

@Composable
private fun PlayPauseButton(
    isPlaying: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AppTheme.colors
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(DesignTokens.Radius.Lg))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(colors.primary, colors.primaryDark),
                ),
            )
            .clickable(onClick = onToggle)
            .padding(vertical = DesignTokens.Spacing.S4),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.S3, Alignment.CenterHorizontally),
    ) {
        Icon(
            imageVector = if (isPlaying) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
            contentDescription = null,
            tint = colors.onPrimary,
            modifier = Modifier.size(28.dp),
        )
        Text(
            text = if (isPlaying) "Pause" else "Play Audio",
            color = colors.onPrimary,
            fontSize = DesignTokens.Typography.Size.Base,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun SlowToggle(
    isSlow: Boolean,
    onToggle: () -> Unit,
) {
    val colors = AppTheme.colors
    val bg: Color = if (isSlow) colors.primary else colors.muted
    val fg: Color = if (isSlow) colors.onPrimary else colors.textSecondary
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(DesignTokens.Radius.Lg))
            .background(bg)
            .clickable(onClick = onToggle)
            .padding(horizontal = DesignTokens.Spacing.S5, vertical = DesignTokens.Spacing.S4),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.S2),
    ) {
        Icon(
            imageVector = Icons.Outlined.Speed,
            contentDescription = null,
            tint = fg,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = "Slow",
            color = fg,
            fontSize = DesignTokens.Typography.Size.Base,
            fontWeight = FontWeight.Medium,
        )
    }
}
