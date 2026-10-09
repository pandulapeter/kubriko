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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.produceState
import androidx.compose.ui.awt.ComposeWindow
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import org.jetbrains.skiko.SystemTheme
import org.jetbrains.skiko.currentSystemTheme
import java.awt.Toolkit

/**
 * Draws the window buttons (and on macOS the rim along the top edge) for the theme the Showcase is in, since the wrong
 * appearance leaves them all but invisible on its background. Follows the system theme the same way `KubrikoTheme`
 * does, as the JDK reads the application's appearance only once. Both properties exist only in the JetBrains Runtime.
 * Also removes the mouse listener once the window leaves the composition.
 */
@Composable
internal fun TitleBarAppearance(
    window: ComposeWindow,
    titleBar: ExtendedTitleBar,
) {
    val isDarkTheme = produceState(initialValue = currentSystemTheme == SystemTheme.DARK) {
        while (isActive) {
            delay(100)
            value = currentSystemTheme == SystemTheme.DARK
        }
    }.value
    SideEffect {
        if (isMacOs) {
            window.rootPane.putClientProperty("apple.awt.windowAppearance", if (isDarkTheme) "NSAppearanceNameDarkAqua" else "NSAppearanceNameAqua")
        }
        // Every property set redraws the title bar.
        if (isWindows && titleBar.customTitleBar.properties[WINDOWS_DARK_CONTROLS] != isDarkTheme) {
            titleBar.customTitleBar.putProperty(WINDOWS_DARK_CONTROLS, isDarkTheme)
        }
    }
    DisposableEffect(titleBar) {
        onDispose { Toolkit.getDefaultToolkit().removeAWTEventListener(titleBar.mouseListener) }
    }
}

/** Whether the caption buttons are drawn for a dark background (light icons) or a light one. */
private const val WINDOWS_DARK_CONTROLS = "controls.dark"
