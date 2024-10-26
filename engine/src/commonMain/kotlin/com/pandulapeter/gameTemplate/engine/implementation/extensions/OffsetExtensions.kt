package com.pandulapeter.gameTemplate.engine.implementation.extensions

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.pandulapeter.gameTemplate.engine.implementation.EngineImpl
import kotlin.math.atan2

fun Offset.toPositionInWorld() = toPositionInWorld(
    viewportOffset = EngineImpl.viewportManager.offset.value,
    scaledHalfViewportSize = EngineImpl.viewportManager.size.value / 2f,
    viewportScaleFactor = EngineImpl.viewportManager.scaleFactor.value,
)

internal fun Offset.toPositionInWorld(
    viewportOffset: Offset,
    scaledHalfViewportSize: Size,
    viewportScaleFactor: Float,
) = viewportOffset + Offset(
    x = x - scaledHalfViewportSize.width,
    y = y - scaledHalfViewportSize.height,
) / viewportScaleFactor

fun Offset.angleTowards(position: Offset) = atan2(position.y - y, position.x - x).toDegrees()