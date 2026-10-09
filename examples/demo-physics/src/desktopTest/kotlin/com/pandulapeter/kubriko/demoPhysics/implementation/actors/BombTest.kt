/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.demoPhysics.implementation.actors

import com.pandulapeter.kubriko.actor.body.BoxBody
import com.pandulapeter.kubriko.collision.mask.CircleCollisionMask
import com.pandulapeter.kubriko.helpers.extensions.sceneUnit
import com.pandulapeter.kubriko.physics.PhysicsBody
import com.pandulapeter.kubriko.physics.RigidBody
import com.pandulapeter.kubriko.testFixtures.awaitProcessed
import com.pandulapeter.kubriko.testFixtures.newManualKubriko
import com.pandulapeter.kubriko.types.SceneOffset
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

class BombTest {

    private class TestRigidBody : RigidBody {
        override val collisionMask = CircleCollisionMask(
            initialRadius = 10.sceneUnit,
            initialPosition = SceneOffset(100.sceneUnit, 0.sceneUnit),
        )
        override val body = BoxBody(initialPosition = collisionMask.position)
        override val physicsBody = PhysicsBody(collisionMask)
    }

    private fun velocityAfter(deltaTimeInMilliseconds: Int, count: Int) = newManualKubriko().use { kubriko ->
        val target = TestRigidBody()
        kubriko.actorManager.add(target)
        kubriko.actorManager.awaitProcessed()
        kubriko.actorManager.add(Bomb(epicenter = SceneOffset.Zero))
        kubriko.actorManager.awaitProcessed()
        kubriko.tick(deltaTimeInMilliseconds, count)
        target.physicsBody.velocity.x.raw
    }

    @Test
    fun totalImpulseDoesNotDependOnTheTickRate() {
        val atSixtyHertz = velocityAfter(17, 6)
        val atOneHundredTwentyHertz = velocityAfter(8, 13)
        val afterOneLongTick = velocityAfter(200, 1)
        assertTrue(atSixtyHertz > 0f)
        assertTrue(abs(atOneHundredTwentyHertz - atSixtyHertz) <= atSixtyHertz * 0.001f, "$atOneHundredTwentyHertz vs $atSixtyHertz")
        assertTrue(abs(afterOneLongTick - atSixtyHertz) <= atSixtyHertz * 0.001f, "$afterOneLongTick vs $atSixtyHertz")
    }

    @Test
    fun bombRemovesItselfAfterItsLifetime() = newManualKubriko().use { kubriko ->
        kubriko.actorManager.add(Bomb(epicenter = SceneOffset.Zero))
        kubriko.actorManager.awaitProcessed()
        kubriko.tick(16, 7)
        kubriko.actorManager.awaitProcessed()
        assertTrue(kubriko.actorManager.allActors.value.none { it is Bomb })
    }
}
