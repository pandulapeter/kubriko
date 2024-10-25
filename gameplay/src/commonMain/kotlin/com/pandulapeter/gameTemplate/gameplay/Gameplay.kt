package com.pandulapeter.gameTemplate.gameplay

import androidx.compose.foundation.gestures.detectTransformGestures
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
import kotlin.math.PI
import kotlin.math.sin

private const val RECTANGLE_SIZE = 100f
private const val RECTANGLE_DISTANCE = 200f
private const val RECTANGLE_COUNT = 200

private val rectangles = (-RECTANGLE_COUNT..RECTANGLE_COUNT).flatMap { x ->
    (-RECTANGLE_COUNT..RECTANGLE_COUNT).map { y ->
        Rectangle(
            color = Color.hsv((0..360).random().toFloat(), 0.2f, 0.9f),
            size = Size(RECTANGLE_SIZE, RECTANGLE_SIZE) * ((50..150).random() / 100f),
            position = Offset(x * RECTANGLE_DISTANCE + (-50..50).random(), y * RECTANGLE_DISTANCE + (-50..50).random()),
            rotationDegrees = (0..360).random().toFloat(),
        )
    }
}

private const val CAMERA_SPEED = 15f
private val CAMERA_SPEED_DIAGONAL = (sin(PI / 4)* CAMERA_SPEED).toFloat()

@Composable
fun GameplayCanvas(
    exit: () -> Unit,
) {
    Box(
        modifier = Modifier.getGameplayCanvasModifier()
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    getEngine().addToCameraOffset(-pan)
                    getEngine().multiplyCameraScaleFactor(zoom)
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
                getEngine().multiplyCameraScaleFactor(
                    when (keys.zoom) {
                        KeyboardZoom.NONE -> 1f
                        KeyboardZoom.ZOOM_IN -> 1.02f
                        KeyboardZoom.ZOOM_OUT -> 0.98f
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