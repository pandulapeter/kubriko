package com.pandulapeter.kubriko.extensions

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.pandulapeter.kubriko.types.Scale
import com.pandulapeter.kubriko.types.SceneOffset

operator fun Size.minus(offset: Offset) = Offset(
    x = width - offset.x,
    y = height - offset.y,
)

operator fun Size.minus(offset: SceneOffset) = SceneOffset(
    x = width.sceneUnit - offset.x,
    y = height.sceneUnit - offset.y,
)

operator fun Size.div(scale: Scale) = Size(
    width = width / scale.horizontal,
    height = height / scale.vertical,
)
