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

import com.pandulapeter.kubriko.collision.mask.CircleCollisionMask
import com.pandulapeter.kubriko.helpers.extensions.sceneUnit
import com.pandulapeter.kubriko.physics.explosions.RaycastExplosion
import com.pandulapeter.kubriko.types.SceneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RaycastExplosionTest {

    @Test
    fun castsTheRequestedNumberOfRays() {
        val explosion = explosion()

        val rays = explosion.rayScatter.rays
        assertEquals(36, rays.size)
        assertEquals(rays.size, rays.map { it.direction }.toSet().size)
    }

    @Test
    fun bodyWithinReachIsPushedAwayFromTheEpicenter() {
        val body = body(x = 50f)
        val explosion = explosion()

        explosion.update(listOf(body))
        explosion.applyBlastImpulse(BLAST_POWER)

        assertTrue(body.velocity.x.raw > 0f)
    }

    @Test
    fun bodyOutOfReachIsUntouched() {
        val body = body(x = 500f)
        val explosion = explosion()

        explosion.update(listOf(body))
        explosion.applyBlastImpulse(BLAST_POWER)

        assertEquals(SceneOffset.Zero, body.velocity)
    }

    @Test
    fun castingTwiceKeepsTheRayCount() {
        val explosion = explosion()

        explosion.rayScatter.castRays(100f.sceneUnit)

        assertEquals(36, explosion.rayScatter.rays.size)
    }

    private fun explosion() = RaycastExplosion(
        epicenter = SceneOffset.Zero,
        noOfRays = 36,
        distance = 100f.sceneUnit,
        worldBodies = emptyList(),
    )

    private fun body(x: Float) = PhysicsBody(
        collisionMask = CircleCollisionMask(
            initialPosition = SceneOffset(x.sceneUnit, 0f.sceneUnit),
            initialRadius = 10f.sceneUnit,
        ),
    )

    companion object {
        /** Large enough that a single ray's impulse clears the body's minimum velocity of 0.1. */
        private val BLAST_POWER = 100_000f.sceneUnit
    }
}
