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
        // Must stay in sync with the backend's translation.LANGUAGE_NAMES.
        // `flag` is a native-script (or language-distinctive) monogram.
        val English    = AppLanguage("en", "English",    "A")
        val Hindi      = AppLanguage("hi", "Hindi",      "ह")
        val Telugu     = AppLanguage("te", "Telugu",     "తె")
        val Tamil      = AppLanguage("ta", "Tamil",      "த")
        val Bengali    = AppLanguage("bn", "Bengali",    "ব")
        val Marathi    = AppLanguage("mr", "Marathi",    "म")
        val Thai       = AppLanguage("th", "Thai",       "ก")
        val Spanish    = AppLanguage("es", "Spanish",    "Ñ")
        val French     = AppLanguage("fr", "French",     "Ç")
        val German     = AppLanguage("de", "German",     "ß")
        val Italian    = AppLanguage("it", "Italian",    "È")
        val Portuguese = AppLanguage("pt", "Portuguese", "Ã")
        val Japanese   = AppLanguage("ja", "Japanese",   "あ")
        val Korean     = AppLanguage("ko", "Korean",     "한")
        val Chinese    = AppLanguage("zh", "Chinese",    "中")
        val Arabic     = AppLanguage("ar", "Arabic",     "ع")
        val Russian    = AppLanguage("ru", "Russian",    "Я")
        val Vietnamese = AppLanguage("vi", "Vietnamese", "Đ")
        val Indonesian = AppLanguage("id", "Indonesian", "In")
        val Turkish    = AppLanguage("tr", "Turkish",    "Ğ")

        val Supported = listOf(
            English, Hindi, Telugu, Tamil, Bengali, Marathi, Thai,
            Spanish, French, German, Italian, Portuguese,
            Japanese, Korean, Chinese, Arabic, Russian,
            Vietnamese, Indonesian, Turkish,
        )

        fun fromCode(code: String): AppLanguage? = Supported.firstOrNull { it.code == code }
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
