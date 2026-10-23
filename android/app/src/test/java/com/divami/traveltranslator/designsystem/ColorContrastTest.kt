package com.divami.traveltranslator.designsystem

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Locks the accessible semantic palette in place. Every foreground token used
 * for body text must clear 4.5:1; UI boundaries and icons must clear 3:1.
 */
class ColorContrastTest {

    @Test
    fun lightPrimaryActionPassesAa() {
        assertAa(DesignTokens.Semantic.PrimaryForeground, DesignTokens.Semantic.Primary)
    }

    @Test
    fun lightPrimaryTextPassesAaOnPageAndTint() {
        assertAa(DesignTokens.Semantic.Primary, DesignTokens.Neutral.Background)
        assertAa(DesignTokens.Semantic.Primary, DesignTokens.Semantic.Secondary)
    }

    @Test
    fun lightSecondaryTextPassesAa() {
        assertAa(DesignTokens.Neutral.TextSecondary, DesignTokens.Neutral.Background)
        assertAa(DesignTokens.Semantic.MutedForeground, DesignTokens.Semantic.Card)
    }

    @Test
    fun lightAccentAndSuccessPassAa() {
        assertAa(DesignTokens.Semantic.Accent, DesignTokens.Semantic.Card)
        assertAa(DesignTokens.Semantic.Success, DesignTokens.Semantic.Card)
        assertAa(DesignTokens.Semantic.Destructive, DesignTokens.Semantic.Card)
    }

    @Test
    fun lightControlBoundariesPassNonTextContrast() {
        assertUi(DesignTokens.Neutral.BorderStrong, DesignTokens.Neutral.Background)
    }

    @Test
    fun darkTextAndActionsPassAa() {
        assertAa(DesignTokens.Dark.Foreground, DesignTokens.Dark.Background)
        assertAa(DesignTokens.Dark.Primary, DesignTokens.Dark.Background)
        assertAa(DesignTokens.Dark.PrimaryForeground, DesignTokens.Dark.Primary)
        assertAa(DesignTokens.Dark.MutedForeground, DesignTokens.Dark.Background)
    }

    @Test
    fun darkControlBoundariesPassNonTextContrast() {
        assertUi(DesignTokens.Dark.BorderStrong, DesignTokens.Dark.Background)
    }

    private fun assertAa(foreground: Color, background: Color) {
        val ratio = contrastRatio(foreground, background)
        assertTrue("Expected >= 4.5:1 but was %.2f:1".format(ratio), ratio >= 4.5)
    }

    private fun assertUi(foreground: Color, background: Color) {
        val ratio = contrastRatio(foreground, background)
        assertTrue("Expected >= 3.0:1 but was %.2f:1".format(ratio), ratio >= 3.0)
    }

    private fun contrastRatio(a: Color, b: Color): Double {
        val la = a.relativeLuminance()
        val lb = b.relativeLuminance()
        val lighter = maxOf(la, lb)
        val darker = minOf(la, lb)
        return (lighter + 0.05) / (darker + 0.05)
    }

    private fun Color.relativeLuminance(): Double {
        fun channel(c: Float): Double {
            val v = c.toDouble()
            return if (v <= 0.04045) v / 12.92 else Math.pow((v + 0.055) / 1.055, 2.4)
        }
        return 0.2126 * channel(red) + 0.7152 * channel(green) + 0.0722 * channel(blue)
    }
}
