package com.pandulapeter.gameTemplate.editor.implementation.extensions

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import com.pandulapeter.gameTemplate.editor.implementation.EditorController
import com.pandulapeter.gameTemplate.engine.Engine

@OptIn(ExperimentalComposeUiApi::class)
internal actual fun Modifier.handleMouseZoom(): Modifier = onPointerEvent(PointerEventType.Scroll) {
        Engine.get().viewportManager.multiplyScaleFactor(
            scaleFactor = 1f - it.changes.first().scrollDelta.y * 0.05f
        )
    }

@OptIn(ExperimentalComposeUiApi::class)
internal actual fun Modifier.handleMouseMove(): Modifier = onPointerEvent(PointerEventType.Move) {
    EditorController.handleMouseMove(it.changes.first().position)
}