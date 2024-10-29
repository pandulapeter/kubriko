package com.pandulapeter.gameTemplate.engine.implementation.extensions

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.pandulapeter.gameTemplate.engine.implementation.EngineImpl
import com.pandulapeter.gameTemplate.engine.types.MapCoordinates

fun Offset.toMapCoordinates(): MapCoordinates = toMapCoordinates(
    viewportCenter = EngineImpl.viewportManager.center.value,
    scaledHalfViewportSize = EngineImpl.viewportManager.size.value / 2f,
    viewportScaleFactor = EngineImpl.viewportManager.scaleFactor.value,
)

internal fun Offset.toMapCoordinates(
    viewportCenter: MapCoordinates,
    scaledHalfViewportSize: Size,
    viewportScaleFactor: Float,
): MapCoordinates = viewportCenter + MapCoordinates(
    x = x - scaledHalfViewportSize.width,
    y = y - scaledHalfViewportSize.height,
) / viewportScaleFactor
