import SwiftUI

public enum CoralButtonVariant {
    case primary
    case secondary
    case text
}

public struct CoralButton: View {
    private let title: String
    private let variant: CoralButtonVariant
    private let isLoading: Bool
    private let leadingIcon: String?
    private let action: () -> Void

    @Environment(\.isEnabled) private var isEnabled
    @State private var isPressed = false

    public init(
        _ title: String,
        variant: CoralButtonVariant = .primary,
        isLoading: Bool = false,
        leadingIcon: String? = nil,
        action: @escaping () -> Void
    ) {
        self.title = title
        self.variant = variant
        self.isLoading = isLoading
        self.leadingIcon = leadingIcon
        self.action = action
    }

    public var body: some View {
        Button {
            guard !isLoading else { return }
            action()
        } label: {
            HStack(spacing: DesignTokens.Spacing.s2) {
                if isLoading {
                    ProgressView()
                        .progressViewStyle(.circular)
                        .tint(foregroundColor)
                } else if let leadingIcon {
                    Image(systemName: leadingIcon)
                        .font(.system(size: DesignTokens.Typography.Size.lg, weight: .medium))
                }
                Text(title)
                    .font(.system(size: DesignTokens.Typography.Size.base, weight: .medium))
            }
            .foregroundStyle(foregroundColor)
            .frame(maxWidth: variant == .text ? nil : .infinity)
            .padding(.horizontal, variant == .text ? DesignTokens.Spacing.s3 : DesignTokens.Spacing.s6)
            .padding(.vertical, variant == .text ? DesignTokens.Spacing.s3 : DesignTokens.Spacing.s4)
            .background(backgroundView)
            .overlay(borderOverlay)
            .clipShape(RoundedRectangle(cornerRadius: DesignTokens.Radius.lg, style: .continuous))
            .scaleEffect(isPressed && isEnabled && !isLoading ? DesignTokens.Motion.PressScale.primary : 1)
            .animation(.easeOut(duration: DesignTokens.Motion.Duration.transition), value: isPressed)
        }
        .buttonStyle(.plain)
        .simultaneousGesture(
            DragGesture(minimumDistance: 0)
                .onChanged { _ in if isEnabled && !isLoading { isPressed = true } }
                .onEnded   { _ in isPressed = false }
        )
        .accessibilityLabel(Text(title))
        .accessibilityAddTraits(.isButton)
        .accessibilityValue(Text(isLoading ? "Loading" : ""))
    }

    @ViewBuilder
    private var backgroundView: some View {
        switch variant {
        case .primary:
            isEnabled ? Color.appPrimary : Color.appInputBg
        case .secondary:
            Color.appCard
        case .text:
            Color.clear
        }
    }

    @ViewBuilder
    private var borderOverlay: some View {
        if variant == .secondary || (variant == .primary && !isEnabled) {
            RoundedRectangle(cornerRadius: DesignTokens.Radius.lg, style: .continuous)
                .stroke(Color.appBorder, lineWidth: 1)
        }
    }

    private var foregroundColor: Color {
        switch variant {
        case .primary:   return isEnabled ? .appOnPrimary    : .appTextSecondary
        case .secondary: return isEnabled ? .appForeground   : .appTextSecondary
        case .text:      return .appTextSecondary
        }
    }
}

#Preview {
    VStack(spacing: DesignTokens.Spacing.s3) {
        CoralButton("Sign In") {}
        CoralButton("Continue with Email", leadingIcon: "envelope.fill") {}
        CoralButton("Continue with Google", variant: .secondary, leadingIcon: "globe") {}
        CoralButton("Loading", isLoading: true) {}
        CoralButton("Disabled") {}.disabled(true)
        CoralButton("Skip for now", variant: .text) {}
    }
    .padding()
    .background(Color.appBackground)
}
