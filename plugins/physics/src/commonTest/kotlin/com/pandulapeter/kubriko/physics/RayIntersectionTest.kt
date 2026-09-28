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
import com.pandulapeter.kubriko.helpers.extensions.sceneUnit
import com.pandulapeter.kubriko.physics.rays.Ray
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneSize
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertSame

class RayIntersectionTest {

    @Test
    fun circleHitPointIsCorrectForRayNotStartingAtTheOrigin() {
        val ray = Ray(startPoint = offset(50f), direction = offset(1f), distance = 200f.sceneUnit)

        ray.updateProjection(listOf(circle(x = 100f)))

        assertHit(ray, expectedX = 90f)
    }

    // The mixed rays run at y = 1: along y = 0 the polygon edge test compares a -0.0 intersection with 0.0 and misses.

    @Test
    fun boxInFrontOfCircleIsTheClosestInEitherOrder() {
        val box = box(x = 100f)
        val circle = circle(x = 150f)

        listOf(listOf(box, circle), listOf(circle, box)).forEach { bodies ->
            val ray = Ray(startPoint = SceneOffset(0f.sceneUnit, 1f.sceneUnit), direction = offset(1f), distance = 400f.sceneUnit)
            ray.updateProjection(bodies)
            assertSame(box, assertHit(ray, expectedX = 90f, expectedY = 1f))
        }
    }

    @Test
    fun circleInFrontOfBoxIsTheClosest() {
        val circle = circle(x = 100f)
        val ray = Ray(startPoint = SceneOffset(0f.sceneUnit, 1f.sceneUnit), direction = offset(1f), distance = 400f.sceneUnit)

        ray.updateProjection(listOf(circle, box(x = 200f)))

        assertSame(circle, assertHit(ray, expectedX = 100f - sqrt(99f), expectedY = 1f))
    }

    private fun assertHit(ray: Ray, expectedX: Float, expectedY: Float = 0f): PhysicsBody {
        val rayInformation = assertNotNull(ray.rayInformation)
        assertEquals(expectedX, rayInformation.coordinates.x.raw, TOLERANCE)
        assertEquals(expectedY, rayInformation.coordinates.y.raw, TOLERANCE)
        return rayInformation.body
    }

    private fun offset(x: Float) = SceneOffset(x.sceneUnit, 0f.sceneUnit)

    private fun circle(x: Float) = PhysicsBody(CircleCollisionMask(initialPosition = offset(x), initialRadius = 10f.sceneUnit))

    private fun box(x: Float) = PhysicsBody(BoxCollisionMask(initialPosition = offset(x), initialSize = SceneSize(20f.sceneUnit, 20f.sceneUnit)))

    private companion object {
        const val TOLERANCE = 0.01f
    }
}
