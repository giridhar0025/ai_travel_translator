import SwiftUI

public struct Waveform: View {
    private let barCount: Int
    private let isAnimated: Bool
    private let color: Color
    private let inactiveColor: Color
    private let height: CGFloat
    private let levels: [CGFloat]?

    @State private var animatedLevels: [CGFloat]
    @State private var timer: Timer?

    public init(
        barCount: Int = 8,
        isAnimated: Bool = false,
        color: Color = .appPrimary,
        inactiveColor: Color = Color.appBorder,
        height: CGFloat = 64,
        levels: [CGFloat]? = nil
    ) {
        self.barCount = barCount
        self.isAnimated = isAnimated
        self.color = color
        self.inactiveColor = inactiveColor
        self.height = height
        self.levels = levels
        _animatedLevels = State(initialValue: Array(repeating: 0.2, count: barCount))
    }

    public var body: some View {
        HStack(alignment: .center, spacing: barCount > 20 ? 2 : 6) {
            ForEach(0..<barCount, id: \.self) { i in
                let value = displayLevel(at: i)
                Capsule()
                    .fill(isAnimated ? color : inactiveColor)
                    .frame(width: barCount > 20 ? 3 : 6, height: max(8, height * value))
                    .animation(.easeInOut(duration: 0.18), value: animatedLevels)
            }
        }
        .frame(height: height)
        .accessibilityHidden(true)
        .onAppear { startIfNeeded() }
        .onDisappear { stop() }
        .onChange(of: isAnimated) { _, newValue in
            newValue ? startIfNeeded() : stop()
        }
    }

    private func displayLevel(at index: Int) -> CGFloat {
        if let levels, index < levels.count { return min(1, max(0.05, levels[index])) }
        return animatedLevels[index]
    }

    private func startIfNeeded() {
        guard isAnimated, levels == nil, timer == nil else { return }
        timer = Timer.scheduledTimer(withTimeInterval: 0.18, repeats: true) { _ in
            animatedLevels = (0..<barCount).map { _ in CGFloat.random(in: 0.2...1.0) }
        }
    }

    private func stop() {
        timer?.invalidate()
        timer = nil
        animatedLevels = Array(repeating: 0.2, count: barCount)
    }
}

#Preview {
    VStack(spacing: DesignTokens.Spacing.s6) {
        Waveform(barCount: 8, isAnimated: true)
        Waveform(barCount: 40, isAnimated: true, height: 80)
        Waveform(barCount: 40, isAnimated: false, height: 80)
    }
    .padding()
    .background(Color.appBackground)
}
