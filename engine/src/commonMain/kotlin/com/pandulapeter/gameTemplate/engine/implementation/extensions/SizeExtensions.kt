package com.pandulapeter.gameTemplate.engine.implementation.extensions

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.pandulapeter.gameTemplate.engine.types.MapCoordinates

internal operator fun Size.minus(offset: Offset) = Offset(
    x = width - offset.x,
    y = height - offset.y,
)

internal operator fun Size.minus(offset: MapCoordinates) = MapCoordinates(
    x = width - offset.x,
    y = height - offset.y,
)