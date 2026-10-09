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
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneSize
import com.pandulapeter.kubriko.types.SceneUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class PhysicsBodyTest {

    @Test
    fun linearImpulseChangesVelocityInverselyToMass() {
        val small = circle(radius = 10f)
        val large = circle(radius = 20f)

        small.applyLinearImpulse(offset(10_000f, 0f))
        large.applyLinearImpulse(offset(10_000f, 0f))

        assertEquals(4f, small.velocity.x.raw / large.velocity.x.raw, 1e-3f)
        assertEquals(0f, small.velocity.y.raw)
    }

    @Test
    fun linearImpulsesAddUp() {
        val body = box()

        body.applyLinearImpulse(offset(4_000f, 0f))
        val velocityAfterOneImpulse = body.velocity.x.raw
        body.applyLinearImpulse(offset(4_000f, 0f))

        assertEquals(2 * velocityAfterOneImpulse, body.velocity.x.raw, 1e-3f)
    }

    @Test
    fun zeroDensityBodyIgnoresImpulses() {
        val body = circle(radius = 10f, density = 0f)

        body.applyLinearImpulse(offset(10_000f, 0f))
        body.applyLinearImpulse(offset(10_000f, 0f), contactPoint = offset(0f, 5f))

        assertEquals(SceneOffset.Zero, body.velocity)
        assertEquals(SceneUnit.Zero, body.angularVelocity)
    }

    @Test
    fun settingDensityToZeroMakesTheBodyStatic() {
        val body = circle(radius = 10f)

        body.density = 0f
        body.applyLinearImpulse(offset(10_000f, 0f), contactPoint = offset(0f, 5f))

        assertEquals(SceneOffset.Zero, body.velocity)
    }

    @Test
    fun offCenterImpulseSpinsTheBody() {
        val body = box()

        body.applyLinearImpulse(offset(4_000f, 0f), contactPoint = offset(0f, 10f))

        assertNotEquals(0f, body.angularVelocity.raw)
    }

    @Test
    fun negligibleVelocityAndForceAreClampedToZero() {
        val body = circle(radius = 10f)

        body.velocity = offset(0.05f, 0.05f)
        body.force = offset(0.05f, 0.05f)
        body.angularVelocity = 0.005f.sceneUnit
        body.torque = 0.05f.sceneUnit

        assertEquals(SceneOffset.Zero, body.velocity)
        assertEquals(SceneOffset.Zero, body.force)
        assertEquals(SceneUnit.Zero, body.angularVelocity)
        assertEquals(SceneUnit.Zero, body.torque)
    }

    @Test
    fun appliedForcesAccumulate() {
        val body = circle(radius = 10f)

        body.applyForce(offset(5f, 0f))
        body.applyForce(offset(0f, 3f))

        assertEquals(offset(5f, 3f), body.force)
    }

    private fun circle(radius: Float, density: Float = 1f) = PhysicsBody(
        collisionMask = CircleCollisionMask(initialRadius = radius.sceneUnit),
        density = density,
    )

    private fun box() = PhysicsBody(
        collisionMask = BoxCollisionMask(initialSize = SceneSize(20f.sceneUnit, 20f.sceneUnit)),
    )

    private fun offset(x: Float, y: Float) = SceneOffset(x.sceneUnit, y.sceneUnit)
}
