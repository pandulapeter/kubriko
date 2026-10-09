/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.collision.extensions

import com.pandulapeter.kubriko.collision.mask.BoxCollisionMask
import com.pandulapeter.kubriko.collision.mask.CircleCollisionMask
import com.pandulapeter.kubriko.collision.mask.CollisionMask
import com.pandulapeter.kubriko.collision.mask.PointCollisionMask
import com.pandulapeter.kubriko.helpers.extensions.sceneUnit
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneSize
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CollisionMaskExtensionsTest {

    /** Spans x 20..40 and y -10..10. */
    private val wall = BoxCollisionMask(initialPosition = offset(30f, 0f), initialSize = SceneSize(20f.sceneUnit, 20f.sceneUnit))

    @Test
    fun slidingMovementReturnsTheWholeMovementWhenThePathIsClear() {
        val mover = circle(0f, 0f)

        val movement = mover.slidingMovement(offset(-50f, 30f), listOf(wall))

        assertOffset(-50f, 30f, movement)
    }

    @Test
    fun headOnSlidingMovementStopsAtTheObstacleSurface() {
        val mover = circle(0f, 0f)

        val movement = mover.slidingMovement(offset(20f, 0f), listOf(wall))

        assertOffset(10f, 0f, movement)
    }

    @Test
    fun angledSlidingMovementKeepsTheTangentialComponent() {
        val mover = circle(0f, 0f)

        val movement = mover.slidingMovement(offset(20f, 5f), listOf(wall))

        assertOffset(10f, 5f, movement)
    }

    @Test
    fun slidingMovementLeavesTheMaskWhereItWas() {
        val mover = circle(3f, 4f)

        mover.slidingMovement(offset(20f, 0f), listOf(wall))

        assertEquals(offset(3f, 4f), mover.position)
    }

    @Test
    fun slidingMovementIgnoresTheReceiverInTheObstacleList() {
        val mover = circle(0f, 0f)

        val movement = mover.slidingMovement(offset(5f, 0f), listOf(mover))

        assertOffset(5f, 0f, movement)
    }

    @Test
    fun depenetrationSeparatesAnOverlappingMask() {
        val mover = circle(15f, 0f)

        val push = mover.depenetrationFrom(listOf(wall))

        assertOffset(-5f, 0f, push)
    }

    @Test
    fun depenetrationIsZeroWithoutOverlap() {
        val mover = circle(-50f, 0f)

        assertEquals(SceneOffset.Zero, mover.depenetrationFrom(listOf(wall, mover)))
    }

    @Test
    fun deepestCollisionIsTheOneWithTheLargestPenetration() {
        val mover = circle(0f, 0f)
        val shallow = circle(18f, 0f)
        val deep = circle(5f, 0f)

        val result = assertNotNull(mover.deepestCollisionWith(listOf(shallow, deep, mover)))

        assertEquals(15f, result.penetration.raw, TOLERANCE)
    }

    @Test
    fun firstCollisionIsTheEarliestOverlappingObstacleInTheList() {
        val mover = circle(0f, 0f)
        val far = circle(100f, 0f)
        val shallow = circle(18f, 0f)
        val deep = circle(5f, 0f)

        val result = assertNotNull(mover.firstCollisionWith(listOf(mover, far, shallow, deep)))

        assertEquals(2f, result.penetration.raw, TOLERANCE)
    }

    @Test
    fun collisionQueriesFindNothingAmongNonOverlappingObstacles() {
        val mover = circle(0f, 0f)
        val obstacles = listOf<CollisionMask>(mover, circle(100f, 0f), wall)

        assertNull(mover.deepestCollisionWith(obstacles))
        assertNull(mover.firstCollisionWith(obstacles))
        assertFalse(mover.collidesWithAny(obstacles))
    }

    @Test
    fun collidesWithAnyFindsASingleOverlap() {
        val mover = circle(15f, 0f)

        assertTrue(mover.collidesWithAny(listOf(circle(100f, 0f), wall)))
    }

    @Test
    fun sceneOffsetCollidesWithTheInsideOfAComplexMaskAndOnlyThePositionOfAPointMask() {
        assertTrue(offset(25f, 5f).isCollidingWith(wall))
        assertFalse(offset(45f, 5f).isCollidingWith(wall))
        assertTrue(offset(1f, 2f).isCollidingWith(PointCollisionMask(offset(1f, 2f))))
        assertFalse(offset(1f, 2.5f).isCollidingWith(PointCollisionMask(offset(1f, 2f))))
    }

    private fun assertOffset(expectedX: Float, expectedY: Float, actual: SceneOffset) {
        assertEquals(expectedX, actual.x.raw, TOLERANCE, "x of $actual")
        assertEquals(expectedY, actual.y.raw, TOLERANCE, "y of $actual")
    }

    private fun circle(x: Float, y: Float) = CircleCollisionMask(initialPosition = offset(x, y), initialRadius = 10f.sceneUnit)

    private fun offset(x: Float, y: Float) = SceneOffset(x.sceneUnit, y.sceneUnit)

    private companion object {
        const val TOLERANCE = 1e-3f
    }
}
