package com.pandulapeter.kubrikoShowcase.implementation.performance.actors

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.key.Key
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.actor.traits.Dynamic
import com.pandulapeter.kubriko.actor.traits.Unique
import com.pandulapeter.kubriko.actor.traits.Visible
import com.pandulapeter.kubriko.implementation.extensions.isAroundPosition
import com.pandulapeter.kubriko.implementation.extensions.require
import com.pandulapeter.kubriko.implementation.extensions.scenePixel
import com.pandulapeter.kubriko.keyboardInput.KeyboardInputAware
import com.pandulapeter.kubriko.keyboardInput.extensions.KeyboardDirectionState
import com.pandulapeter.kubriko.keyboardInput.extensions.directionState
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.manager.StateManager
import com.pandulapeter.kubriko.manager.ViewportManager
import com.pandulapeter.kubriko.sceneEditor.Editable
import com.pandulapeter.kubriko.sceneEditor.Exposed
import com.pandulapeter.kubriko.serialization.integration.Serializable
import com.pandulapeter.kubriko.serialization.typeSerializers.SerializableSceneOffset
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.ScenePixel
import com.pandulapeter.kubriko.types.SceneSize
import com.pandulapeter.kubrikoShowcase.implementation.performance.actors.traits.Destructible
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.serialization.SerialName
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.math.PI
import kotlin.math.sin

class Character private constructor(state: State) : Editable<Character>, Unique, Dynamic, Visible, KeyboardInputAware {

    @set:Exposed(name = "position")
    override var position: SceneOffset = state.position

    override val boundingBox = SceneSize(
        width = Radius * 2f,
        height = Radius * 2f,
    )
    override var drawingOrder = 0f
    private var sizeMultiplier = 1f
    private lateinit var actorManager: ActorManager
    private lateinit var stateManager: StateManager
    private lateinit var viewportManager: ViewportManager

    override fun onAdd(kubriko: Kubriko) {
        actorManager = kubriko.require()
        stateManager = kubriko.require()
        viewportManager = kubriko.require()
    }

    override fun handleActiveKeys(activeKeys: ImmutableSet<Key>) = move(activeKeys.directionState)

    override fun onKeyPressed(key: Key) = when (key) {
        Key.Spacebar -> triggerExplosion()
        else -> Unit
    }

    override fun update(deltaTimeInMillis: Float) {
        drawingOrder = -position.y.raw - pivotOffset.y.raw - 100f
        viewportManager.addToCameraPosition(calculateViewportOffsetDelta().raw)
        if (sizeMultiplier > 1f) {
            sizeMultiplier -= 0.01f * deltaTimeInMillis
        } else {
            sizeMultiplier = 1f
        }
    }

    private fun findDestructibleActorsNearby(
        position: SceneOffset,
        range: ScenePixel,
    ) = actorManager.allActors.value
        .filterIsInstance<Destructible>()
        .filter {
            it.isAroundPosition(
                position = position,
                range = range,
            )
        }

    override fun DrawScope.draw() = drawCircle(
        color = lerp(Color.Red, Color.Green, ((1f + MAX_SIZE_MULTIPLIER) - sizeMultiplier) / MAX_SIZE_MULTIPLIER),
        radius = (Radius * sizeMultiplier).raw,
        center = boundingBox.center.raw,
    )

    override fun save() = State(position = position)

    private fun calculateViewportOffsetDelta() = viewportManager.cameraPosition.value.let { viewportOffset ->
        viewportManager.scaleFactor.value.let { scaleFactor ->
            (viewportOffset - position) * VIEWPORT_FOLLOWING_SPEED_MULTIPLIER * scaleFactor * scaleFactor
        }
    }

    private fun move(directionState: KeyboardDirectionState) {
        if (stateManager.isRunning.value) {
            position += when (directionState) {
                KeyboardDirectionState.NONE -> SceneOffset.Zero
                KeyboardDirectionState.LEFT -> SceneOffset(-Speed, ScenePixel.Zero)
                KeyboardDirectionState.UP_LEFT -> SceneOffset(-SpeedDiagonal, -SpeedDiagonal)
                KeyboardDirectionState.UP -> SceneOffset(ScenePixel.Zero, -Speed)
                KeyboardDirectionState.UP_RIGHT -> SceneOffset(SpeedDiagonal, -SpeedDiagonal)
                KeyboardDirectionState.RIGHT -> SceneOffset(Speed, ScenePixel.Zero)
                KeyboardDirectionState.DOWN_RIGHT -> SceneOffset(SpeedDiagonal, SpeedDiagonal)
                KeyboardDirectionState.DOWN -> SceneOffset(ScenePixel.Zero, Speed)
                KeyboardDirectionState.DOWN_LEFT -> SceneOffset(-SpeedDiagonal, SpeedDiagonal)
            }
        }
    }

    private fun triggerExplosion() {
        if (stateManager.isRunning.value) {
            sizeMultiplier = MAX_SIZE_MULTIPLIER
            findDestructibleActorsNearby(
                position = position + pivotOffset,
                range = ExplosionRange,
            ).forEach { it.destroy(this) }
        }
    }

    @kotlinx.serialization.Serializable
    data class State(
        @SerialName("position") val position: SerializableSceneOffset = SceneOffset.Zero
    ) : Serializable.State<Character> {

        override fun restore() = Character(this)

        override fun serialize() = Json.encodeToString(this)
    }

    companion object {
        private val Radius = 50f.scenePixel
        private val Speed = 6f.scenePixel
        private val SpeedDiagonal = (sin(PI / 4) * Speed.raw).toFloat().scenePixel
        private const val VIEWPORT_FOLLOWING_SPEED_MULTIPLIER = 0.03f
        private const val MAX_SIZE_MULTIPLIER = 3f
        private val ExplosionRange = 500f.scenePixel
    }
}
