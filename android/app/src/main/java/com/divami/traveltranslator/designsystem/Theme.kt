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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.text.googlefonts.Font as GoogleFontFont
import com.divami.traveltranslator.R

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

private val GoogleFontsProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs,
)

private val InterGoogleFont = GoogleFont("Inter")

private val InterFontFamily = FontFamily(
    GoogleFontFont(googleFont = InterGoogleFont, fontProvider = GoogleFontsProvider, weight = FontWeight.Normal),
    GoogleFontFont(googleFont = InterGoogleFont, fontProvider = GoogleFontsProvider, weight = FontWeight.Medium),
    GoogleFontFont(googleFont = InterGoogleFont, fontProvider = GoogleFontsProvider, weight = FontWeight.SemiBold),
    GoogleFontFont(googleFont = InterGoogleFont, fontProvider = GoogleFontsProvider, weight = FontWeight.Bold),
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
    displayLarge   = TextStyle(fontFamily = InterFontFamily, fontSize = DesignTokens.Typography.Size.Xxl, fontWeight = FontWeight.SemiBold),
    headlineLarge  = TextStyle(fontFamily = InterFontFamily, fontSize = DesignTokens.Typography.Size.Xl,  fontWeight = FontWeight.SemiBold),
    titleLarge     = TextStyle(fontFamily = InterFontFamily, fontSize = DesignTokens.Typography.Size.Lg,  fontWeight = FontWeight.Medium),
    bodyLarge      = TextStyle(fontFamily = InterFontFamily, fontSize = DesignTokens.Typography.Size.Base, fontWeight = FontWeight.Normal),
    bodyMedium     = TextStyle(fontFamily = InterFontFamily, fontSize = DesignTokens.Typography.Size.Sm,  fontWeight = FontWeight.Normal),
    labelMedium    = TextStyle(fontFamily = InterFontFamily, fontSize = DesignTokens.Typography.Size.Sm,  fontWeight = FontWeight.Medium),
    labelSmall     = TextStyle(fontFamily = InterFontFamily, fontSize = DesignTokens.Typography.Size.Xs,  fontWeight = FontWeight.Normal),
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
        ) {
            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.material3.LocalTextStyle provides androidx.compose.material3.LocalTextStyle.current.copy(fontFamily = InterFontFamily),
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
