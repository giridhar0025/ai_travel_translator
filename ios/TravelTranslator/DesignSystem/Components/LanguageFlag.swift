import SwiftUI

public struct AppLanguage: Hashable, Sendable {
    public let code: String
    public let name: String
    public let flag: String

    public static let english = AppLanguage(code: "en", name: "English", flag: "🇬🇧")
    public static let hindi   = AppLanguage(code: "hi", name: "Hindi",   flag: "🇮🇳")
    public static let telugu  = AppLanguage(code: "te", name: "Telugu",  flag: "🇮🇳")
    public static let thai    = AppLanguage(code: "th", name: "Thai",    flag: "🇹🇭")
    public static let supported: [AppLanguage] = [.english, .hindi, .telugu, .thai]
}

public struct LanguageFlag: View {
    private let language: AppLanguage
    private let showName: Bool
    private let flagSize: CGFloat

    public init(_ language: AppLanguage, showName: Bool = true, flagSize: CGFloat = DesignTokens.Typography.Size.xxl) {
        self.language = language
        self.showName = showName
        self.flagSize = flagSize
    }

    public var body: some View {
        HStack(spacing: DesignTokens.Spacing.s2) {
            Text(language.flag)
                .font(.system(size: flagSize))
            if showName {
                Text(language.name)
                    .font(.appBody)
                    .foregroundStyle(Color.appForeground)
            }
        }
        .accessibilityElement(children: .combine)
        .accessibilityLabel(Text(language.name))
    }
}

#Preview {
    VStack(alignment: .leading, spacing: DesignTokens.Spacing.s3) {
        ForEach(AppLanguage.supported, id: \.code) { LanguageFlag($0) }
    }
    .padding()
    .background(Color.appBackground)
}
