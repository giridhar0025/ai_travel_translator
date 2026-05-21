// AUTO-GENERATED. Do not edit. Source: shared/design-tokens.json
// Run `node tools/gen-tokens.mjs` to regenerate.
package com.divami.traveltranslator.designsystem

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object DesignTokens {
    object Brand {
        val Coral = Color(0xFFFF6B4A)
        val CoralDark = Color(0xFFE85533)
        val CoralLight = Color(0xFFFFF0ED)
        val Amber = Color(0xFFF59E0B)
        val SuccessGreen = Color(0xFF22C55E)
        val Destructive = Color(0xFFD4183D)
    }

    object Semantic {
        val Primary = Color(0xFFFF6B4A)
        val PrimaryForeground = Color(0xFFFFFFFF)
        val Secondary = Color(0xFFFFF0ED)
        val SecondaryForeground = Color(0xFFE85533)
        val Accent = Color(0xFFF59E0B)
        val AccentForeground = Color(0xFFFFFFFF)
        val Destructive = Color(0xFFD4183D)
        val DestructiveForeground = Color(0xFFFFFFFF)
        val Card = Color(0xFFFFFFFF)
        val CardForeground = Color(0xFF111827)
        val Popover = Color(0xFFFFFFFF)
        val PopoverForeground = Color(0xFF111827)
        val Muted = Color(0xFFF8FAFC)
        val MutedForeground = Color(0xFF6B7280)
        val Ring = Color(0xFFFF6B4A)
    }

    object Neutral {
        val Background = Color(0xFFF8FAFC)
        val Foreground = Color(0xFF111827)
        val TextSecondary = Color(0xFF6B7280)
        val Input = Color.Transparent
        val InputBg = Color(0xFFF3F3F5)
        val SwitchBg = Color(0xFFCBCED4)
        val Border = Color(0x1A000000)
    }

    data class CategoryColors(val bg: Color, val fg: Color)
    object Category {
        val Restaurants = CategoryColors(Color(0xFFFFEDD5), Color(0xFFEA580C))
        val Directions = CategoryColors(Color(0xFFDBEAFE), Color(0xFF2563EB))
        val Transport = CategoryColors(Color(0xFFDCFCE7), Color(0xFF16A34A))
        val Shopping = CategoryColors(Color(0xFFF3E8FF), Color(0xFF9333EA))
        val Emergencies = CategoryColors(Color(0xFFFEE2E2), Color(0xFFDC2626))
    }

    object Dark {
        val Background = Color(0xFF252525)
        val Foreground = Color(0xFFFAFAFA)
        val Card = Color(0xFF252525)
        val CardForeground = Color(0xFFFAFAFA)
        val Popover = Color(0xFF252525)
        val PopoverForeground = Color(0xFFFAFAFA)
        val Primary = Color(0xFFFAFAFA)
        val PrimaryForeground = Color(0xFF343434)
        val Secondary = Color(0xFF444444)
        val SecondaryForeground = Color(0xFFFAFAFA)
        val Muted = Color(0xFF444444)
        val MutedForeground = Color(0xFFB5B5B5)
        val Accent = Color(0xFF444444)
        val AccentForeground = Color(0xFFFAFAFA)
        val Destructive = Color(0xFF7F1D1D)
        val DestructiveForeground = Color(0xFFEF4444)
        val Border = Color(0xFF444444)
        val Input = Color(0xFF444444)
        val Ring = Color(0xFF707070)
    }

    object Radius {
        val Sm = 8.dp
        val Md = 10.dp
        val Lg = 12.dp
        val Xl = 16.dp
        val Modal = 24.dp
    }

    object Spacing {
        val S1 = 4.dp
        val S2 = 8.dp
        val S3 = 12.dp
        val S4 = 16.dp
        val S5 = 20.dp
        val S6 = 24.dp
        val S8 = 32.dp
        val S10 = 40.dp
    }

    object Typography {
        const val WeightNormal = 400
        const val WeightMedium = 500
        val RootFontSize = 16.sp
        object Size {
            val Xs = 12.sp
            val Sm = 14.sp
            val Base = 16.sp
            val Lg = 18.sp
            val Xl = 20.sp
            val Xxl = 24.sp
        }
        const val LineHeight = 1.5f
    }

    object Motion {
        object Easing {
            val Standard = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)
            val EaseOut = CubicBezierEasing(0f, 0f, 0.2f, 1f)
            val Pulse = CubicBezierEasing(0.4f, 0f, 0.6f, 1f)
        }
        object Duration {
            const val TransitionMs = 200
            const val FadeInMs = 600
            const val PulseMs = 2000
            const val PingMs = 1500
        }
        object PressScale {
            const val Primary = 0.98f
            const val Emphatic = 0.95f
        }
    }

    object TouchTarget {
        val Min = 48.dp
    }
}
