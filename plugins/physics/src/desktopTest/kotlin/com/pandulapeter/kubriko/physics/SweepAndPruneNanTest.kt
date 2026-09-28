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
import kotlin.test.assertTrue

class SweepAndPruneNanTest {

    @Test
    fun nanBodyDoesNotHideOtherContacts() {
        val kubriko = newManualKubriko(
            ActorManager.newInstance(shouldComposeLayers = false),
            PhysicsManager.newInstance(initialGravity = SceneOffset.Zero),
        )
        try {
            val firstBodies = List(PAIR_COUNT) { TestBall(position = SceneOffset((it * 100f).sceneUnit, 0f.sceneUnit)) }
            val secondBodies = List(PAIR_COUNT) { TestBall(position = SceneOffset((it * 100f + 5f).sceneUnit, 0f.sceneUnit)) }
            val nanBody = TestBall(position = SceneOffset(Float.NaN.sceneUnit, 0f.sceneUnit))
            val finiteBodies = firstBodies + secondBodies
            val startPositions = finiteBodies.associateWith { it.physicsBody.position }
            kubriko.actorManager.add(firstBodies + nanBody + secondBodies)

            kubriko.tickUntil { finiteBodies.any { it.physicsBody.position != startPositions.getValue(it) } }

            val untouchedPairCount = (0 until PAIR_COUNT).count { index ->
                firstBodies[index].physicsBody.position == startPositions.getValue(firstBodies[index]) ||
                        secondBodies[index].physicsBody.position == startPositions.getValue(secondBodies[index])
            }
            assertTrue(untouchedPairCount == 0, "$untouchedPairCount of $PAIR_COUNT pairs were not resolved.")
        } finally {
            kubriko.dispose()
        }
    }

    private companion object {
        const val PAIR_COUNT = 200
    }
}
