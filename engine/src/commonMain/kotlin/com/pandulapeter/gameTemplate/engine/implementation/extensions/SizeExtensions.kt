package com.pandulapeter.gameTemplate.engine.implementation.extensions

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.pandulapeter.gameTemplate.engine.types.WorldCoordinates

internal operator fun Size.minus(offset: Offset) = Offset(
    x = width - offset.x,
    y = height - offset.y,
)

internal operator fun Size.minus(offset: WorldCoordinates) = WorldCoordinates(
    x = width - offset.x,
    y = height - offset.y,
)