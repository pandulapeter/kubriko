package com.pandulapeter.gameTemplate.editor.implementation.extensions

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.PointerMatcher
import androidx.compose.foundation.gestures.onDrag
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import com.pandulapeter.gameTemplate.editor.implementation.EditorController
import com.pandulapeter.gameTemplate.engine.Engine
import com.pandulapeter.gameTemplate.engine.gameObject.properties.Visible

@OptIn(ExperimentalComposeUiApi::class)
internal actual fun Modifier.handleMouseMove(): Modifier = onPointerEvent(PointerEventType.Move) {
    EditorController.handleMouseMove(it.changes.first().position)
}

@OptIn(ExperimentalComposeUiApi::class)
internal actual fun Modifier.handleMouseZoom(): Modifier = onPointerEvent(PointerEventType.Scroll) {
    Engine.get().viewportManager.multiplyScaleFactor(
        scaleFactor = 1f - it.changes.first().scrollDelta.y * 0.05f
    )
}

@OptIn(ExperimentalFoundationApi::class)
internal actual fun Modifier.handleMouseDrag(): Modifier = onDrag(
    matcher = PointerMatcher.mouse(PointerButton.Tertiary),
) { screenCoordinates ->
    Engine.get().viewportManager.addToOffset(screenCoordinates)
}.onDrag(
    matcher = PointerMatcher.mouse(PointerButton.Primary),
) { screenCoordinates ->
    if (Engine.get().inputManager.run { isKeyPressed(Key.ShiftLeft) || isKeyPressed(Key.ShiftRight) }) {
        Engine.get().viewportManager.addToOffset(screenCoordinates)
    } else {
        (EditorController.selectedGameObject.value.first as? Visible)?.let { visible ->
            visible.position = EditorController.mouseWorldPosition.value
            EditorController.notifyGameObjectUpdate()
        }
    }
}