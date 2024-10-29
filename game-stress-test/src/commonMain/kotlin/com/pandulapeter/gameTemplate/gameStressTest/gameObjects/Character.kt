package com.pandulapeter.gameTemplate.gameStressTest.gameObjects

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.key.Key
import com.pandulapeter.gameTemplate.engine.Engine
import com.pandulapeter.gameTemplate.engine.gameObject.GameObject
import com.pandulapeter.gameTemplate.engine.gameObject.State
import com.pandulapeter.gameTemplate.engine.gameObject.editor.Editable
import com.pandulapeter.gameTemplate.engine.gameObject.traits.AvailableInEditor
import com.pandulapeter.gameTemplate.engine.gameObject.traits.Dynamic
import com.pandulapeter.gameTemplate.engine.gameObject.traits.Unique
import com.pandulapeter.gameTemplate.engine.gameObject.traits.Visible
import com.pandulapeter.gameTemplate.engine.implementation.extensions.KeyboardDirectionState
import com.pandulapeter.gameTemplate.engine.implementation.extensions.directionState
import com.pandulapeter.gameTemplate.engine.implementation.serializers.SerializableWorldCoordinates
import com.pandulapeter.gameTemplate.engine.types.WorldCoordinates
import com.pandulapeter.gameTemplate.engine.types.WorldSize
import com.pandulapeter.gameTemplate.gameStressTest.gameObjects.traits.Destructible
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
    state: CharacterState,
) : GameObject<Character>, AvailableInEditor, Unique, Visible, Dynamic, CoroutineScope {

    @set:Editable(typeId = "position")
    override var position: WorldCoordinates = state.position

    override var isSelectedInEditor = false
    override val boundingBox = WorldSize(
        width = RADIUS * 2f,
        height = RADIUS * 2f,
    )
    override var drawingOrder = 0f
    private var sizeMultiplier = 1f
    private var nearbyGameObjectPositions = emptyList<WorldCoordinates>()

    override val coroutineContext = SupervisorJob() + Dispatchers.Default

    init {
        // TODO: Should be traits instead
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

    override fun update(deltaTimeInMillis: Float) {
        drawingOrder = -position.y - pivotOffset.y - 100f
        Engine.get().viewportManager.addToCenter(calculateViewportOffsetDelta().rawOffset)
        if (sizeMultiplier > 1f) {
            sizeMultiplier -= 0.01f * deltaTimeInMillis
        } else {
            sizeMultiplier = 1f
        }
        nearbyGameObjectPositions = Engine.get().gameObjectManager.findGameObjectsWithPivotsAroundPosition(
            position = position + pivotOffset,
            range = RADIUS * 5f
        ).mapNotNull { (it as? Visible)?.position }
    }


    override fun createEditorInstance(position: WorldCoordinates) = Character(
        state = CharacterState(
            position = position,
        )
    )

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

    override fun saveState() = CharacterState(position = position)

    private fun calculateViewportOffsetDelta() = Engine.get().viewportManager.center.value.let { viewportOffset ->
        Engine.get().viewportManager.scaleFactor.value.let { scaleFactor ->
            (viewportOffset - position) * VIEWPORT_FOLLOWING_SPEED_MULTIPLIER * scaleFactor * scaleFactor
        }
    }

    private fun move(directionState: KeyboardDirectionState) {
        if (Engine.get().stateManager.isRunning.value) {
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
        if (Engine.get().stateManager.isRunning.value) {
            sizeMultiplier = MAX_SIZE_MULTIPLIER
            Engine.get().gameObjectManager.findGameObjectsWithPivotsAroundPosition(
                position = position + pivotOffset,
                range = RADIUS * 5f
            )
                .filterIsInstance<Destructible>()
                .forEach { it.destroy(this) }
        }
    }

    @Serializable
    data class CharacterState(
        @SerialName("position") val position: SerializableWorldCoordinates = WorldCoordinates.Zero
    ) : State<Character> {
        override val typeId = TYPE_ID

        override fun restore() = Character(this)

        override fun serialize() = Json.encodeToString(this)
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
