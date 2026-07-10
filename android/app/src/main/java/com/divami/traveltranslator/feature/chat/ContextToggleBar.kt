package com.divami.traveltranslator.feature.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.divami.traveltranslator.designsystem.AppTheme
import com.divami.traveltranslator.designsystem.DesignTokens

@Composable
fun ContextToggleBar(
    selected: ContextMode,
    onSelect: (ContextMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.S2),
    ) {
        ContextMode.entries.forEach { mode ->
            ContextChip(
                mode = mode,
                selected = mode == selected,
                onClick = { onSelect(mode) },
            )
        }
    }
}

@Composable
private fun ContextChip(
    mode: ContextMode,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = AppTheme.colors
    val shape = RoundedCornerShape(DesignTokens.Radius.Md)

    // Matches the reference: selected chip is solid coral with white text for
    // every mode (Casual/Formal/Tourist), so the active mode is always obvious.
    // Unselected chips are muted. No hardcoded brand colors.
    val (bg, fg) = if (selected) {
        colors.primary to colors.onPrimary
    } else {
        colors.muted to colors.textSecondary
    }

    val borderColor = Color.Transparent

    Row(
        modifier = Modifier
            .clip(shape)
            .background(bg)
            .border(width = 1.dp, color = borderColor, shape = shape)
            .clickable(onClick = onClick)
            .padding(horizontal = DesignTokens.Spacing.S3, vertical = DesignTokens.Spacing.S2),
    ) {
        Text(
            text = mode.label,
            color = fg,
            fontSize = DesignTokens.Typography.Size.Xs,
            fontWeight = FontWeight.Medium,
        )
    }
}
