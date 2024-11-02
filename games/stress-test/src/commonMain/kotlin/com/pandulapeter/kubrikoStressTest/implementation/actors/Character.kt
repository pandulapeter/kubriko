package com.pandulapeter.kubrikoStressTest.implementation.actors

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.key.Key
import com.pandulapeter.kubriko.actor.traits.Dynamic
import com.pandulapeter.kubriko.actor.traits.Unique
import com.pandulapeter.kubriko.actor.traits.Visible
import com.pandulapeter.kubriko.implementation.extensions.KeyboardDirectionState
import com.pandulapeter.kubriko.implementation.extensions.directionState
import com.pandulapeter.kubriko.implementation.extensions.isAroundPosition
import com.pandulapeter.kubriko.implementation.extensions.scenePixel
import com.pandulapeter.kubriko.sceneEditor.EditableProperty
import com.pandulapeter.kubriko.sceneSerializer.Editable
import com.pandulapeter.kubriko.sceneSerializer.serializers.SerializableSceneOffset
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.ScenePixel
import com.pandulapeter.kubriko.types.SceneSize
import com.pandulapeter.kubrikoStressTest.implementation.GameplayController
import com.pandulapeter.kubrikoStressTest.implementation.actors.traits.Destructible
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

class Character private constructor(state: CharacterState) : Editable<Character>, Unique, Dynamic, Visible, CoroutineScope {

    @set:EditableProperty(name = "position")
    override var position: SceneOffset = state.position

    override val boundingBox = SceneSize(
        width = Radius * 2f,
        height = Radius * 2f,
    )
    override var drawingOrder = 0f
    private var sizeMultiplier = 1f
    private var nearbyActorPositions = emptyList<SceneOffset>()

    override val coroutineContext = SupervisorJob() + Dispatchers.Default

    init {
        // TODO: Should be traits instead (InputAware maybe)
        GameplayController.kubriko.inputManager.activeKeys
            .filter { it.isNotEmpty() }
            .onEach { move(it.directionState) }
            .launchIn(this)
        GameplayController.kubriko.inputManager.onKeyPressed
            .onEach { key ->
                when (key) {
                    Key.Spacebar -> triggerExplosion()
                }
            }
            .launchIn(this)
    }

    override fun update(deltaTimeInMillis: Float) {
        drawingOrder = -position.y.raw - pivotOffset.y.raw - 100f
        GameplayController.kubriko.viewportManager.addToCameraPosition(calculateViewportOffsetDelta().raw)
        if (sizeMultiplier > 1f) {
            sizeMultiplier -= 0.01f * deltaTimeInMillis
        } else {
            sizeMultiplier = 1f
        }
        nearbyActorPositions = findDestructibleActorsNearby(
            position = position + pivotOffset,
            range = EXPLOSION_RANGE,
        ).map { it.position }
    }

    override fun draw(scope: DrawScope) {
        nearbyActorPositions.forEach { nearbyObjectPosition ->
            scope.drawLine(
                color = Color.Red,
                start = pivotOffset.raw,
                end = (nearbyObjectPosition - position + pivotOffset).raw,
                strokeWidth = 2f,
            )
        }
        scope.drawCircle(
            color = lerp(Color.Red, Color.Green, ((1f + MAX_SIZE_MULTIPLIER) - sizeMultiplier) / MAX_SIZE_MULTIPLIER),
            radius = (Radius * sizeMultiplier).raw,
            center = boundingBox.center.raw,
        )
    }

    override fun save() = CharacterState(position = position)

    private fun calculateViewportOffsetDelta() = GameplayController.kubriko.viewportManager.cameraPosition.value.let { viewportOffset ->
        GameplayController.kubriko.viewportManager.scaleFactor.value.let { scaleFactor ->
            (viewportOffset - position) * VIEWPORT_FOLLOWING_SPEED_MULTIPLIER * scaleFactor * scaleFactor
        }
    }

    private fun move(directionState: KeyboardDirectionState) {
        if (GameplayController.kubriko.stateManager.isRunning.value) {
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
        if (GameplayController.kubriko.stateManager.isRunning.value) {
            sizeMultiplier = MAX_SIZE_MULTIPLIER
            findDestructibleActorsNearby(
                position = position + pivotOffset,
                range = EXPLOSION_RANGE,
            ).forEach { it.destroy(this) }
        }
    }

    private fun findDestructibleActorsNearby(
        position: SceneOffset,
        range: Float,
    ) = GameplayController.kubriko.actorManager.allActors.value
        .filterIsInstance<Destructible>()
        .filter {
            it.isAroundPosition(
                position = position,
                range = range,
            )
        }

    @Serializable
    data class CharacterState(
        @SerialName("position") val position: SerializableSceneOffset = SceneOffset.Zero
    ) : Editable.State<Character> {

        override fun restore() = Character(this)

        override fun serialize() = Json.encodeToString(this)
    }

    companion object {
        private val Radius = 50f.scenePixel
        private val Speed = 6f.scenePixel
        private val SpeedDiagonal = (sin(PI / 4) * Speed.raw).toFloat().scenePixel
        private const val VIEWPORT_FOLLOWING_SPEED_MULTIPLIER = 0.03f
        private const val MAX_SIZE_MULTIPLIER = 3f
        private const val EXPLOSION_RANGE = 500f
    }
}
