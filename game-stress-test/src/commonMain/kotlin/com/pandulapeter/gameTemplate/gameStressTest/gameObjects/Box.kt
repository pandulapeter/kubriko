package com.pandulapeter.gameTemplate.gameStressTest.gameObjects

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.lerp
import com.pandulapeter.gameTemplate.engine.gameObject.GameObject
import com.pandulapeter.gameTemplate.engine.gameObject.traits.Colorful
import com.pandulapeter.gameTemplate.engine.gameObject.traits.Movable
import com.pandulapeter.gameTemplate.engine.gameObject.traits.Rotatable
import com.pandulapeter.gameTemplate.engine.gameObject.traits.Visible
import com.pandulapeter.gameTemplate.engine.implementation.extensions.angleTowards

abstract class Box<T: Box<T>>(
    override var color: Color,
    edgeSize: Float,
    final override var position: Offset,
    final override var rotationDegrees: Float,
) : GameObject<T>(), Rotatable, Movable, Colorful {

    final override var bounds = Size(edgeSize, edgeSize)
        set(value) {
            field = value
            pivot = bounds.center
        }
    final override var pivot = bounds.center
    final override var depth = -position.y - pivot.y
    final override var directionDegrees = 0f
    final override var speed = 0f
    private var isDestroyed = 0f

    override fun update(deltaTimeMillis: Float) {
        super.update(deltaTimeMillis)
        if (speed > 0) {
            speed -= 0.02f * deltaTimeMillis
        } else {
            speed = 0f
        }
        if (isDestroyed > 0) {
            if (isDestroyed < 1f) {
                isDestroyed += 0.001f * deltaTimeMillis
            } else {
                isDestroyed = 1f
            }
        }
    }

    final override fun draw(scope: DrawScope) = scope.drawRect(
        color = lerp(color, Color.Black, isDestroyed),
        size = bounds,
    )

    fun destroy(character: Visible) {
        isDestroyed = 0.01f
        directionDegrees = 180f - angleTowards(character)
        speed = 10f
    }
}
