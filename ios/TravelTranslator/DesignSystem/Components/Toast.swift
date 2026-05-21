import SwiftUI

public enum ToastKind {
    case info
    case success
    case error
}

@MainActor
@Observable
public final class ToastCenter {
    public static let shared = ToastCenter()
    public private(set) var current: ToastModel?
    private var task: Task<Void, Never>?

    public func show(_ message: String, kind: ToastKind = .info, duration: TimeInterval = 2.5) {
        current = ToastModel(id: UUID(), message: message, kind: kind)
        task?.cancel()
        task = Task { [weak self] in
            try? await Task.sleep(nanoseconds: UInt64(duration * 1_000_000_000))
            if !Task.isCancelled { self?.current = nil }
        }
    }

    public func dismiss() {
        task?.cancel()
        current = nil
    }

    public struct ToastModel: Identifiable, Equatable {
        public let id: UUID
        public let message: String
        public let kind: ToastKind
        public static func == (a: ToastModel, b: ToastModel) -> Bool { a.id == b.id }
    }
}

public struct ToastOverlay: View {
    @Bindable private var center: ToastCenter

    public init(center: ToastCenter = .shared) {
        self._center = Bindable(center)
    }

    public var body: some View {
        VStack {
            Spacer()
            if let toast = center.current {
                ToastView(model: toast)
                    .padding(.horizontal, DesignTokens.Spacing.s6)
                    .padding(.bottom, DesignTokens.Spacing.s8)
                    .transition(.move(edge: .bottom).combined(with: .opacity))
            }
        }
        .animation(.spring(response: 0.35, dampingFraction: 0.85), value: center.current)
        .allowsHitTesting(center.current != nil)
    }
}

private struct ToastView: View {
    let model: ToastCenter.ToastModel

    var body: some View {
        HStack(spacing: DesignTokens.Spacing.s3) {
            Image(systemName: iconName)
                .foregroundStyle(iconTint)
            Text(model.message)
                .font(.appBody)
                .foregroundStyle(Color.appCardForeground)
            Spacer(minLength: 0)
        }
        .padding(.horizontal, DesignTokens.Spacing.s4)
        .padding(.vertical, DesignTokens.Spacing.s3)
        .background(Color.appCard)
        .overlay(
            RoundedRectangle(cornerRadius: DesignTokens.Radius.lg, style: .continuous)
                .stroke(Color.appBorder, lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: DesignTokens.Radius.lg, style: .continuous))
        .shadow(color: Color.black.opacity(0.12), radius: 14, x: 0, y: 6)
    }

    private var iconName: String {
        switch model.kind {
        case .info:    return "info.circle.fill"
        case .success: return "checkmark.circle.fill"
        case .error:   return "exclamationmark.circle.fill"
        }
    }

    private var iconTint: Color {
        switch model.kind {
        case .info:    return Color.appPrimary
        case .success: return Color.appSuccess
        case .error:   return Color.appDestructive
        }
    }
}
