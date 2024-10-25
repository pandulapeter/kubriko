package com.pandulapeter.gameTemplate.gameplayController

import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.pandulapeter.gameTemplate.engine.EngineCanvas
import com.pandulapeter.gameTemplate.gameplayController.implementation.extensions.handleMouseZoom
import com.pandulapeter.gameTemplate.gameplayController.implementation.extensions.handleTouchGestures


@Composable
fun GameplayCanvas(
    modifier: Modifier = Modifier,
) = EngineCanvas(
    modifier = modifier
        .handleMouseZoom()
        .handleTouchGestures()
        .background(Color.White),
)