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

open class PointBody internal constructor(
    initialPosition: SceneOffset = SceneOffset.Zero,
) {
    private var _axisAlignedBoundingBox: AxisAlignedBoundingBox? = null
    var axisAlignedBoundingBox: AxisAlignedBoundingBox
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
    var position = initialPosition
        set(value) {
            if (field != value) {
                field = value
                isAxisAlignedBoundingBoxDirty = true
            }
        }

    protected open fun createAxisAlignedBoundingBox() = AxisAlignedBoundingBox(
        min = position,
        max = position,
    )

    open fun DrawScope.drawDebugBounds(color: Color, stroke: Stroke) = drawCircle(
        color = color,
        radius = 2f,
        center = size.center,
        style = stroke,
    )

    companion object {
        operator fun invoke(
            initialPosition: SceneOffset = SceneOffset.Zero,
        ) = PointBody(
            initialPosition = initialPosition,
        )
    }
}