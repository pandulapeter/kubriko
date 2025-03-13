/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.actor.body

import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneSize

// TODO: Could be optimized by making it mutable
data class AxisAlignedBoundingBox(
    val min: SceneOffset,
    val max: SceneOffset
) {
    val size = SceneSize(
        width = max.x - min.x,
        height = max.y - min.y,
    )
    val left = min.x
    val top = min.y
    val right = max.x
    val bottom = max.y

    fun withOffset(offset: SceneOffset) = AxisAlignedBoundingBox(
        min = min + offset,
        max = max + offset,
    )
}