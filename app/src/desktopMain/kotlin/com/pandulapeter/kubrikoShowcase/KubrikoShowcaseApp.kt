package com.pandulapeter.kubrikoShowcase

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.pandulapeter.kubriko.demoPerformance.PerformanceDemoSceneEditor
import com.pandulapeter.kubriko.demoPhysics.PhysicsDemoSceneEditor
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.awt.Dimension
import java.awt.Rectangle
import java.awt.event.WindowStateListener

fun main() = application {
    val windowState = rememberWindowState()
    val coroutineScope = rememberCoroutineScope()
    val previousBounds = remember { mutableStateOf<Rectangle?>(null) }
    val previousWindowPlacement = remember { mutableStateOf<WindowPlacement?>(null) }
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
    PerformanceDemoSceneEditor(
        defaultSceneFolderPath = "../examples/demo-performance/src/commonMain/composeResources/files/scenes"
    )
    PhysicsDemoSceneEditor(
        defaultSceneFolderPath = "../examples/demo-physics/src/commonMain/composeResources/files/scenes"
    )
}

private val isInFullscreenMode = mutableStateOf(false)