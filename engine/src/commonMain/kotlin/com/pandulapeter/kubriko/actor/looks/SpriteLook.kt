/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.actor.looks

import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize

/**
 * A region of a [texture] stretched over the actor's body, multiplied by a [tint], like `drawImage` with a modulating
 * color filter. Its texture is clamped at the region's edges, so it never wraps around, and its edges are smoothed.
 *
 * Sprites sharing a texture (a sprite sheet, or an atlas page) and a [filterQuality] are drawn together in one call.
 * A sprite too small on screen to show anything of its texture (under [STAND_IN_SIZE_IN_PIXELS] in both directions) is
 * drawn in the texture's mean color instead, tinted the same way, which keeps it among the untextured looks. Where a
 * device cannot sample textures per vertex ([com.pandulapeter.kubriko.helpers.TriangleBatchSupport]), each sprite is
 * drawn on its own through `drawImageRect` instead.
 *
 * @param texture The image to draw from, or null to draw nothing (while it loads, for example).
 * @param srcOffset The top-left corner of the region, in the texture's pixels.
 * @param srcSize The size of the region, in the texture's pixels. [IntSize.Zero], the default, stands for the whole
 *   texture.
 * @param tint The color the texture is multiplied by. White, the default, leaves it unchanged.
 * @param filterQuality How the texture is filtered: [FilterQuality.None] for crisp pixel art, [FilterQuality.Low] (the
 *   default, as for `drawImage`) to interpolate, [FilterQuality.Medium] or higher to also average it out when drawn
 *   much smaller than its size.
 */
class SpriteLook(
    texture: ImageBitmap?,
    srcOffset: IntOffset = IntOffset.Zero,
    srcSize: IntSize = IntSize.Zero,
    tint: Color = Color.White,
    filterQuality: FilterQuality = FilterQuality.Low,
) : Look() {

    /** The image to draw from, or null to draw nothing. Changing it rebuilds the geometry on the next frame. */
    var texture = texture
        set(value) {
            if (field !== value) {
                field = value
                markChanged()
            }
        }

    /** The top-left corner of the region, in the texture's pixels. */
    var srcOffset = srcOffset
        set(value) {
            if (field != value) {
                field = value
                markChanged()
            }
        }

    /** The size of the region, in the texture's pixels; [IntSize.Zero] stands for the whole texture. */
    var srcSize = srcSize
        set(value) {
            if (field != value) {
                field = value
                markChanged()
            }
        }

    /** The color the texture is multiplied by. */
    var tint = tint
        set(value) {
            if (field != value) {
                field = value
                tintArgb = value.toArgb()
                tintColorFilter = null
                markChanged()
            }
        }

    /** How the texture is filtered. */
    var filterQuality = filterQuality
        set(value) {
            if (field != value) {
                field = value
                markChanged()
            }
        }

    internal var tintArgb = tint.toArgb()
        private set

    private var tintColorFilter: ColorFilter? = null

    /** The color filter that applies [tint] to a sprite drawn on its own, null for white; built once per tint. */
    internal fun colorFilterForTint(): ColorFilter? {
        if (tintArgb == WHITE) return null
        return tintColorFilter ?: ColorFilter.tint(tint, BlendMode.Modulate).also { tintColorFilter = it }
    }

    internal fun regionWidth(texture: ImageBitmap) = if (srcSize == IntSize.Zero) texture.width else srcSize.width

    internal fun regionHeight(texture: ImageBitmap) = if (srcSize == IntSize.Zero) texture.height else srcSize.height

    companion object {
        /**
         * Below this many screen pixels in both directions a sprite shows nothing of its texture, so it is drawn in the
         * texture's mean color instead.
         */
        const val STAND_IN_SIZE_IN_PIXELS = 4f

        private const val WHITE = -1
    }
}
