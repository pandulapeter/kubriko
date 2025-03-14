/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.collision.extensions

import com.pandulapeter.kubriko.collision.mask.CollisionMask
import com.pandulapeter.kubriko.helpers.extensions.isWithin
import com.pandulapeter.kubriko.types.SceneOffset

fun SceneOffset.isCollidingWith(
    collisionMask: CollisionMask
) = if (isWithin(collisionMask.axisAlignedBoundingBox)) {
    // TODO: Perform detailed check using the collision masks
    true
} else {
    false
}