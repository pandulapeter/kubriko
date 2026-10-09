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

import com.pandulapeter.kubriko.collision.mask.BoxCollisionMask
import com.pandulapeter.kubriko.collision.mask.CircleCollisionMask
import com.pandulapeter.kubriko.helpers.extensions.isOverlapping
import com.pandulapeter.kubriko.helpers.extensions.sceneUnit
import com.pandulapeter.kubriko.physics.implementation.SweepAndPrune
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneSize
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SweepAndPruneTest {

    @Test
    fun pairsMatchTheNaiveLoopInTheSameOrder() {
        repeat(SEED_COUNT) { seed ->
            val bodies = randomBodies(Random(seed))

            assertEquals(naivePairs(bodies), SweepAndPrune().pairs(bodies), "Seed $seed")
        }
    }

    @Test
    fun pairsStillMatchAfterBodiesMoveBetweenCalls() {
        repeat(SEED_COUNT) { seed ->
            val random = Random(seed)
            val bodies = randomBodies(random)
            val sweepAndPrune = SweepAndPrune()
            sweepAndPrune.pairs(bodies)

            repeat(10) {
                val body = bodies[random.nextInt(bodies.size)]
                body.collisionMask.position = randomPosition(random)
            }

            assertEquals(naivePairs(bodies), sweepAndPrune.pairs(bodies), "Seed $seed")
        }
    }

    @Test
    fun nanBodyProducesNoPairAndKeepsTheOthers() {
        repeat(SEED_COUNT) { seed ->
            val bodies = randomBodies(Random(seed))
            val expectedPairs = naivePairs(bodies)
            val nanBody = PhysicsBody(
                CircleCollisionMask(
                    initialPosition = SceneOffset(Float.NaN.sceneUnit, 0f.sceneUnit),
                    initialRadius = 10f.sceneUnit,
                ),
            )
            val nanIndex = bodies.size / 2
            val bodiesWithNan = bodies.toMutableList().apply { add(nanIndex, nanBody) }

            val pairs = SweepAndPrune().pairs(bodiesWithNan)

            assertTrue(pairs.none { it.first == nanIndex || it.second == nanIndex }, "Seed $seed")
            val shiftedExpectedPairs = expectedPairs.map { (i, j) ->
                (if (i >= nanIndex) i + 1 else i) to (if (j >= nanIndex) j + 1 else j)
            }
            assertEquals(shiftedExpectedPairs, pairs, "Seed $seed")
        }
    }

    private fun SweepAndPrune.pairs(bodies: List<PhysicsBody>): List<Pair<Int, Int>> {
        val pairCount = findPairs(bodies)
        return List(pairCount) { firstBodyIndexAt(it) to secondBodyIndexAt(it) }
    }

    private fun naivePairs(bodies: List<PhysicsBody>): List<Pair<Int, Int>> {
        val pairs = mutableListOf<Pair<Int, Int>>()
        for (i in bodies.indices) {
            for (j in i + 1 until bodies.size) {
                val bodyA = bodies[i]
                val bodyB = bodies[j]
                if (bodyA.invMass == 0f && bodyB.invMass == 0f || bodyA.isParticle && bodyB.isParticle) {
                    continue
                }
                if (bodyA.collisionMask.axisAlignedBoundingBox.isOverlapping(bodyB.collisionMask.axisAlignedBoundingBox)) {
                    pairs.add(i to j)
                }
            }
        }
        return pairs
    }

    private fun randomBodies(random: Random) = List(BODY_COUNT) { index ->
        val position = randomPosition(random)
        val collisionMask = if (random.nextBoolean()) {
            CircleCollisionMask(
                initialPosition = position,
                initialRadius = random.nextInt(5, 40).toFloat().sceneUnit,
            )
        } else {
            BoxCollisionMask(
                initialPosition = position,
                initialSize = SceneSize(random.nextInt(10, 80).toFloat().sceneUnit, random.nextInt(10, 80).toFloat().sceneUnit),
            )
        }
        PhysicsBody(
            collisionMask = collisionMask,
            density = if (index % 7 == 0) 0f else 1f,
        ).apply {
            isParticle = index % 5 == 0
        }
    }

    private fun randomPosition(random: Random) = SceneOffset(
        random.nextInt(0, 500).toFloat().sceneUnit,
        random.nextInt(0, 500).toFloat().sceneUnit,
    )

    private companion object {
        const val SEED_COUNT = 20
        const val BODY_COUNT = 50
    }
}
