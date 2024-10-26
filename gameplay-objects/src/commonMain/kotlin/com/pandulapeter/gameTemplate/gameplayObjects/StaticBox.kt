package com.pandulapeter.gameTemplate.gameplayObjects

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.pandulapeter.gameTemplate.engine.gameObject.GameObject
import com.pandulapeter.gameTemplate.engine.gameObject.properties.Movable
import com.pandulapeter.gameTemplate.engine.gameObject.properties.Rotatable
import com.pandulapeter.gameTemplate.engine.gameObject.properties.Visible
import com.pandulapeter.gameTemplate.engine.implementation.extensions.angleTowards

data class StaticBox(
    val color: Color,
    val edgeSize: Float,
    override var position: Offset,
    override val rotationDegrees: Float,
) : GameObject(), Rotatable, Clickable, Movable {

    override val size = Size(edgeSize, edgeSize)
    override val pivot = size.center
    override var depth = -position.y - pivot.y
    override var directionDegrees = 0f
    override var speed = 0f
    private var isClicked = false
        set(value) {
            field = value
            depth = -position.y - pivot.y - (if (isClicked) 100f else 0f)
        }

    override fun draw(scope: DrawScope) = scope.drawRect(
        color = if (isClicked) Color.Black else color,
        topLeft = Offset.Zero,
        size = size,
    )

    override fun update(deltaTimeMillis: Float) {
        super.update(deltaTimeMillis)
        if (speed > 0) {
            speed -= 0.02f * deltaTimeMillis
        } else {
            speed = 0f
        }
    }

    override fun onClicked() {
        isClicked = !isClicked
    }

    fun onAttacked(character: Visible) {
        isClicked = true
        directionDegrees = 180f - angleTowards(character)
        speed = 10f
    }
}
