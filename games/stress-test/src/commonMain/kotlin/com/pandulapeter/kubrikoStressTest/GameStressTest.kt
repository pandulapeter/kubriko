package com.pandulapeter.kubrikoStressTest

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import com.pandulapeter.kubriko.EngineCanvas
import com.pandulapeter.kubriko.debugInfo.DebugInfo
import com.pandulapeter.kubrikoStressTest.implementation.GameplayController
import com.pandulapeter.kubrikoStressTest.implementation.UserInterface
import com.pandulapeter.kubrikoStressTest.implementation.extensions.handleDragAndPan
import com.pandulapeter.kubrikoStressTest.implementation.extensions.handleMouseZoom

@Composable
fun GameStressTest(
    modifier: Modifier = Modifier,
) {
    EngineCanvas(
        modifier = Modifier
            .handleMouseZoom(
                stateManager = GameplayController.kubriko.stateManager,
                viewportManager = GameplayController.kubriko.viewportManager,
            )
            .handleDragAndPan(
                stateManager = GameplayController.kubriko.stateManager,
                viewportManager = GameplayController.kubriko.viewportManager,
            ),
        kubriko = GameplayController.kubriko,
    )
    UserInterface(
        modifier = modifier,
        isRunning = GameplayController.kubriko.stateManager.isRunning.collectAsState().value,
        updateIsRunning = GameplayController.kubriko.stateManager::updateIsRunning,
        debugInfo = { DebugInfo(kubriko = GameplayController.kubriko) },
    )
}