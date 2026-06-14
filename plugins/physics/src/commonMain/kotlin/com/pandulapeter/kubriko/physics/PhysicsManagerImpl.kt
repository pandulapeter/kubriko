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
    private val arbiterPool = ArrayList<Arbiter>()
    override val gravity = MutableStateFlow(initialGravity)
    private val actualGravity by autoInitializingLazy {
        gravity.asStateFlowOnMainThread(initialGravity)
    }
    override val simulationSpeed = MutableStateFlow(initialSimulationSpeed)

    private fun acquireArbiter(bodyA: PhysicsBody, bodyB: PhysicsBody): Arbiter =
        if (arbiterPool.isEmpty()) Arbiter(bodyA, bodyB, penetrationCorrection)
        else arbiterPool.removeAt(arbiterPool.lastIndex).also { it.reset(bodyA, bodyB, penetrationCorrection) }

    // Sweep-and-prune scratch state. The sorted index order is kept between frames, so the
    // insertion sort below is nearly O(n) thanks to temporal coherence. All buffers are
    // tick-thread-private and only grow.
    private var sweepBodiesSnapshot: List<PhysicsBody>? = null
    private var sweepSortedIndices = IntArray(0)
    private var sweepMinX = FloatArray(0)
    private var sweepMaxX = FloatArray(0)
    private var sweepPairs = LongArray(0)

    // Leftover real time carried between ticks by the fixed-timestep accumulator below.
    private var accumulatedTimeInMilliseconds = 0

    override fun onUpdate(deltaTimeInMilliseconds: Int) {
        if (!stateManager.isRunning.value || deltaTimeInMilliseconds <= 0) {
            return
        }
        // Fixed-timestep accumulator. Advancing the simulation by one Euler step the size of the whole
        // tick delta is unstable once the delta grows (e.g. a single ~100 ms step at 10 FPS): fast bodies
        // jump clean past each other before any collision is detected (tunneling), springs overshoot
        // their rest length and inject energy (joints spiraling), and deep penetrations never resolve
        // (contacts stuck, re-triggering every tick). Splitting the same elapsed time into constant
        // FIXED_TIME_STEP_IN_MILLISECONDS sub-steps keeps each integration step inside the stable range
        // it was tuned for, so behavior is frame-rate independent. The sub-step dt keeps the original
        // `* simulationSpeed / 100` scaling, so simulationSpeed still behaves as a pure time multiplier:
        // the sub-step count tracks real elapsed time while each step advances by simulationSpeed times
        // the fixed quantum.
        accumulatedTimeInMilliseconds += deltaTimeInMilliseconds
        val subStepDt = FIXED_TIME_STEP_IN_MILLISECONDS * simulationSpeed.value / 100f
        var stepsRemaining = MAXIMUM_SUB_STEPS_PER_TICK
        while (accumulatedTimeInMilliseconds >= FIXED_TIME_STEP_IN_MILLISECONDS && stepsRemaining > 0) {
            step(subStepDt)
            accumulatedTimeInMilliseconds -= FIXED_TIME_STEP_IN_MILLISECONDS
            stepsRemaining--
        }
        // Spiral-of-death guard: if the simulation is still more than a whole step behind after exhausting
        // the per-tick budget (a long stall, or a frame rate below ~7.5 FPS), drop the backlog rather than
        // letting it grow unbounded — catching it all up later would stall frames and destabilize the sim.
        if (accumulatedTimeInMilliseconds >= FIXED_TIME_STEP_IN_MILLISECONDS) {
            accumulatedTimeInMilliseconds = 0
        }
    }

    // One full simulation step: recycle last step's arbiters, rebuild the contact set for the bodies'
    // current positions, integrate, then resolve penetration. Collisions are re-detected every sub-step,
    // which is what prevents tunneling when a tick is split into several steps.
    private fun step(dt: Float) {
        arbiterPool.addAll(arbiters)
        arbiters.clear()
        broadPhaseCheck()
        semiImplicit(dt)
        for (i in arbiters.indices) {
            arbiters[i].penetrationResolution()
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
            b.force = SceneOffset.Zero
            b.torque = SceneUnit.Zero
        }
    }

    private fun applyForces(dt: Float) {
        val bodies = rigidBodies.value
        for (i in bodies.indices) {
            val b = bodies[i]
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
        val currentJoints = joints.value
        for (i in currentJoints.indices) {
            currentJoints[i].applyTension()
        }
        for (i in arbiters.indices) {
            arbiters[i].solve()
        }
    }

    private fun applyLinearDrag(body: PhysicsBody) {
        // Zero dampening or zero velocity produce a zero drag force, which applyForce turns into a
        // no-op — skipping early avoids two square roots per body per frame in the common case.
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
        body.applyForce(dragForceVector)
    }

    // Sweep-and-prune broad phase: bodies are kept sorted by the left edge of their bounding box,
    // so each body only needs to be tested against the neighbors whose x-extents can still overlap
    // instead of every other body. Candidate pairs are collected and re-sorted into the exact
    // (i, j) order the previous nested-loop implementation produced, which keeps the solver's
    // arbiter order — and therefore the simulation results — identical.
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
            sweepMinX[i] = aabb.left.raw
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
        // The constant simulation step. 16 ms matches the per-step dt the simulation was tuned against
        // at 60 FPS (16 ms * default simulationSpeed 1 / 100 ≈ the previous variable-delta step), so the
        // engine behaves the same at typical frame rates and only changes — for the better — when ticks
        // are throttled and one tick now covers several steps.
        const val FIXED_TIME_STEP_IN_MILLISECONDS = 16

        // Upper bound on sub-steps per tick, bounding worst-case cost and preventing the spiral of death.
        // 8 steps cover a single ~128 ms tick, so frame rates down to ~7.5 FPS stay fully time-accurate;
        // below that the simulation degrades gracefully (runs slower) instead of becoming unstable.
        const val MAXIMUM_SUB_STEPS_PER_TICK = 8
    }
}