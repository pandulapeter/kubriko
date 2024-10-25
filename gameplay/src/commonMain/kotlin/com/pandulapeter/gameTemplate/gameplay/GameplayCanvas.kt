package com.pandulapeter.gameTemplate.gameplay

import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.pandulapeter.gameTemplate.engine.EngineCanvas
import com.pandulapeter.gameTemplate.gameplay.implementation.extensions.handleMouseZoom
import com.pandulapeter.gameTemplate.gameplay.implementation.extensions.handleTouchGestures
import com.pandulapeter.gameTemplate.gameplay.implementation.handleKeyReleased
import com.pandulapeter.gameTemplate.gameplay.implementation.handleKeys


@Composable
fun GameplayCanvas(
    modifier: Modifier = Modifier,
) = EngineCanvas(
    modifier = modifier
        .handleMouseZoom()
        .handleTouchGestures()
        .background(Color.White),
    handleKeys = ::handleKeys,
    handleKeyReleased = ::handleKeyReleased,
)