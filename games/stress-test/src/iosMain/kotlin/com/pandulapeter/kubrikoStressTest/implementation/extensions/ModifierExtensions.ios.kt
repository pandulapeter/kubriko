package com.pandulapeter.kubrikoStressTest.implementation.extensions

import androidx.compose.ui.Modifier
import com.pandulapeter.kubriko.managers.StateManager
import com.pandulapeter.kubriko.managers.ViewportManager

internal actual fun Modifier.handleMouseZoom(
    stateManager: StateManager,
    viewportManager: ViewportManager,
): Modifier = this