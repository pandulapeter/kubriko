/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoShowcase

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.CanvasBasedWindow
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.w3c.dom.events.Event
import org.w3c.dom.events.KeyboardEvent

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    CanvasBasedWindow(applyDefaultStyles = false) {
        val isInFullscreenMode = remember { mutableStateOf(false) }
        val webEscapePressEvent = remember {
            MutableSharedFlow<Unit>(
                extraBufferCapacity = 1,
                onBufferOverflow = BufferOverflow.DROP_OLDEST
            )
        }
        val initialPath = remember { window.location.pathname.removePrefix(BuildConfig.WEB_ROOT_PATH_NAME) }
        val rootPath = remember { if (window.location.pathname == "/") "/" else "/${BuildConfig.WEB_ROOT_PATH_NAME}/" }
        val currentPath = remember { mutableStateOf(window.location.pathname) }
        DisposableEffect(Unit) {
            val onPopStateEventListener: (Event) -> Unit = {
                currentPath.value = window.location.pathname
            }
            val fullscreenListener: (Event) -> Unit = {
                if (isInFullscreenMode.value) {
                    isInFullscreenMode.value = document.fullscreenElement != null
                }
            }
            val keyUpListener: (Event) -> Unit = { event ->
                (event as? KeyboardEvent)?.let { keyboardEvent ->
                    if (keyboardEvent.code == "Escape") {
                        webEscapePressEvent.tryEmit(Unit)
                    }
                }
            }
            window.addEventListener(EVENT_POP_STATE, onPopStateEventListener)
            document.addEventListener(EVENT_FULLSCREEN_CHANGE, fullscreenListener)
            window.addEventListener(EVENT_KEY_UP, keyUpListener)
            onDispose {
                window.removeEventListener(EVENT_POP_STATE, onPopStateEventListener)
                document.removeEventListener(EVENT_FULLSCREEN_CHANGE, fullscreenListener)
                window.removeEventListener(EVENT_KEY_UP, keyUpListener)
            }
        }
        Box(
            modifier = Modifier.drawBehind {
                drawRect(
                    color = Color.Transparent,
                    size = size,
                    blendMode = BlendMode.Clear
                )
            }.fillMaxSize(),
        ) {
            KubrikoShowcase(
                isInFullscreenMode = isInFullscreenMode.value,
                getIsInFullscreenMode = { isInFullscreenMode.value },
                onFullscreenModeToggled = {
                    isInFullscreenMode.value = !isInFullscreenMode.value
                    if (isInFullscreenMode.value) {
                        document.documentElement?.requestFullscreen()
                    } else {
                        if (document.fullscreenElement != null) {
                            document.exitFullscreen()
                        }
                    }
                },
                webEscapePressEvent = webEscapePressEvent.asSharedFlow(),
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
                    window.dispatchEvent(Event(EVENT_POP_STATE))
                },
            )
        }
    }
}

private const val EVENT_POP_STATE = "popstate"
private const val EVENT_FULLSCREEN_CHANGE = "fullscreenchange"
private const val EVENT_KEY_UP = "keyup"
