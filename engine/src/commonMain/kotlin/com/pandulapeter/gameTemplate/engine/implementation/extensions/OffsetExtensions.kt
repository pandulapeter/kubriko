package com.pandulapeter.gameTemplate.engine.implementation.extensions

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size

internal operator fun Size.minus(offset: Offset) = Offset(
    x = width - offset.x,
    y = height - offset.y,
)

internal fun Offset.toWorldCoordinates(
    viewportOffset: Offset,
    viewportScaleFactor: Float,
) = Offset(
    x = viewportOffset.x + (x * viewportScaleFactor),
    y = viewportOffset.y + (y * viewportScaleFactor),
)