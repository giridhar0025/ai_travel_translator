import SwiftUI

public enum AppTab: String, CaseIterable, Identifiable, Sendable {
    case chat, phrases, history, settings
    public var id: String { rawValue }

    public var label: String {
        switch self {
        case .chat:     return "Chat"
        case .phrases:  return "Phrases"
        case .history:  return "History"
        case .settings: return "Settings"
        }
    }

    public var systemImage: String {
        switch self {
        case .chat:     return "message"
        case .phrases:  return "text.book.closed"
        case .history:  return "clock.arrow.circlepath"
        case .settings: return "gearshape"
        }
    }
}

public struct BottomNavBar: View {
    @Binding private var selection: AppTab

    public init(selection: Binding<AppTab>) {
        self._selection = selection
    }

    public var body: some View {
        HStack(spacing: 0) {
            ForEach(AppTab.allCases) { tab in
                let active = tab == selection
                Button {
                    selection = tab
                } label: {
                    VStack(spacing: DesignTokens.Spacing.s1) {
                        Image(systemName: systemImageName(for: tab, active: active))
                            .font(.system(size: 22, weight: active ? .semibold : .regular))
                        Text(tab.label)
                            .font(.system(size: DesignTokens.Typography.Size.xs, weight: .medium))
                    }
                    .foregroundStyle(active ? Color.appPrimary : Color.appTextSecondary)
                    .frame(maxWidth: .infinity, minHeight: DesignTokens.TouchTarget.min + 8)
                    .contentShape(Rectangle())
                }
                .buttonStyle(.plain)
                .accessibilityLabel(Text(tab.label))
                .accessibilityAddTraits(active ? [.isButton, .isSelected] : .isButton)
            }
        }
        .padding(.horizontal, DesignTokens.Spacing.s2)
        .padding(.top, DesignTokens.Spacing.s2)
        .padding(.bottom, DesignTokens.Spacing.s1)
        .background(
            Color.appCard
                .overlay(Color.appBorder.frame(height: 1), alignment: .top)
                .ignoresSafeArea(edges: .bottom)
        )
    }

    private func systemImageName(for tab: AppTab, active: Bool) -> String {
        switch tab {
        case .chat:     return active ? "bubble.left.fill" : "bubble.left"
        case .phrases:  return active ? "book.fill" : "book"
        case .history:  return "clock.arrow.circlepath"
        case .settings: return active ? "gearshape.fill" : "gearshape"
        }
    }
}

#Preview {
    @Previewable @State var tab: AppTab = .chat
    return VStack {
        Spacer()
        BottomNavBar(selection: $tab)
    }
    .background(Color.appBackground)
}
