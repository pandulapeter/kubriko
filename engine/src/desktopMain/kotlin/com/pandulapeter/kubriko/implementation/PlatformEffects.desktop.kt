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
import com.pandulapeter.kubriko.types.TargetFrameRate
import java.awt.DisplayMode
import java.awt.GraphicsEnvironment

@Composable
internal actual fun PlatformFocusEffect(onFocusChanged: (Boolean) -> Unit) {
    LifecycleFocusEffect(onFocusChanged = onFocusChanged)
}

@Composable
internal actual fun PlatformFrameRateHint(targetFrameRate: TargetFrameRate) = Unit

@Composable
internal actual fun PlatformMaximumDisplayRefreshRateEffect(onMaximumDisplayRefreshRateChanged: (Float?) -> Unit) {
    val currentOnMaximumDisplayRefreshRateChanged by rememberUpdatedState(onMaximumDisplayRefreshRateChanged)
    DisposableEffect(Unit) {
        // The primary screen rather than the one the window happens to sit on: AWT reports the rate per
        // screen device, and a headless environment has none at all.
        val displayMode = runCatching { GraphicsEnvironment.getLocalGraphicsEnvironment().defaultScreenDevice.displayMode }.getOrNull()
        currentOnMaximumDisplayRefreshRateChanged(displayMode?.refreshRate?.takeIf { it != DisplayMode.REFRESH_RATE_UNKNOWN }?.toFloat())
        onDispose { }
    }
}
