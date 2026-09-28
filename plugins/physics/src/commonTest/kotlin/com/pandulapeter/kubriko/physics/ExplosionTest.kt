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
import com.pandulapeter.kubriko.physics.explosions.ProximityExplosion
import com.pandulapeter.kubriko.types.SceneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ExplosionTest {

    @Test
    fun bodyAtEpicenterDoesNotStopTheImpulseForLaterBodies() {
        val atEpicenter = body(x = 0f)
        val other = body(x = 10f)
        val explosion = ProximityExplosion(epicenter = SceneOffset.Zero, proximity = 100f.sceneUnit)

        explosion.update(listOf(atEpicenter, other))
        explosion.applyBlastImpulse(1000f.sceneUnit)

        assertTrue(other.velocity.x.raw > 0f)
        assertEquals(SceneOffset.Zero, atEpicenter.velocity)
    }

    private fun body(x: Float) = PhysicsBody(
        collisionMask = CircleCollisionMask(
            initialPosition = SceneOffset(x.sceneUnit, 0f.sceneUnit),
            initialRadius = 5f.sceneUnit,
        ),
    )
}
