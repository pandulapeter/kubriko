/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.collision.mask

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.DrawStyle
import com.pandulapeter.kubriko.actor.body.AxisAlignedBoundingBox
import com.pandulapeter.kubriko.types.SceneOffset

/**
 * Represents a shape used for collision detection.
 */
sealed interface CollisionMask {

    /**
     * The smallest axis-aligned box that fully encloses this shape.
     */
    val axisAlignedBoundingBox: AxisAlignedBoundingBox

    /**
     * The center position of this collision mask in scene units.
     */
    var position: SceneOffset

    /**
     * Draws the shape for debugging purposes.
     *
     * @param color The color to use for the debug visualization.
     * @param style The drawing style (e.g., stroke or fill).
     */
    fun DrawScope.drawDebugBounds(color: Color, style: DrawStyle)
}