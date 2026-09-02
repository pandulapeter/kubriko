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
import androidx.compose.ui.window.WindowState
import com.pandulapeter.kubriko.manager.MetadataManager
import com.pandulapeter.kubriko.types.TargetFrameRate
import org.apache.commons.lang3.SystemUtils
import java.awt.DisplayMode
import java.awt.GraphicsEnvironment

internal actual fun getDefaultFocusDebounce() = 0L

internal actual fun getPlatform(): MetadataManager.Platform = when {
    SystemUtils.IS_OS_WINDOWS -> MetadataManager.Platform.Desktop.Windows(
        windowsVersion = SystemUtils.OS_VERSION,
        javaVersion = SystemUtils.JAVA_VERSION,
    )

    SystemUtils.IS_OS_MAC -> MetadataManager.Platform.Desktop.MacOS(
        macOSVersion = SystemUtils.OS_VERSION,
        javaVersion = SystemUtils.JAVA_VERSION,
    )

    else -> MetadataManager.Platform.Desktop.Linux(
        linuxVersion = SystemUtils.OS_VERSION,
        javaVersion = SystemUtils.JAVA_VERSION,
    )
}

@Composable
internal actual fun PlatformFocusEffect(onFocusChanged: (Boolean) -> Unit) {
    LifecycleFocusEffect(onFocusChanged = onFocusChanged)
}

@Composable
internal actual fun PlatformFrameRateHint(targetFrameRate: TargetFrameRate) = Unit

@Composable
internal actual fun PlatformMaximumDisplayRefreshRateEffect(onMaximumDisplayRefreshRateChanged: (Float?) -> Unit) {
    DisposableEffect(Unit) {
        // The primary screen rather than the one the window happens to sit on: AWT reports the rate per
        // screen device, and a headless environment has none at all.
        val displayMode = runCatching { GraphicsEnvironment.getLocalGraphicsEnvironment().defaultScreenDevice.displayMode }.getOrNull()
        onMaximumDisplayRefreshRateChanged(displayMode?.refreshRate?.takeIf { it != DisplayMode.REFRESH_RATE_UNKNOWN }?.toFloat())
        onDispose { }
    }
}

lateinit var windowState: WindowState
