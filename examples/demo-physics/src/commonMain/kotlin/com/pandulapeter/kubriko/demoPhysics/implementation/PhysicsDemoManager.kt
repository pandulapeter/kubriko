package com.pandulapeter.kubriko.demoPhysics.implementation

import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.actor.Actor
import com.pandulapeter.kubriko.demoPhysics.implementation.actors.BouncyBall
import com.pandulapeter.kubriko.demoPhysics.implementation.actors.Platform
import com.pandulapeter.kubriko.implementation.extensions.require
import com.pandulapeter.kubriko.implementation.extensions.scenePixel
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.manager.Manager
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneSize
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

internal class PhysicsDemoManager : Manager() {

    private val _demoType = MutableStateFlow(PhysicsDemoType.RIGID_BODY_COLLISIONS)
    val demoType = _demoType.asStateFlow()
    private lateinit var actorManager: ActorManager

    override fun onInitialize(kubriko: Kubriko) {
        actorManager = kubriko.require()
        demoType.onEach(::resetDemo).launchIn(scope)
    }

    fun setSelectedDemoType(demoType: PhysicsDemoType) = _demoType.update { demoType }

    private fun resetDemo(demoType: PhysicsDemoType) {
        actorManager.removeAll()
        actorManager.add(actors = demoType.createActors())
    }

    private fun PhysicsDemoType.createActors(): Array<Actor> = when (this) {
        PhysicsDemoType.RIGID_BODY_COLLISIONS -> ((0..40).map {
            BouncyBall(
                radius = (10..50).random().toFloat().scenePixel,
                initialPosition = SceneOffset(
                    x = (-500..500).random().toFloat().scenePixel,
                    y = (-800..0).random().toFloat().scenePixel,
                ),
            )
        } + Platform(
            initialPosition = SceneOffset(0f.scenePixel, 350f.scenePixel),
            boundingBox = SceneSize(800f.scenePixel, 40f.scenePixel),
        ))

        PhysicsDemoType.JOINTS -> emptyList()
    }.toTypedArray()
}