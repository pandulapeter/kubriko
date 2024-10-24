package com.pandulapeter.gameTemplate.engine

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope

abstract class GameObject(
    open val size: Size = Size.Zero,
    open val pivot: Offset = Offset(size.width / 2f, size.height / 2f),
) {
    open var position: Offset = Offset.Zero
    open var rotationDegrees: Float = 0f
    open var scaleFactor: Float = 1f

    open fun update(deltaTimeMillis: Float) = Unit

    open fun draw(scope: DrawScope) = Unit
}