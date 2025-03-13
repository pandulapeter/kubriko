/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.physics

import com.pandulapeter.kubriko.actor.traits.Positionable
import com.pandulapeter.kubriko.collision.Collidable
import com.pandulapeter.kubriko.physics.implementation.dynamics.Body

interface RigidBody : Positionable, Collidable {

    // TODO Should be merged with collisionMask
    val physicsBody: Body
}