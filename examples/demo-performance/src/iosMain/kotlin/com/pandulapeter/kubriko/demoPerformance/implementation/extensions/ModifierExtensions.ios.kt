package com.pandulapeter.kubriko.demoPerformance.implementation.extensions

import androidx.compose.ui.Modifier
import com.pandulapeter.kubriko.manager.StateManager
import com.pandulapeter.kubriko.manager.ViewportManager

internal actual fun Modifier.handleMouseZoom(
    stateManager: StateManager,
    viewportManager: ViewportManager,
): Modifier = this