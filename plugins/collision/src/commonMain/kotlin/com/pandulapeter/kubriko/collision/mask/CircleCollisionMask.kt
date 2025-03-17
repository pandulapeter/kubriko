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
import com.pandulapeter.kubriko.helpers.extensions.clamp
import com.pandulapeter.kubriko.helpers.extensions.length
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneSize
import com.pandulapeter.kubriko.types.SceneUnit

class CircleCollisionMask(
    initialPosition: SceneOffset = SceneOffset.Zero,
    initialRadius: SceneUnit = SceneUnit.Zero,
) : PointCollisionMask(
    initialPosition = initialPosition,
), ComplexCollisionMask {
    var radius = initialRadius.clamp(min = SceneUnit.Zero)
        set(value) {
            val newValue = value.clamp(min = SceneUnit.Zero)
            if (field != newValue) {
                field = newValue
                isAxisAlignedBoundingBoxDirty = true
            }
        }
    override val size get() = SceneSize(radius * 2, radius * 2)

    override fun isSceneOffsetInside(sceneOffset: SceneOffset) = (position - sceneOffset).length() <= radius

    override fun createAxisAlignedBoundingBox() = AxisAlignedBoundingBox(
        min = SceneOffset.Zero - size.center,
        max = SceneOffset(size.width, size.height) - size.center,
    )

    override fun DrawScope.drawDebugBounds(color: Color, style: DrawStyle) = this@CircleCollisionMask.size.raw.let { size ->
        drawCircle(
            color = color,
            radius = radius.raw,
            center = size.center,
            style = style,
        )
    }
}