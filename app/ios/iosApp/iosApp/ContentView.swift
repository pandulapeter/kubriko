/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
import UIKit
import SwiftUI
import ComposeApp

struct ComposeView: UIViewControllerRepresentable {
    @Binding var isInFullscreenMode: Bool

    func makeUIViewController(context: Context) -> UIViewController {
        let isInFullscreenMode = $isInFullscreenMode
        return KubrikoShowcaseViewControllerKt.KubrikoShowcaseViewController(
            onFullscreenModeChanged: { isFullscreen in isInFullscreenMode.wrappedValue = isFullscreen.boolValue }
        )
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    @State private var isInFullscreenMode = KubrikoShowcaseViewControllerKt.isKubrikoShowcaseInFullscreenMode()

    var body: some View {
        ComposeView(isInFullscreenMode: $isInFullscreenMode)
            .ignoresSafeArea()
            .statusBarHidden(isInFullscreenMode)
            .modifier(FullscreenSystemOverlays(isInFullscreenMode: isInFullscreenMode))
    }
}

/// Hides the home indicator and makes the first swipe from a screen edge only reveal the system UI while the
/// Showcase is in fullscreen mode. Both modifiers need iOS 16; earlier versions keep the default behavior.
private struct FullscreenSystemOverlays: ViewModifier {
    let isInFullscreenMode: Bool

    func body(content: Content) -> some View {
        if #available(iOS 16.0, *) {
            content
                .persistentSystemOverlays(isInFullscreenMode ? .hidden : .automatic)
                .defersSystemGestures(on: isInFullscreenMode ? .all : [])
        } else {
            content
        }
    }
}
