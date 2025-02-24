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
import androidx.compose.ui.window.ComposeUIViewController

// TODO: Hide status bar in fullscreen mode
fun KubrikoShowcaseViewController() = ComposeUIViewController {
    KubrikoShowcase(
        isInFullscreenMode = isInFullscreenMode.value,
        getIsInFullscreenMode = { isInFullscreenMode.value },
        onFullscreenModeToggled = { isInFullscreenMode.value = !isInFullscreenMode.value },
    )
}

private val isInFullscreenMode = mutableStateOf(false)