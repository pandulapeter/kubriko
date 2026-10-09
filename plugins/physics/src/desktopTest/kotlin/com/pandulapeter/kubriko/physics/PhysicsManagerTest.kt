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

import com.pandulapeter.kubriko.helpers.extensions.sceneUnit
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.physics.joints.Joint
import com.pandulapeter.kubriko.physics.joints.JointToBody
import com.pandulapeter.kubriko.physics.joints.JointToPoint
import com.pandulapeter.kubriko.testFixtures.ManualKubriko
import com.pandulapeter.kubriko.testFixtures.awaitProcessed
import com.pandulapeter.kubriko.testFixtures.newManualKubriko
import com.pandulapeter.kubriko.types.SceneOffset
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PhysicsManagerTest {

    private var kubriko: ManualKubriko? = null

    @AfterTest
    fun tearDown() {
        kubriko?.dispose()
    }

    @Test
    fun bodyAcceleratesAlongTheGravityVector() {
        val kubriko = newPhysicsKubriko(gravity = offset(10f, 0f))
        val ball = TestBall(isAffectedByGravity = true)
        kubriko.actorManager.add(ball)

        kubriko.tickUntil { ball.physicsBody.position != SceneOffset.Zero }
        kubriko.tick(count = 30)

        assertTrue(ball.physicsBody.velocity.x.raw > 0f, "Velocity: ${ball.physicsBody.velocity}")
        assertEquals(0f, ball.physicsBody.velocity.y.raw)
        assertTrue(ball.physicsBody.position.x.raw > 0f, "Position: ${ball.physicsBody.position}")
    }

    @Test
    fun bodyNotAffectedByGravityStaysAtRest() {
        val kubriko = newPhysicsKubriko(gravity = offset(0f, 10f))
        val floating = TestBall(isAffectedByGravity = false)
        val falling = TestBall(position = offset(1_000f, 0f), isAffectedByGravity = true)
        kubriko.actorManager.add(floating, falling)

        kubriko.tickUntil { falling.physicsBody.position.y.raw > 0f }
        kubriko.tick(count = 30)

        assertEquals(SceneOffset.Zero, floating.physicsBody.position)
        assertEquals(SceneOffset.Zero, floating.physicsBody.velocity)
    }

    @Test
    fun changedGravityTakesEffect() {
        val physicsManager = PhysicsManager.newInstance(initialGravity = SceneOffset.Zero)
        val kubriko = newPhysicsKubriko(physicsManager)
        val ball = TestBall(isAffectedByGravity = true)
        kubriko.actorManager.add(ball)
        kubriko.actorManager.awaitProcessed()
        kubriko.tick(count = 10)
        assertEquals(SceneOffset.Zero, ball.physicsBody.position)

        physicsManager.gravity.value = offset(0f, -10f)

        kubriko.tickUntil { ball.physicsBody.position.y.raw < 0f }
        assertEquals(0f, ball.physicsBody.position.x.raw)
    }

    @Test
    fun removedBodyIsNoLongerSimulated() {
        val kubriko = newPhysicsKubriko(gravity = SceneOffset.Zero)
        val ball = TestBall().apply { physicsBody.velocity = offset(10f, 0f) }
        kubriko.actorManager.add(ball)
        kubriko.tickUntil { ball.physicsBody.position != SceneOffset.Zero }

        kubriko.actorManager.remove(ball)
        kubriko.actorManager.awaitProcessed()
        kubriko.tickUntil {
            val position = ball.physicsBody.position
            kubriko.tick()
            position == ball.physicsBody.position
        }
        val positionAfterRemoval = ball.physicsBody.position
        kubriko.tick(count = 30)

        assertEquals(positionAfterRemoval, ball.physicsBody.position)
    }

    @Test
    fun nanBodyDoesNotHideOtherContacts() {
        val kubriko = newPhysicsKubriko(gravity = SceneOffset.Zero)
        val firstBodies = List(PAIR_COUNT) { TestBall(position = offset(it * 100f, 0f)) }
        val secondBodies = List(PAIR_COUNT) { TestBall(position = offset(it * 100f + 5f, 0f)) }
        val nanBody = TestBall(position = offset(Float.NaN, 0f))
        val finiteBodies = firstBodies + secondBodies
        val startPositions = finiteBodies.associateWith { it.physicsBody.position }
        kubriko.actorManager.add(firstBodies + nanBody + secondBodies)

        kubriko.tickUntil { finiteBodies.any { it.physicsBody.position != startPositions.getValue(it) } }

        val untouchedPairCount = (0 until PAIR_COUNT).count { index ->
            firstBodies[index].physicsBody.position == startPositions.getValue(firstBodies[index]) ||
                    secondBodies[index].physicsBody.position == startPositions.getValue(secondBodies[index])
        }
        assertEquals(0, untouchedPairCount, "$untouchedPairCount of $PAIR_COUNT pairs were not resolved.")
    }

    @Test
    fun stretchedJointPullsTheBodyTowardsItsAnchor() {
        val kubriko = newPhysicsKubriko(gravity = SceneOffset.Zero)
        val ball = TestBall(position = offset(200f, 0f))
        kubriko.actorManager.add(ball, TestJoint(JointToPoint(ball.physicsBody, SceneOffset.Zero, 50f.sceneUnit, JOINT_CONSTANT, JOINT_DAMPENING, false, SceneOffset.Zero)))

        kubriko.tickUntil { ball.physicsBody.position.x.raw < 150f }

        assertEquals(0f, ball.physicsBody.position.y.raw, 1e-3f)
    }

    @Test
    fun slackJointDoesNotPushACloserBody() {
        val kubriko = newPhysicsKubriko(gravity = SceneOffset.Zero)
        val ball = TestBall(position = offset(20f, 0f))
        val probe = TestBall(position = offset(1_000f, 0f)).apply { physicsBody.velocity = offset(10f, 0f) }
        kubriko.actorManager.add(ball, probe, TestJoint(JointToPoint(ball.physicsBody, SceneOffset.Zero, 50f.sceneUnit, JOINT_CONSTANT, JOINT_DAMPENING, true, SceneOffset.Zero)))

        kubriko.tickUntil { probe.physicsBody.position.x.raw > 1_000f }
        kubriko.tick(count = 30)

        assertEquals(offset(20f, 0f), ball.physicsBody.position)
    }

    @Test
    fun jointBetweenTwoBodiesPullsThemTogetherSymmetrically() {
        val kubriko = newPhysicsKubriko(gravity = SceneOffset.Zero)
        val left = TestBall(position = offset(-100f, 0f))
        val right = TestBall(position = offset(100f, 0f))
        kubriko.actorManager.add(
            left,
            right,
            TestJoint(JointToBody(left.physicsBody, right.physicsBody, 50f.sceneUnit, JOINT_CONSTANT, JOINT_DAMPENING, false, SceneOffset.Zero, SceneOffset.Zero)),
        )

        kubriko.tickUntil { right.physicsBody.position.x.raw - left.physicsBody.position.x.raw < 150f }

        assertEquals(0f, left.physicsBody.position.x.raw + right.physicsBody.position.x.raw, 1e-2f)
    }

    @Test
    fun simulationSpeedScalesTheDistanceTravelled() {
        val normalDistance = distanceTravelledInOneSecond(simulationSpeed = 1f)
        val halfDistance = distanceTravelledInOneSecond(simulationSpeed = 0.5f)

        assertEquals(0.5f, halfDistance / normalDistance, 1e-3f)
    }

    private fun distanceTravelledInOneSecond(simulationSpeed: Float): Float {
        val kubriko = newManualKubriko(
            ActorManager.newInstance(shouldComposeLayers = false),
            PhysicsManager.newInstance(initialGravity = SceneOffset.Zero, initialSimulationSpeed = simulationSpeed),
        )
        try {
            val ball = TestBall().apply { physicsBody.velocity = offset(100f, 0f) }
            kubriko.actorManager.add(ball)
            kubriko.tickUntil { ball.physicsBody.position != SceneOffset.Zero }
            val start = ball.physicsBody.position.x.raw
            kubriko.tick(count = 60)
            return ball.physicsBody.position.x.raw - start
        } finally {
            kubriko.dispose()
        }
    }

    private fun newPhysicsKubriko(gravity: SceneOffset) = newPhysicsKubriko(PhysicsManager.newInstance(initialGravity = gravity))

    private fun newPhysicsKubriko(physicsManager: PhysicsManager) = newManualKubriko(
        ActorManager.newInstance(shouldComposeLayers = false),
        physicsManager,
    ).also { kubriko = it }

    private class TestJoint(override val physicsJoint: Joint) : JointWrapper

    private fun offset(x: Float, y: Float) = SceneOffset(x.sceneUnit, y.sceneUnit)

    private companion object {
        const val PAIR_COUNT = 200
        const val JOINT_CONSTANT = 50f
        const val JOINT_DAMPENING = 1f
    }
}
