import SwiftUI

public struct SearchBar: View {
    private let placeholder: String
    @Binding private var text: String
    @FocusState private var isFocused: Bool

    public init(_ placeholder: String = "Search…", text: Binding<String>) {
        self.placeholder = placeholder
        self._text = text
    }

    public var body: some View {
        HStack(spacing: DesignTokens.Spacing.s3) {
            Image(systemName: "magnifyingglass")
                .foregroundStyle(Color.appTextSecondary)
                .frame(width: 20, height: 20)
            TextField(placeholder, text: $text)
                .font(.appBody)
                .foregroundStyle(Color.appForeground)
                .textInputAutocapitalization(.never)
                .autocorrectionDisabled()
                .focused($isFocused)
                .submitLabel(.search)
            if !text.isEmpty {
                Button {
                    text = ""
                } label: {
                    Image(systemName: "xmark.circle.fill")
                        .foregroundStyle(Color.appTextSecondary)
                }
                .buttonStyle(.plain)
                .accessibilityLabel("Clear search")
            }
        }
        .padding(.horizontal, DesignTokens.Spacing.s4)
        .padding(.vertical, DesignTokens.Spacing.s3)
        .frame(minHeight: DesignTokens.TouchTarget.min)
        .background(Color.appBackground)
        .overlay(
            RoundedRectangle(cornerRadius: DesignTokens.Radius.lg, style: .continuous)
                .stroke(isFocused ? Color.appRing : Color.appBorder, lineWidth: isFocused ? 2 : 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: DesignTokens.Radius.lg, style: .continuous))
        .animation(.easeOut(duration: DesignTokens.Motion.Duration.transition), value: isFocused)
    }
}

#Preview {
    @Previewable @State var query = ""
    return SearchBar("Search conversations…", text: $query)
        .padding()
        .background(Color.appBackground)
}
