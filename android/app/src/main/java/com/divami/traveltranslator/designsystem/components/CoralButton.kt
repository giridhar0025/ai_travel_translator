package com.divami.traveltranslator.designsystem.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.divami.traveltranslator.designsystem.AppTheme
import com.divami.traveltranslator.designsystem.DesignTokens
import com.divami.traveltranslator.designsystem.minimumTouchTarget

enum class CoralButtonVariant { Primary, Secondary, Text }

@Composable
fun CoralButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: CoralButtonVariant = CoralButtonVariant.Primary,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    whiteOnCoral: Boolean = false,
) {
    val colors = AppTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isFocused by interactionSource.collectIsFocusedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled && !isLoading) DesignTokens.Motion.PressScale.Primary else 1f,
        label = "coralButtonScale",
    )

    val bg: Color
    val fg: Color
    val border: BorderStroke?
    when {
        whiteOnCoral -> {
            bg = colors.card
            fg = colors.primary
            border = null
        }
        variant == CoralButtonVariant.Primary -> {
            bg = if (enabled) colors.primary else colors.inputBg
            fg = if (enabled) colors.onPrimary else colors.textSecondary
            border = if (enabled) null else BorderStroke(1.dp, colors.border)
        }
        variant == CoralButtonVariant.Secondary -> {
            bg = colors.card
            fg = if (enabled) colors.foreground else colors.textSecondary
            border = BorderStroke(1.dp, colors.borderStrong)
        }
        else -> { // Text
            bg = Color.Transparent
            fg = colors.textSecondary
            border = null
        }
    }

    val shape = RoundedCornerShape(DesignTokens.Radius.Lg)
    var rowModifier = modifier.scale(scale)
    if (variant != CoralButtonVariant.Text) {
        rowModifier = rowModifier.fillMaxWidth()
    }
    rowModifier = rowModifier
        .minimumTouchTarget()
        .clip(shape)
        .background(bg)
    val effectiveBorder = if (isFocused) BorderStroke(2.dp, colors.ring) else border
    if (effectiveBorder != null) {
        rowModifier = rowModifier.border(effectiveBorder, shape)
    }
    rowModifier = rowModifier
        .clickable(
            interactionSource = interactionSource,
            indication = LocalIndication.current,
            enabled = enabled && !isLoading,
            onClick = onClick,
        )
        .semantics { role = Role.Button }
        .padding(
            horizontal = if (variant == CoralButtonVariant.Text) DesignTokens.Spacing.S3 else DesignTokens.Spacing.S6,
            vertical = if (variant == CoralButtonVariant.Text) DesignTokens.Spacing.S3 else DesignTokens.Spacing.S4,
        )

    Row(
        modifier = rowModifier,
        horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.S2, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = fg,
                strokeWidth = 2.dp,
            )
        } else if (leadingIcon != null) {
            Icon(imageVector = leadingIcon, contentDescription = null, tint = fg)
        }
        Text(
            text = text,
            color = fg,
            fontSize = DesignTokens.Typography.Size.Base,
            fontWeight = FontWeight.Medium,
        )
        if (!isLoading && trailingIcon != null) {
            Icon(imageVector = trailingIcon, contentDescription = null, tint = fg)
        }
    }
}
