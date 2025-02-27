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

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.pandulapeter.kubriko.demoPerformance.PerformanceDemoSceneEditor
import com.pandulapeter.kubriko.demoPhysics.PhysicsDemoSceneEditor
import com.pandulapeter.kubriko.gameAnnoyedPenguins.AnnoyedPenguinsGameSceneEditor
import com.pandulapeter.kubriko.implementation.windowState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.awt.Dimension
import java.awt.Rectangle
import java.awt.event.WindowStateListener

fun main() = application {
    windowState = rememberWindowState(
        size = DpSize(860.dp, 640.dp),
    )
    val coroutineScope = rememberCoroutineScope()
    val previousBounds = remember { mutableStateOf<Rectangle?>(null) }
    val previousWindowPlacement = remember { mutableStateOf<WindowPlacement?>(null) }
    val isInFullscreenMode = remember { mutableStateOf(false) }
    Window(
        onCloseRequest = ::exitApplication,
        state = windowState,
        title = "Kubriko",
    ) {
        DisposableEffect(Unit) {
            val listener = WindowStateListener {
                if (isInFullscreenMode.value) {
                    isInFullscreenMode.value = windowState.placement == WindowPlacement.Fullscreen
                }
            }
            window.addWindowStateListener(listener)
            onDispose {
                window.removeWindowStateListener(listener)
            }
        }
        window.minimumSize = Dimension(400, 400)
        KubrikoShowcase(
            isInFullscreenMode = isInFullscreenMode.value,
            getIsInFullscreenMode = { isInFullscreenMode.value },
            onFullscreenModeToggled = {
                isInFullscreenMode.value = !isInFullscreenMode.value
                if (isInFullscreenMode.value) {
                    previousBounds.value = window.bounds
                    previousWindowPlacement.value = windowState.placement
                    windowState.placement = WindowPlacement.Fullscreen
                } else {
                    previousWindowPlacement.value?.let { previousWindowPlacement ->
                        windowState.placement = previousWindowPlacement
                        previousBounds.value?.let {
                            coroutineScope.launch {
                                delay(100)
                                window.bounds = it
                            }
                        }
                    }
                }
            },
        )
    }
    AnnoyedPenguinsGameSceneEditor(
        defaultSceneFolderPath = "../examples/game-annoyed-penguins/src/commonMain/composeResources/files/scenes"
    )
    PerformanceDemoSceneEditor(
        defaultSceneFolderPath = "../examples/test-performance/src/commonMain/composeResources/files/scenes"
    )
    PhysicsDemoSceneEditor(
        defaultSceneFolderPath = "../examples/demo-physics/src/commonMain/composeResources/files/scenes"
    )
}