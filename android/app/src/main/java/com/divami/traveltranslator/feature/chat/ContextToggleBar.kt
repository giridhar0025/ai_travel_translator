package com.divami.traveltranslator.feature.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.divami.traveltranslator.R
import com.divami.traveltranslator.designsystem.AppTheme
import com.divami.traveltranslator.designsystem.DesignTokens
import com.divami.traveltranslator.designsystem.minimumTouchTarget

@Composable
fun ContextToggleBar(
    selected: ContextMode,
    onSelect: (ContextMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.S2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ContextMode.entries.forEach { mode ->
            ContextChip(
                mode = mode,
                selected = mode == selected,
                onClick = { onSelect(mode) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ContextChip(
    mode: ContextMode,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
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

    Row(
        modifier = modifier
            .minimumTouchTarget()
            .heightIn(min = DesignTokens.TouchTarget.Min)
            .clip(shape)
            .background(bg)
            .border(width = 1.dp, color = if (selected) bg else colors.borderStrong, shape = shape)
            .clickable(onClick = onClick)
            .semantics {
                role = Role.RadioButton
                this.selected = selected
            }
            .padding(horizontal = DesignTokens.Spacing.S2, vertical = DesignTokens.Spacing.S2),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = when (mode) {
                ContextMode.Formal -> stringResource(R.string.context_formal)
                ContextMode.Casual -> stringResource(R.string.context_casual)
                ContextMode.Tourist -> stringResource(R.string.context_tourist)
            },
            color = fg,
            fontSize = DesignTokens.Typography.Size.Xs,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
