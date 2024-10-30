package com.pandulapeter.gameTemplate.gamePong

import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.pandulapeter.gameTemplate.engine.Engine
import com.pandulapeter.gameTemplate.engine.EngineCanvas
import com.pandulapeter.gameTemplate.gamePong.implementation.GameplayController
import com.pandulapeter.gameTemplate.gamePong.implementation.UserInterface

@Composable
fun GamePong(
    modifier: Modifier = Modifier,
) {
    val engine = remember { Engine.newInstance() }
    val gameplayController = remember { GameplayController(engine) }
    EngineCanvas(
        modifier = Modifier.background(Color.White),
        engine = engine,
    )
    UserInterface(
        modifier = modifier,
        metadata = gameplayController.metadata.collectAsState().value,
        isRunning = engine.stateManager.isRunning.collectAsState().value,
        updateIsRunning = engine.stateManager::updateIsRunning,
    )
}