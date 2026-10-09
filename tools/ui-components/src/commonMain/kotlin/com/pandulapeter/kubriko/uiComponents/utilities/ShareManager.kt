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

/**
 * Shares text through the platform's own sharing UI. Obtain an instance with [rememberShareManager].
 */
interface ShareManager {
    /**
     * Whether [shareText] does anything on this platform: true on Android and iOS, false on Desktop and the web.
     */
    val isSharingSupported: Boolean

    /**
     * Offers [text] to other apps: through the system share chooser on Android, and the share sheet on iOS (presented from
     * the active scene's top-most view controller, as a centered popover on iPad). Does nothing where [isSharingSupported]
     * is false.
     */
    fun shareText(text: String)
}

/**
 * Returns the [ShareManager] of the current platform.
 */
@Composable
expect fun rememberShareManager(): ShareManager