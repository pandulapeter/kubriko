package com.pandulapeter.kubrikoStressTest.implementation.extensions

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import com.pandulapeter.kubriko.managers.StateManager
import com.pandulapeter.kubriko.managers.ViewportManager

@OptIn(ExperimentalComposeUiApi::class)
internal actual fun Modifier.handleMouseZoom(
    stateManager: StateManager,
    viewportManager: ViewportManager,
): Modifier = onPointerEvent(PointerEventType.Scroll) {
    if (stateManager.isRunning.value) {
        viewportManager.multiplyScaleFactor(1f - it.changes.first().scrollDelta.y * 0.05f)
    }
}