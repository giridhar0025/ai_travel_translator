import SwiftUI

#if DEBUG
public struct ComponentGallery: View {
    @State private var email = ""
    @State private var password = ""
    @State private var search = ""
    @State private var selectedLanguage: AppLanguage = .english
    @State private var tab: AppTab = .chat
    @State private var sheetOpen = false
    @State private var toastCenter = ToastCenter()

    public init() {}

    public var body: some View {
        ZStack {
            Color.appBackground.ignoresSafeArea()
            VStack(spacing: 0) {
                ScrollViewReader { proxy in
                    ScrollView {
                        VStack(alignment: .leading, spacing: DesignTokens.Spacing.s6) {
                            section("Buttons", id: "buttons") {
                                CoralButton("Sign In") { toastCenter.show("Primary tapped") }
                                CoralButton("Continue with Email", leadingIcon: "envelope.fill") {}
                                CoralButton("Continue with Google", variant: .secondary, leadingIcon: "g.circle.fill") {}
                                CoralButton("Loading", isLoading: true) {}
                                CoralButton("Disabled") {}.disabled(true)
                                CoralButton("Skip for now", variant: .text) {}
                            }
                            section("Text fields", id: "textfields") {
                                CoralTextField("your@email.com", text: $email, label: "Email", kind: .email, leadingIcon: "envelope")
                                CoralTextField("••••••••", text: $password, label: "Password", kind: .password, leadingIcon: "lock")
                            }
                            section("Search", id: "search") {
                                SearchBar("Search conversations…", text: $search)
                            }
                            section("Card", id: "card") {
                                CoralCard {
                                    VStack(alignment: .leading, spacing: DesignTokens.Spacing.s2) {
                                        Text("Static card").font(.appSubheading)
                                        Text("Used as a surface around content.")
                                            .font(.appBody)
                                            .foregroundStyle(Color.appTextSecondary)
                                    }
                                }
                                CoralCard(onTap: { toastCenter.show("Card tapped") }) {
                                    HStack {
                                        Text("Tappable card").font(.appBody)
                                        Spacer()
                                        Image(systemName: "chevron.right").foregroundStyle(Color.appTextSecondary)
                                    }
                                }
                            }
                            section("Radio cards (language)", id: "radios") {
                                ForEach(AppLanguage.supported, id: \.code) { lang in
                                    RadioCard(
                                        title: "\(lang.flag)  \(lang.name)",
                                        isSelected: lang == selectedLanguage
                                    ) {
                                        selectedLanguage = lang
                                    }
                                }
                            }
                            section("Language flags", id: "flags") {
                                ForEach(AppLanguage.supported, id: \.code) { LanguageFlag($0) }
                            }
                            section("Waveforms", id: "waveforms") {
                                VStack(alignment: .leading, spacing: DesignTokens.Spacing.s4) {
                                    Text("8-bar live (animated)").font(.appLabel).foregroundStyle(Color.appTextSecondary)
                                    Waveform(barCount: 8, isAnimated: true)
                                    Text("40-bar player (animated)").font(.appLabel).foregroundStyle(Color.appTextSecondary)
                                    Waveform(barCount: 40, isAnimated: true, height: 80)
                                    Text("40-bar idle").font(.appLabel).foregroundStyle(Color.appTextSecondary)
                                    Waveform(barCount: 40, isAnimated: false, height: 80)
                                }
                            }
                            section("Bottom sheet", id: "sheet") {
                                CoralButton("Open bottom sheet") { sheetOpen = true }
                            }
                            section("Toast", id: "toast") {
                                HStack(spacing: DesignTokens.Spacing.s2) {
                                    CoralButton("Info",    variant: .secondary) { toastCenter.show("Info message",    kind: .info) }
                                    CoralButton("Success", variant: .secondary) { toastCenter.show("Saved!",          kind: .success) }
                                    CoralButton("Error",   variant: .secondary) { toastCenter.show("Something broke", kind: .error) }
                                }
                            }
                        }
                        .padding(DesignTokens.Spacing.s6)
                    }
                    .onAppear {
                        if let target = launchScrollTarget() {
                            DispatchQueue.main.asyncAfter(deadline: .now() + 0.05) {
                                withAnimation(.none) { proxy.scrollTo(target, anchor: .top) }
                            }
                        }
                        if launchHasFlag("--openSheet") {
                            DispatchQueue.main.asyncAfter(deadline: .now() + 0.2) { sheetOpen = true }
                        }
                        if let kindRaw = launchValue(for: "--showToast") {
                            let kind: ToastKind = {
                                switch kindRaw {
                                case "success": return .success
                                case "error":   return .error
                                default:        return .info
                                }
                            }()
                            DispatchQueue.main.asyncAfter(deadline: .now() + 0.2) {
                                toastCenter.show("\(kindRaw.capitalized) toast", kind: kind, duration: 60)
                            }
                        }
                    }
                }
                BottomNavBar(selection: $tab)
            }
            ToastOverlay(center: toastCenter)
        }
        .appBottomSheet(isPresented: $sheetOpen) {
            VStack(alignment: .leading, spacing: DesignTokens.Spacing.s4) {
                Text("Pick a language").font(.appHeading)
                ForEach(AppLanguage.supported, id: \.code) { lang in
                    RadioCard(
                        title: "\(lang.flag)  \(lang.name)",
                        isSelected: lang == selectedLanguage
                    ) {
                        selectedLanguage = lang
                        sheetOpen = false
                    }
                }
            }
        }
    }

    @ViewBuilder
    private func section<Content: View>(_ title: String, id: String, @ViewBuilder content: () -> Content) -> some View {
        VStack(alignment: .leading, spacing: DesignTokens.Spacing.s3) {
            Text(title)
                .font(.appHeading)
                .foregroundStyle(Color.appForeground)
            content()
        }
        .id(id)
    }

    private func launchScrollTarget() -> String? { launchValue(for: "--scrollTo") }
    private func launchValue(for flag: String) -> String? {
        let args = ProcessInfo.processInfo.arguments
        guard let i = args.firstIndex(of: flag), i + 1 < args.count else { return nil }
        return args[i + 1]
    }
    private func launchHasFlag(_ flag: String) -> Bool {
        ProcessInfo.processInfo.arguments.contains(flag)
    }
}

#Preview { ComponentGallery() }
#endif
