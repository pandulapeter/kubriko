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

import androidx.lifecycle.Lifecycle
import com.pandulapeter.kubriko.manager.MetadataManager
import kotlinx.browser.window
import org.w3c.dom.Window

internal actual fun getDefaultFocusDebounce() = 0L

internal actual fun getPlatform(): MetadataManager.Platform = MetadataManager.Platform.Web(
    userAgent = window.navigator.userAgent,
)

/**
 * When opening the web app on an iPhone the lifecycle never seems to get to RESUMED.
 * On iPad the situation seems to be a bit better, but it's still possible to make the app get stuck in STARTED state while have full focus.
 * Using STARTED to mean RESUMED fixes the blocker issues, but we lose the ability to detect when the app gets backgrounded.
 */
internal actual val activeLifecycleState: Lifecycle.State = if (window.isRunningOnIpad() || window.isRunningOnIphone()) {
    Lifecycle.State.STARTED
} else {
    Lifecycle.State.RESUMED
}

fun Window.isRunningOnAndroid() =
    navigator.userAgent.contains("Android")

fun Window.isRunningOnIphone() =
    navigator.userAgent.contains("iPhone") || (!navigator.userAgent.contains("Chrome") && navigator.maxTouchPoints > 0 && window.innerWidth / window.innerHeight > 1.6)

fun Window.isRunningOnIpad() =
    navigator.userAgent.contains("iPad") || (!navigator.userAgent.contains("Chrome") && navigator.maxTouchPoints > 0 && window.innerWidth / window.innerHeight <= 1.6)