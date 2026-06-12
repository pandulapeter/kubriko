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

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import com.pandulapeter.kubriko.implementation.isRunningOnIphone
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.events.Event

@ExperimentalWasmJsInterop
@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    ComposeViewport(
        configure = {
            isA11YEnabled = false
        }
    ) {
        val isInFullscreenMode = remember { mutableStateOf(if (window.isRunningOnIphone()) null else false) }
        val initialPath = remember { window.location.pathname.removePrefix(BuildConfig.WEB_ROOT_PATH_NAME) }
        val rootPath = remember { if (window.location.pathname == "/") "/" else "/${BuildConfig.WEB_ROOT_PATH_NAME}/" }
        val currentPath = remember { mutableStateOf(window.location.pathname) }
        DisposableEffect(Unit) {
            val onPopStateEventListener: (Event) -> Unit = {
                currentPath.value = window.location.pathname
            }
            val fullscreenListener: (Event) -> Unit = {
                if (isInFullscreenMode.value == true) {
                    isInFullscreenMode.value = document.fullscreenElement != null
                }
            }
            window.addEventListener(EVENT_POP_STATE, onPopStateEventListener)
            document.addEventListener(EVENT_FULLSCREEN_CHANGE, fullscreenListener)
            onDispose {
                window.removeEventListener(EVENT_POP_STATE, onPopStateEventListener)
                document.removeEventListener(EVENT_FULLSCREEN_CHANGE, fullscreenListener)
            }
        }
        KubrikoShowcase(
            isInFullscreenMode = isInFullscreenMode.value,
            getIsInFullscreenMode = { isInFullscreenMode.value },
            onFullscreenModeToggled = {
                isInFullscreenMode.value?.let { currentValue ->
                    isInFullscreenMode.value = !currentValue
                    if (currentValue) {
                        if (document.fullscreenElement != null) {
                            document.exitFullscreen()
                        }
                    } else {
                        document.documentElement?.requestFullscreen()
                    }
                }
            },
            deeplink = currentPath.value.removePrefix(rootPath),
            onDestinationChanged = { deeplink ->
                val modifiedDeeplink = deeplink?.let { rootPath + deeplink }
                if (deeplink == null && currentPath.value.removePrefix(rootPath).isNotBlank() && initialPath == rootPath) {
                    window.history.back()
                } else if (deeplink != null && currentPath.value.removePrefix(rootPath).isBlank()) {
                    window.history.pushState(null, "", modifiedDeeplink ?: rootPath)
                } else {
                    window.history.replaceState(null, "", modifiedDeeplink ?: rootPath)
                }
                if (document.fullscreenElement != null) {
                    document.exitFullscreen()
                }
                window.dispatchEvent(Event(EVENT_POP_STATE))
            },
        )
    }
}

private const val EVENT_POP_STATE = "popstate"
private const val EVENT_FULLSCREEN_CHANGE = "fullscreenchange"
