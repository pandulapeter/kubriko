package com.pandulapeter.gameTemplate.engine.implementation.extensions

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import kotlin.math.atan2

internal fun Offset.toWorldCoordinates(
    viewportOffset: Offset,
    scaledHalfViewportSize: Size,
    viewportScaleFactor: Float,
) = viewportOffset + Offset(
    x = x - scaledHalfViewportSize.width,
    y = y - scaledHalfViewportSize.height,
) / viewportScaleFactor

fun Offset.angleTowards(position: Offset) = atan2(position.y - y, position.x - x).toDegrees()