/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.uiComponents.utilities

import androidx.compose.runtime.Composable
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGRectMake
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UISceneActivationStateForegroundActive
import platform.UIKit.UIViewController
import platform.UIKit.UIWindow
import platform.UIKit.UIWindowScene
import platform.UIKit.popoverPresentationController

object ShareManagerImpl : ShareManager {
    override val isSharingSupported = true

    @OptIn(ExperimentalForeignApi::class)
    override fun shareText(text: String) {
        val presenter = findTopViewController() ?: return
        val controller = UIActivityViewController(listOf(text), null)
        controller.popoverPresentationController?.let { popover ->
            popover.sourceView = presenter.view
            popover.sourceRect = presenter.view.bounds.useContents {
                CGRectMake(origin.x + size.width / 2, origin.y + size.height / 2, 0.0, 0.0)
            }
            popover.permittedArrowDirections = 0u
        }
        presenter.presentViewController(
            viewControllerToPresent = controller,
            animated = true,
            completion = null,
        )
    }

    private fun findActiveWindow(): UIWindow? {
        val windows = UIApplication.sharedApplication.connectedScenes
            .filterIsInstance<UIWindowScene>()
            .firstOrNull { it.activationState == UISceneActivationStateForegroundActive }
            ?.windows
            ?.filterIsInstance<UIWindow>()
        return windows?.firstOrNull { it.isKeyWindow() } ?: windows?.firstOrNull() ?: UIApplication.sharedApplication.keyWindow
    }

    private fun findTopViewController(): UIViewController? {
        var presenter = findActiveWindow()?.rootViewController
        while (presenter?.presentedViewController != null) {
            presenter = presenter.presentedViewController
        }
        return presenter
    }
}

@Composable
actual fun rememberShareManager(): ShareManager = ShareManagerImpl
