package com.pandulapeter.gameTemplate.gameplay.implementation.gameObjects

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.pandulapeter.gameTemplate.engine.gameObject.properties.Dynamic
import com.pandulapeter.gameTemplate.engine.gameObject.GameObject
import com.pandulapeter.gameTemplate.engine.gameObject.properties.Rotatable
import com.pandulapeter.gameTemplate.engine.gameObject.properties.Scalable
import com.pandulapeter.gameTemplate.engine.gameObject.properties.Visible

internal data class DynamicBox(
    val color: Color,
    val edgeSize: Float,
    override var position: Offset,
    override var rotationDegrees: Float,
    override var scaleFactor: Float
) : GameObject(), Visible, Dynamic, Rotatable, Scalable {

    override val size = Size(edgeSize, edgeSize)
    private var isGrowing = true

    override fun update(deltaTimeMillis: Float) {
        rotationDegrees += 0.1f * deltaTimeMillis
        while (rotationDegrees > 360f) {
            rotationDegrees -= 360f
        }
        if (scaleFactor >= 1.6f) {
            isGrowing = false
        }
        if (scaleFactor <= 0.5f) {
            isGrowing = true
        }
        if (isGrowing) {
            scaleFactor += 0.001f * deltaTimeMillis
        } else {
            scaleFactor -= 0.001f * deltaTimeMillis
        }
    }

    override fun draw(scope: DrawScope) = scope.drawRect(
        color = color,
        topLeft = Offset.Zero,
        size = size,
    )
}
