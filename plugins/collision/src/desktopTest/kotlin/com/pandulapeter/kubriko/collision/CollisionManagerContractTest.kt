/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.collision

import com.pandulapeter.kubriko.actor.body.BoxBody
import com.pandulapeter.kubriko.collision.extensions.isCollidingWith
import com.pandulapeter.kubriko.collision.extensions.slidingMovement
import com.pandulapeter.kubriko.collision.mask.CircleCollisionMask
import com.pandulapeter.kubriko.collision.mask.CollisionMask
import com.pandulapeter.kubriko.collision.mask.PolygonCollisionMask
import com.pandulapeter.kubriko.helpers.extensions.rad
import com.pandulapeter.kubriko.helpers.extensions.sceneUnit
import com.pandulapeter.kubriko.testFixtures.ManualKubriko
import com.pandulapeter.kubriko.testFixtures.awaitProcessed
import com.pandulapeter.kubriko.testFixtures.measureAllocatedBytesPerRun
import com.pandulapeter.kubriko.testFixtures.newManualKubriko
import com.pandulapeter.kubriko.types.SceneOffset
import java.util.Collections
import java.util.IdentityHashMap
import kotlin.math.PI
import kotlin.random.Random
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CollisionManagerContractTest {

    @Test
    fun detectorHearsOnlyItsTypes() = withKubriko { collisionManager, kubriko ->
        val overlappingA = TypeA(circle(5f, 0f))
        val overlappingB = TypeB(circle(5f, 0f))
        val farA = TypeA(circle(1_000f, 0f))
        val detector = RecordingDetector(circle(0f, 0f), listOf(TypeA::class))
        kubriko.register(collisionManager, overlappingA, overlappingB, farA, detector)

        detector.clear()
        kubriko.tick()

        assertEquals(setOf<Collidable>(overlappingA), detector.reported)
    }

    @Test
    fun detectorHearsSubtypesOfItsTypes() = withKubriko { collisionManager, kubriko ->
        val overlappingA = TypeA(circle(5f, 0f))
        val overlappingB = TypeB(circle(-5f, 0f))
        val detector = RecordingDetector(circle(0f, 0f), listOf(TestCollidable::class))
        kubriko.register(collisionManager, overlappingA, overlappingB, detector)

        detector.clear()
        kubriko.tick()

        assertEquals(setOf<Collidable>(overlappingA, overlappingB), detector.reported)
    }

    @Test
    fun detectorIsNeverReportedCollidingWithItself() = withKubriko { collisionManager, kubriko ->
        val first = RecordingDetector(circle(0f, 0f), listOf(RecordingDetector::class))
        val second = RecordingDetector(circle(5f, 0f), listOf(RecordingDetector::class))
        kubriko.register(collisionManager, first, second)

        first.clear()
        second.clear()
        kubriko.tick()

        assertEquals(setOf<Collidable>(second), first.reported)
        assertEquals(setOf<Collidable>(first), second.reported)
    }

    @Test
    fun detectorIsNotCalledWithoutOverlap() = withKubriko { collisionManager, kubriko ->
        val farA = TypeA(circle(1_000f, 0f))
        val detector = CountingDetector(circle(0f, 0f), listOf(TypeA::class))
        kubriko.register(collisionManager, farA, detector)

        kubriko.tick(count = 3)

        assertEquals(0, detector.callCount)
    }

    @Test
    fun managerSlidingMovementIsBlockedOnlyByTheCollidablesItAccepts() = withKubriko { collisionManager, kubriko ->
        val mover = TypeA(circle(0f, 0f))
        val solid = TypeA(circle(30f, 0f))
        val ghost = TypeB(circle(-30f, 0f))
        kubriko.register(collisionManager, mover, solid, ghost)

        val blocked = mover.slidingMovement(SceneOffset(20f.sceneUnit, 0f.sceneUnit), collisionManager) { it is TypeA }
        val unblocked = mover.slidingMovement(SceneOffset((-20f).sceneUnit, 0f.sceneUnit), collisionManager) { it is TypeA }

        assertEquals(10f, blocked.x.raw, 1e-3f)
        assertEquals(-20f, unblocked.x.raw, 1e-3f)
    }

    @Test
    fun noCallbackAfterRemoval()= withKubriko { collisionManager, kubriko ->
        val overlappingA = TypeA(circle(5f, 0f))
        val overlappingB = TypeB(circle(5f, 0f))
        val detector = RecordingDetector(circle(0f, 0f), listOf(TypeA::class))
        kubriko.register(collisionManager, overlappingA, overlappingB, detector)

        kubriko.actorManager.remove(overlappingA)
        kubriko.actorManager.awaitProcessed()
        kubriko.tickUntil { overlappingA !in collisionManager.collidables.value }
        repeat(5) {
            detector.clear()
            kubriko.tick()
            assertTrue(detector.reported.isEmpty(), "Reported ${detector.reported} after the removal.")
        }
    }

    @Test
    fun broadPhaseMatchesBruteForceWithFewCandidates() = assertBroadPhaseMatchesBruteForce(collidableCount = 20)

    @Test
    fun broadPhaseMatchesBruteForceWithManyCandidates() = assertBroadPhaseMatchesBruteForce(collidableCount = 200)

    @Test
    fun steadyStateTickAllocationIsBounded() = withKubriko { collisionManager, kubriko ->
        val random = Random(1)
        val collidables = List(300) { TypeA(randomMask(random)) }
        val detectors = List(30) { CountingDetector(randomMask(random), listOf(TypeA::class)) }
        kubriko.register(collisionManager, *(collidables + detectors).toTypedArray())

        val allocatedBytesPerTick = measureAllocatedBytesPerRun { kubriko.tick() }

        assertTrue(allocatedBytesPerTick <= ALLOCATION_BUDGET_IN_BYTES, "Measured $allocatedBytesPerTick B/tick.")
    }

    private fun assertBroadPhaseMatchesBruteForce(collidableCount: Int) {
        (1..10).forEach { seed ->
            withKubriko { collisionManager, kubriko ->
                val random = Random(seed)
                val collidables = List(collidableCount) { TypeA(randomMask(random)) }
                val detectors = List(collidableCount / 10) { RecordingDetector(randomMask(random), listOf(TypeA::class)) }
                kubriko.register(collisionManager, *(collidables + detectors).toTypedArray())
                val allMasks = (collidables + detectors).map { it.collisionMask }
                repeat(20) { tickIndex ->
                    allMasks.forEach { mask ->
                        mask.position += SceneOffset((random.nextFloat() * 100f - 50f).sceneUnit, (random.nextFloat() * 100f - 50f).sceneUnit)
                    }
                    detectors.forEach { it.clear() }
                    kubriko.tick()
                    detectors.forEachIndexed { detectorIndex, detector ->
                        val expected = collisionManager.collidables.value
                            .filter { it is TypeA && it !== detector && detector.isCollidingWith(it) }
                            .toSet()
                        val message = "Seed $seed, $collidableCount candidates, tick $tickIndex, detector $detectorIndex"
                        assertEquals(expected, detector.reported, message)
                        assertFalse(detector.hasReportedDuplicates, "$message reported a candidate twice.")
                    }
                }
            }
        }
    }

    private fun withKubriko(block: (CollisionManager, ManualKubriko) -> Unit) {
        val collisionManager = CollisionManager.newInstance()
        val kubriko = newManualKubriko(collisionManager)
        try {
            block(collisionManager, kubriko)
        } finally {
            kubriko.dispose()
        }
    }

    private fun ManualKubriko.register(collisionManager: CollisionManager, vararg collidables: Collidable) {
        actorManager.add(*collidables)
        val detectorCount = collidables.count { it is CollisionDetector }
        // The tick tickUntil returns on may still have run with the old lists, so one more follows.
        tickUntil { collisionManager.collidables.value.size == collidables.size && collisionManager.collisionDetectors.value.size == detectorCount }
        tick()
    }

    private fun randomMask(random: Random): CollisionMask {
        val position = SceneOffset((random.nextFloat() * AREA_SIZE).sceneUnit, (random.nextFloat() * AREA_SIZE).sceneUnit)
        return CollisionMaskContractTest.CATALOGUE[random.nextInt(CollisionMaskContractTest.CATALOGUE.size)].create(position).also { mask ->
            if (mask is PolygonCollisionMask) {
                mask.rotation = (random.nextFloat() * 2 * PI).toFloat().rad
            }
        }
    }

    private fun circle(x: Float, y: Float) = CircleCollisionMask(initialPosition = SceneOffset(x.sceneUnit, y.sceneUnit), initialRadius = 10f.sceneUnit)

    private open class TestCollidable(override val collisionMask: CollisionMask) : Collidable {
        override val body = BoxBody()
    }

    private class TypeA(collisionMask: CollisionMask) : TestCollidable(collisionMask)

    private class TypeB(collisionMask: CollisionMask) : TestCollidable(collisionMask)

    private class RecordingDetector(
        collisionMask: CollisionMask,
        override val collidableTypes: List<KClass<out Collidable>>,
    ) : TestCollidable(collisionMask), CollisionDetector {
        val reported: MutableSet<Collidable> = Collections.newSetFromMap(IdentityHashMap())
        var hasReportedDuplicates = false
            private set

        fun clear() {
            reported.clear()
            hasReportedDuplicates = false
        }

        override fun onCollisionDetected(collidables: List<Collidable>) {
            collidables.forEach { collidable ->
                if (!reported.add(collidable)) {
                    hasReportedDuplicates = true
                }
            }
        }
    }

    private class CountingDetector(
        collisionMask: CollisionMask,
        override val collidableTypes: List<KClass<out Collidable>>,
    ) : TestCollidable(collisionMask), CollisionDetector {
        var callCount = 0

        override fun onCollisionDetected(collidables: List<Collidable>) {
            callCount++
        }
    }

    private companion object {
        const val AREA_SIZE = 2_000f
        const val ALLOCATION_BUDGET_IN_BYTES = 1_024.0
    }
}
