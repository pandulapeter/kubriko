/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
import UIKit
import SwiftUI
import ComposeApp

extension Notification.Name {
    static let windowSizeChanged = Notification.Name("windowSizeChanged")
}

struct ComposeView: UIViewControllerRepresentable {
    
    func makeUIViewController(context: Context) -> UIViewController {
        KubrikoShowcaseViewControllerKt.KubrikoShowcaseViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    @Environment(\.horizontalSizeClass) var horizontalSizeClass
    @Environment(\.verticalSizeClass) var verticalSizeClass
    
    var body: some View {
        ComposeView()
            .ignoresSafeArea()
            .onChange(of: horizontalSizeClass) { newValue in
                NotificationCenter.default.post(
                    name: .windowSizeChanged,
                    object: nil
                )
            }
            .onChange(of: verticalSizeClass) { newValue in
                NotificationCenter.default.post(
                    name: .windowSizeChanged,
                    object: nil
                )
            }
        
    }
}



