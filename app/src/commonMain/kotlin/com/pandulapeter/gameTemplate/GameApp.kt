package com.pandulapeter.gameTemplate

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.pandulapeter.gameTemplate.gameplayController.GameplayCanvas
import com.pandulapeter.gameTemplate.ui.UserInterface

@Composable
fun GameApp(
    modifier: Modifier = Modifier,
) {
    GameplayCanvas()
    UserInterface(modifier)
}