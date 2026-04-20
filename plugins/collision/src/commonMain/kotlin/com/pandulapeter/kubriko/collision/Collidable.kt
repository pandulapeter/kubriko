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

import com.pandulapeter.kubriko.actor.traits.Positionable
import com.pandulapeter.kubriko.collision.mask.CollisionMask

/**
 * A trait for actors that can collide with other actors.
 *
 * Implementing this interface allows the actor to be processed by the [CollisionManager].
 */
interface Collidable : Positionable {

    /**
     * The shape used for collision detection.
     */
    val collisionMask: CollisionMask
}