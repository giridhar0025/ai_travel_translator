import SwiftUI
import UIKit

public extension Color {
    static let appBackground      = dyn(light: DesignTokens.Neutral.background,       dark: DesignTokens.Dark.background)
    static let appForeground      = dyn(light: DesignTokens.Neutral.foreground,       dark: DesignTokens.Dark.foreground)
    static let appCard            = dyn(light: DesignTokens.Semantic.card,            dark: DesignTokens.Dark.card)
    static let appCardForeground  = dyn(light: DesignTokens.Semantic.cardForeground,  dark: DesignTokens.Dark.cardForeground)
    static let appMuted           = dyn(light: DesignTokens.Semantic.muted,           dark: DesignTokens.Dark.muted)
    static let appMutedForeground = dyn(light: DesignTokens.Semantic.mutedForeground, dark: DesignTokens.Dark.mutedForeground)
    static let appBorder          = dyn(light: DesignTokens.Neutral.border,           dark: DesignTokens.Dark.border)
    static let appInputBg         = dyn(light: DesignTokens.Neutral.inputBg,          dark: DesignTokens.Dark.input)
    static let appTextSecondary   = dyn(light: DesignTokens.Neutral.textSecondary,    dark: DesignTokens.Dark.mutedForeground)
    static let appDestructive     = dyn(light: DesignTokens.Semantic.destructive,     dark: DesignTokens.Dark.destructive)

    static let appPrimary         = DesignTokens.Brand.coral
    static let appPrimaryDark     = DesignTokens.Brand.coralDark
    static let appPrimaryLight    = DesignTokens.Brand.coralLight
    static let appAccent          = DesignTokens.Brand.amber
    static let appSuccess         = DesignTokens.Brand.successGreen
    static let appOnPrimary       = Color.white
    static let appRing            = DesignTokens.Brand.coral
}

private func dyn(light: Color, dark: Color) -> Color {
    Color(uiColor: UIColor { tc in
        tc.userInterfaceStyle == .dark ? UIColor(dark) : UIColor(light)
    })
}

public extension Font {
    static let appXs   = Font.system(size: DesignTokens.Typography.Size.xs)
    static let appSm   = Font.system(size: DesignTokens.Typography.Size.sm)
    static let appBase = Font.system(size: DesignTokens.Typography.Size.base)
    static let appLg   = Font.system(size: DesignTokens.Typography.Size.lg)
    static let appXl   = Font.system(size: DesignTokens.Typography.Size.xl)
    static let appXxl  = Font.system(size: DesignTokens.Typography.Size.xxl)

    static let appTitle      = Font.system(size: DesignTokens.Typography.Size.xxl, weight: .semibold)
    static let appHeading    = Font.system(size: DesignTokens.Typography.Size.xl,  weight: .semibold)
    static let appSubheading = Font.system(size: DesignTokens.Typography.Size.lg,  weight: .medium)
    static let appBody       = Font.system(size: DesignTokens.Typography.Size.base, weight: .regular)
    static let appLabel      = Font.system(size: DesignTokens.Typography.Size.sm,  weight: .medium)
    static let appCaption    = Font.system(size: DesignTokens.Typography.Size.xs,  weight: .regular)
}
