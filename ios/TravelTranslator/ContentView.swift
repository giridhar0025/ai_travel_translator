import SwiftUI

struct ContentView: View {
    var body: some View {
        #if DEBUG
        ComponentGallery()
        #else
        Color.appBackground.ignoresSafeArea()
        #endif
    }
}

#Preview { ContentView() }
