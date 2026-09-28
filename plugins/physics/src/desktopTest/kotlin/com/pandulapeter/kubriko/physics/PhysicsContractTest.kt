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

import com.pandulapeter.kubriko.actor.body.BoxBody
import com.pandulapeter.kubriko.collision.mask.BoxCollisionMask
import com.pandulapeter.kubriko.collision.mask.CircleCollisionMask
import com.pandulapeter.kubriko.helpers.extensions.get
import com.pandulapeter.kubriko.helpers.extensions.rad
import com.pandulapeter.kubriko.helpers.extensions.sceneUnit
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.manager.StateManager
import com.pandulapeter.kubriko.testFixtures.ManualKubriko
import com.pandulapeter.kubriko.testFixtures.awaitCondition
import com.pandulapeter.kubriko.testFixtures.measureAllocatedBytesPerRun
import com.pandulapeter.kubriko.testFixtures.newManualKubriko
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneSize
import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.random.Random
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PhysicsContractTest {

    @Test
    fun identicalRunsAreBitForBitIdentical() {
        val firstRun = runFallingScene()
        val secondRun = runFallingScene()

        firstRun.indices.forEach { checkpoint ->
            val first = firstRun[checkpoint]
            val second = secondRun[checkpoint]
            first.indices.forEach { index ->
                assertEquals(
                    first[index].toRawBits(),
                    second[index].toRawBits(),
                    "Body ${index / VALUES_PER_BODY} diverged by tick ${(checkpoint + 1) * CHECKPOINT_INTERVAL}.",
                )
            }
        }
    }

    @Test
    fun staticBodiesNeverMove() = withPhysics { kubriko, _ ->
        val scene = FallingScene()
        val staticSnapshotBefore = snapshot(scene.staticBodies)
        kubriko.register(scene)

        kubriko.tick(count = SCENE_TICK_COUNT)

        assertBitsEqual(staticSnapshotBefore, snapshot(scene.staticBodies), "A static body moved.")
    }

    @Test
    fun aBallComesToRestOnTheFloor() = withPhysics { kubriko, _ ->
        val floorTop = 300f
        val floor = box(x = 0f, y = floorTop + 30f, width = 400f, height = 60f, density = 0f)
        val ball = circle(x = 0f, y = floorTop - 200f - 10f, radius = 10f, restitution = 0f)
        kubriko.actorManager.add(listOf(floor, ball))
        val startY = ball.physicsBody.position.y
        kubriko.tickUntil { ball.physicsBody.position.y != startY }

        val lastYs = ArrayList<Float>()
        val lastSpeeds = ArrayList<Float>()
        repeat(900) { tickIndex ->
            kubriko.tick()
            val y = ball.physicsBody.position.y.raw
            assertTrue(y < floorTop, "The ball tunneled into the floor at tick $tickIndex (y = $y).")
            if (tickIndex >= 900 - 60) {
                lastYs.add(y)
                lastSpeeds.add(ball.physicsBody.velocity.length())
            }
        }

        assertTrue(lastYs.max() - lastYs.min() <= 1f, "The ball did not settle: $lastYs")
        assertTrue(lastSpeeds.average() < 0.5, "The ball kept moving: $lastSpeeds")
        assertTrue(abs(lastYs.last() + 10f - floorTop) <= 2f, "The ball rests at ${lastYs.last()}, not on the floor at $floorTop.")
    }

    @Test
    fun noGravityNoMotion() = withPhysics(gravity = SceneOffset.Zero) { kubriko, _ ->
        val resting = circle(x = 0f, y = 0f)
        val probe = circle(x = 1_000f, y = 0f).apply { physicsBody.velocity = offset(10f, 0f) }
        kubriko.actorManager.add(listOf(resting, probe))
        val probeStart = probe.physicsBody.position
        val restingStart = snapshot(listOf(resting))
        kubriko.tickUntil { probe.physicsBody.position != probeStart }

        kubriko.tick(count = 300)

        assertBitsEqual(restingStart, snapshot(listOf(resting)), "A body at rest moved without gravity.")
    }

    @Test
    fun headOnCollisionConservesMomentum() = withPhysics(gravity = SceneOffset.Zero) { kubriko, _ ->
        val left = circle(x = -100f, y = 0f, radius = 20f, restitution = 1f, friction = 0f).apply { physicsBody.velocity = offset(50f, 0f) }
        val right = circle(x = 100f, y = 0f, radius = 20f, restitution = 1f, friction = 0f).apply { physicsBody.velocity = offset(-50f, 0f) }
        kubriko.actorManager.add(listOf(left, right))
        val leftStart = left.physicsBody.position
        kubriko.tickUntil { left.physicsBody.position != leftStart }

        var haveSeparated = false
        repeat(60) { tickIndex ->
            kubriko.tick()
            val leftVelocity = left.physicsBody.velocity
            val rightVelocity = right.physicsBody.velocity
            assertTrue(abs(leftVelocity.x.raw + rightVelocity.x.raw) <= 0.5f, "Momentum changed at tick $tickIndex: $leftVelocity, $rightVelocity")
            assertTrue(abs(leftVelocity.y.raw) <= 1e-3f && abs(rightVelocity.y.raw) <= 1e-3f, "Vertical velocity at tick $tickIndex: $leftVelocity, $rightVelocity")
            assertTrue(left.physicsBody.position.x < right.physicsBody.position.x, "The circles passed through each other at tick $tickIndex.")
            if (leftVelocity.x.raw < 0f && rightVelocity.x.raw > 0f) {
                haveSeparated = true
            }
        }
        assertTrue(haveSeparated, "The circles never bounced apart.")
    }

    @Test
    fun simulationSpeedZeroFreezesTheWorld() = withPhysics { kubriko, physicsManager ->
        val scene = FallingScene()
        kubriko.register(scene)
        physicsManager.simulationSpeed.value = 0f
        val before = snapshot(scene.dynamicBodies)

        kubriko.tick(count = 100)

        assertBitsEqual(before, snapshot(scene.dynamicBodies), "A body moved at simulation speed 0.")
    }

    @Test
    fun pausedStateStopsTheSimulation() = withPhysics { kubriko, _ ->
        val scene = FallingScene()
        kubriko.register(scene)
        val stateManager = kubriko.kubriko.get<StateManager>()
        stateManager.updateIsRunning(false)
        awaitCondition { !stateManager.isRunning.value }
        val before = snapshot(scene.dynamicBodies)

        kubriko.tick(count = 100)
        assertBitsEqual(before, snapshot(scene.dynamicBodies), "A body moved while paused.")

        stateManager.updateIsRunning(true)
        awaitCondition { stateManager.isRunning.value }
        kubriko.tick(count = 5)
        assertTrue(!snapshot(scene.dynamicBodies).contentEquals(before), "Nothing moved after resuming.")
    }

    @Test
    fun steadyStateTickAllocationWithoutContactsIsBounded() = withPhysics(gravity = SceneOffset.Zero) { kubriko, _ ->
        val bodies = List(200) { index ->
            val column = index % 20 - 9.5f
            val row = index / 20 - 4.5f
            circle(x = column * 50f, y = row * 50f).apply { physicsBody.velocity = offset(column, row) }
        }
        kubriko.actorManager.add(bodies)
        val start = bodies.first().physicsBody.position
        kubriko.tickUntil { bodies.first().physicsBody.position != start }

        val allocatedBytesPerTick = measureAllocatedBytesPerRun(warmUpRuns = 2_000, measuredRuns = 500) { kubriko.tick() }

        assertTrue(allocatedBytesPerTick <= ALLOCATION_BUDGET_IN_BYTES, "Measured $allocatedBytesPerTick B/tick.")
    }

    @Ignore("measured 1 266 B/tick with 200 contacts")
    @Test
    fun steadyStateTickAllocationWithContacts() = withPhysics { kubriko, _ ->
        val floor = box(x = 0f, y = 500f, width = 6_000f, height = 100f, density = 0f)
        val bodies = List(200) { index -> circle(x = (index - 100) * 25f, y = 430f) }
        kubriko.actorManager.add(listOf(floor) + bodies)
        kubriko.tick(count = 600)

        val allocatedBytesPerTick = measureAllocatedBytesPerRun(warmUpRuns = 2_000, measuredRuns = 500) { kubriko.tick() }

        println("Measured $allocatedBytesPerTick B/tick with 200 contacts.")
    }

    /**
     * 60 circles and boxes above a static floor and two static ramps, nothing touching at the start.
     */
    private class FallingScene {
        val staticBodies = listOf(
            box(x = 0f, y = 500f, width = 2_000f, height = 100f, density = 0f),
            box(x = -300f, y = 300f, width = 400f, height = 20f, rotation = 0.3f, density = 0f),
            box(x = 300f, y = 300f, width = 400f, height = 20f, rotation = -0.3f, density = 0f),
        )
        val dynamicBodies = Random(42).let { random ->
            List(60) { index ->
                val x = (index % 10) * 60f - 270f + random.nextFloat() * 20f - 10f
                val y = (index / 10) * 60f - 200f + random.nextFloat() * 20f - 10f
                if (index % 2 == 0) circle(x = x, y = y) else box(x = x, y = y, width = 20f, height = 20f)
            }
        }
        val allBodies = staticBodies + dynamicBodies
    }

    private fun runFallingScene(): List<FloatArray> {
        val snapshots = ArrayList<FloatArray>()
        withPhysics { kubriko, _ ->
            val scene = FallingScene()
            kubriko.register(scene)
            repeat(SCENE_TICK_COUNT / CHECKPOINT_INTERVAL) {
                kubriko.tick(count = CHECKPOINT_INTERVAL)
                snapshots.add(snapshot(scene.allBodies))
            }
        }
        return snapshots
    }

    private fun ManualKubriko.register(scene: FallingScene) {
        actorManager.add(scene.allBodies)
        val starts = scene.dynamicBodies.map { it.physicsBody.position }
        tickUntil { scene.dynamicBodies.indices.any { scene.dynamicBodies[it].physicsBody.position != starts[it] } }
    }

    private fun withPhysics(
        gravity: SceneOffset = PhysicsManager.newInstance().gravity.value,
        block: (ManualKubriko, PhysicsManager) -> Unit,
    ) {
        val physicsManager = PhysicsManager.newInstance(initialGravity = gravity)
        val kubriko = newManualKubriko(
            ActorManager.newInstance(shouldComposeLayers = false, shouldPutFarAwayActorsToSleep = false),
            physicsManager,
        )
        try {
            block(kubriko, physicsManager)
        } finally {
            kubriko.dispose()
        }
    }

    private class TestBody(override val physicsBody: PhysicsBody) : RigidBody {
        override val collisionMask = physicsBody.collisionMask
        override val body = BoxBody()
    }

    private companion object {
        const val SCENE_TICK_COUNT = 600
        const val CHECKPOINT_INTERVAL = 100
        const val VALUES_PER_BODY = 5
        const val ALLOCATION_BUDGET_IN_BYTES = 1_024.0

        fun offset(x: Float, y: Float) = SceneOffset(x.sceneUnit, y.sceneUnit)

        fun circle(
            x: Float,
            y: Float,
            radius: Float = 10f,
            restitution: Float = 0.8f,
            friction: Float? = null,
        ) = TestBody(
            PhysicsBody(
                collisionMask = CircleCollisionMask(initialPosition = offset(x, y), initialRadius = radius.sceneUnit),
                restitution = restitution,
            ).apply {
                if (friction != null) {
                    staticFriction = friction
                    dynamicFriction = friction
                }
            }
        )

        fun box(
            x: Float,
            y: Float,
            width: Float,
            height: Float,
            rotation: Float = 0f,
            density: Float = 1f,
        ) = TestBody(
            PhysicsBody(
                collisionMask = BoxCollisionMask(
                    initialPosition = offset(x, y),
                    initialSize = SceneSize(width.sceneUnit, height.sceneUnit),
                    initialRotation = rotation.rad,
                ),
                rotation = rotation.rad,
                density = density,
            )
        )

        /** Position, rotation and velocity of every body, in order. */
        fun snapshot(bodies: List<TestBody>) = FloatArray(bodies.size * VALUES_PER_BODY).also { values ->
            bodies.forEachIndexed { index, testBody ->
                val body = testBody.physicsBody
                values[index * VALUES_PER_BODY] = body.position.x.raw
                values[index * VALUES_PER_BODY + 1] = body.position.y.raw
                values[index * VALUES_PER_BODY + 2] = body.rotation.raw
                values[index * VALUES_PER_BODY + 3] = body.velocity.x.raw
                values[index * VALUES_PER_BODY + 4] = body.velocity.y.raw
            }
        }

        fun assertBitsEqual(expected: FloatArray, actual: FloatArray, message: String) = expected.indices.forEach { index ->
            assertEquals(expected[index].toRawBits(), actual[index].toRawBits(), "$message (body ${index / VALUES_PER_BODY})")
        }

        fun SceneOffset.length() = sqrt(x.raw * x.raw + y.raw * y.raw)
    }
}
