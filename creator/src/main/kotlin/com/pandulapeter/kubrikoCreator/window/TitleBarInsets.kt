/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.window

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalPlatformWindowInsets
import androidx.compose.ui.platform.PlatformInsets
import androidx.compose.ui.platform.PlatformWindowInsets

/**
 * Tells the shared UI about the strip [extendContentIntoTitleBar] lays the content under, as the system bar inset at
 * the top that Android's status bar and iOS's are, so that every screen already keeps its content clear of the window
 * buttons while its background reaches under them. A full screen window has no title bar (on macOS, until the pointer
 * reaches the top of the screen, and then the system draws it over the content), so it gets none.
 *
 * Compose Desktop has no public way to set the insets; this is the composition local its own `WindowInsets` read.
 */
@OptIn(InternalComposeUiApi::class)
@Composable
internal fun TitleBarInsets(
    titleBar: ExtendedTitleBar?,
    isFullscreen: Boolean,
    content: @Composable () -> Unit,
) {
    // The content is composed from this one call site whatever the insets are, since one composed from two would be
    // thrown away and built again at every change into or out of full screen, every scroll position with it.
    val platformInsets = LocalPlatformWindowInsets.current
    val titleBarHeight = titleBar?.takeUnless { isFullscreen }?.let { with(LocalDensity.current) { it.height.roundToPx() } }
    val insets = remember(platformInsets, titleBarHeight) {
        if (titleBarHeight == null) platformInsets else object : PlatformWindowInsets by platformInsets {
            override val captionBar = PlatformInsets(top = titleBarHeight)
            override val systemBars = PlatformInsets(top = titleBarHeight)

            // A dialog or a popup asks for the insets without the ones it has already kept clear of.
            override fun excluding(safeInsets: Boolean, ime: Boolean) = if (safeInsets) platformInsets.excluding(safeInsets, ime) else this
        }
    }
    CompositionLocalProvider(LocalPlatformWindowInsets provides insets, content = content)
}
