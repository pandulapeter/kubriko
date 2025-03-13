/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.collision

import com.pandulapeter.kubriko.actor.body.PointBody
import com.pandulapeter.kubriko.actor.traits.Positionable

//TODO: Documentation
interface Collidable : Positionable {

    // TODO: Use a different base class and map existing bodies to it, to support other mask shapes
    val collisionMask: PointBody get() = body
}