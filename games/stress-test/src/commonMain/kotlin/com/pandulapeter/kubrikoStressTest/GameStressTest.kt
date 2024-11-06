package com.pandulapeter.kubrikoStressTest

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.pandulapeter.kubriko.KubrikoCanvas
import com.pandulapeter.kubriko.debugMenu.DebugMenu
import com.pandulapeter.kubrikoStressTest.implementation.KubrikoWrapper
import com.pandulapeter.kubrikoStressTest.implementation.UserInterface
import com.pandulapeter.kubrikoStressTest.implementation.extensions.handleDragAndPan
import com.pandulapeter.kubrikoStressTest.implementation.extensions.handleMouseZoom

@Composable
fun GameStressTest(
    modifier: Modifier = Modifier,
) {
    val kubrikoWrapper = remember { KubrikoWrapper() }
    DebugMenu(
        kubriko = kubrikoWrapper.kubriko
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