package com.pandulapeter.gameTemplate

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import com.pandulapeter.gameTemplate.gameplay.GameplayCanvas
import com.pandulapeter.gameTemplate.gameplay.GameplayController
import com.pandulapeter.gameTemplate.ui.UserInterface

@Composable
fun App(
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(Unit) { GameplayController.get().start() }
    GameplayCanvas()
    UserInterface(modifier)
}