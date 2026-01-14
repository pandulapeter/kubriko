/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoShowcase

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.window.ComposeUIViewController
import kotlinx.cinterop.ExperimentalForeignApi
import platform.UIKit.UIApplication
import platform.UIKit.UIViewAutoresizingFlexibleHeight
import platform.UIKit.UIViewAutoresizingFlexibleWidth
import platform.UIKit.UIViewController
import platform.UIKit.addChildViewController
import platform.UIKit.didMoveToParentViewController

@OptIn(ExperimentalForeignApi::class)
fun KubrikoShowcaseViewController() = object : UIViewController(nibName = null, bundle = null) {

    override fun prefersStatusBarHidden() = isInFullscreenMode.value

    private val composeViewController = ComposeUIViewController {
        KubrikoShowcase(
            isInFullscreenMode = isInFullscreenMode.value,
            getIsInFullscreenMode = { isInFullscreenMode.value },
            onFullscreenModeToggled = {
                isInFullscreenMode.value = !isInFullscreenMode.value
                UIApplication.sharedApplication.keyWindow?.rootViewController?.setNeedsStatusBarAppearanceUpdate()
            },
        )
    }

    override fun viewDidLoad() {
        super.viewDidLoad()
        addChildViewController(composeViewController)
        view.addSubview(composeViewController.view)
        composeViewController.didMoveToParentViewController(this)
        composeViewController.view.setFrame(view.bounds)
        composeViewController.view.autoresizingMask = UIViewAutoresizingFlexibleWidth or UIViewAutoresizingFlexibleHeight
    }
}

private val isInFullscreenMode = mutableStateOf(false)