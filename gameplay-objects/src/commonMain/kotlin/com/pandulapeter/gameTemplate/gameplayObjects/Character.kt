package com.pandulapeter.gameTemplate.gameplayObjects

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.lerp
import com.pandulapeter.gameTemplate.engine.Engine
import com.pandulapeter.gameTemplate.engine.gameObject.GameObject
import com.pandulapeter.gameTemplate.engine.gameObject.properties.Dynamic
import com.pandulapeter.gameTemplate.engine.gameObject.properties.Visible
import com.pandulapeter.gameTemplate.engine.implementation.extensions.KeyboardDirectionState
import kotlin.math.PI
import kotlin.math.sin

data class Character(
    override var position: Offset,
) : GameObject(), Visible, Dynamic {

    override val bounds = Size(RADIUS * 2, RADIUS * 2)
    override val pivot = bounds.center
    override var depth = -position.y - pivot.y
    private var fightMultiplier = 1f
    private var nearbyGameObjectPositions = emptyList<Offset>()

    override fun update(deltaTimeMillis: Float) {
        depth = -position.y - pivot.y - 100f
        Engine.get().viewportManager.addToOffset(calculateViewportOffsetDelta())
        if (fightMultiplier > 1f) {
            fightMultiplier -= 0.01f * deltaTimeMillis
        } else {
            fightMultiplier = 1f
        }
        nearbyGameObjectPositions = Engine.get().gameObjectManager.findGameObjectsWithPivotsAroundPosition(
            position = position + pivot,
            range = RADIUS * 5f
        ).map { it.position }
    }

    private fun calculateViewportOffsetDelta() = Engine.get().viewportManager.offset.value.let { viewportOffset ->
        Engine.get().viewportManager.scaleFactor.value.let { scaleFactor ->
            (position - viewportOffset) * VIEWPORT_FOLLOWING_SPEED_MULTIPLIER * scaleFactor * scaleFactor
        }
    }

    override fun draw(scope: DrawScope) {
        nearbyGameObjectPositions.forEach { nearbyObjectPosition ->
            scope.drawLine(
                color = Color.Red,
                start = pivot,
                end = nearbyObjectPosition - position + pivot,
                strokeWidth = 2f,
            )
        }
        scope.drawCircle(
            color = lerp(Color.Red, Color.Green, ((1f + MAX_FIGHT_MULTIPLIER) - fightMultiplier) / MAX_FIGHT_MULTIPLIER),
            radius = RADIUS * fightMultiplier,
            center = pivot,
        )
    }

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

    fun fight() {
        fightMultiplier = MAX_FIGHT_MULTIPLIER
        Engine.get().gameObjectManager.findGameObjectsWithPivotsAroundPosition(
            position = position + pivot,
            range = RADIUS * 5f
        ).forEach { (it as? StaticBox)?.onAttacked(this) ?: (it as? DynamicBox)?.onAttacked(this) }
    }

    companion object {
        private const val VIEWPORT_FOLLOWING_SPEED_MULTIPLIER = 0.03f
        private const val MAX_FIGHT_MULTIPLIER = 3f
        private const val RADIUS = 50f
        private const val SPEED = 6f
        private val SPEED_DIAGONAL = (sin(PI / 4) * SPEED).toFloat()
    }
}
