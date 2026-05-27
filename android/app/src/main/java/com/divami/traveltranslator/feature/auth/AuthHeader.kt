package com.divami.traveltranslator.feature.auth

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.divami.traveltranslator.designsystem.AppTheme
import com.divami.traveltranslator.designsystem.DesignTokens

@Composable
fun AuthHeader(
    onBack: () -> Unit,
    title: String,
    subtitle: String? = null,
) {
    val colors = AppTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = DesignTokens.Spacing.S4)
            .padding(top = DesignTokens.Spacing.S2, bottom = DesignTokens.Spacing.S8),
    ) {
        // #32 — 48 dp tap target: outer Box is 48×48 dp, 24 dp icon centred inside.
        Box(
            modifier = Modifier
                .size(DesignTokens.TouchTarget.Min)
                .clip(RoundedCornerShape(DesignTokens.Radius.Md))
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = "Back",
                tint = colors.foreground,
                modifier = Modifier.size(24.dp),
            )
        }
        Spacer(Modifier.height(DesignTokens.Spacing.S6))
        Column(modifier = Modifier.padding(horizontal = DesignTokens.Spacing.S2)) {
            Text(
                text = title,
                color = colors.foreground,
                fontSize = DesignTokens.Typography.Size.Xxl,
                fontWeight = FontWeight.SemiBold,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    color = colors.textSecondary,
                    fontSize = DesignTokens.Typography.Size.Base,
                    modifier = Modifier.padding(top = DesignTokens.Spacing.S2),
                )
            }
        }
    }
}
