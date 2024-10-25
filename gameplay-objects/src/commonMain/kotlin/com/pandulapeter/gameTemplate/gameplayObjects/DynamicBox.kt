package com.pandulapeter.gameTemplate.gameplayObjects

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.pandulapeter.gameTemplate.engine.gameObject.properties.Dynamic
import com.pandulapeter.gameTemplate.engine.gameObject.GameObject
import com.pandulapeter.gameTemplate.engine.gameObject.properties.Rotatable
import com.pandulapeter.gameTemplate.engine.gameObject.properties.Scalable
import com.pandulapeter.gameTemplate.engine.gameObject.properties.Visible
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

data class DynamicBox(
    val color: Color,
    val edgeSize: Float,
    override var position: Offset,
    override var rotationDegrees: Float,
    override var scaleFactor: Float
) : GameObject(), Visible, Dynamic, Rotatable, Scalable, Clickable {

    override val size = Size(edgeSize, edgeSize)
    private var isGrowing = true
    private var isClicked = false

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
        position = Offset(
            x = position.x + cos(rotationDegrees * (PI / 180f)).toFloat() * 2f,
            y = position.y - sin(rotationDegrees* (PI / 180f)).toFloat() * 2f,
        )
    }

    override fun draw(scope: DrawScope) = scope.drawRect(
        color = if (isClicked) Color.Black else color,
        topLeft = Offset.Zero,
        size = size,
    )

    override fun onClicked() {
        isClicked = !isClicked
    }
}
