package com.pandulapeter.gameTemplate.gameStressTest.implementation.extensions

import androidx.compose.ui.Modifier
import com.pandulapeter.gameTemplate.engine.managers.StateManager
import com.pandulapeter.gameTemplate.engine.managers.ViewportManager

internal actual fun Modifier.handleMouseZoom(
    stateManager: StateManager,
    viewportManager: ViewportManager,
): Modifier = this