/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.implementation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.awt.LocalAwtWindow
import com.pandulapeter.kubriko.types.TargetFrameRate
import java.awt.DisplayMode
import java.awt.GraphicsEnvironment
import java.beans.PropertyChangeListener

@Composable
internal actual fun PlatformFocusEffect(onFocusChanged: (Boolean) -> Unit) {
    LifecycleFocusEffect(onFocusChanged = onFocusChanged)
}

@Composable
internal actual fun PlatformFrameRateHint(targetFrameRate: TargetFrameRate) = Unit

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal actual fun PlatformMaximumDisplayRefreshRateEffect(onMaximumDisplayRefreshRateChanged: (Float?) -> Unit) {
    val currentOnMaximumDisplayRefreshRateChanged by rememberUpdatedState(onMaximumDisplayRefreshRateChanged)
    val window = LocalAwtWindow.current
    DisposableEffect(window) {
        // AWT reports the rate per screen device: the one the window is on, or the primary screen when there is no
        // window (an offscreen scene). A headless environment has none at all.
        fun update() {
            val displayMode = runCatching {
                (window?.graphicsConfiguration?.device ?: GraphicsEnvironment.getLocalGraphicsEnvironment().defaultScreenDevice).displayMode
            }.getOrNull()
            currentOnMaximumDisplayRefreshRateChanged(displayMode?.refreshRate?.takeIf { it != DisplayMode.REFRESH_RATE_UNKNOWN }?.toFloat())
        }

        val listener = PropertyChangeListener { update() }
        window?.addPropertyChangeListener(GRAPHICS_CONFIGURATION_PROPERTY, listener)
        update()
        onDispose { window?.removePropertyChangeListener(GRAPHICS_CONFIGURATION_PROPERTY, listener) }
    }
}

private const val GRAPHICS_CONFIGURATION_PROPERTY = "graphicsConfiguration"
