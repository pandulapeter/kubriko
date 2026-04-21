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

import com.pandulapeter.kubriko.collision.Collidable

/**
 * An interface for actors that have a physical presence in the world.
 *
 * Actors implementing this interface are automatically tracked by the [PhysicsManager].
 */
interface RigidBody : Collidable {

    /**
     * The physical body associated with this actor.
     */
    val physicsBody: PhysicsBody
}
