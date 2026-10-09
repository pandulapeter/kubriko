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

import kotlinx.browser.window
import org.w3c.dom.Window

/** Whether the browser runs on an Android device, judged by its user agent. */
fun Window.isRunningOnAndroid() =
    navigator.userAgent.contains("Android")

/**
 * Whether the browser runs on an iPhone: its user agent says so, or it is a touch screen outside Chrome with a wide
 * (phone-in-landscape) window.
 */
fun Window.isRunningOnIphone() =
    navigator.userAgent.contains("iPhone") || (!navigator.userAgent.contains("Chrome") && navigator.maxTouchPoints > 0 && window.innerWidth / window.innerHeight > 1.6)

/**
 * Whether the browser runs on an iPad: its user agent says so, or it is a touch screen outside Chrome whose window is
 * not that wide.
 */
fun Window.isRunningOnIpad() =
    navigator.userAgent.contains("iPad") || (!navigator.userAgent.contains("Chrome") && navigator.maxTouchPoints > 0 && window.innerWidth / window.innerHeight <= 1.6)
