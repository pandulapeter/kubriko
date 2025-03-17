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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.key
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
import com.pandulapeter.kubriko.gameBlockysJourney.BlockysJourneyGameSceneEditor
import com.pandulapeter.kubriko.implementation.windowState
import com.pandulapeter.kubriko.manager.MetadataManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kubriko.app.generated.resources.Res
import kubriko.app.generated.resources.ic_icon
import org.jetbrains.compose.resources.painterResource
import java.awt.Dimension
import java.awt.Point
import java.awt.Rectangle
import java.awt.event.WindowStateListener

fun main() {
    System.setProperty("apple.awt.application.name", "Kubriko Showcase")
    application {
        windowState = rememberWindowState(
            size = DpSize(860.dp, 660.dp),
        )
        val coroutineScope = rememberCoroutineScope()
        val previousBounds = remember { mutableStateOf<Rectangle?>(null) }
        val previousWindowPlacement = remember { mutableStateOf<WindowPlacement?>(null) }
        val previousWindowLocation = remember { mutableStateOf<Point?>(null) }
        val windowSize = remember { mutableStateOf(windowState.size) }
        val isInFullscreenMode = remember { mutableStateOf(false) }

        val isRunningOnWindows = remember { MetadataManager.newInstance().platform is MetadataManager.Platform.Desktop.Windows }

        @Composable
        fun KubrikoShowcaseWindow(
            undecorated: Boolean,
            resizable: Boolean,
        ) = Window(
            onCloseRequest = ::exitApplication,
            state = windowState,
            title = "Kubriko Showcase",
            undecorated = undecorated,
            resizable = resizable,
            icon = painterResource(Res.drawable.ic_icon),
        ) {
            DisposableEffect(Unit) {
                val listener = WindowStateListener {
                    if (isInFullscreenMode.value == true) {
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
                    isInFullscreenMode.value.let { currentValue ->
                        isInFullscreenMode.value = !currentValue
                        if (currentValue) {
                            previousWindowPlacement.value?.let { previousWindowPlacement ->
                                windowState.placement = previousWindowPlacement
                                windowState.size = windowSize.value
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
                        } else {
                            windowSize.value = windowState.size
                            previousBounds.value = window.bounds
                            previousWindowPlacement.value = windowState.placement
                            previousWindowLocation.value = window.location
                            windowState.placement = WindowPlacement.Fullscreen
                        }
                    }
                },
            )
        }
        AnnoyedPenguinsGameSceneEditor(
            defaultSceneFolderPath = "../examples/game-annoyed-penguins/src/commonMain/composeResources/files/scenes"
        )
        BlockysJourneyGameSceneEditor(
            defaultSceneFolderPath = "../examples/game-blockys-journey/src/commonMain/composeResources/files/scenes"
        )
        PerformanceDemoSceneEditor(
            defaultSceneFolderPath = "../examples/demo-performance/src/commonMain/composeResources/files/scenes"
        )
        PhysicsDemoSceneEditor(
            defaultSceneFolderPath = "../examples/demo-physics/src/commonMain/composeResources/files/scenes"
        )

        if (isRunningOnWindows) {
            key(isInFullscreenMode.value) {
                KubrikoShowcaseWindow(
                    undecorated = isInFullscreenMode.value,
                    resizable = !isInFullscreenMode.value,
                )
            }
        } else {
            KubrikoShowcaseWindow(
                undecorated = false,
                resizable = true,
            )
        }
    }
}