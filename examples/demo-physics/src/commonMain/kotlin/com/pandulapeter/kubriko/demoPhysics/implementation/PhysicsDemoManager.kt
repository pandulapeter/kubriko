package com.pandulapeter.kubriko.demoPhysics.implementation

import androidx.compose.ui.geometry.Offset
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.actor.traits.Unique
import com.pandulapeter.kubriko.demoPhysics.implementation.actors.DynamicBox
import com.pandulapeter.kubriko.demoPhysics.implementation.actors.DynamicChain
import com.pandulapeter.kubriko.demoPhysics.implementation.actors.DynamicCircle
import com.pandulapeter.kubriko.demoPhysics.implementation.actors.DynamicPolygon
import com.pandulapeter.kubriko.implementation.extensions.cos
import com.pandulapeter.kubriko.implementation.extensions.sceneUnit
import com.pandulapeter.kubriko.implementation.extensions.sin
import com.pandulapeter.kubriko.implementation.extensions.toSceneOffset
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.manager.Manager
import com.pandulapeter.kubriko.manager.ViewportManager
import com.pandulapeter.kubriko.physics.implementation.physics.geometry.Polygon
import com.pandulapeter.kubriko.physics.implementation.physics.math.Vec2
import com.pandulapeter.kubriko.pointerInput.PointerInputAware
import com.pandulapeter.kubriko.sceneEditor.Editable
import com.pandulapeter.kubriko.sceneEditor.EditableMetadata
import com.pandulapeter.kubriko.serialization.SerializationManager
import com.pandulapeter.kubriko.types.AngleRadians
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneSize
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kubriko.examples.demo_physics.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.MissingResourceException

internal class PhysicsDemoManager(
    private val sceneJson: MutableStateFlow<String>?,
) : Manager(), PointerInputAware, Unique {

    private val _actionType = MutableStateFlow(ActionType.SHAPE)
    val actionType = _actionType.asStateFlow()
    private val actorManager by manager<ActorManager>()
    private val serializationManager by manager<SerializationManager<EditableMetadata<*>, Editable<*>>>()
    private val viewportManager by manager<ViewportManager>()
    private val _shouldShowLoadingIndicator = MutableStateFlow(true)
    val shouldShowLoadingIndicator = _shouldShowLoadingIndicator.asStateFlow()

    override fun onInitialize(kubriko: Kubriko) {
        actorManager.add(this)
        actorManager.allActors
            .filter { it.size > 1 }
            .onEach {
                delay(100)
                _shouldShowLoadingIndicator.update { false }
            }
            .launchIn(scope)
        sceneJson?.filter { it.isNotBlank() }?.onEach(::processJson)?.launchIn(scope)
        loadMap()
    }

    fun changeSelectedActionType() = _actionType.update { currentActionType ->
        val values = ActionType.entries
        val nextIndex = (currentActionType.ordinal + 1) % values.size
        values[nextIndex]
    }

    override fun onPointerReleased(screenOffset: Offset) = screenOffset.toSceneOffset(viewportManager).let { pointerSceneOffset ->
        when (actionType.value) {
            ActionType.SHAPE -> actorManager.add(
                when (ShapeType.entries.random()) {
                    ShapeType.BOX -> createDynamicBox(pointerSceneOffset)
                    ShapeType.CIRCLE -> createDynamicCircle(pointerSceneOffset)
                    ShapeType.POLYGON -> createDynamicPolygon(pointerSceneOffset)
                }
            )

            ActionType.CHAIN -> actorManager.add(
                DynamicChain(
                    linkCount = (10..20).random(),
                    initialCenterOffset = pointerSceneOffset,
                )
            )

            ActionType.EXPLOSION -> Unit // TODO: Explosion
        }
    }

    @OptIn(ExperimentalResourceApi::class)
    private fun loadMap() = scope.launch {
        try {
            val json = Res.readBytes("files/scenes/$SCENE_NAME").decodeToString()
            sceneJson?.update { json } ?: processJson(json)
        } catch (_: MissingResourceException) {
        }
    }

    private fun processJson(json: String) {
        _shouldShowLoadingIndicator.update { true }
        actorManager.removeAll()
        actorManager.add(this)
        val deserializedActors = serializationManager.deserializeActors(json)
        actorManager.add(deserializedActors)
    }

    private fun createDynamicBox(
        pointerSceneOffset: SceneOffset,
    ) = DynamicBox(
        size = SceneSize(
            width = (60..120).random().toFloat().sceneUnit,
            height = (60..120).random().toFloat().sceneUnit,
        ),
        initialOffset = pointerSceneOffset,
    )

    private fun createDynamicCircle(
        pointerSceneOffset: SceneOffset,
    ) = DynamicCircle(
        radius = (30..60).random().toFloat().sceneUnit,
        initialOffset = pointerSceneOffset,
    )

    private fun createDynamicPolygon(
        pointerSceneOffset: SceneOffset,
    ) = DynamicPolygon(
        initialOffset = pointerSceneOffset,
        shape = Polygon(
            vertList = (3..10).random().let { sideCount ->
                (0..sideCount).map { sideIndex ->
                    val angle = AngleRadians.TwoPi / sideCount * (sideIndex + 0.75f)
                    Vec2(
                        x = (30..120).random().sceneUnit * angle.cos,
                        y = (30..120).random().sceneUnit * angle.sin,
                    )
                }
            },
        ),
    )

    private enum class ShapeType {
        BOX, CIRCLE, POLYGON
    }

    companion object {
        const val SCENE_NAME = "scene_physics_test.json"
    }
}