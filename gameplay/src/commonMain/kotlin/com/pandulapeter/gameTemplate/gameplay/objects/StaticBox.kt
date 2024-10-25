package com.pandulapeter.gameTemplate.gameplay.objects

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.pandulapeter.gameTemplate.engine.models.GameObject

internal data class StaticBox(
    val color: Color,
    val edgeSize: Float,
    override var position: Offset,
    override var rotationDegrees: Float,
) : GameObject(
    size = Size(edgeSize, edgeSize),
) {
    override fun draw(scope: DrawScope) = scope.drawRect(
        color = color,
        topLeft = Offset.Zero,
        size = size,
    )
}
