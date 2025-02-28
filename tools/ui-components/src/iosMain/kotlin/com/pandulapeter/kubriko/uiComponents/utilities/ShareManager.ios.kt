/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.uiComponents.utilities

import androidx.compose.runtime.Composable
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication

object ShareManagerImpl : ShareManager {
    override val isSharingSupported = true

    override fun shareText(text: String) {
        UIApplication.sharedApplication.keyWindow?.rootViewController?.presentViewController(
            UIActivityViewController(listOf(text), null),
            animated = true,
            completion = null,
        )
    }
}

@Composable
actual fun rememberShareManager(): ShareManager = ShareManagerImpl