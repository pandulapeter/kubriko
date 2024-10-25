package com.pandulapeter.gameTemplate.gameplay.objects

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.pandulapeter.gameTemplate.engine.models.Dynamic
import com.pandulapeter.gameTemplate.engine.models.GameObject

internal data class DynamicBox(
    val color: Color,
    val edgeSize: Float,
    override var position: Offset,
    override var rotationDegrees: Float,
) : GameObject(
    size = Size(edgeSize, edgeSize),
), Dynamic {

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
