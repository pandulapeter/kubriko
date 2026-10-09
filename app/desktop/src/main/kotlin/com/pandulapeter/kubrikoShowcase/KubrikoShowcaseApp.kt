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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.awt.SwingWindow
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowDecoration
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.pandulapeter.kubriko.demoPerformance.PerformanceDemoSceneEditor
import com.pandulapeter.kubriko.demoPhysics.PhysicsDemoSceneEditor
import com.pandulapeter.kubriko.gameAnnoyedPenguins.AnnoyedPenguinsGameSceneEditor
import com.pandulapeter.kubriko.gameBlockysJourney.BlockysJourneyGameSceneEditor
import com.pandulapeter.kubriko.implementation.windowState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kubriko.app.desktop.generated.resources.Res
import kubriko.app.desktop.generated.resources.ic_icon
import kubriko.app.desktop.generated.resources.kubriko_showcase
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import java.awt.Color
import java.awt.Component
import java.awt.Container
import java.awt.Dimension
import java.awt.Point
import java.awt.Rectangle
import java.awt.event.WindowStateListener

@OptIn(ExperimentalComposeUiApi::class)
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
        val previousWindowPosition = remember { mutableStateOf<WindowPosition?>(null) }
        val windowSize = remember { mutableStateOf(windowState.size) }
        val isInFullscreenMode = remember { mutableStateOf(false) }


        @Composable
        fun KubrikoShowcaseWindow(
            undecorated: Boolean,
            resizable: Boolean,
        ) {
            var titleBar by remember { mutableStateOf<ExtendedTitleBar?>(null) }
            SwingWindow(
                onCloseRequest = ::exitApplication,
                state = windowState,
                title = stringResource(Res.string.kubriko_showcase),
                decoration = if (undecorated) WindowDecoration.Undecorated() else WindowDecoration.SystemDefault,
                resizable = resizable,
                icon = painterResource(Res.drawable.ic_icon),
                init = { window -> if (!undecorated) titleBar = window.extendContentIntoTitleBar() },
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
                titleBar?.let { TitleBarAppearance(window = window, titleBar = it) }
                TitleBarInsets(
                    titleBar = titleBar,
                    isFullscreen = windowState.placement == WindowPlacement.Fullscreen,
                ) {
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
                        },
                        onBackgroundColorChanged = { color -> window.setUndrawnAreaColor(Color(color.toArgb())) },
                    )
                }
            }
        }
        AnnoyedPenguinsGameSceneEditor(
            defaultSceneFolderPath = "../../examples/game-annoyed-penguins/src/commonMain/composeResources/files/scenes"
        )
        BlockysJourneyGameSceneEditor(
            defaultSceneFolderPath = "../../examples/game-blockys-journey/src/commonMain/composeResources/files/scenes"
        )
        PerformanceDemoSceneEditor(
            defaultSceneFolderPath = "../../examples/demo-performance/src/commonMain/composeResources/files/scenes"
        )
        PhysicsDemoSceneEditor(
            defaultSceneFolderPath = "../../examples/demo-physics/src/commonMain/composeResources/files/scenes"
        )

        if (isWindows) {
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

/**
 * What the window shows wherever the Showcase has not been drawn yet - the edge a fast resize uncovers before the next
 * frame fills it - which is white unless it is told otherwise. On macOS that is the window's own background, and the
 * native surface the content is rendered into, a heavyweight component that takes its color when it is created rather
 * than following its ancestors'. On Windows it is the opaque Swing panels between the two as well, which Swing repaints
 * the uncovered edge with before the next frame arrives, in the look and feel's panel gray.
 */
private fun ComposeWindow.setUndrawnAreaColor(color: Color) {
    background = color
    fun Component.paintUndrawnArea() {
        if (isWindows || !isLightweight) background = color
        (this as? Container)?.components?.forEach { it.paintUndrawnArea() }
    }
    rootPane.paintUndrawnArea()
}
