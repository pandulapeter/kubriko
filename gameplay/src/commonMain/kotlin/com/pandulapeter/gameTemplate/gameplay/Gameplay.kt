package com.pandulapeter.gameTemplate.gameplay

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import com.pandulapeter.gameTemplate.engine.EngineCanvas

private const val RECTANGLE_SIZE = 100f
private const val RECTANGLE_DISTANCE = 140f
private const val RECTANGLE_COUNT_ROOT = 100

private val rectangles = (0..RECTANGLE_COUNT_ROOT).flatMap { x ->
    (0..RECTANGLE_COUNT_ROOT).map { y ->
        Rectangle(
            color = Color.hsv((0..360).random().toFloat(), 0.1f, 0.9f),
            size = Size(RECTANGLE_SIZE, RECTANGLE_SIZE),
            position = Offset(x * RECTANGLE_DISTANCE, y * RECTANGLE_DISTANCE)
        )
    }
}

@Composable
fun GameplayCanvas() = EngineCanvas(
    gameObjects = rectangles
)