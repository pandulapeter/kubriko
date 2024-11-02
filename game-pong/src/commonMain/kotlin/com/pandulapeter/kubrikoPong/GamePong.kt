package com.pandulapeter.kubrikoPong

import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.pandulapeter.kubriko.engine.Kubriko
import com.pandulapeter.kubriko.engine.EngineCanvas
import com.pandulapeter.kubrikoPong.implementation.GameObjectRegistry
import com.pandulapeter.kubrikoPong.implementation.GameplayController
import com.pandulapeter.kubrikoPong.implementation.UserInterface

@Composable
fun GamePong(
    modifier: Modifier = Modifier,
) {
    val kubriko = remember { Kubriko.newInstance(editableMetadata = GameObjectRegistry.typesAvailableInEditor) }
    val gameplayController = remember { GameplayController(kubriko) }
    EngineCanvas(
        modifier = Modifier.background(Color.White),
        kubriko = kubriko,
    )
    UserInterface(
        modifier = modifier,
        metadata = gameplayController.metadata.collectAsState().value,
        isRunning = kubriko.stateManager.isRunning.collectAsState().value,
        updateIsRunning = kubriko.stateManager::updateIsRunning,
    )
}