package com.pandulapeter.kubrikoPong

import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.KubrikoCanvas
import com.pandulapeter.kubriko.debugInfo.DebugInfo
import com.pandulapeter.kubrikoPong.implementation.GameplayController
import com.pandulapeter.kubrikoPong.implementation.UserInterface

@Composable
fun GamePong(
    modifier: Modifier = Modifier,
) {
    val gameplayController = remember { GameplayController(Kubriko.newInstance()) }
    KubrikoCanvas(
        modifier = Modifier.background(Color.White),
        kubriko = gameplayController.kubriko,
    )
    UserInterface(
        modifier = modifier,
        isRunning = gameplayController.stateManager.isRunning.collectAsState().value,
        updateIsRunning = gameplayController.stateManager::updateIsRunning,
        debugInfo = { DebugInfo(kubriko = gameplayController.kubriko) },
    )
}