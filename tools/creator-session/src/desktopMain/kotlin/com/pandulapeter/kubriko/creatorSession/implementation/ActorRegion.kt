/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.creatorSession.implementation

import com.pandulapeter.kubriko.actor.body.AxisAlignedBoundingBox
import com.pandulapeter.kubriko.types.SceneOffset

/**
 * Where some Actors are within the game's view, as fractions of it from 0 to 1, clamped to it.
 */
internal data class ActorRegion(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
) {
    companion object {

        /**
         * The region that [bounds] take up together, in a view showing the scene from [viewTopLeft] to [viewBottomRight],
         * or null when there are none or they are all outside it.
         */
        fun of(bounds: List<AxisAlignedBoundingBox>, viewTopLeft: SceneOffset, viewBottomRight: SceneOffset): ActorRegion? {
            if (bounds.isEmpty()) return null
            val width = viewBottomRight.x.raw - viewTopLeft.x.raw
            val height = viewBottomRight.y.raw - viewTopLeft.y.raw
            if (width <= 0f || height <= 0f) return null
            fun x(scene: Float) = ((scene - viewTopLeft.x.raw) / width).coerceIn(0f, 1f)
            fun y(scene: Float) = ((scene - viewTopLeft.y.raw) / height).coerceIn(0f, 1f)
            val region = ActorRegion(
                left = x(bounds.minOf { it.minXRaw }),
                top = y(bounds.minOf { it.minYRaw }),
                right = x(bounds.maxOf { it.maxXRaw }),
                bottom = y(bounds.maxOf { it.maxYRaw }),
            )
            return region.takeIf { it.right > it.left && it.bottom > it.top }
        }
    }
}
