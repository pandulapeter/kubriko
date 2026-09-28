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
import com.pandulapeter.kubriko.helpers.extensions.rad
import com.pandulapeter.kubriko.helpers.extensions.sceneUnit
import com.pandulapeter.kubriko.physics.rays.Ray
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneSize
import kotlin.math.PI
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame

class RayIntersectionTest {

    @Test
    fun circleHitPointIsCorrectForRayNotStartingAtTheOrigin() {
        val ray = Ray(startPoint = offset(50f), direction = offset(1f), distance = 200f.sceneUnit)

        ray.updateProjection(listOf(circle(x = 100f)))

        assertHit(ray, expectedX = 90f)
    }

    @Test
    fun boxInFrontOfCircleIsTheClosestInEitherOrder() {
        val box = box(x = 100f)
        val circle = circle(x = 150f)

        listOf(listOf(box, circle), listOf(circle, box)).forEach { bodies ->
            val ray = Ray(startPoint = offset(0f), direction = offset(1f), distance = 400f.sceneUnit)
            ray.updateProjection(bodies)
            assertSame(box, assertHit(ray, expectedX = 90f))
        }
    }

    @Test
    fun circleInFrontOfBoxIsTheClosest() {
        val circle = circle(x = 100f)
        val ray = Ray(startPoint = offset(0f), direction = offset(1f), distance = 400f.sceneUnit)

        ray.updateProjection(listOf(circle, box(x = 200f)))

        assertSame(circle, assertHit(ray, expectedX = 90f))
    }

    @Test
    fun axisAlignedRaysHitTheNearEdgeOfABox() {
        listOf(0f, 7.3f).forEach { c ->
            assertHit(castRay(startX = 0f, startY = c, directionX = 1f, directionY = 0f, boxX = 100f, boxY = c), expectedX = 90f, expectedY = c)
            assertHit(castRay(startX = 200f, startY = c, directionX = -1f, directionY = 0f, boxX = 100f, boxY = c), expectedX = 110f, expectedY = c)
            assertHit(castRay(startX = c, startY = 0f, directionX = 0f, directionY = 1f, boxX = c, boxY = 100f), expectedX = c, expectedY = 90f)
            assertHit(castRay(startX = c, startY = 200f, directionX = 0f, directionY = -1f, boxX = c, boxY = 100f), expectedX = c, expectedY = 110f)
        }
    }

    @Test
    fun nearVerticalRayFromAnAngleHitsTheNearEdge() {
        val ray = Ray(startPoint = SceneOffset(7.3f.sceneUnit, 0f.sceneUnit), direction = (PI / 2).toFloat().rad, distance = 400f.sceneUnit)

        ray.updateProjection(listOf(box(x = 7.3f, y = 100f)))

        assertHit(ray, expectedX = 7.3f, expectedY = 90f)
    }

    @Test
    fun rayEndingBeforeABoxMissesIt() {
        val ray = Ray(startPoint = offset(0f), direction = offset(1f), distance = 50f.sceneUnit)

        ray.updateProjection(listOf(box(x = 100f)))

        assertNull(ray.rayInformation)
    }

    private fun castRay(startX: Float, startY: Float, directionX: Float, directionY: Float, boxX: Float, boxY: Float) = Ray(
        startPoint = SceneOffset(startX.sceneUnit, startY.sceneUnit),
        direction = SceneOffset(directionX.sceneUnit, directionY.sceneUnit),
        distance = 400f.sceneUnit,
    ).also { it.updateProjection(listOf(box(x = boxX, y = boxY))) }

    private fun assertHit(ray: Ray, expectedX: Float, expectedY: Float = 0f): PhysicsBody {
        val rayInformation = assertNotNull(ray.rayInformation)
        assertEquals(expectedX, rayInformation.coordinates.x.raw, TOLERANCE)
        assertEquals(expectedY, rayInformation.coordinates.y.raw, TOLERANCE)
        return rayInformation.body
    }

    private fun offset(x: Float, y: Float = 0f) = SceneOffset(x.sceneUnit, y.sceneUnit)

    private fun circle(x: Float, y: Float = 0f) = PhysicsBody(CircleCollisionMask(initialPosition = offset(x, y), initialRadius = 10f.sceneUnit))

    private fun box(x: Float, y: Float = 0f) = PhysicsBody(BoxCollisionMask(initialPosition = offset(x, y), initialSize = SceneSize(20f.sceneUnit, 20f.sceneUnit)))

    private companion object {
        const val TOLERANCE = 0.01f
    }
}
