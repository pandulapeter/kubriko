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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalPlatformWindowInsets
import androidx.compose.ui.platform.PlatformInsets
import androidx.compose.ui.platform.PlatformWindowInsets

/**
 * Reports the strip [extendContentIntoTitleBar] lays the content under as a system bar inset at the top, the way a
 * phone's status bar is, so the Showcase keeps its content clear of the window buttons while its background reaches
 * under them. A full screen window has no title bar, so it gets none.
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
    val platformInsets = LocalPlatformWindowInsets.current
    val density = LocalDensity.current
    val titleBarHeight = titleBar?.takeUnless { isFullscreen }?.let { with(density) { it.height.roundToPx() } }
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
