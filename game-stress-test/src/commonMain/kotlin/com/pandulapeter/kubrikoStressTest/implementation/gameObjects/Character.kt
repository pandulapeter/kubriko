package com.pandulapeter.kubrikoStressTest.implementation.gameObjects

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.key.Key
import com.pandulapeter.kubriko.engine.editorIntegration.EditableProperty
import com.pandulapeter.kubriko.engine.implementation.extensions.KeyboardDirectionState
import com.pandulapeter.kubriko.engine.implementation.extensions.directionState
import com.pandulapeter.kubriko.engine.implementation.serializers.SerializableWorldCoordinates
import com.pandulapeter.kubriko.engine.traits.Dynamic
import com.pandulapeter.kubriko.engine.traits.Editable
import com.pandulapeter.kubriko.engine.traits.Unique
import com.pandulapeter.kubriko.engine.traits.Visible
import com.pandulapeter.kubriko.engine.types.WorldCoordinates
import com.pandulapeter.kubriko.engine.types.WorldSize
import com.pandulapeter.kubrikoStressTest.implementation.GameplayController
import com.pandulapeter.kubrikoStressTest.implementation.gameObjects.traits.Destructible
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
    override var position: WorldCoordinates = state.position

    override val boundingBox = WorldSize(
        width = RADIUS * 2f,
        height = RADIUS * 2f,
    )
    override var drawingOrder = 0f
    private var sizeMultiplier = 1f
    private var nearbyGameObjectPositions = emptyList<WorldCoordinates>()

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
        drawingOrder = -position.y - pivotOffset.y - 100f
        GameplayController.kubriko.viewportManager.addToCenter(calculateViewportOffsetDelta().rawOffset)
        if (sizeMultiplier > 1f) {
            sizeMultiplier -= 0.01f * deltaTimeInMillis
        } else {
            sizeMultiplier = 1f
        }
        nearbyGameObjectPositions = GameplayController.kubriko.instanceManager.findVisibleInstancesWithPivotsAroundPosition(
            position = position + pivotOffset,
            range = EXPLOSION_RANGE,
        ).mapNotNull { (it as? Visible)?.position }
    }

    override fun draw(scope: DrawScope) {
        nearbyGameObjectPositions.forEach { nearbyObjectPosition ->
            scope.drawLine(
                color = Color.Red,
                start = pivotOffset.rawOffset,
                end = (nearbyObjectPosition - position + pivotOffset).rawOffset,
                strokeWidth = 2f,
            )
        }
        scope.drawCircle(
            color = lerp(Color.Red, Color.Green, ((1f + MAX_SIZE_MULTIPLIER) - sizeMultiplier) / MAX_SIZE_MULTIPLIER),
            radius = RADIUS * sizeMultiplier,
            center = boundingBox.center.rawOffset,
        )
    }

    override fun save() = CharacterState(position = position)

    private fun calculateViewportOffsetDelta() = GameplayController.kubriko.viewportManager.center.value.let { viewportOffset ->
        GameplayController.kubriko.viewportManager.scaleFactor.value.let { scaleFactor ->
            (viewportOffset - position) * VIEWPORT_FOLLOWING_SPEED_MULTIPLIER * scaleFactor * scaleFactor
        }
    }

    private fun move(directionState: KeyboardDirectionState) {
        if (GameplayController.kubriko.stateManager.isRunning.value) {
            position += when (directionState) {
                KeyboardDirectionState.NONE -> WorldCoordinates.Zero
                KeyboardDirectionState.LEFT -> WorldCoordinates(-SPEED, 0f)
                KeyboardDirectionState.UP_LEFT -> WorldCoordinates(-SPEED_DIAGONAL, -SPEED_DIAGONAL)
                KeyboardDirectionState.UP -> WorldCoordinates(0f, -SPEED)
                KeyboardDirectionState.UP_RIGHT -> WorldCoordinates(SPEED_DIAGONAL, -SPEED_DIAGONAL)
                KeyboardDirectionState.RIGHT -> WorldCoordinates(SPEED, 0f)
                KeyboardDirectionState.DOWN_RIGHT -> WorldCoordinates(SPEED_DIAGONAL, SPEED_DIAGONAL)
                KeyboardDirectionState.DOWN -> WorldCoordinates(0f, SPEED)
                KeyboardDirectionState.DOWN_LEFT -> WorldCoordinates(-SPEED_DIAGONAL, SPEED_DIAGONAL)
            }
        }
    }

    private fun triggerExplosion() {
        if (GameplayController.kubriko.stateManager.isRunning.value) {
            sizeMultiplier = MAX_SIZE_MULTIPLIER
            GameplayController.kubriko.instanceManager.findVisibleInstancesWithPivotsAroundPosition(
                position = position + pivotOffset,
                range = EXPLOSION_RANGE,
            ).filterIsInstance<Destructible>().forEach { it.destroy(this) }
        }
    }

    @Serializable
    data class CharacterState(
        @SerialName("position") val position: SerializableWorldCoordinates = WorldCoordinates.Zero
    ) : Editable.State<Character> {

        override fun restore() = Character(this)

        override fun serialize() = Json.encodeToString(this)
    }

    companion object {
        private const val VIEWPORT_FOLLOWING_SPEED_MULTIPLIER = 0.03f
        private const val MAX_SIZE_MULTIPLIER = 3f
        private const val EXPLOSION_RANGE = 500f
        private const val RADIUS = 50f
        private const val SPEED = 6f
        private val SPEED_DIAGONAL = (sin(PI / 4) * SPEED).toFloat()
    }
}
