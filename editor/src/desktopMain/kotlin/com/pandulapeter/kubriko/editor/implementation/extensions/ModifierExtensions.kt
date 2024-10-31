package com.pandulapeter.kubriko.editor.implementation.extensions

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.PointerMatcher
import androidx.compose.foundation.gestures.onDrag
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import com.pandulapeter.kubriko.engine.actor.traits.AvailableInEditor
import com.pandulapeter.kubriko.engine.implementation.extensions.occupiesPosition
import com.pandulapeter.kubriko.engine.managers.InputManager
import com.pandulapeter.kubriko.engine.managers.ViewportManager
import com.pandulapeter.kubriko.engine.types.WorldCoordinates

private var startOffset: WorldCoordinates? = null
private var isDragging = false

// TODO: Fix some clicks registering as drag
@OptIn(ExperimentalComposeUiApi::class)
internal fun Modifier.handleMouseClick(
    getSelectedInstance: () -> AvailableInEditor<*>?,
    getMouseWorldCoordinates: () -> WorldCoordinates,
    onLeftClick: (Offset) -> Unit,
    onRightClick: (Offset) -> Unit,
): Modifier = onPointerEvent(PointerEventType.Press) { event ->
    when (event.button) {
        PointerButton.Primary -> getSelectedInstance()?.let { selectedInstance ->
            getMouseWorldCoordinates().let { mouseWorldCoordinates ->
                if (selectedInstance.occupiesPosition(mouseWorldCoordinates)) {
                    startOffset = mouseWorldCoordinates - selectedInstance.position
                }
            }
        }
    }
}.onPointerEvent(PointerEventType.Release) { event ->
    when (event.button) {
        PointerButton.Primary -> {
            startOffset = null
            if (!isDragging) {
                event.changes.first().position.let(onLeftClick)
            }
            isDragging = false
        }

        PointerButton.Secondary -> {
            if (!isDragging) {
                event.changes.first().position.let(onRightClick)
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
internal fun Modifier.handleMouseMove(
    onMouseMove: (Offset) -> Unit,
): Modifier = onPointerEvent(PointerEventType.Move) {
    onMouseMove(it.changes.first().position)
}

@OptIn(ExperimentalComposeUiApi::class)
internal fun Modifier.handleMouseZoom(
    viewportManager: ViewportManager,
): Modifier = onPointerEvent(PointerEventType.Scroll) {
    viewportManager.multiplyScaleFactor(
        scaleFactor = 1f - it.changes.first().scrollDelta.y * 0.05f
    )
}

@OptIn(ExperimentalFoundationApi::class)
internal fun Modifier.handleMouseDrag(
    inputManager: InputManager,
    viewportManager: ViewportManager,
    getSelectedInstance: () -> AvailableInEditor<*>?,
    getMouseWorldCoordinates: () -> WorldCoordinates,
    notifySelectedInstanceUpdate: () -> Unit,
): Modifier = onDrag(
    matcher = PointerMatcher.mouse(PointerButton.Tertiary),
) { screenCoordinates ->
    viewportManager.addToCenter(screenCoordinates)
}.onDrag(
    matcher = PointerMatcher.mouse(PointerButton.Primary),
) { screenCoordinates ->
    isDragging = true
    if (inputManager.run { isKeyPressed(Key.ShiftLeft) || isKeyPressed(Key.ShiftRight) }) {
        viewportManager.addToCenter(screenCoordinates)
    } else {
        startOffset?.let { startOffset ->
            getSelectedInstance()?.let { selectedInstance ->
                selectedInstance.position = getMouseWorldCoordinates() - startOffset
                notifySelectedInstanceUpdate()
            }
        }
    }
}