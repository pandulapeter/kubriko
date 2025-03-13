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

import com.pandulapeter.kubriko.collision.Collidable
import com.pandulapeter.kubriko.helpers.extensions.isInside

fun Collidable.isOverlappingWith(
    other: Collidable
): Boolean {
    val adjustedAABB = collisionMask.axisAlignedBoundingBox.copy(
        min = collisionMask.axisAlignedBoundingBox.min + body.position,
        max = collisionMask.axisAlignedBoundingBox.max + body.position,
    )
    val otherAdjustedAABB = other.collisionMask.axisAlignedBoundingBox.copy(
        min = other.collisionMask.axisAlignedBoundingBox.min + other.body.position,
        max = other.collisionMask.axisAlignedBoundingBox.max + other.body.position,
    )
    return if (adjustedAABB.isInside(otherAdjustedAABB)) {
        // TODO: Perform detailed check using the collision masks
        true
    } else {
        false
    }
}