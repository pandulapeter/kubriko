package com.pandulapeter.kubriko.demoPhysics.implementation

import androidx.compose.ui.geometry.Offset
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.actor.traits.Unique
import com.pandulapeter.kubriko.demoPhysics.implementation.actors.DynamicBox
import com.pandulapeter.kubriko.demoPhysics.implementation.actors.DynamicChain
import com.pandulapeter.kubriko.demoPhysics.implementation.actors.DynamicCircle
import com.pandulapeter.kubriko.demoPhysics.implementation.actors.DynamicPolygon
import com.pandulapeter.kubriko.demoPhysics.implementation.actors.StaticBox
import com.pandulapeter.kubriko.demoPhysics.implementation.actors.StaticCircle
import com.pandulapeter.kubriko.demoPhysics.implementation.actors.StaticPolygon
import com.pandulapeter.kubriko.implementation.extensions.cos
import com.pandulapeter.kubriko.implementation.extensions.require
import com.pandulapeter.kubriko.implementation.extensions.sceneUnit
import com.pandulapeter.kubriko.implementation.extensions.sin
import com.pandulapeter.kubriko.implementation.extensions.toSceneOffset
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.manager.Manager
import com.pandulapeter.kubriko.manager.ViewportManager
import com.pandulapeter.kubriko.physics.implementation.physics.geometry.Polygon
import com.pandulapeter.kubriko.physics.implementation.physics.math.Vec2
import com.pandulapeter.kubriko.pointerInput.PointerInputAware
import com.pandulapeter.kubriko.types.AngleRadians
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneSize
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

internal class PhysicsDemoManager : Manager(), PointerInputAware, Unique {

    private val _actionType = MutableStateFlow(ActionType.SHAPE)
    val actionType = _actionType.asStateFlow()
    private lateinit var actorManager: ActorManager
    private lateinit var viewportManager: ViewportManager

    override fun onInitialize(kubriko: Kubriko) {
        actorManager = kubriko.require()
        viewportManager = kubriko.require()
        actorManager.add(
            actors = listOf(this)
                    + (0..5).map { createStaticCircle() }
                    + (0..3).map { createStaticBox() }
                    + (0..10).map { createStaticPolygon() }
        )
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

    private fun createStaticCircle() = StaticCircle(
        initialOffset = randomSceneOffset(),
        radius = (30..120).random().sceneUnit,
    )

    private fun createStaticBox() = StaticBox(
        initialOffset = randomSceneOffset(),
        size = SceneSize(400.sceneUnit, 40.sceneUnit),
        isRotating = listOf(true, false).random(),
    )

    private fun createStaticPolygon() = StaticPolygon(
        initialOffset = randomSceneOffset(),
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
        isRotating = true,
    )

    private fun randomSceneOffset() = SceneOffset(
        x = (-1200..1200).random().sceneUnit,
        y = (-1200..1200).random().sceneUnit,
    )

    private enum class ShapeType {
        BOX, CIRCLE, POLYGON
    }
}