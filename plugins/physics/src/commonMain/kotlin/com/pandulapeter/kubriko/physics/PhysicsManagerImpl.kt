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
import com.pandulapeter.kubriko.helpers.extensions.isOverlapping
import com.pandulapeter.kubriko.helpers.extensions.length
import com.pandulapeter.kubriko.helpers.extensions.rad
import com.pandulapeter.kubriko.helpers.extensions.scalar
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.manager.StateManager
import com.pandulapeter.kubriko.physics.implementation.Arbiter
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

    private var sweepBodiesSnapshot: List<PhysicsBody>? = null
    private var sweepSortedIndices = IntArray(0)
    private var sweepMinX = FloatArray(0)
    private var sweepMaxX = FloatArray(0)
    private var sweepPairs = LongArray(0)

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

    /**
     * Sweep-and-prune broad phase: bodies are kept sorted by the left edge of their bounding box,
     * so each body only needs to be tested against the neighbors whose x-extents can still overlap
     * instead of every other body. Candidate pairs are re-sorted into the (i, j) order of a nested
     * loop, which keeps the solver's arbiter order and so the results deterministic — do not remove
     * the pair sort.
     */
    private fun broadPhaseCheck() {
        val bodies = rigidBodies.value
        val bodyCount = bodies.size
        if (bodyCount < 2) {
            return
        }
        if (bodies !== sweepBodiesSnapshot) {
            sweepBodiesSnapshot = bodies
            if (sweepSortedIndices.size < bodyCount) {
                sweepSortedIndices = IntArray(bodyCount)
                sweepMinX = FloatArray(bodyCount)
                sweepMaxX = FloatArray(bodyCount)
            }
            // The previous permutation may not cover 0 until bodyCount anymore; start from identity.
            for (i in 0 until bodyCount) {
                sweepSortedIndices[i] = i
            }
        }
        // One bounding box read per body (the pair loop below would otherwise re-read them O(n²) times).
        for (i in 0 until bodyCount) {
            val aabb = bodies[i].collisionMask.axisAlignedBoundingBox
            val left = aabb.left.raw
            // The sort cannot order NaN, so such a body (which overlaps nothing) goes last instead of splitting it.
            sweepMinX[i] = if (left.isNaN()) Float.POSITIVE_INFINITY else left
            sweepMaxX[i] = aabb.right.raw
        }
        // Insertion sort by minX; nearly sorted from the previous frame.
        val sorted = sweepSortedIndices
        for (k in 1 until bodyCount) {
            val index = sorted[k]
            val key = sweepMinX[index]
            var m = k - 1
            while (m >= 0 && sweepMinX[sorted[m]] > key) {
                sorted[m + 1] = sorted[m]
                m--
            }
            sorted[m + 1] = index
        }
        var pairCount = 0
        for (a in 0 until bodyCount) {
            val i = sorted[a]
            val bodyA = bodies[i]
            val maxXa = sweepMaxX[i]
            for (b in a + 1 until bodyCount) {
                val j = sorted[b]
                // isOverlapping treats touching edges as non-overlapping, so >= prunes exactly the
                // pairs it would reject on the x axis — and every later index sorts even further right.
                if (sweepMinX[j] >= maxXa) {
                    break
                }
                val bodyB = bodies[j]
                if (bodyA.invMass == 0f && bodyB.invMass == 0f || bodyA.isParticle && bodyB.isParticle) {
                    continue
                }
                if (bodyA.collisionMask.axisAlignedBoundingBox.isOverlapping(bodyB.collisionMask.axisAlignedBoundingBox)) {
                    if (pairCount == sweepPairs.size) {
                        sweepPairs = sweepPairs.copyOf(maxOf(16, sweepPairs.size * 2))
                    }
                    sweepPairs[pairCount++] = if (i < j) {
                        (i.toLong() shl 32) or j.toLong()
                    } else {
                        (j.toLong() shl 32) or i.toLong()
                    }
                }
            }
        }
        // Restore the original deterministic pair order before running the narrow phase.
        sweepPairs.sort(fromIndex = 0, toIndex = pairCount)
        for (k in 0 until pairCount) {
            val packed = sweepPairs[k]
            narrowPhaseCheck(
                bodyA = bodies[(packed ushr 32).toInt()],
                bodyB = bodies[(packed and 0xFFFFFFFFL).toInt()],
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