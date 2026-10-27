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

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.graphics.luminance

/**
 * Draws the window buttons, and on macOS the rim along the window's top edge, for the theme the app is in rather than
 * the system's, since with the content laid out under them the app's background is theirs: dark mode buttons on a
 * light theme are all but invisible, and the other way around. The macOS property exists only in the JetBrains
 * Runtime, which the build runs on for this; any other JDK ignores it and keeps the system's appearance.
 */
@Composable
internal fun TitleBarAppearance(
    window: ComposeWindow,
    titleBar: ExtendedTitleBar,
) {
    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
    SideEffect {
        if (isMacOs) {
            window.rootPane.putClientProperty("apple.awt.windowAppearance", if (isDarkTheme) "NSAppearanceNameDarkAqua" else "NSAppearanceNameAqua")
        }
        // Every property set redraws the title bar, and this runs with every recomposition.
        if (isWindows && titleBar.customTitleBar.properties[WINDOWS_DARK_CONTROLS] != isDarkTheme) {
            titleBar.customTitleBar.putProperty(WINDOWS_DARK_CONTROLS, isDarkTheme)
        }
    }
}

/** Whether the caption buttons are drawn for a dark background (light icons) or a light one. */
private const val WINDOWS_DARK_CONTROLS = "controls.dark"
