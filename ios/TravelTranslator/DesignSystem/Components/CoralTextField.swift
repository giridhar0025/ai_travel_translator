import SwiftUI

public enum CoralTextFieldKind {
    case text
    case email
    case password
}

public struct CoralTextField: View {
    private let label: String?
    private let placeholder: String
    private let kind: CoralTextFieldKind
    private let leadingIcon: String?
    @Binding private var text: String
    @FocusState private var isFocused: Bool
    @State private var passwordVisible = false

    public init(
        _ placeholder: String,
        text: Binding<String>,
        label: String? = nil,
        kind: CoralTextFieldKind = .text,
        leadingIcon: String? = nil
    ) {
        self.placeholder = placeholder
        self._text = text
        self.label = label
        self.kind = kind
        self.leadingIcon = leadingIcon
    }

    public var body: some View {
        VStack(alignment: .leading, spacing: DesignTokens.Spacing.s2) {
            if let label {
                Text(label)
                    .font(.appLabel)
                    .foregroundStyle(Color.appForeground)
            }
            HStack(spacing: DesignTokens.Spacing.s3) {
                if let leadingIcon {
                    Image(systemName: leadingIcon)
                        .foregroundStyle(Color.appTextSecondary)
                        .frame(width: 20, height: 20)
                }
                Group {
                    if kind == .password && !passwordVisible {
                        SecureField(placeholder, text: $text)
                    } else {
                        TextField(placeholder, text: $text)
                            .keyboardType(kind == .email ? .emailAddress : .default)
                            .textContentType(kind == .email ? .emailAddress : nil)
                            .textInputAutocapitalization(kind == .email ? .never : .sentences)
                            .autocorrectionDisabled(kind == .email || kind == .password)
                    }
                }
                .font(.appBody)
                .foregroundStyle(Color.appForeground)
                .focused($isFocused)

                if kind == .password {
                    Button {
                        passwordVisible.toggle()
                    } label: {
                        Image(systemName: passwordVisible ? "eye.slash" : "eye")
                            .foregroundStyle(Color.appTextSecondary)
                    }
                    .buttonStyle(.plain)
                    .accessibilityLabel(passwordVisible ? "Hide password" : "Show password")
                }
            }
            .padding(.horizontal, DesignTokens.Spacing.s4)
            .padding(.vertical, DesignTokens.Spacing.s3)
            .frame(minHeight: DesignTokens.TouchTarget.min)
            .background(Color.appCard)
            .overlay(
                RoundedRectangle(cornerRadius: DesignTokens.Radius.lg, style: .continuous)
                    .stroke(isFocused ? Color.appRing : Color.appBorder, lineWidth: isFocused ? 2 : 1)
            )
            .clipShape(RoundedRectangle(cornerRadius: DesignTokens.Radius.lg, style: .continuous))
            .animation(.easeOut(duration: DesignTokens.Motion.Duration.transition), value: isFocused)
        }
    }
}

#Preview {
    @Previewable @State var email = ""
    @Previewable @State var password = ""
    return VStack(spacing: DesignTokens.Spacing.s4) {
        CoralTextField("your@email.com", text: $email, label: "Email", kind: .email, leadingIcon: "envelope")
        CoralTextField("••••••••", text: $password, label: "Password", kind: .password, leadingIcon: "lock")
    }
    .padding()
    .background(Color.appBackground)
}
