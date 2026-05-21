import SwiftUI

public extension View {
    /// Presents a bottom sheet styled to match the prototype's modals.
    /// Uses `.sheet` under the hood with token-aligned detents + drag indicator.
    func appBottomSheet<Content: View>(
        isPresented: Binding<Bool>,
        detents: Set<PresentationDetent> = [.medium, .large],
        @ViewBuilder content: @escaping () -> Content
    ) -> some View {
        sheet(isPresented: isPresented) {
            BottomSheetContainer(content: content)
                .presentationDetents(detents)
                .presentationDragIndicator(.visible)
                .presentationCornerRadius(DesignTokens.Radius.modal)
                .presentationBackground(Color.appCard)
        }
    }
}

private struct BottomSheetContainer<Content: View>: View {
    @ViewBuilder var content: () -> Content
    var body: some View {
        VStack(spacing: 0) {
            content()
        }
        .padding(.horizontal, DesignTokens.Spacing.s6)
        .padding(.top, DesignTokens.Spacing.s4)
        .padding(.bottom, DesignTokens.Spacing.s6)
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}
