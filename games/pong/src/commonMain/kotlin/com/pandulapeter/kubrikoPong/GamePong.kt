package com.pandulapeter.kubrikoPong

import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.KubrikoCanvas
import com.pandulapeter.kubriko.debugMenu.DebugMenu
import com.pandulapeter.kubrikoPong.implementation.GameplayManager
import com.pandulapeter.kubrikoPong.implementation.UserInterface

@Composable
fun GamePong(
    modifier: Modifier = Modifier,
) {
    val gameplayManager = remember { GameplayManager() }
    val kubriko = remember {
        Kubriko.newInstance(
            gameplayManager,
        )
    }
    DebugMenu(kubriko = kubriko) {
        KubrikoCanvas(
            modifier = Modifier.background(Color.White),
            kubriko = kubriko,
        )
        UserInterface(
            modifier = modifier,
            isRunning = gameplayManager.stateManager.isRunning.collectAsState().value,
            updateIsRunning = gameplayManager.stateManager::updateIsRunning,
        )
    }
}