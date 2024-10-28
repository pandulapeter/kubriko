package com.pandulapeter.gameTemplate.gameStressTest

import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.pandulapeter.gameTemplate.engine.EngineCanvas
import com.pandulapeter.gameTemplate.gameStressTest.implementation.extensions.handleMouseZoom
import com.pandulapeter.gameTemplate.gameStressTest.implementation.extensions.handleDragAndPan


@Composable
fun GameplayCanvas(
    modifier: Modifier = Modifier,
) = EngineCanvas(
    modifier = modifier
        .handleMouseZoom()
        .handleDragAndPan()
        .background(Color.White),
)