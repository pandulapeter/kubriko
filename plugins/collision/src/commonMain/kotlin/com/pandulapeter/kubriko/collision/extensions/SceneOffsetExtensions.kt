/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.collision.extensions

import com.pandulapeter.kubriko.actor.body.Body
import com.pandulapeter.kubriko.extensions.isWithin
import com.pandulapeter.kubriko.types.SceneOffset

fun SceneOffset.isWithin(
    other: Body
) = if (isWithin(other.axisAlignedBoundingBox)) {
    // TODO: Perform detailed check using the collision masks
    true
} else {
    false
}