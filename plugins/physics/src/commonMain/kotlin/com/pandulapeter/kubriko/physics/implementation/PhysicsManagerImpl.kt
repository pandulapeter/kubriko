package com.pandulapeter.kubriko.physics.implementation

import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.implementation.extensions.require
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.manager.StateManager
import com.pandulapeter.kubriko.physics.JointWrapper
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
            gravity = Vec2(gravity.x.raw, gravity.y.raw),
            getRigidBodies = { rigidBodiesForPhysicsEngine.value },
            getJoints = { jointsForPhysicsEngine.value },
        )
    }
    private lateinit var actorManager: ActorManager
    private lateinit var stateManager: StateManager
    private val rigidBodiesForPhysicsEngine by lazy {
        actorManager.allActors
            .map { it.filterIsInstance<RigidBody>().map { it.physicsBody } }
            .stateIn(scope, SharingStarted.Eagerly, emptyList())
    }
    private val jointsForPhysicsEngine by lazy {
        actorManager.allActors
            .map { it.filterIsInstance<JointWrapper>().map { it.physicsJoint } }
            .stateIn(scope, SharingStarted.Eagerly, emptyList())
    }

    override fun onInitialize(kubriko: Kubriko) {
        actorManager = kubriko.require()
        stateManager = kubriko.require()
    }

    override fun onUpdate(deltaTimeInMillis: Float, gameTimeNanos: Long) {
        if (stateManager.isRunning.value && deltaTimeInMillis > 0) {
            world.step(deltaTimeInMillis / 100f)
        }
    }
}