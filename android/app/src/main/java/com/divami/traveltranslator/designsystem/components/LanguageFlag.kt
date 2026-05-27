package com.divami.traveltranslator.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.divami.traveltranslator.designsystem.AppTheme
import com.divami.traveltranslator.designsystem.DesignTokens

data class AppLanguage(val code: String, val displayName: String, val flag: String) {
    companion object {
        val English = AppLanguage("en", "English", "A")   // Latin monogram
        val Hindi   = AppLanguage("hi", "Hindi",   "ह")   // Devanagari monogram
        val Telugu  = AppLanguage("te", "Telugu",  "తె")  // Telugu script monogram
        val Thai    = AppLanguage("th", "Thai",    "ก")   // Thai script monogram
        val Supported = listOf(English, Hindi, Telugu, Thai)
    }
}

/**
 * Renders a language identity chip (native-script monogram) with an optional display name.
 *
 * @param language   The language to display.
 * @param modifier   Modifier applied to the outer Row (or the chip alone when [compact] is true).
 * @param showName   Legacy alias — when false, behaves like compact = true.
 * @param compact    When true, renders only the monogram chip without the display name.
 */
@Composable
fun LanguageFlag(
    language: AppLanguage,
    modifier: Modifier = Modifier,
    showName: Boolean = true,
    compact: Boolean = false,
) {
    val colors = AppTheme.colors
    val showLabel = showName && !compact

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.S2),
    ) {
        // Monogram chip
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(
                    color = colors.primaryLight,
                    shape = RoundedCornerShape(DesignTokens.Radius.Sm),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = language.flag,
                color = colors.primary,
                fontSize = DesignTokens.Typography.Size.Sm,
                fontWeight = FontWeight.SemiBold,
            )
        }

        if (showLabel) {
            Text(
                text = language.displayName,
                color = colors.foreground,
                fontSize = DesignTokens.Typography.Size.Base,
            )
        }
    }
}
