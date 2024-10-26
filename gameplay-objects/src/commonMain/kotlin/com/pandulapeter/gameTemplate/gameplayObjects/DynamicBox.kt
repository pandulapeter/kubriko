package com.pandulapeter.gameTemplate.gameplayObjects

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.pandulapeter.gameTemplate.engine.gameObject.GameObject
import com.pandulapeter.gameTemplate.engine.gameObject.properties.Movable
import com.pandulapeter.gameTemplate.engine.gameObject.properties.Rotatable
import com.pandulapeter.gameTemplate.engine.gameObject.properties.Scalable
import com.pandulapeter.gameTemplate.engine.gameObject.properties.Visible
import com.pandulapeter.gameTemplate.engine.implementation.extensions.angleTowards
import com.pandulapeter.gameTemplate.engine.implementation.extensions.toRadians
import kotlin.math.cos
import kotlin.math.sin

data class DynamicBox(
    val color: Color,
    val edgeSize: Float,
    override var position: Offset,
    override var rotationDegrees: Float,
    override var scaleFactor: Float
) : GameObject(), Rotatable, Scalable, Clickable, Movable {

    override val size = Size(edgeSize, edgeSize)
    override val pivot = size.center
    override var depth = -position.y - pivot.y
    override var directionDegrees = 0f
    override var speed = 0f
    private var isGrowing = true
    private var isClicked = false

    override fun update(deltaTimeMillis: Float) {
        super.update(deltaTimeMillis)
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
        position += Offset(
            x = cos(rotationDegrees.toRadians()),
            y = -sin(rotationDegrees.toRadians()),
        )
        depth = -position.y - pivot.y - (if (isClicked) 100f else 0f)
        if (speed > 0) {
            speed -= 0.02f * deltaTimeMillis
        } else {
            speed = 0f
        }
    }

    override fun draw(scope: DrawScope) = scope.drawRect(
        color = if (isClicked) Color.Black else color,
        topLeft = Offset.Zero,
        size = size,
    )

    override fun onClicked() {
        isClicked = !isClicked
    }

    fun onAttacked(character: Visible) {
        isClicked = true
        directionDegrees = 180f - angleTowards(character)
        speed = 10f
    }
}
