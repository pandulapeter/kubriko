package com.pandulapeter.kubrikoStressTest

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import com.pandulapeter.kubriko.KubrikoCanvas
import com.pandulapeter.kubriko.debugInfo.DebugInfo
import com.pandulapeter.kubrikoStressTest.implementation.GameplayController
import com.pandulapeter.kubrikoStressTest.implementation.UserInterface
import com.pandulapeter.kubrikoStressTest.implementation.extensions.handleDragAndPan
import com.pandulapeter.kubrikoStressTest.implementation.extensions.handleMouseZoom

@Composable
fun GameStressTest(
    modifier: Modifier = Modifier,
) {
    KubrikoCanvas(
        modifier = Modifier
            .handleMouseZoom(
                stateManager = GameplayController.stateManager,
                viewportManager = GameplayController.viewportManager,
            )
            .handleDragAndPan(
                stateManager = GameplayController.stateManager,
                viewportManager = GameplayController.viewportManager,
            ),
        kubriko = GameplayController.kubriko,
    )
    UserInterface(
        modifier = modifier,
        isRunning = GameplayController.stateManager.isRunning.collectAsState().value,
        updateIsRunning = GameplayController.stateManager::updateIsRunning,
        debugInfo = { DebugInfo(kubriko = GameplayController.kubriko) },
    )
}