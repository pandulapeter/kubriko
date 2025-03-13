/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.collision.mask

import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.pandulapeter.kubriko.actor.body.AxisAlignedBoundingBox
import com.pandulapeter.kubriko.types.SceneOffset

open class PointCollisionMask internal constructor() : CollisionMask {
    private var _axisAlignedBoundingBox: AxisAlignedBoundingBox? = null
    override var axisAlignedBoundingBox: AxisAlignedBoundingBox
        get() {
            if (isAxisAlignedBoundingBoxDirty) {
                _axisAlignedBoundingBox = null
                isAxisAlignedBoundingBoxDirty = false
            }
            return _axisAlignedBoundingBox ?: createAxisAlignedBoundingBox().also { _axisAlignedBoundingBox = it }
        }
        protected set(value) {
            _axisAlignedBoundingBox = value
        }
    protected var isAxisAlignedBoundingBoxDirty = false

    protected open fun createAxisAlignedBoundingBox() = AxisAlignedBoundingBox(
        min = SceneOffset.Zero,
        max = SceneOffset.Zero,
    )

    override fun DrawScope.drawDebugBounds(color: Color, stroke: Stroke) = drawCircle(
        color = color,
        radius = 2f,
        center = size.center,
        style = stroke,
    )

    companion object {
        operator fun invoke(
        ) = PointCollisionMask()
    }
}