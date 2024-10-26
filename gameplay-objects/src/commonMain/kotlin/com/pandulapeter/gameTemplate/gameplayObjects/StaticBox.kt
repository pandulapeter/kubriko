package com.pandulapeter.gameTemplate.gameplayObjects

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color

class StaticBox(
    color: Color,
    edgeSize: Float,
    position: Offset,
    rotationDegrees: Float,
) : Box(
    color = color,
    edgeSize = edgeSize,
    position = position,
    rotationDegrees = rotationDegrees,
)
