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

internal actual fun getDefaultFocusDebounce() = 0L

internal actual fun getPlatform(): MetadataManager.Platform = MetadataManager.Platform.Web(
    userAgent = window.navigator.userAgent,
)

/**
 * When opening the web app from an iPhone the lifecycle never seems to get to RESUMED.
 * Using STARTED instead fixes blocker issues, but we lose the ability to detect when the app gets backgrounded.
 */
internal actual val activeLifecycleState: Lifecycle.State = if (window.navigator.userAgent.contains("iPhone"))  {
    Lifecycle.State.STARTED
} else{
    Lifecycle.State.RESUMED
}