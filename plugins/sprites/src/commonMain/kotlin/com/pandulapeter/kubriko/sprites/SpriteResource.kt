/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.sprites

import org.jetbrains.compose.resources.DrawableResource

/**
 * A drawable together with the rotation the image loader bakes into its bitmap. This is the key [SpriteManager]
 * caches the loaded bitmaps by, so the same drawable with two rotations is loaded as two separate sprites.
 *
 * @param drawableResource The drawable to load.
 * @param rotation The rotation applied to the loaded bitmap.
 */
data class SpriteResource(
    val drawableResource: DrawableResource,
    val rotation: Rotation = Rotation.NONE,
) {
    /**
     * A clockwise rotation, in steps of 90 degrees, applied to a sprite's bitmap when it is loaded.
     */
    enum class Rotation {
        /** The bitmap is used as it is. */
        NONE,

        /** The bitmap is rotated by 90 degrees clockwise (its width and height are swapped). */
        DEGREES_90,

        /** The bitmap is rotated by 180 degrees. */
        DEGREES_180,

        /** The bitmap is rotated by 270 degrees clockwise (its width and height are swapped). */
        DEGREES_270,
    }
}
