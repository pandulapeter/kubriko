package com.pandulapeter.gameTemplate.gameStressTest.gameObjects

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.key.Key
import com.pandulapeter.gameTemplate.engine.Engine
import com.pandulapeter.gameTemplate.engine.gameObject.GameObject
import com.pandulapeter.gameTemplate.engine.gameObject.traits.Dynamic
import com.pandulapeter.gameTemplate.engine.gameObject.traits.Unique
import com.pandulapeter.gameTemplate.engine.gameObject.traits.Visible
import com.pandulapeter.gameTemplate.engine.implementation.extensions.KeyboardDirectionState
import com.pandulapeter.gameTemplate.engine.implementation.extensions.directionState
import com.pandulapeter.gameTemplate.engine.implementation.extensions.getTrait
import com.pandulapeter.gameTemplate.engine.implementation.serializers.SerializableOffset
import com.pandulapeter.gameTemplate.gameStressTest.gameObjects.traits.Destroyable
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
    state: SerializerHolder,
) : GameObject<Character>(), CoroutineScope {

    private var sizeMultiplier = 1f
    private var nearbyGameObjectPositions = emptyList<Offset>()

    private val visible: Visible by lazy {
        Visible(
            bounds = Size(RADIUS * 2, RADIUS * 2),
            position = state.position,
            depth = -state.position.y,
            draw = { scope ->
                nearbyGameObjectPositions.forEach { nearbyObjectPosition ->
                    scope.drawLine(
                        color = Color.Red,
                        start = pivot,
                        end = nearbyObjectPosition - position + pivot,
                        strokeWidth = 2f,
                    )
                }
                scope.drawCircle(
                    color = lerp(Color.Red, Color.Green, ((1f + MAX_SIZE_MULTIPLIER) - sizeMultiplier) / MAX_SIZE_MULTIPLIER),
                    radius = RADIUS * sizeMultiplier,
                    center = bounds.center,
                )
            },
        )
    }
    private val dynamic: Dynamic by lazy {
        Dynamic(
            updater = { deltaTimeMillis ->
                visible.depth = -visible.position.y - visible.pivot.y - 100f
                Engine.get().viewportManager.addToOffset(calculateViewportOffsetDelta())
                if (sizeMultiplier > 1f) {
                    sizeMultiplier -= 0.01f * deltaTimeMillis
                } else {
                    sizeMultiplier = 1f
                }
                nearbyGameObjectPositions = Engine.get().gameObjectManager.findGameObjectsWithPivotsAroundPosition(
                    position = visible.position + visible.pivot,
                    range = RADIUS * 5f
                ).mapNotNull { it.getTrait<Visible>()?.position }

            }
        )
    }
    override val traits = setOf(
        Unique,
        visible,
        dynamic,
    )

    @Serializable
    data class SerializerHolder(
        @SerialName("position") val position: SerializableOffset = Offset.Zero
    ) : Serializer<Character> {
        override val typeId = TYPE_ID

        override fun instantiate() = Character(this)

        override fun serialize() = Json.encodeToString(this)
    }

    override fun getState() = SerializerHolder(position = visible.position)

    override val coroutineContext = SupervisorJob() + Dispatchers.Default

    init {
        Engine.get().inputManager.activeKeys
            .filter { it.isNotEmpty() }
            .onEach { move(it.directionState) }
            .launchIn(this)
        Engine.get().inputManager.onKeyPressed
            .onEach { key ->
                when (key) {
                    Key.Spacebar -> triggerExplosion()
                }
            }
            .launchIn(this)
    }

    private fun calculateViewportOffsetDelta() = Engine.get().viewportManager.offset.value.let { viewportOffset ->
        Engine.get().viewportManager.scaleFactor.value.let { scaleFactor ->
            (viewportOffset - visible.position) * VIEWPORT_FOLLOWING_SPEED_MULTIPLIER * scaleFactor * scaleFactor
        }
    }

    private fun move(directionState: KeyboardDirectionState) {
        if (Engine.get().stateManager.isRunning.value) {
            visible.position += when (directionState) {
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

    private fun triggerExplosion() {
        if (Engine.get().stateManager.isRunning.value) {
            sizeMultiplier = MAX_SIZE_MULTIPLIER
            Engine.get().gameObjectManager.findGameObjectsWithPivotsAroundPosition(
                position = visible.position + visible.pivot,
                range = RADIUS * 5f
            )
                .mapNotNull { it.getTrait<Destroyable>() }
                .forEach { it.destroy(visible) }
        }
    }

    companion object {
        const val TYPE_ID = "character"
        private const val VIEWPORT_FOLLOWING_SPEED_MULTIPLIER = 0.03f
        private const val MAX_SIZE_MULTIPLIER = 3f
        private const val RADIUS = 50f
        private const val SPEED = 6f
        private val SPEED_DIAGONAL = (sin(PI / 4) * SPEED).toFloat()
    }
}
