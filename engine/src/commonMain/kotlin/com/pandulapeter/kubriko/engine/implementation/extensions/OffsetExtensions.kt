package com.pandulapeter.kubriko.engine.implementation.extensions

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.pandulapeter.kubriko.engine.managers.ViewportManager
import com.pandulapeter.kubriko.engine.types.WorldCoordinates

fun Offset.toWorldCoordinates(viewportManager: ViewportManager): WorldCoordinates = toWorldCoordinates(
    viewportCenter = viewportManager.center.value,
    viewportSize = viewportManager.size.value,
    viewportScaleFactor = viewportManager.scaleFactor.value,
)

fun Offset.toWorldCoordinates(
    viewportCenter: WorldCoordinates,
    viewportSize: Size,
    viewportScaleFactor: Float,
): WorldCoordinates = viewportCenter + WorldCoordinates(
    x = x - viewportSize.width / 2,
    y = y - viewportSize.height / 2,
) / viewportScaleFactor
