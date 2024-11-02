package com.pandulapeter.kubriko.engine.implementation.extensions

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.pandulapeter.kubriko.engine.types.SceneOffset

internal operator fun Size.minus(offset: Offset) = Offset(
    x = width - offset.x,
    y = height - offset.y,
)

internal operator fun Size.minus(offset: SceneOffset) = SceneOffset(
    x = width.scenePixel - offset.x,
    y = height.scenePixel - offset.y,
)