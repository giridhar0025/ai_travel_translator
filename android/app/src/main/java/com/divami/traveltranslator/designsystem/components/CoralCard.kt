package com.divami.traveltranslator.designsystem.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.divami.traveltranslator.designsystem.AppTheme
import com.divami.traveltranslator.designsystem.DesignTokens

@Composable
fun CoralCard(
    modifier: Modifier = Modifier,
    padding: Dp = DesignTokens.Spacing.S4,
    cornerRadius: Dp = DesignTokens.Radius.Lg,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val colors = AppTheme.colors
    val shape = RoundedCornerShape(cornerRadius)
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && onClick != null) DesignTokens.Motion.PressScale.Primary else 1f,
        label = "coralCardScale",
    )

    var m = modifier
        .fillMaxWidth()
        .scale(scale)
        .shadow(elevation = 4.dp, shape = shape, clip = false)
        .clip(shape)
        .background(colors.card)
        .border(width = 1.dp, color = colors.border, shape = shape)
    if (onClick != null) {
        m = m.clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick,
        )
    }
    m = m.padding(padding)

    Column(modifier = m) { content() }
}
