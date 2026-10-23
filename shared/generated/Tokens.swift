// AUTO-GENERATED. Do not edit. Source: shared/design-tokens.json
// Run `node tools/gen-tokens.mjs` to regenerate.

import SwiftUI

public enum DesignTokens {
    public enum Brand {
        public static let coral = Color(argb: 0xFFFF6B4A)
        public static let coralDark = Color(argb: 0xFFE85533)
        public static let coralLight = Color(argb: 0xFFFFF0ED)
        public static let amber = Color(argb: 0xFFF59E0B)
        public static let successGreen = Color(argb: 0xFF22C55E)
        public static let destructive = Color(argb: 0xFFD4183D)
    }

    public enum Semantic {
        public static let primary = Color(argb: 0xFFC2410C)
        public static let primaryDark = Color(argb: 0xFF9A3412)
        public static let primaryForeground = Color(argb: 0xFFFFFFFF)
        public static let secondary = Color(argb: 0xFFFFF0ED)
        public static let secondaryForeground = Color(argb: 0xFF9A3412)
        public static let accent = Color(argb: 0xFFB45309)
        public static let accentForeground = Color(argb: 0xFFFFFFFF)
        public static let success = Color(argb: 0xFF15803D)
        public static let successForeground = Color(argb: 0xFFFFFFFF)
        public static let destructive = Color(argb: 0xFFB91C1C)
        public static let destructiveForeground = Color(argb: 0xFFFFFFFF)
        public static let card = Color(argb: 0xFFFFFFFF)
        public static let cardForeground = Color(argb: 0xFF111827)
        public static let popover = Color(argb: 0xFFFFFFFF)
        public static let popoverForeground = Color(argb: 0xFF111827)
        public static let muted = Color(argb: 0xFFF1F5F9)
        public static let mutedForeground = Color(argb: 0xFF475569)
        public static let ring = Color(argb: 0xFFC2410C)
    }

    public enum Neutral {
        public static let background = Color(argb: 0xFFF8FAFC)
        public static let foreground = Color(argb: 0xFF111827)
        public static let textSecondary = Color(argb: 0xFF475569)
        public static let input = Color.clear
        public static let inputBg = Color(argb: 0xFFF3F3F5)
        public static let switchBg = Color(argb: 0xFFCBD5E1)
        public static let border = Color(argb: 0xFFE2E8F0)
        public static let borderStrong = Color(argb: 0xFF64748B)
    }

    public struct CategoryColors: Sendable {
        public let bg: Color
        public let fg: Color
    }
    public enum Category {
        public static let restaurants = CategoryColors(bg: Color(argb: 0xFFFFEDD5), fg: Color(argb: 0xFFEA580C))
        public static let directions = CategoryColors(bg: Color(argb: 0xFFDBEAFE), fg: Color(argb: 0xFF2563EB))
        public static let transport = CategoryColors(bg: Color(argb: 0xFFDCFCE7), fg: Color(argb: 0xFF16A34A))
        public static let shopping = CategoryColors(bg: Color(argb: 0xFFF3E8FF), fg: Color(argb: 0xFF9333EA))
        public static let emergencies = CategoryColors(bg: Color(argb: 0xFFFEE2E2), fg: Color(argb: 0xFFDC2626))
    }

    public enum Dark {
        public static let background = Color(argb: 0xFF121212)
        public static let foreground = Color(argb: 0xFFF8FAFC)
        public static let card = Color(argb: 0xFF1E1E1E)
        public static let cardForeground = Color(argb: 0xFFF8FAFC)
        public static let popover = Color(argb: 0xFF1E1E1E)
        public static let popoverForeground = Color(argb: 0xFFF8FAFC)
        public static let primary = Color(argb: 0xFFFF8A70)
        public static let primaryDark = Color(argb: 0xFFFF6B4A)
        public static let primaryForeground = Color(argb: 0xFF2B0B05)
        public static let secondary = Color(argb: 0xFF3A211C)
        public static let secondaryForeground = Color(argb: 0xFFFFB4A2)
        public static let muted = Color(argb: 0xFF262626)
        public static let mutedForeground = Color(argb: 0xFFCBD5E1)
        public static let accent = Color(argb: 0xFFFCD34D)
        public static let accentForeground = Color(argb: 0xFF2B1A00)
        public static let success = Color(argb: 0xFF86EFAC)
        public static let successForeground = Color(argb: 0xFF052E16)
        public static let destructive = Color(argb: 0xFFFF8A80)
        public static let destructiveForeground = Color(argb: 0xFF2B0B05)
        public static let border = Color(argb: 0xFF3F3F46)
        public static let borderStrong = Color(argb: 0xFF94A3B8)
        public static let input = Color(argb: 0xFF2A2A2A)
        public static let ring = Color(argb: 0xFFFF8A70)
    }

    public enum Radius {
        public static let sm: CGFloat = 8
        public static let md: CGFloat = 10
        public static let lg: CGFloat = 12
        public static let xl: CGFloat = 16
        public static let modal: CGFloat = 24
    }

    public enum Spacing {
        public static let s1: CGFloat = 4
        public static let s2: CGFloat = 8
        public static let s3: CGFloat = 12
        public static let s4: CGFloat = 16
        public static let s5: CGFloat = 20
        public static let s6: CGFloat = 24
        public static let s8: CGFloat = 32
        public static let s10: CGFloat = 40
    }

    public enum Typography {
        public static let weightNormal: Font.Weight = .regular
        public static let weightMedium: Font.Weight = .medium
        public static let rootFontSize: CGFloat = 16
        public enum Size {
            public static let xs: CGFloat = 12
            public static let sm: CGFloat = 14
            public static let base: CGFloat = 16
            public static let lg: CGFloat = 18
            public static let xl: CGFloat = 20
            public static let xxl: CGFloat = 24
        }
        public static let lineHeight: CGFloat = 1.5
    }

    public enum Motion {
        public enum Easing {
            public static let standard: (Double, Double, Double, Double) = (0.4, 0, 0.2, 1)
            public static let easeOut: (Double, Double, Double, Double) = (0, 0, 0.2, 1)
            public static let pulse: (Double, Double, Double, Double) = (0.4, 0, 0.6, 1)
        }
        public enum Duration {
            public static let transition: TimeInterval = 0.2
            public static let fadeIn: TimeInterval = 0.6
            public static let pulse: TimeInterval = 2
            public static let ping: TimeInterval = 1.5
        }
        public enum PressScale {
            public static let primary: CGFloat = 0.98
            public static let emphatic: CGFloat = 0.95
        }
    }

    public enum TouchTarget {
        public static let min: CGFloat = 44
    }
}

public extension Color {
    init(argb: UInt32) {
        let a = Double((argb >> 24) & 0xFF) / 255
        let r = Double((argb >> 16) & 0xFF) / 255
        let g = Double((argb >>  8) & 0xFF) / 255
        let b = Double( argb        & 0xFF) / 255
        self.init(.sRGB, red: r, green: g, blue: b, opacity: a)
    }
}
