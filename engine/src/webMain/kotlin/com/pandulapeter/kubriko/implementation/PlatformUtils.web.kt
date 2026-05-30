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
import com.pandulapeter.kubriko.manager.MetadataManager
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.Window
import org.w3c.dom.events.Event

internal actual fun getDefaultFocusDebounce() = 0L

internal actual fun getPlatform(): MetadataManager.Platform = MetadataManager.Platform.Web(
    userAgent = window.navigator.userAgent,
)

@Composable
internal actual fun PlatformFocusEffect(onFocusChanged: (Boolean) -> Unit) {
    DisposableEffect(Unit) {
        fun updateFocus() {
            onFocusChanged(document.hasFocus())
        }
        val listener: (Event) -> Unit = { updateFocus() }
        window.addEventListener(EVENT_FOCUS, listener)
        window.addEventListener(EVENT_BLUR, listener)
        window.addEventListener(EVENT_PAGE_SHOW, listener)
        window.addEventListener(EVENT_PAGE_HIDE, listener)
        document.addEventListener(EVENT_VISIBILITY_CHANGE, listener)
        updateFocus()
        onDispose {
            window.removeEventListener(EVENT_FOCUS, listener)
            window.removeEventListener(EVENT_BLUR, listener)
            window.removeEventListener(EVENT_PAGE_SHOW, listener)
            window.removeEventListener(EVENT_PAGE_HIDE, listener)
            document.removeEventListener(EVENT_VISIBILITY_CHANGE, listener)
        }
    }
}

fun Window.isRunningOnAndroid() =
    navigator.userAgent.contains("Android")

fun Window.isRunningOnIphone() =
    navigator.userAgent.contains("iPhone") || (!navigator.userAgent.contains("Chrome") && navigator.maxTouchPoints > 0 && window.innerWidth / window.innerHeight > 1.6)

fun Window.isRunningOnIpad() =
    navigator.userAgent.contains("iPad") || (!navigator.userAgent.contains("Chrome") && navigator.maxTouchPoints > 0 && window.innerWidth / window.innerHeight <= 1.6)

private const val EVENT_BLUR = "blur"
private const val EVENT_FOCUS = "focus"
private const val EVENT_PAGE_HIDE = "pagehide"
private const val EVENT_PAGE_SHOW = "pageshow"
private const val EVENT_VISIBILITY_CHANGE = "visibilitychange"
