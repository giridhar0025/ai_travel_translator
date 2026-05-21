package com.divami.traveltranslator.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.divami.traveltranslator.designsystem.AppTheme
import com.divami.traveltranslator.designsystem.DesignTokens

@Composable
fun RadioCard(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null,
) {
    val colors = AppTheme.colors
    val shape = RoundedCornerShape(DesignTokens.Radius.Lg)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (isSelected) colors.primaryLight else colors.card)
            .border(
                width = 2.dp,
                color = if (isSelected) colors.primary else colors.border,
                shape = shape,
            )
            .clickable(onClick = onClick)
            .defaultMinSize(minHeight = DesignTokens.TouchTarget.Min)
            .padding(horizontal = DesignTokens.Spacing.S6, vertical = DesignTokens.Spacing.S4),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.S3),
    ) {
        if (leading != null) leading()
        Text(
            text = title,
            color = if (isSelected) colors.primary else colors.foreground,
            fontSize = DesignTokens.Typography.Size.Base,
            modifier = Modifier.weight(1f),
        )
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(if (isSelected) colors.primary else androidx.compose.ui.graphics.Color.Transparent)
                .border(width = 2.dp, color = if (isSelected) colors.primary else colors.border, shape = CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(colors.onPrimary),
                )
            }
        }
    }
}
