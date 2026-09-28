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
    @Binding var isStatusBarHidden: Bool

    func makeUIViewController(context: Context) -> UIViewController {
        let isStatusBarHidden = $isStatusBarHidden
        return KubrikoShowcaseViewControllerKt.KubrikoShowcaseViewController(
            onFullscreenModeChanged: { isFullscreen in isStatusBarHidden.wrappedValue = isFullscreen.boolValue }
        )
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    @State private var isStatusBarHidden = false

    var body: some View {
        ComposeView(isStatusBarHidden: $isStatusBarHidden)
            .ignoresSafeArea()
            .statusBarHidden(isStatusBarHidden)
    }
}
