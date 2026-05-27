package com.divami.traveltranslator.feature.welcome

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.divami.traveltranslator.designsystem.AppTheme
import com.divami.traveltranslator.designsystem.DesignTokens
import com.divami.traveltranslator.designsystem.components.CoralButton

@Composable
fun WelcomeScreen(onStart: () -> Unit) {
    val systemInsets: PaddingValues = WindowInsets.systemBars.asPaddingValues()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppTheme.colors.background)
            .padding(systemInsets),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = DesignTokens.Spacing.S6, vertical = DesignTokens.Spacing.S6),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.weight(1f))

            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(DesignTokens.Radius.Modal))
                    .background(DesignTokens.Brand.Coral),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Translate,
                    contentDescription = "Travel Translator AI",
                    tint = Color.White,
                    modifier = Modifier.size(48.dp),
                )
            }

            Text(
                text = "Travel Translator AI",
                color = AppTheme.colors.foreground,
                fontSize = 32.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = DesignTokens.Spacing.S6),
            )
            Text(
                text = "Speak any language. Anywhere.",
                color = AppTheme.colors.textSecondary,
                fontSize = DesignTokens.Typography.Size.Lg,
                modifier = Modifier.padding(top = DesignTokens.Spacing.S2),
            )

            Spacer(modifier = Modifier.weight(1f))

            CoralButton(
                text = "Get Started",
                onClick = onStart,
                trailingIcon = Icons.AutoMirrored.Outlined.ArrowForward,
                modifier = Modifier.padding(bottom = DesignTokens.Spacing.S6),
            )
        }
    }
}
