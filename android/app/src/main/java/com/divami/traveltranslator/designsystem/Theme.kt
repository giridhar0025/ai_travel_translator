package com.divami.traveltranslator.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

@Immutable
data class AppColors(
    val background: Color,
    val foreground: Color,
    val card: Color,
    val cardForeground: Color,
    val muted: Color,
    val mutedForeground: Color,
    val border: Color,
    /** Strong boundary for inputs/controls: must reach 3:1 against its surface. */
    val borderStrong: Color,
    val inputBg: Color,
    val textSecondary: Color,
    val destructive: Color,
    val primary: Color,
    val primaryDark: Color,
    val primaryLight: Color,
    val accent: Color,
    val success: Color,
    val onPrimary: Color,
    val onSuccess: Color,
    val ring: Color,
)

// The platform font is deliberate: it is available offline on first launch and
// has script fallbacks for Devanagari, Telugu, Thai, Arabic and CJK.
private val AppFontFamily = FontFamily.Default

private val LightAppColors = AppColors(
    background = DesignTokens.Neutral.Background,
    foreground = DesignTokens.Neutral.Foreground,
    card = DesignTokens.Semantic.Card,
    cardForeground = DesignTokens.Semantic.CardForeground,
    muted = DesignTokens.Semantic.Muted,
    mutedForeground = DesignTokens.Semantic.MutedForeground,
    border = DesignTokens.Neutral.Border,
    borderStrong = DesignTokens.Neutral.BorderStrong,
    inputBg = DesignTokens.Neutral.InputBg,
    textSecondary = DesignTokens.Neutral.TextSecondary,
    destructive = DesignTokens.Semantic.Destructive,
    primary = DesignTokens.Semantic.Primary,
    primaryDark = DesignTokens.Semantic.PrimaryDark,
    primaryLight = DesignTokens.Semantic.Secondary,
    accent = DesignTokens.Semantic.Accent,
    success = DesignTokens.Semantic.Success,
    onPrimary = DesignTokens.Semantic.PrimaryForeground,
    onSuccess = DesignTokens.Semantic.SuccessForeground,
    ring = DesignTokens.Semantic.Ring,
)

private val DarkAppColors = AppColors(
    background = DesignTokens.Dark.Background,
    foreground = DesignTokens.Dark.Foreground,
    card = DesignTokens.Dark.Card,
    cardForeground = DesignTokens.Dark.CardForeground,
    muted = DesignTokens.Dark.Muted,
    mutedForeground = DesignTokens.Dark.MutedForeground,
    border = DesignTokens.Dark.Border,
    borderStrong = DesignTokens.Dark.BorderStrong,
    inputBg = DesignTokens.Dark.Input,
    textSecondary = DesignTokens.Dark.MutedForeground,
    destructive = DesignTokens.Dark.Destructive,
    primary = DesignTokens.Dark.Primary,
    primaryDark = DesignTokens.Dark.PrimaryDark,
    primaryLight = DesignTokens.Dark.Secondary,
    accent = DesignTokens.Dark.Accent,
    success = DesignTokens.Dark.Success,
    onPrimary = DesignTokens.Dark.PrimaryForeground,
    onSuccess = DesignTokens.Dark.SuccessForeground,
    ring = DesignTokens.Dark.Ring,
)

val LocalAppColors = staticCompositionLocalOf { LightAppColors }

private fun appTextStyle(
    size: TextUnit,
    weight: FontWeight,
): TextStyle = TextStyle(
    fontFamily = AppFontFamily,
    fontSize = size,
    fontWeight = weight,
    lineHeight = (size.value * 1.45f).sp,
)

private val AppTypography = Typography(
    displayLarge = appTextStyle(DesignTokens.Typography.Size.Xxl, FontWeight.SemiBold),
    headlineLarge = appTextStyle(DesignTokens.Typography.Size.Xl, FontWeight.SemiBold),
    titleLarge = appTextStyle(DesignTokens.Typography.Size.Lg, FontWeight.Medium),
    bodyLarge = appTextStyle(DesignTokens.Typography.Size.Base, FontWeight.Normal),
    bodyMedium = appTextStyle(DesignTokens.Typography.Size.Sm, FontWeight.Normal),
    labelMedium = appTextStyle(DesignTokens.Typography.Size.Sm, FontWeight.Medium),
    labelSmall = appTextStyle(DesignTokens.Typography.Size.Xs, FontWeight.Normal),
)

private fun AppColors.toMaterialColorScheme(dark: Boolean) = if (dark) {
    darkColorScheme(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = primaryLight,
        onPrimaryContainer = primaryDark,
        background = background,
        onBackground = foreground,
        surface = card,
        onSurface = cardForeground,
        surfaceVariant = muted,
        onSurfaceVariant = mutedForeground,
        outline = borderStrong,
        error = destructive,
        onError = onPrimary,
    )
} else {
    lightColorScheme(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = primaryLight,
        onPrimaryContainer = primaryDark,
        background = background,
        onBackground = foreground,
        surface = card,
        onSurface = cardForeground,
        surfaceVariant = muted,
        onSurfaceVariant = mutedForeground,
        outline = borderStrong,
        error = destructive,
        onError = onPrimary,
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
        ) {
            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.material3.LocalTextStyle provides androidx.compose.material3.LocalTextStyle.current.copy(
                    fontFamily = AppFontFamily,
                ),
                content = content,
            )
        }
    }
}

object AppTheme {
    val colors: AppColors
        @Composable @ReadOnlyComposable
        get() = LocalAppColors.current
}
