package com.pandulapeter.gameTemplate.gameplayObjects

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.pandulapeter.gameTemplate.engine.gameObject.GameObject
import com.pandulapeter.gameTemplate.engine.gameObject.properties.Dynamic
import com.pandulapeter.gameTemplate.engine.gameObject.properties.Visible
import com.pandulapeter.gameTemplate.engine.implementation.extensions.KeyboardDirectionState
import kotlin.math.PI
import kotlin.math.sin

data class Character(
    override var position: Offset,
) : GameObject(), Visible, Dynamic {

    override val size = Size(RADIUS * 2, RADIUS * 2)
    override val pivot = size.center
    override var depth = -position.y - pivot.y

    override fun update(deltaTimeMillis: Float) {
        depth = -position.y - pivot.y - 100f
    }

    override fun draw(scope: DrawScope) = scope.drawCircle(
        color = Color.Green,
        radius = RADIUS,
        center = pivot,
    )

    fun move(directionState: KeyboardDirectionState) {
        position += when (directionState) {
            KeyboardDirectionState.NONE -> Offset.Zero
            KeyboardDirectionState.LEFT -> Offset(-SPEED, 0f)
            KeyboardDirectionState.UP_LEFT -> Offset(-SPEED_DIAGONAL, -SPEED_DIAGONAL)
            KeyboardDirectionState.UP -> Offset(0f, -SPEED)
            KeyboardDirectionState.UP_RIGHT -> Offset(SPEED_DIAGONAL, -SPEED_DIAGONAL)
            KeyboardDirectionState.RIGHT -> Offset(SPEED, 0f)
            KeyboardDirectionState.DOWN_RIGHT -> Offset(SPEED_DIAGONAL, SPEED_DIAGONAL)
            KeyboardDirectionState.DOWN -> Offset(0f, SPEED)
            KeyboardDirectionState.DOWN_LEFT -> Offset(-SPEED_DIAGONAL, SPEED_DIAGONAL)
        }
    }

    companion object {
        private const val RADIUS = 50f
        private const val SPEED = 15f
        private val SPEED_DIAGONAL = (sin(PI / 4) * SPEED).toFloat()
    }
}
