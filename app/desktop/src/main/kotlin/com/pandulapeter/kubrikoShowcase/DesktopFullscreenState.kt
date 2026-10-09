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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.awt.Point
import java.awt.Rectangle

/**
 * Whether the Showcase is in fullscreen mode, and the window geometry saved on entering it, restored on leaving it.
 */
internal class DesktopFullscreenState {

    var isInFullscreenMode by mutableStateOf(false)
        private set
    private val previousBounds = mutableStateOf<Rectangle?>(null)
    private val previousWindowPlacement = mutableStateOf<WindowPlacement?>(null)
    private val previousWindowLocation = mutableStateOf<Point?>(null)
    private val previousWindowPosition = mutableStateOf<WindowPosition?>(null)
    private val windowSize = mutableStateOf(DpSize.Unspecified)

    fun toggle(window: ComposeWindow, windowState: WindowState, coroutineScope: CoroutineScope) {
        isInFullscreenMode.let { currentValue ->
            isInFullscreenMode = !currentValue
            if (currentValue) {
                previousWindowPlacement.value?.let { previousWindowPlacement ->
                    windowState.placement = previousWindowPlacement
                    windowState.size = windowSize.value
                    if (isWindows) {
                        previousWindowPosition.value?.let { windowState.position = it }
                    } else {
                        previousWindowLocation.value?.let {
                            window.setLocation(it.x, it.y)
                        }
                        previousBounds.value?.let {
                            coroutineScope.launch {
                                delay(100)
                                window.bounds = it
                            }
                        }
                    }
                }
            } else {
                windowSize.value = windowState.size
                previousBounds.value = window.bounds
                previousWindowPlacement.value = windowState.placement
                previousWindowLocation.value = window.location
                previousWindowPosition.value = windowState.position
                windowState.placement = WindowPlacement.Fullscreen
            }
        }
    }

    /** Leaves fullscreen mode when the system takes the window out of it (e.g. Escape on macOS). */
    fun onWindowStateChanged(windowState: WindowState) {
        if (isInFullscreenMode) {
            isInFullscreenMode = windowState.placement == WindowPlacement.Fullscreen
        }
    }
}

@Composable
internal fun rememberDesktopFullscreenState() = remember { DesktopFullscreenState() }
