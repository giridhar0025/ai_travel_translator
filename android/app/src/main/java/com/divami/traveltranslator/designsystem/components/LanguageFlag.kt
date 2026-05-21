package com.divami.traveltranslator.designsystem.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.TextUnit
import com.divami.traveltranslator.designsystem.AppTheme
import com.divami.traveltranslator.designsystem.DesignTokens

data class AppLanguage(val code: String, val displayName: String, val flag: String) {
    companion object {
        val English = AppLanguage("en", "English", "🇬🇧")
        val Hindi   = AppLanguage("hi", "Hindi",   "🇮🇳")
        val Telugu  = AppLanguage("te", "Telugu",  "🇮🇳")
        val Thai    = AppLanguage("th", "Thai",    "🇹🇭")
        val Supported = listOf(English, Hindi, Telugu, Thai)
    }
}

@Composable
fun LanguageFlag(
    language: AppLanguage,
    modifier: Modifier = Modifier,
    showName: Boolean = true,
    flagSize: TextUnit = DesignTokens.Typography.Size.Xxl,
) {
    val colors = AppTheme.colors
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.S2),
    ) {
        Text(text = language.flag, fontSize = flagSize)
        if (showName) {
            Text(
                text = language.displayName,
                color = colors.foreground,
                fontSize = DesignTokens.Typography.Size.Base,
            )
        }
    }
}
