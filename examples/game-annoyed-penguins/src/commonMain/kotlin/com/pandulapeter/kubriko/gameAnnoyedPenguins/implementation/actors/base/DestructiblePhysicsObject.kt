/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.gameAnnoyedPenguins.implementation.actors.base

import com.pandulapeter.kubriko.actor.traits.Dynamic
import com.pandulapeter.kubriko.actor.traits.Visible
import com.pandulapeter.kubriko.collision.mask.ComplexCollisionMask
import com.pandulapeter.kubriko.collision.mask.PolygonCollisionMask
import com.pandulapeter.kubriko.physics.RigidBody
import com.pandulapeter.kubriko.sceneEditor.Editable
import com.pandulapeter.kubriko.types.SceneOffset

internal abstract class DestructiblePhysicsObject<T : DestructiblePhysicsObject<T>> : Visible, Editable<T>, RigidBody, Dynamic {

    abstract override val collisionMask: ComplexCollisionMask

    override fun update(deltaTimeInMilliseconds: Int) {
        if (deltaTimeInMilliseconds > 0) {
            body.position = SceneOffset(physicsBody.position.x, physicsBody.position.y)
            body.rotation = physicsBody.orientation
            collisionMask.position = body.position
            (collisionMask as? PolygonCollisionMask)?.rotation = body.rotation
        }
    }
}