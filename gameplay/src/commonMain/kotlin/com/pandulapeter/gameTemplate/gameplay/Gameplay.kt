package com.pandulapeter.gameTemplate.gameplay

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.pointer.pointerInput
import com.pandulapeter.gameTemplate.engine.EngineCanvas
import com.pandulapeter.gameTemplate.engine.getEngine

private const val RECTANGLE_SIZE = 100f
private const val RECTANGLE_DISTANCE = 140f
private const val RECTANGLE_COUNT_ROOT = 15

private val rectangles = (0..RECTANGLE_COUNT_ROOT).flatMap { x ->
    (0..RECTANGLE_COUNT_ROOT).map { y ->
        Rectangle(
            color = Color.hsv((0..360).random().toFloat(), 0.1f, 0.9f),
            size = Size(RECTANGLE_SIZE, RECTANGLE_SIZE),
            position = Offset(x * RECTANGLE_DISTANCE, y * RECTANGLE_DISTANCE)
        )
    }
}

private const val KEYBOARD_CAMERA_CONTROL_SPEED = 10f

@Composable
fun GameplayCanvas(
    exit: () -> Unit,
) {
    Box(
        modifier = Modifier
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    getEngine().addToCameraOffset(-dragAmount)
                }
            }
    ) {
        EngineCanvas(
            gameObjects = rectangles,
            onKey = { key ->
                when (key) {
                    Key.DirectionLeft, Key.A -> getEngine().addToCameraOffset(Offset(-KEYBOARD_CAMERA_CONTROL_SPEED, 0f))
                    Key.DirectionUp, Key.W -> getEngine().addToCameraOffset(Offset(0f, -KEYBOARD_CAMERA_CONTROL_SPEED))
                    Key.DirectionRight, Key.D -> getEngine().addToCameraOffset(Offset(KEYBOARD_CAMERA_CONTROL_SPEED, 0f))
                    Key.DirectionDown, Key.S -> getEngine().addToCameraOffset(Offset(0f, KEYBOARD_CAMERA_CONTROL_SPEED))
                }
            },
            onKeyRelease = { key ->
                when (key) {
                    Key.Escape, Key.Back, Key.Backspace -> exit()
                }
            }
        )
    }
}