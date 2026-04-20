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

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.DrawStyle
import com.pandulapeter.kubriko.actor.Actor

/**
 * Represents the logical shape of an [Actor] in the scene.
 * Bodies are used for rendering: they determine the clipping bounds and are also used for checking if the actor is within the visible viewport.
 */
sealed interface Body {

    /**
     * The axis-aligned bounding box that contains the entire body.
     */
    val axisAlignedBoundingBox: AxisAlignedBoundingBox

    /**
     * Draws the debug representation of the body's boundaries.
     */
    fun DrawScope.drawDebugBounds(color: Color, style: DrawStyle)
}