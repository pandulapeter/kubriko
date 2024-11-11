package com.pandulapeter.kubriko.physics.implementation

import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.implementation.extensions.require
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.physics.PhysicsManager
import com.pandulapeter.kubriko.physics.RigidBody
import com.pandulapeter.kubriko.physics.implementation.physics.dynamics.World
import com.pandulapeter.kubriko.physics.implementation.physics.math.Vec2
import com.pandulapeter.kubriko.types.SceneOffset
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

// TODO: Encapsulate everything related to the imported library
internal class PhysicsManagerImpl(
    gravity: SceneOffset,
) : PhysicsManager() {

    private val world by lazy {
        World(
            gravity = Vec2(gravity.x.raw.toDouble(), gravity.y.raw.toDouble()),
            getRigidBodies = { rigidBodiesForPhysicsEngine.value }
        )
    }
    private lateinit var actorManager: ActorManager
    private val rigidBodiesForPhysicsEngine by lazy {
        actorManager.allActors
            .map { it.filterIsInstance<RigidBody>().map { it.body } }
            .stateIn(scope, SharingStarted.Eagerly, emptyList())
    }

    override fun onInitialize(kubriko: Kubriko) {
        actorManager = kubriko.require()
    }

    override fun onUpdate(deltaTimeInMillis: Float, gameTimeNanos: Long) {
        if (deltaTimeInMillis > 0) {
            world.step(deltaTimeInMillis / 100.0)
        }
    }
}