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
private const val RECTANGLE_COUNT = 15

private val rectangles = (-RECTANGLE_COUNT..RECTANGLE_COUNT).flatMap { x ->
    (-RECTANGLE_COUNT..RECTANGLE_COUNT).map { y ->
        Rectangle(
            color = Color.hsv((0..360).random().toFloat(), 0.1f, 0.9f),
            size = Size(RECTANGLE_SIZE, RECTANGLE_SIZE),
            position = Offset(x * RECTANGLE_DISTANCE, y * RECTANGLE_DISTANCE)
        )
    }
}

private const val CAMERA_SPEED = 10f
private const val CAMERA_SPEED_DIAGONAL = 7.07f

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
            onKeys = { keys ->
                getEngine().addToCameraOffset(
                    when (keys.direction) {
                        KeyboardDirection.NONE -> Offset.Zero
                        KeyboardDirection.LEFT -> Offset(-CAMERA_SPEED, 0f)
                        KeyboardDirection.UP_LEFT -> Offset(-CAMERA_SPEED_DIAGONAL, -CAMERA_SPEED_DIAGONAL)
                        KeyboardDirection.UP -> Offset(0f, -CAMERA_SPEED)
                        KeyboardDirection.UP_RIGHT -> Offset(CAMERA_SPEED_DIAGONAL, -CAMERA_SPEED_DIAGONAL)
                        KeyboardDirection.RIGHT -> Offset(CAMERA_SPEED, 0f)
                        KeyboardDirection.DOWN_RIGHT -> Offset(CAMERA_SPEED_DIAGONAL, CAMERA_SPEED_DIAGONAL)
                        KeyboardDirection.DOWN -> Offset(0f, CAMERA_SPEED)
                        KeyboardDirection.DOWN_LEFT -> Offset(-CAMERA_SPEED_DIAGONAL, CAMERA_SPEED_DIAGONAL)
                    }
                )
            },
            onKeyRelease = { key ->
                when (key) {
                    Key.Escape, Key.Back, Key.Backspace -> exit()
                }
            }
        )
    }
}