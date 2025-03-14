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
import androidx.compose.ui.graphics.drawscope.DrawStyle
import com.pandulapeter.kubriko.actor.body.AxisAlignedBoundingBox
import com.pandulapeter.kubriko.types.SceneOffset

open class PointCollisionMask internal constructor(
    initialPosition: SceneOffset,
) : CollisionMask {
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
    override var position = initialPosition
        set(value) {
            if (field != value) {
                field = value
                isAxisAlignedBoundingBoxDirty = true
            }
        }

    protected open fun createAxisAlignedBoundingBox() = AxisAlignedBoundingBox(
        min = SceneOffset.Zero,
        max = SceneOffset.Zero,
    )

    override fun DrawScope.drawDebugBounds(color: Color, style: DrawStyle) = drawCircle(
        color = color,
        radius = 2f,
        center = size.center,
        style = style,
    )

    companion object {
        operator fun invoke(
            initialPosition: SceneOffset = SceneOffset.Zero,
        ) = PointCollisionMask(
            initialPosition = initialPosition,
        )
    }
}