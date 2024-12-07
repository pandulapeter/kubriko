package com.pandulapeter.kubriko.demoPhysics.implementation

import androidx.compose.ui.geometry.Offset
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.actor.Actor
import com.pandulapeter.kubriko.actor.traits.Unique
import com.pandulapeter.kubriko.demoPhysics.implementation.actors.BouncyBall
import com.pandulapeter.kubriko.demoPhysics.implementation.actors.Chain
import com.pandulapeter.kubriko.demoPhysics.implementation.actors.Platform
import com.pandulapeter.kubriko.demoPhysics.implementation.actors.StaticBall
import com.pandulapeter.kubriko.implementation.extensions.require
import com.pandulapeter.kubriko.implementation.extensions.scenePixel
import com.pandulapeter.kubriko.implementation.extensions.toSceneOffset
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.manager.Manager
import com.pandulapeter.kubriko.manager.ViewportManager
import com.pandulapeter.kubriko.pointerInput.PointerInputAware
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.ScenePixel
import com.pandulapeter.kubriko.types.SceneSize
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
        PhysicsDemoType.RIGID_BODY_COLLISIONS -> ((0..2).map {
            BouncyBall(
                radius = (30..60).random().toFloat().scenePixel,
                initialOffset = SceneOffset(
                    x = (-500..500).random().toFloat().scenePixel,
                    y = (-800..0).random().toFloat().scenePixel,
                ),
            )
        } + Platform(
            initialPosition = SceneOffset(0f.scenePixel, 350f.scenePixel),
            size = SceneSize(800f.scenePixel, 40f.scenePixel),
        ))

        PhysicsDemoType.CHAINS -> listOf(
            Chain(
                initialCenterOffset = SceneOffset(ScenePixel.Zero, (-200f).scenePixel),
            ),
            StaticBall(
                initialOffset = SceneOffset.Zero,
                radius = 60.scenePixel,
            )
        )
    }.toTypedArray()

    override fun onClick(screenOffset: Offset) {
        if (demoType.value == PhysicsDemoType.RIGID_BODY_COLLISIONS) {
            actorManager.add(
                BouncyBall(
                    radius = (30..60).random().toFloat().scenePixel,
                    initialOffset = screenOffset.toSceneOffset(viewportManager),
                )
            )
        }
    }
}