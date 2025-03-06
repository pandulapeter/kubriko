/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.implementation

import androidx.compose.ui.window.WindowState
import androidx.lifecycle.Lifecycle
import com.pandulapeter.kubriko.manager.MetadataManager
import org.apache.commons.lang3.SystemUtils

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

internal actual val activeLifecycleState: Lifecycle.State = Lifecycle.State.RESUMED

lateinit var windowState: WindowState