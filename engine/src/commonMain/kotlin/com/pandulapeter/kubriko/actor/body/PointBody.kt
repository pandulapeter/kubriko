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

import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.pandulapeter.kubriko.types.SceneOffset

open class PointBody(
    initialPosition: SceneOffset = SceneOffset.Zero,
) : Body {
    override var position = initialPosition
        set(value) {
            if (field != value) {
                field = value
                isAxisAlignedBoundingBoxDirty = true
            }
        }
    protected var isAxisAlignedBoundingBoxDirty = false
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

    protected open fun createAxisAlignedBoundingBox() = AxisAlignedBoundingBox(
        min = position,
        max = position,
    )

    override fun DrawScope.drawDebugBounds(color: Color, stroke: Stroke) = drawCircle(
        color = color,
        radius = 2f,
        center = size.center,
        style = stroke,
    )
}