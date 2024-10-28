package com.pandulapeter.gameTemplate.gamePong

import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.pandulapeter.gameTemplate.engine.EngineCanvas

@Composable
fun GamePong(
    modifier: Modifier = Modifier,
) {
    EngineCanvas(
        modifier = Modifier.background(Color.White),
    )
    UserInterface(modifier)
}