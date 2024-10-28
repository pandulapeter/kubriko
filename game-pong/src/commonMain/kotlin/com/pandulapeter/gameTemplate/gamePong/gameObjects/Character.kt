package com.pandulapeter.gameTemplate.gamePong.gameObjects

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.pandulapeter.gameTemplate.engine.Engine
import com.pandulapeter.gameTemplate.engine.gameObject.GameObject
import com.pandulapeter.gameTemplate.engine.gameObject.traits.Dynamic
import com.pandulapeter.gameTemplate.engine.gameObject.traits.Unique
import com.pandulapeter.gameTemplate.engine.gameObject.traits.Visible
import com.pandulapeter.gameTemplate.engine.implementation.extensions.KeyboardDirectionState
import com.pandulapeter.gameTemplate.engine.implementation.extensions.directionState
import com.pandulapeter.gameTemplate.engine.implementation.serializers.SerializableOffset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.math.PI
import kotlin.math.sin

class Character private constructor(
    state: StateHolder,
) : GameObject<Character>(), Visible, Dynamic, Unique, CoroutineScope {

    @Serializable
    data class StateHolder(
        @SerialName("position") val position: SerializableOffset = Offset.Zero
    ) : State<Character> {
        override val typeId = TYPE_ID

        override fun instantiate() = Character(this)

        override fun serialize() = Json.encodeToString(this)
    }

    override fun getState() = StateHolder(position = position)

    override val coroutineContext = SupervisorJob() + Dispatchers.Default

    init {
        Engine.get().inputManager.activeKeys
            .filter { it.isNotEmpty() }
            .onEach { move(it.directionState) }
            .launchIn(this)
    }

    override var position: Offset = state.position
    override var bounds = Size(RADIUS * 2, RADIUS * 2)
    override var pivot = bounds.center
    override var depth = -1f

    override fun update(deltaTimeMillis: Float) {
        Engine.get().viewportManager.addToOffset(calculateViewportOffsetDelta())
    }

    private fun calculateViewportOffsetDelta() = Engine.get().viewportManager.offset.value.let { viewportOffset ->
        Engine.get().viewportManager.scaleFactor.value.let { scaleFactor ->
            (viewportOffset - position) * VIEWPORT_FOLLOWING_SPEED_MULTIPLIER * scaleFactor * scaleFactor
        }
    }

    override fun draw(scope: DrawScope) {
        scope.drawCircle(
            color = Color.Blue,
            radius = RADIUS,
            center = bounds.center,
        )
    }

    private fun move(directionState: KeyboardDirectionState) {
        if (Engine.get().stateManager.isRunning.value) {
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
    }

    companion object {
        const val TYPE_ID = "character"
        private const val VIEWPORT_FOLLOWING_SPEED_MULTIPLIER = 0.03f
        private const val RADIUS = 50f
        private const val SPEED = 6f
        private val SPEED_DIAGONAL = (sin(PI / 4) * SPEED).toFloat()
    }
}
