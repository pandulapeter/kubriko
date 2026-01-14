/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoShowcase.implementation.ui

import com.pandulapeter.kubriko.implementation.isRunningOnAndroid
import com.pandulapeter.kubriko.implementation.isRunningOnIpad
import com.pandulapeter.kubriko.implementation.isRunningOnIphone
import kotlinx.browser.window
import kubriko.app.generated.resources.Res
import kubriko.app.generated.resources.welcome_disclaimer_obfuscation
import kubriko.app.generated.resources.welcome_disclaimer_web_android
import kubriko.app.generated.resources.welcome_disclaimer_web_general
import kubriko.app.generated.resources.welcome_disclaimer_web_ipad
import kubriko.app.generated.resources.welcome_disclaimer_web_iphone
import kubriko.app.generated.resources.welcome_disclaimer_web_not_chrome_or_firefox

internal actual fun getWarningTexts() = buildList {
    if (isUnoptimized()) {
        add(Res.string.welcome_disclaimer_obfuscation)
    }
    add(Res.string.welcome_disclaimer_web_general)
    if (window.isRunningOnIphone()) {
        add(Res.string.welcome_disclaimer_web_iphone)
    } else if (window.isRunningOnIpad()) {
        add(Res.string.welcome_disclaimer_web_ipad)
    } else if (window.isRunningOnAndroid()) {
        add(Res.string.welcome_disclaimer_web_android)
    } else if (window.navigator.userAgent.let { !it.contains("Chrome") && !it.contains("Firefox") }) {
        add(Res.string.welcome_disclaimer_web_not_chrome_or_firefox)
    }
}

private fun isUnoptimized() = try {
    throw Error()
} catch (e: Throwable) {
    e.stackTraceToString().contains("isUnoptimized")
}