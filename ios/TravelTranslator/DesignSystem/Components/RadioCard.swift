import SwiftUI

public struct RadioCard<Leading: View>: View {
    private let title: String
    private let isSelected: Bool
    private let onTap: () -> Void
    private let leading: Leading

    public init(
        title: String,
        isSelected: Bool,
        @ViewBuilder leading: () -> Leading = { EmptyView() },
        onTap: @escaping () -> Void
    ) {
        self.title = title
        self.isSelected = isSelected
        self.onTap = onTap
        self.leading = leading()
    }

    public var body: some View {
        Button(action: onTap) {
            HStack(spacing: DesignTokens.Spacing.s3) {
                leading
                Text(title)
                    .font(.appBody)
                    .foregroundStyle(isSelected ? Color.appPrimary : Color.appForeground)
                Spacer()
                ZStack {
                    Circle()
                        .stroke(isSelected ? Color.appPrimary : Color.appBorder, lineWidth: 2)
                        .frame(width: 20, height: 20)
                    if isSelected {
                        Circle()
                            .fill(Color.appPrimary)
                            .frame(width: 20, height: 20)
                        Circle()
                            .fill(Color.white)
                            .frame(width: 8, height: 8)
                    }
                }
            }
            .padding(.horizontal, DesignTokens.Spacing.s6)
            .padding(.vertical, DesignTokens.Spacing.s4)
            .frame(maxWidth: .infinity, minHeight: DesignTokens.TouchTarget.min)
            .background(isSelected ? Color.appPrimaryLight : Color.appCard)
            .overlay(
                RoundedRectangle(cornerRadius: DesignTokens.Radius.lg, style: .continuous)
                    .stroke(isSelected ? Color.appPrimary : Color.appBorder, lineWidth: 2)
            )
            .clipShape(RoundedRectangle(cornerRadius: DesignTokens.Radius.lg, style: .continuous))
            .animation(.easeOut(duration: DesignTokens.Motion.Duration.transition), value: isSelected)
        }
        .buttonStyle(.plain)
    }
}

#Preview {
    VStack(spacing: DesignTokens.Spacing.s3) {
        RadioCard(title: "🇬🇧 English", isSelected: true) {}
        RadioCard(title: "🇮🇳 Hindi",   isSelected: false) {}
        RadioCard(title: "🇮🇳 Telugu",  isSelected: false) {}
        RadioCard(title: "🇹🇭 Thai",    isSelected: false) {}
    }
    .padding()
    .background(Color.appBackground)
}
