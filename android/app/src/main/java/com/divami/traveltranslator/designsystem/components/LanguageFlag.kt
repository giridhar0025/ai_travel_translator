package com.divami.traveltranslator.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.divami.traveltranslator.designsystem.AppTheme
import com.divami.traveltranslator.designsystem.DesignTokens

data class AppLanguage(
    val code: String,
    val displayName: String,
    val flag: String,
    val nativeName: String = displayName,
) {
    /** Search both the English name and the name a traveler recognises. */
    val searchableText: String get() = "$displayName $nativeName $code"

    /** Prefer the native name in pickers; fall back to English when identical. */
    val pickerLabel: String get() = if (nativeName == displayName) displayName else "$nativeName · $displayName"

    companion object {
        // Must stay in sync with the backend's translation.LANGUAGE_NAMES.
        // `flag` is a native-script (or language-distinctive) monogram.
        val English    = AppLanguage("en", "English",    "A",  "English")
        val Hindi      = AppLanguage("hi", "Hindi",      "ह",  "हिन्दी")
        val Telugu     = AppLanguage("te", "Telugu",     "తె", "తెలుగు")
        val Tamil      = AppLanguage("ta", "Tamil",      "த",  "தமிழ்")
        val Bengali    = AppLanguage("bn", "Bengali",    "ব",  "বাংলা")
        val Marathi    = AppLanguage("mr", "Marathi",    "म",  "मराठी")
        val Thai       = AppLanguage("th", "Thai",       "ก",  "ไทย")
        val Spanish    = AppLanguage("es", "Spanish",    "Ñ",  "Español")
        val French     = AppLanguage("fr", "French",     "Ç",  "Français")
        val German     = AppLanguage("de", "German",     "ß",  "Deutsch")
        val Italian    = AppLanguage("it", "Italian",    "È",  "Italiano")
        val Portuguese = AppLanguage("pt", "Portuguese", "Ã",  "Português")
        val Japanese   = AppLanguage("ja", "Japanese",   "あ",  "日本語")
        val Korean     = AppLanguage("ko", "Korean",     "한",  "한국어")
        val Chinese    = AppLanguage("zh", "Chinese",    "中",  "中文")
        val Arabic     = AppLanguage("ar", "Arabic",     "ع",  "العربية")
        val Russian    = AppLanguage("ru", "Russian",    "Я",  "Русский")
        val Vietnamese = AppLanguage("vi", "Vietnamese", "Đ",  "Tiếng Việt")
        val Indonesian = AppLanguage("id", "Indonesian", "In", "Bahasa Indonesia")
        val Turkish    = AppLanguage("tr", "Turkish",    "Ğ",  "Türkçe")

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
                .defaultMinSize(minWidth = 28.dp, minHeight = 28.dp)
                .clearAndSetSemantics { }
                .background(
                    color = colors.primaryLight,
                    shape = RoundedCornerShape(DesignTokens.Radius.Sm),
                )
                .padding(horizontal = 4.dp, vertical = 2.dp),
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
