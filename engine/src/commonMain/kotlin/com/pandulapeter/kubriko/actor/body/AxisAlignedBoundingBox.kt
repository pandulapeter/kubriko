/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.actor.body

import com.pandulapeter.kubriko.helpers.extensions.sceneUnit
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneSize

/**
 * An axis-aligned rectangle used for collision detection and visibility checks.
 *
 * Each [Body] and collision mask owns a single instance that it mutates in place as it moves, so
 * per-frame updates allocate nothing and scene coordinates carry full [Float] range and precision.
 * Consumers should read the current values through the owner's property rather than storing the
 * box - a stored reference keeps reflecting the owner's latest bounds.
 */
class AxisAlignedBoundingBox(min: SceneOffset, max: SceneOffset) {

    var minXRaw: Float = min.x.raw
        private set
    var minYRaw: Float = min.y.raw
        private set
    var maxXRaw: Float = max.x.raw
        private set
    var maxYRaw: Float = max.y.raw
        private set

    val left get() = minXRaw.sceneUnit
    val top get() = minYRaw.sceneUnit
    val right get() = maxXRaw.sceneUnit
    val bottom get() = maxYRaw.sceneUnit
    val min: SceneOffset get() = SceneOffset(left, top)
    val max: SceneOffset get() = SceneOffset(right, bottom)
    val size
        get() = SceneSize(
            width = right - left,
            height = bottom - top,
        )

    /**
     * Overwrites the bounds in place. Only the [Body] or collision mask that owns this box should call this.
     */
    fun update(min: SceneOffset, max: SceneOffset) {
        minXRaw = min.x.raw
        minYRaw = min.y.raw
        maxXRaw = max.x.raw
        maxYRaw = max.y.raw
    }
}
