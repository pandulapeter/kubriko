package com.pandulapeter.kubrikoPerformanceTest

import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.pandulapeter.kubriko.KubrikoCanvas
import com.pandulapeter.kubriko.debugMenu.DebugMenu
import com.pandulapeter.kubrikoPerformanceTest.implementation.KubrikoWrapper
import com.pandulapeter.kubrikoPerformanceTest.implementation.UserInterface
import com.pandulapeter.kubrikoPerformanceTest.implementation.extensions.handleDragAndPan
import com.pandulapeter.kubrikoPerformanceTest.implementation.extensions.handleMouseZoom

@Composable
fun GamePerformanceTest(
    modifier: Modifier = Modifier,
) = MaterialTheme {
    val kubrikoWrapper = remember { KubrikoWrapper() }
    DebugMenu(
        contentModifier = modifier,
        kubriko = kubrikoWrapper.kubriko,
    ) {
        KubrikoCanvas(
            modifier = Modifier
                .handleMouseZoom(
                    stateManager = kubrikoWrapper.gameplayManager.stateManager,
                    viewportManager = kubrikoWrapper.gameplayManager.viewportManager,
                )
                .handleDragAndPan(
                    stateManager = kubrikoWrapper.gameplayManager.stateManager,
                    viewportManager = kubrikoWrapper.gameplayManager.viewportManager,
                ),
            kubriko = kubrikoWrapper.kubriko,
        )
        UserInterface(
            modifier = modifier,
            isRunning = kubrikoWrapper.gameplayManager.stateManager.isRunning.collectAsState().value,
            updateIsRunning = kubrikoWrapper.gameplayManager.stateManager::updateIsRunning,
        )
    }
}