/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.physics

import com.pandulapeter.kubriko.helpers.extensions.isOverlapping
import com.pandulapeter.kubriko.helpers.extensions.length
import com.pandulapeter.kubriko.helpers.extensions.normalized
import com.pandulapeter.kubriko.helpers.extensions.rad
import com.pandulapeter.kubriko.helpers.extensions.scalar
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.manager.StateManager
import com.pandulapeter.kubriko.physics.implementation.Arbiter
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneUnit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

internal class PhysicsManagerImpl(
    initialGravity: SceneOffset,
    initialSimulationSpeed: Float,
    private val penetrationCorrection: Float,
    isLoggingEnabled: Boolean,
    instanceNameForLogging: String?,
) : PhysicsManager(isLoggingEnabled, instanceNameForLogging) {
    private val actorManager by manager<ActorManager>()
    private val stateManager by manager<StateManager>()
    private val rigidBodies by lazy {
        actorManager.allActors
            .map { it.filterIsInstance<RigidBody>().map { it.physicsBody } }
            .stateIn(scope, SharingStarted.Eagerly, emptyList())
    }
    private val joints by lazy {
        actorManager.allActors
            .map { it.filterIsInstance<JointWrapper>().map { it.physicsJoint } }
            .stateIn(scope, SharingStarted.Eagerly, emptyList())
    }
    private val arbiters = mutableListOf<Arbiter>()
    override val gravity = MutableStateFlow(initialGravity)
    private val actualGravity by autoInitializingLazy {
        gravity.asStateFlow(initialGravity)
    }
    override val simulationSpeed = MutableStateFlow(initialSimulationSpeed)

    override fun onUpdate(deltaTimeInMilliseconds: Int) {
        if (stateManager.isRunning.value && deltaTimeInMilliseconds > 0) {
            arbiters.clear()
            broadPhaseCheck()
            semiImplicit(deltaTimeInMilliseconds * simulationSpeed.value / 100f)
            for (contact in arbiters) {
                contact.penetrationResolution()
            }
        }
    }

    private fun semiImplicit(dt: Float) {
        applyForces(dt)
        solve()
        for (b in rigidBodies.value) {
            if (b.invMass == 0f) {
                continue
            }
            b.position += b.velocity.scalar(dt)
            b.orientation += (b.angularVelocity * dt).raw.rad
            b.force = SceneOffset.Zero
            b.torque = SceneUnit.Zero
        }
    }

    private fun applyForces(dt: Float) {
        for (b in rigidBodies.value) {
            if (b.invMass == 0f) {
                continue
            }
            applyLinearDrag(b)
            if (b.isAffectedByGravity) {
                b.velocity += actualGravity.value.scalar(dt)
            }
            b.velocity += b.force.scalar(b.invMass).scalar(dt)
            b.angularVelocity += b.torque * b.invInertia * dt
        }
    }

    private fun solve() {
        for (joint in joints.value) {
            joint.applyTension()
        }
        for (arbiter in arbiters) {
            arbiter.solve()
        }
    }

    private fun applyLinearDrag(body: PhysicsBody) {
        val velocityMagnitude = body.velocity.length()
        val dragForceMagnitude = velocityMagnitude * velocityMagnitude * body.linearDampening
        val dragForceVector = body.velocity.normalized().scalar(-dragForceMagnitude)
        body.applyForce(dragForceVector)
    }

    private fun broadPhaseCheck() {
        val bodies = rigidBodies.value
        for (i in bodies.indices) {
            val bodyA = bodies[i]
            for (x in i + 1 until bodies.size) {
                val bodyB = bodies[x]
                if (bodyA.invMass == 0f && bodyB.invMass == 0f || bodyA.isParticle && bodyB.isParticle) {
                    continue
                }
                if (bodyA.aabb.isOverlapping(bodyB.aabb)) {
                    narrowPhaseCheck(bodyA, bodyB)
                }
            }
        }
    }

    private fun narrowPhaseCheck(bodyA: PhysicsBody, bodyB: PhysicsBody) {
        val contactQuery = Arbiter(bodyA, bodyB, penetrationCorrection)
        contactQuery.narrowPhaseCheck()
        if (contactQuery.isColliding) {
            arbiters.add(contactQuery)
        }
    }
}