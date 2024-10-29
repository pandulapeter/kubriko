package com.pandulapeter.gameTemplate.engine.implementation.extensions

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.pandulapeter.gameTemplate.engine.implementation.EngineImpl
import com.pandulapeter.gameTemplate.engine.types.WorldCoordinates

fun Offset.toMapCoordinates(): WorldCoordinates = toMapCoordinates(
    viewportCenter = EngineImpl.viewportManager.center.value,
    scaledHalfViewportSize = EngineImpl.viewportManager.size.value / 2f,
    viewportScaleFactor = EngineImpl.viewportManager.scaleFactor.value,
)

internal fun Offset.toMapCoordinates(
    viewportCenter: WorldCoordinates,
    scaledHalfViewportSize: Size,
    viewportScaleFactor: Float,
): WorldCoordinates = viewportCenter + WorldCoordinates(
    x = x - scaledHalfViewportSize.width,
    y = y - scaledHalfViewportSize.height,
) / viewportScaleFactor
