/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.physics

import com.pandulapeter.kubriko.collision.mask.PolygonCollisionMask
import com.pandulapeter.kubriko.helpers.extensions.length
import com.pandulapeter.kubriko.helpers.extensions.rad
import com.pandulapeter.kubriko.helpers.extensions.scalar
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.manager.StateManager
import com.pandulapeter.kubriko.physics.implementation.Arbiter
import com.pandulapeter.kubriko.physics.implementation.SweepAndPrune
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneUnit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

internal class PhysicsManagerImpl(
    initialGravity: SceneOffset,
    initialSimulationSpeed: Float,
    private val penetrationCorrection: Float,
    isLoggingEnabled: Boolean,
    instanceNameForLogging: String?,
) : PhysicsManager(isLoggingEnabled, instanceNameForLogging) {
    private val actorManager by manager<ActorManager>()
    private val stateManager by manager<StateManager>()
    private val rigidBodies by autoInitializingLazy {
        actorManager.allActors
            .map { it.filterIsInstance<RigidBody>().map { it.physicsBody } }
            .asStateFlow(emptyList())
    }
    private val joints by autoInitializingLazy {
        actorManager.allActors
            .map { it.filterIsInstance<JointWrapper>().map { it.physicsJoint } }
            .asStateFlow(emptyList())
    }
    private val arbiters = mutableListOf<Arbiter>()
    private val arbiterPool = ArrayList<Arbiter>()
    override val gravity = MutableStateFlow(initialGravity)
    private val actualGravity by autoInitializingLazy {
        gravity.asStateFlowOnMainThread(initialGravity)
    }
    override val simulationSpeed = MutableStateFlow(initialSimulationSpeed)

    private fun acquireArbiter(bodyA: PhysicsBody, bodyB: PhysicsBody): Arbiter = if (arbiterPool.isEmpty()) {
        Arbiter(bodyA, bodyB, penetrationCorrection)
    } else {
        arbiterPool.removeAt(arbiterPool.lastIndex).also { it.reset(bodyA, bodyB, penetrationCorrection) }
    }

    private val sweepAndPrune = SweepAndPrune()

    /** Real time carried between ticks by the fixed-timestep accumulator in [onUpdate]. */
    private var accumulatedTimeInMilliseconds = 0

    override fun onUpdate(deltaTimeInMilliseconds: Int) {
        if (!stateManager.isRunning.value || deltaTimeInMilliseconds <= 0) {
            return
        }
        // Fixed-timestep accumulator: the tick's real time is split into constant FIXED_TIME_STEP_IN_MILLISECONDS
        // sub-steps, each advancing simulationSpeed times that quantum, so integration stays stable at any frame rate.
        accumulatedTimeInMilliseconds += minOf(deltaTimeInMilliseconds, MAXIMUM_ACCUMULATED_DELTA_IN_MILLISECONDS)
        val subStepDt = FIXED_TIME_STEP_IN_MILLISECONDS * simulationSpeed.value / 100f
        var stepsRemaining = MAXIMUM_SUB_STEPS_PER_TICK
        var didStep = false
        while (accumulatedTimeInMilliseconds >= FIXED_TIME_STEP_IN_MILLISECONDS && stepsRemaining > 0) {
            step(subStepDt)
            accumulatedTimeInMilliseconds -= FIXED_TIME_STEP_IN_MILLISECONDS
            stepsRemaining--
            didStep = true
        }
        // Forces apply across every sub-step and are cleared once per tick, only when a step ran, so a force set
        // on a tick that ran no step survives to the one that does.
        if (didStep) {
            clearForcesAndTorques()
        }
        // Spiral-of-death guard: if the simulation is still more than a whole step behind after exhausting
        // the per-tick budget (a long stall, or a frame rate below ~7.5 FPS), drop the backlog rather than
        // letting it grow unbounded — catching it all up later would stall frames and destabilize the sim.
        if (accumulatedTimeInMilliseconds >= FIXED_TIME_STEP_IN_MILLISECONDS) {
            accumulatedTimeInMilliseconds = 0
        }
    }

    private fun clearForcesAndTorques() {
        val bodies = rigidBodies.value
        for (i in bodies.indices) {
            val b = bodies[i]
            if (b.invMass == 0f) {
                continue
            }
            b.force = SceneOffset.Zero
            b.torque = SceneUnit.Zero
        }
    }

    /**
     * One full simulation step: recycle last step's arbiters, rebuild the contact set for the bodies'
     * current positions, integrate, then resolve penetration. Collisions are re-detected every sub-step,
     * which is what prevents tunneling when a tick is split into several steps.
     */
    private fun step(dt: Float) {
        for (i in arbiters.indices) {
            arbiterPool.add(arbiters[i])
        }
        arbiters.clear()
        // Integration moves the body, not its mask, so the mask is refreshed before every broad phase;
        // do not hoist this out of step(), or sub-steps would detect collisions at stale positions.
        syncCollisionMasksWithBodies()
        broadPhaseCheck()
        semiImplicit(dt)
        for (i in arbiters.indices) {
            arbiters[i].penetrationResolution()
        }
    }

    private fun syncCollisionMasksWithBodies() {
        val bodies = rigidBodies.value
        for (i in bodies.indices) {
            val b = bodies[i]
            // Static bodies (invMass == 0) never move during the simulation, so their mask is already correct.
            if (b.invMass == 0f) {
                continue
            }
            b.collisionMask.position = b.position
            (b.collisionMask as? PolygonCollisionMask)?.rotation = b.rotation
        }
    }

    private fun semiImplicit(dt: Float) {
        applyForces(dt)
        solve()
        val bodies = rigidBodies.value
        for (i in bodies.indices) {
            val b = bodies[i]
            if (b.invMass == 0f) {
                continue
            }
            b.position += b.velocity.scalar(dt)
            b.rotation += (b.angularVelocity * dt).raw.rad
            // force/torque are intentionally NOT cleared per sub-step: they must apply across every sub-step
            // of the tick to stay frame-rate independent. They are cleared once per tick in onUpdate.
        }
    }

    private fun applyForces(dt: Float) {
        val bodies = rigidBodies.value
        for (i in bodies.indices) {
            val b = bodies[i]
            if (b.invMass == 0f) {
                continue
            }
            applyLinearDrag(b, dt)
            if (b.isAffectedByGravity) {
                b.velocity += actualGravity.value.scalar(dt)
            }
            b.velocity += b.force.scalar(b.invMass).scalar(dt)
            b.angularVelocity += b.torque * b.invInertia * dt
        }
    }

    private fun solve() {
        val currentJoints = joints.value
        for (i in currentJoints.indices) {
            currentJoints[i].applyTension()
        }
        for (i in arbiters.indices) {
            arbiters[i].solve()
        }
    }

    private fun applyLinearDrag(body: PhysicsBody, dt: Float) {
        // Zero dampening or zero velocity produce a zero drag force (a no-op) — skipping early avoids two
        // square roots per body per sub-step in the common case.
        if (body.linearDampening == 0f) {
            return
        }
        val velocityMagnitude = body.velocity.length()
        if (velocityMagnitude == SceneUnit.Zero) {
            return
        }
        val dragForceMagnitude = velocityMagnitude * velocityMagnitude * body.linearDampening
        // Inlined normalization reusing the already computed magnitude instead of normalized(),
        // which would calculate the same square root a second time.
        val dragForceVector = SceneOffset(
            x = body.velocity.x / velocityMagnitude,
            y = body.velocity.y / velocityMagnitude,
        ).scalar(-dragForceMagnitude)
        // Integrated straight into velocity: body.force persists across the tick's sub-steps, so drag routed
        // through it would accumulate.
        body.velocity += dragForceVector.scalar(body.invMass).scalar(dt)
    }

    private fun broadPhaseCheck() {
        val bodies = rigidBodies.value
        val pairCount = sweepAndPrune.findPairs(bodies)
        for (k in 0 until pairCount) {
            narrowPhaseCheck(
                bodyA = bodies[sweepAndPrune.firstBodyIndexAt(k)],
                bodyB = bodies[sweepAndPrune.secondBodyIndexAt(k)],
            )
        }
    }

    private fun narrowPhaseCheck(bodyA: PhysicsBody, bodyB: PhysicsBody) {
        val contactQuery = acquireArbiter(bodyA, bodyB)
        contactQuery.narrowPhaseCheck()
        if (contactQuery.isColliding) {
            arbiters.add(contactQuery)
        } else {
            arbiterPool.add(contactQuery)
        }
    }

    private companion object {
        /**
         * The constant simulation step: 16 ms × the default simulationSpeed 1 / 100 is the per-step dt the
         * simulation was tuned against at 60 FPS.
         */
        const val FIXED_TIME_STEP_IN_MILLISECONDS = 16

        /**
         * Upper bound on sub-steps per tick, bounding worst-case cost and preventing the spiral of death.
         * 8 steps cover a single ~128 ms tick, so frame rates down to ~7.5 FPS stay fully time-accurate;
         * below that the simulation degrades gracefully (runs slower) instead of becoming unstable.
         */
        const val MAXIMUM_SUB_STEPS_PER_TICK = 8

        /**
         * Any delta at or above this already runs every sub-step and then has its backlog dropped, so clamping to it
         * changes nothing except keeping the Int accumulator from overflowing on a garbage delta.
         */
        const val MAXIMUM_ACCUMULATED_DELTA_IN_MILLISECONDS = FIXED_TIME_STEP_IN_MILLISECONDS * (MAXIMUM_SUB_STEPS_PER_TICK + 1)
    }
}