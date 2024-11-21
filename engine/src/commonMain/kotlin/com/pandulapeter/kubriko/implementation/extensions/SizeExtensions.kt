package com.pandulapeter.kubriko.implementation.extensions

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.pandulapeter.kubriko.types.SceneOffset

operator fun Size.minus(offset: Offset) = Offset(
    x = width - offset.x,
    y = height - offset.y,
)

operator fun Size.minus(offset: SceneOffset) = SceneOffset(
    x = width.scenePixel - offset.x,
    y = height.scenePixel - offset.y,
)