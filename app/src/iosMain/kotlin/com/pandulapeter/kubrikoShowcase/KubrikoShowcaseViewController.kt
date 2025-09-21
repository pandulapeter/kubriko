/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoShowcase

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.uikit.ComposeUIViewControllerDelegate
import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIApplication

fun KubrikoShowcaseViewController() = ComposeUIViewController(
    configure = {
        delegate = object : ComposeUIViewControllerDelegate {
            override val prefersStatusBarHidden: Boolean? get() = isInFullscreenMode.value
        }
    }
) {
    KubrikoShowcase(
        isInFullscreenMode = isInFullscreenMode.value,
        getIsInFullscreenMode = { isInFullscreenMode.value },
        onFullscreenModeToggled = {
            isInFullscreenMode.value = !isInFullscreenMode.value
            UIApplication.sharedApplication.keyWindow?.rootViewController?.setNeedsStatusBarAppearanceUpdate()
        },
    )
}

private val isInFullscreenMode = mutableStateOf(false)