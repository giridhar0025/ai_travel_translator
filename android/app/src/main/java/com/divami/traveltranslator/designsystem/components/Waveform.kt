package com.divami.traveltranslator.designsystem.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.divami.traveltranslator.designsystem.AppTheme
import com.divami.traveltranslator.designsystem.DesignTokens
import kotlinx.coroutines.delay
import kotlin.random.Random

@Composable
fun Waveform(
    modifier: Modifier = Modifier,
    barCount: Int = 8,
    isAnimated: Boolean = false,
    activeColor: Color = AppTheme.colors.primary,
    inactiveColor: Color = AppTheme.colors.border,
    height: Dp = 64.dp,
    levels: List<Float>? = null,
) {
    val current = remember(barCount) { mutableStateListOf<Float>().apply { repeat(barCount) { add(0.2f) } } }

    LaunchedEffect(isAnimated, barCount, levels) {
        if (levels != null) return@LaunchedEffect
        if (isAnimated) {
            while (true) {
                for (i in 0 until barCount) current[i] = 0.2f + Random.nextFloat() * 0.8f
                delay(180)
            }
        } else {
            for (i in 0 until barCount) current[i] = 0.2f
        }
    }

    val barWidth: Dp = if (barCount > 20) 3.dp else 6.dp
    val gap: Dp      = if (barCount > 20) 2.dp else 6.dp

    Row(
        modifier = modifier.height(height),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(gap),
    ) {
        for (i in 0 until barCount) {
            val raw = levels?.getOrNull(i) ?: current[i]
            val animated by animateFloatAsState(
                targetValue = raw.coerceIn(0.05f, 1f),
                label = "waveformBar$i",
            )
            Box(
                modifier = Modifier
                    .size(width = barWidth, height = height * animated)
                    .clip(RoundedCornerShape(50))
                    .background(if (isAnimated) activeColor else inactiveColor),
            )
        }
    }
}
