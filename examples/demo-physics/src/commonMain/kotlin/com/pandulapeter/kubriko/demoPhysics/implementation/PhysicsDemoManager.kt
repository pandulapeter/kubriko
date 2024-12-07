package com.pandulapeter.kubriko.demoPhysics.implementation

import androidx.compose.ui.geometry.Offset
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.actor.Actor
import com.pandulapeter.kubriko.actor.traits.Unique
import com.pandulapeter.kubriko.demoPhysics.implementation.actors.BouncyBall
import com.pandulapeter.kubriko.demoPhysics.implementation.actors.BouncyBox
import com.pandulapeter.kubriko.demoPhysics.implementation.actors.Chain
import com.pandulapeter.kubriko.demoPhysics.implementation.actors.DynamicPlatform
import com.pandulapeter.kubriko.demoPhysics.implementation.actors.StaticBall
import com.pandulapeter.kubriko.demoPhysics.implementation.actors.StaticCircle
import com.pandulapeter.kubriko.demoPhysics.implementation.actors.StaticPlatform
import com.pandulapeter.kubriko.demoPhysics.implementation.actors.StaticPolygon
import com.pandulapeter.kubriko.implementation.extensions.require
import com.pandulapeter.kubriko.implementation.extensions.sceneUnit
import com.pandulapeter.kubriko.implementation.extensions.toSceneOffset
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.manager.Manager
import com.pandulapeter.kubriko.manager.ViewportManager
import com.pandulapeter.kubriko.physics.RigidBody
import com.pandulapeter.kubriko.physics.implementation.physics.geometry.Circle
import com.pandulapeter.kubriko.physics.implementation.physics.geometry.Polygon
import com.pandulapeter.kubriko.pointerInput.PointerInputAware
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneSize
import com.pandulapeter.kubriko.types.SceneUnit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

internal class PhysicsDemoManager : Manager(), PointerInputAware, Unique {

    private val _demoType = MutableStateFlow(PhysicsDemoType.RIGID_BODY_COLLISIONS)
    val demoType = _demoType.asStateFlow()
    private lateinit var actorManager: ActorManager
    private lateinit var viewportManager: ViewportManager

    override fun onInitialize(kubriko: Kubriko) {
        actorManager = kubriko.require()
        viewportManager = kubriko.require()
        demoType.onEach(::resetDemo).launchIn(scope)
    }

    fun setSelectedDemoType(demoType: PhysicsDemoType) = _demoType.update { demoType }

    private fun resetDemo(demoType: PhysicsDemoType) {
        actorManager.removeAll()
        actorManager.add(actors = demoType.createActors() + this)
    }

    private fun PhysicsDemoType.createActors(): Array<Actor> = when (this) {
        PhysicsDemoType.RIGID_BODY_COLLISIONS -> listOf(
            BouncyBall(
                radius = 40.sceneUnit,
                initialOffset = SceneOffset(270.sceneUnit, (-200).sceneUnit),
            ),
            BouncyBox(
                sideSize = 100.sceneUnit,
                initialOffset = SceneOffset(170.sceneUnit, (-300).sceneUnit),
            ),
            BouncyBall(
                radius = 60.sceneUnit,
                initialOffset = SceneOffset(70.sceneUnit, (-150).sceneUnit),
            ),
            DynamicPlatform(
                initialPosition = SceneOffset((-200).sceneUnit, 0.sceneUnit),
                size = SceneSize(40.sceneUnit, 500.sceneUnit),
            ),
            StaticPlatform(
                initialPosition = SceneOffset(400.sceneUnit, 150.sceneUnit),
                size = SceneSize(400.sceneUnit, 40.sceneUnit),
            ),
        ) + (0..10).map { createRandomObject() }

        PhysicsDemoType.CHAINS -> listOf(
            Chain(
                initialCenterOffset = SceneOffset(SceneUnit.Zero, (-200).sceneUnit),
            ),
            StaticBall(
                initialOffset = SceneOffset.Zero,
                radius = 60.sceneUnit,
            ),
            StaticBall(
                initialOffset = SceneOffset((-100).sceneUnit, (-100).sceneUnit),
                radius = 60.sceneUnit,
            ),
        ) + (0..10).map { createRandomObject() }
    }.toTypedArray()

    override fun onPointerReleased(screenOffset: Offset) = screenOffset.toSceneOffset(viewportManager).let { pointerSceneOffset ->
        when (demoType.value) {
            PhysicsDemoType.RIGID_BODY_COLLISIONS -> actorManager.add(
                if (listOf(true, false).random()) {
                    BouncyBall(
                        radius = (30..60).random().toFloat().sceneUnit,
                        initialOffset = pointerSceneOffset,
                    )
                } else {
                    BouncyBox(
                        sideSize = (60..120).random().toFloat().sceneUnit,
                        initialOffset = pointerSceneOffset,
                    )
                }
            )

            PhysicsDemoType.CHAINS -> actorManager.add(
                Chain(
                    initialCenterOffset = pointerSceneOffset,
                )
            )
        }
    }

    private fun createRandomObject(): RigidBody {
        val offset = SceneOffset(
            x = (-600..600).random().sceneUnit,
            y = (-600..600).random().sceneUnit,
        )
        return when (listOf(true, false).random()) {
            true -> StaticCircle(
                initialPosition = offset,
                shape = Circle((10..30).random().sceneUnit)
            )

            false -> StaticPolygon(
                initialPosition = offset,
                shape = Polygon(
                    radius = (20..60).random().sceneUnit,
                    noOfSides = (3..10).random(),
                ),
            )
        }
    }
}