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
import com.pandulapeter.kubriko.testFixtures.newManualKubriko
import com.pandulapeter.kubriko.types.SceneOffset
import kotlin.test.Test
import kotlin.test.assertNotEquals

class PhysicsAccumulatorTest {

    @Test
    fun hugeDeltaDoesNotFreezeTheSimulation() {
        val kubriko = newManualKubriko(
            ActorManager.newInstance(shouldComposeLayers = false, shouldPutFarAwayActorsToSleep = false),
            PhysicsManager.newInstance(initialGravity = SceneOffset.Zero),
        )
        try {
            val ball = TestBall()
            ball.physicsBody.velocity = SceneOffset(10f.sceneUnit, 0f.sceneUnit)
            kubriko.actorManager.add(ball)
            val startPosition = ball.physicsBody.position
            kubriko.tickUntil { ball.physicsBody.position != startPosition }

            kubriko.tick(deltaTimeInMilliseconds = 20)
            kubriko.tick(deltaTimeInMilliseconds = Int.MAX_VALUE)
            val positionAfterHugeDelta = ball.physicsBody.position
            repeat(10) { kubriko.tick() }

            assertNotEquals(positionAfterHugeDelta, ball.physicsBody.position)
        } finally {
            kubriko.dispose()
        }
    }
}
