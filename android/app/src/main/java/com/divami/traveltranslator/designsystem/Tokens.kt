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
        val Primary = Color(0xFFC2410C)
        val PrimaryDark = Color(0xFF9A3412)
        val PrimaryForeground = Color(0xFFFFFFFF)
        val Secondary = Color(0xFFFFF0ED)
        val SecondaryForeground = Color(0xFF9A3412)
        val Accent = Color(0xFFB45309)
        val AccentForeground = Color(0xFFFFFFFF)
        val Success = Color(0xFF15803D)
        val SuccessForeground = Color(0xFFFFFFFF)
        val Destructive = Color(0xFFB91C1C)
        val DestructiveForeground = Color(0xFFFFFFFF)
        val Card = Color(0xFFFFFFFF)
        val CardForeground = Color(0xFF111827)
        val Popover = Color(0xFFFFFFFF)
        val PopoverForeground = Color(0xFF111827)
        val Muted = Color(0xFFF1F5F9)
        val MutedForeground = Color(0xFF475569)
        val Ring = Color(0xFFC2410C)
    }

    object Neutral {
        val Background = Color(0xFFF8FAFC)
        val Foreground = Color(0xFF111827)
        val TextSecondary = Color(0xFF475569)
        val Input = Color.Transparent
        val InputBg = Color(0xFFF3F3F5)
        val SwitchBg = Color(0xFFCBD5E1)
        val Border = Color(0xFFE2E8F0)
        val BorderStrong = Color(0xFF64748B)
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
        val Background = Color(0xFF121212)
        val Foreground = Color(0xFFF8FAFC)
        val Card = Color(0xFF1E1E1E)
        val CardForeground = Color(0xFFF8FAFC)
        val Popover = Color(0xFF1E1E1E)
        val PopoverForeground = Color(0xFFF8FAFC)
        val Primary = Color(0xFFFF8A70)
        val PrimaryDark = Color(0xFFFF6B4A)
        val PrimaryForeground = Color(0xFF2B0B05)
        val Secondary = Color(0xFF3A211C)
        val SecondaryForeground = Color(0xFFFFB4A2)
        val Muted = Color(0xFF262626)
        val MutedForeground = Color(0xFFCBD5E1)
        val Accent = Color(0xFFFCD34D)
        val AccentForeground = Color(0xFF2B1A00)
        val Success = Color(0xFF86EFAC)
        val SuccessForeground = Color(0xFF052E16)
        val Destructive = Color(0xFFFF8A80)
        val DestructiveForeground = Color(0xFF2B0B05)
        val Border = Color(0xFF3F3F46)
        val BorderStrong = Color(0xFF94A3B8)
        val Input = Color(0xFF2A2A2A)
        val Ring = Color(0xFFFF8A70)
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
