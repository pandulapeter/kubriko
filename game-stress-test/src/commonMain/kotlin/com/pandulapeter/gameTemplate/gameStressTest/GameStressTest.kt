package com.pandulapeter.gameTemplate.gameStressTest

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun GameStressTest(
    modifier: Modifier = Modifier,
) {
    GameplayCanvas()
    UserInterface(modifier)
}