package com.divami.traveltranslator.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight

@Immutable
data class AppColors(
    val background: Color,
    val foreground: Color,
    val card: Color,
    val cardForeground: Color,
    val muted: Color,
    val mutedForeground: Color,
    val border: Color,
    val inputBg: Color,
    val textSecondary: Color,
    val destructive: Color,
    val primary: Color,
    val primaryDark: Color,
    val primaryLight: Color,
    val accent: Color,
    val success: Color,
    val onPrimary: Color,
    val ring: Color,
)

private val LightAppColors = AppColors(
    background = DesignTokens.Neutral.Background,
    foreground = DesignTokens.Neutral.Foreground,
    card = DesignTokens.Semantic.Card,
    cardForeground = DesignTokens.Semantic.CardForeground,
    muted = DesignTokens.Semantic.Muted,
    mutedForeground = DesignTokens.Semantic.MutedForeground,
    border = DesignTokens.Neutral.Border,
    inputBg = DesignTokens.Neutral.InputBg,
    textSecondary = DesignTokens.Neutral.TextSecondary,
    destructive = DesignTokens.Semantic.Destructive,
    primary = DesignTokens.Brand.Coral,
    primaryDark = DesignTokens.Brand.CoralDark,
    primaryLight = DesignTokens.Brand.CoralLight,
    accent = DesignTokens.Brand.Amber,
    success = DesignTokens.Brand.SuccessGreen,
    onPrimary = Color.White,
    ring = DesignTokens.Brand.Coral,
)

private val DarkAppColors = LightAppColors.copy(
    background = DesignTokens.Dark.Background,
    foreground = DesignTokens.Dark.Foreground,
    card = DesignTokens.Dark.Card,
    cardForeground = DesignTokens.Dark.CardForeground,
    muted = DesignTokens.Dark.Muted,
    mutedForeground = DesignTokens.Dark.MutedForeground,
    border = DesignTokens.Dark.Border,
    inputBg = DesignTokens.Dark.Input,
    textSecondary = DesignTokens.Dark.MutedForeground,
    destructive = DesignTokens.Dark.Destructive,
)

val LocalAppColors = staticCompositionLocalOf { LightAppColors }

private val AppTypography = Typography(
    displayLarge   = TextStyle(fontSize = DesignTokens.Typography.Size.Xxl, fontWeight = FontWeight.SemiBold),
    headlineLarge  = TextStyle(fontSize = DesignTokens.Typography.Size.Xl,  fontWeight = FontWeight.SemiBold),
    titleLarge     = TextStyle(fontSize = DesignTokens.Typography.Size.Lg,  fontWeight = FontWeight.Medium),
    bodyLarge      = TextStyle(fontSize = DesignTokens.Typography.Size.Base, fontWeight = FontWeight.Normal),
    bodyMedium     = TextStyle(fontSize = DesignTokens.Typography.Size.Sm,  fontWeight = FontWeight.Normal),
    labelMedium    = TextStyle(fontSize = DesignTokens.Typography.Size.Sm,  fontWeight = FontWeight.Medium),
    labelSmall     = TextStyle(fontSize = DesignTokens.Typography.Size.Xs,  fontWeight = FontWeight.Normal),
)

private fun AppColors.toMaterialColorScheme(dark: Boolean) = if (dark) {
    darkColorScheme(
        primary = primary, onPrimary = onPrimary,
        background = background, onBackground = foreground,
        surface = card, onSurface = cardForeground,
        surfaceVariant = muted, onSurfaceVariant = mutedForeground,
        error = destructive,
    )
} else {
    lightColorScheme(
        primary = primary, onPrimary = onPrimary,
        background = background, onBackground = foreground,
        surface = card, onSurface = cardForeground,
        surfaceVariant = muted, onSurfaceVariant = mutedForeground,
        error = destructive,
    )
}

@Composable
fun TravelTranslatorTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val appColors = if (darkTheme) DarkAppColors else LightAppColors
    androidx.compose.runtime.CompositionLocalProvider(LocalAppColors provides appColors) {
        MaterialTheme(
            colorScheme = appColors.toMaterialColorScheme(darkTheme),
            typography = AppTypography,
            content = content,
        )
    }
}

object AppTheme {
    val colors: AppColors
        @Composable @ReadOnlyComposable
        get() = LocalAppColors.current
}
