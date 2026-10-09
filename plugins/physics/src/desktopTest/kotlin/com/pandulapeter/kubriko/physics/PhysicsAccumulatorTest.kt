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
import com.pandulapeter.kubriko.testFixtures.ManualKubriko
import com.pandulapeter.kubriko.testFixtures.newManualKubriko
import com.pandulapeter.kubriko.types.SceneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class PhysicsAccumulatorTest {

    @Test
    fun hugeDeltaDoesNotFreezeTheSimulation() = withMovingBall { kubriko, ball ->
        kubriko.tick(deltaTimeInMilliseconds = 20)
        kubriko.tick(deltaTimeInMilliseconds = Int.MAX_VALUE)
        val positionAfterHugeDelta = ball.physicsBody.position
        kubriko.tick(count = 10)

        assertNotEquals(positionAfterHugeDelta, ball.physicsBody.position)
    }

    @Test
    fun distanceTravelledDoesNotDependOnTheFrameRate() {
        val distances = FRAME_DURATIONS.map { frameDuration ->
            withMovingBall { kubriko, ball ->
                val start = ball.physicsBody.position.x.raw
                kubriko.tick(deltaTimeInMilliseconds = frameDuration, count = SIMULATED_MILLISECONDS / frameDuration)
                ball.physicsBody.position.x.raw - start
            }
        }

        assertTrue(distances.first() > 0f)
        distances.forEachIndexed { index, distance ->
            assertEquals(distances.first(), distance, 1e-2f, "At ${FRAME_DURATIONS[index]} ms per frame")
        }
    }

    @Test
    fun continuousForceAcceleratesEquallyAtAnyFrameRate() {
        val velocities = FRAME_DURATIONS.map { frameDuration ->
            withMovingBall { kubriko, ball ->
                val start = ball.physicsBody.velocity.y.raw
                repeat(SIMULATED_MILLISECONDS / frameDuration) {
                    ball.physicsBody.applyForce(SceneOffset(0f.sceneUnit, 1_000f.sceneUnit))
                    kubriko.tick(deltaTimeInMilliseconds = frameDuration)
                }
                ball.physicsBody.velocity.y.raw - start
            }
        }

        assertTrue(velocities.first() > 0f)
        velocities.forEachIndexed { index, velocity ->
            assertEquals(velocities.first(), velocity, 1e-2f, "At ${FRAME_DURATIONS[index]} ms per frame")
        }
    }

    @Test
    fun forceIsClearedAfterTheTickThatAppliesIt() = withMovingBall { kubriko, ball ->
        ball.physicsBody.applyForce(SceneOffset(0f.sceneUnit, 1_000f.sceneUnit))
        kubriko.tick()
        val velocityAfterForce = ball.physicsBody.velocity

        kubriko.tick(count = 10)

        assertEquals(SceneOffset.Zero, ball.physicsBody.force)
        assertEquals(velocityAfterForce, ball.physicsBody.velocity)
    }

    /** Runs [block] with a ball already moving through a world without gravity, so the simulation is known to include it. */
    private fun <T> withMovingBall(block: (ManualKubriko, TestBall) -> T): T {
        val kubriko = newManualKubriko(
            ActorManager.newInstance(shouldComposeLayers = false),
            PhysicsManager.newInstance(initialGravity = SceneOffset.Zero),
        )
        try {
            val ball = TestBall()
            ball.physicsBody.velocity = SceneOffset(10f.sceneUnit, 0f.sceneUnit)
            kubriko.actorManager.add(ball)
            kubriko.tickUntil { ball.physicsBody.position != SceneOffset.Zero }
            return block(kubriko, ball)
        } finally {
            kubriko.dispose()
        }
    }

    private companion object {
        const val SIMULATED_MILLISECONDS = 960
        val FRAME_DURATIONS = listOf(16, 20, 32, 48, 96)
    }
}
