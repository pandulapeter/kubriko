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

import com.pandulapeter.kubriko.actor.body.BoxBody
import com.pandulapeter.kubriko.collision.mask.CircleCollisionMask
import com.pandulapeter.kubriko.helpers.extensions.sceneUnit
import com.pandulapeter.kubriko.types.SceneOffset

/**
 * A circular [RigidBody] for tests.
 */
internal class TestBall(
    position: SceneOffset = SceneOffset.Zero,
    radius: Float = 10f,
    density: Float = 1f,
    isAffectedByGravity: Boolean = false,
) : RigidBody {
    override val collisionMask = CircleCollisionMask(initialPosition = position, initialRadius = radius.sceneUnit)
    override val body = BoxBody(initialPosition = position)
    override val physicsBody = PhysicsBody(
        collisionMask = collisionMask,
        density = density,
        isAffectedByGravity = isAffectedByGravity,
    )
}
