package com.pandulapeter.gameTemplate.gameplay

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.pandulapeter.gameTemplate.engine.GameObject

internal data class Rectangle(
    val color: Color,
    override val size: Size,
    override var position: Offset,
    override var rotationDegrees: Float,
) : GameObject(
    size = size,
) {
    override fun update(deltaTimeMillis: Float) {
        rotationDegrees += 0.1f * deltaTimeMillis
        while (rotationDegrees > 360f) {
            rotationDegrees -= 360f
        }
    }

    override fun draw(scope: DrawScope) = scope.drawRect(
        color = color,
        topLeft = Offset.Zero,
        size = size,
    )
}
