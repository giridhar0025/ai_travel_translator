import SwiftUI

public struct CoralCard<Content: View>: View {
    private let padding: CGFloat
    private let cornerRadius: CGFloat
    private let isInteractive: Bool
    private let onTap: (() -> Void)?
    private let content: Content

    @State private var isPressed = false

    public init(
        padding: CGFloat = DesignTokens.Spacing.s4,
        cornerRadius: CGFloat = DesignTokens.Radius.lg,
        isInteractive: Bool = false,
        onTap: (() -> Void)? = nil,
        @ViewBuilder content: () -> Content
    ) {
        self.padding = padding
        self.cornerRadius = cornerRadius
        self.isInteractive = isInteractive || onTap != nil
        self.onTap = onTap
        self.content = content()
    }

    public var body: some View {
        let card = content
            .padding(padding)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(Color.appCard)
            .overlay(
                RoundedRectangle(cornerRadius: cornerRadius, style: .continuous)
                    .stroke(Color.appBorder, lineWidth: 1)
            )
            .clipShape(RoundedRectangle(cornerRadius: cornerRadius, style: .continuous))
            .shadow(color: Color.black.opacity(0.06), radius: 6, x: 0, y: 2)
            .scaleEffect(isPressed && isInteractive ? DesignTokens.Motion.PressScale.primary : 1)
            .animation(.easeOut(duration: DesignTokens.Motion.Duration.transition), value: isPressed)

        if let onTap {
            Button(action: onTap) { card }
                .buttonStyle(.plain)
                .simultaneousGesture(
                    DragGesture(minimumDistance: 0)
                        .onChanged { _ in isPressed = true }
                        .onEnded   { _ in isPressed = false }
                )
        } else {
            card
        }
    }
}

#Preview {
    VStack(spacing: DesignTokens.Spacing.s4) {
        CoralCard {
            VStack(alignment: .leading, spacing: DesignTokens.Spacing.s2) {
                Text("Static card").font(.appSubheading)
                Text("Used as a surface around content.")
                    .font(.appBody)
                    .foregroundStyle(Color.appTextSecondary)
            }
        }
        CoralCard(onTap: {}) {
            HStack {
                Text("Tappable card").font(.appBody)
                Spacer()
                Image(systemName: "chevron.right").foregroundStyle(Color.appTextSecondary)
            }
        }
    }
    .padding()
    .background(Color.appBackground)
}
