package com.pandulapeter.gameTemplate.gameStressTest

import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.pandulapeter.gameTemplate.engine.EngineCanvas
import com.pandulapeter.gameTemplate.gameStressTest.implementation.GameplayController
import com.pandulapeter.gameTemplate.gameStressTest.implementation.UserInterface
import com.pandulapeter.gameTemplate.gameStressTest.implementation.extensions.handleDragAndPan
import com.pandulapeter.gameTemplate.gameStressTest.implementation.extensions.handleMouseZoom

@Composable
fun GameStressTest(
    modifier: Modifier = Modifier,
) {
    EngineCanvas(
        modifier = Modifier
            .handleMouseZoom(
                stateManager = GameplayController.engine.stateManager,
                viewportManager = GameplayController.engine.viewportManager,
            )
            .handleDragAndPan(
                stateManager = GameplayController.engine.stateManager,
                viewportManager = GameplayController.engine.viewportManager,
            )
            .background(Color.White),
        engine = GameplayController.engine,
    )
    UserInterface(
        modifier = modifier,
        metadata = GameplayController.metadata.collectAsState().value,
        isRunning = GameplayController.engine.stateManager.isRunning.collectAsState().value,
        updateIsRunning = GameplayController.engine.stateManager::updateIsRunning,
    )
}