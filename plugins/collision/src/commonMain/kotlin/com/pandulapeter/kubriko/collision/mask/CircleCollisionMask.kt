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

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.pandulapeter.kubriko.actor.body.AxisAlignedBoundingBox
import com.pandulapeter.kubriko.helpers.extensions.bottomRight
import com.pandulapeter.kubriko.helpers.extensions.clamp
import com.pandulapeter.kubriko.helpers.extensions.cos
import com.pandulapeter.kubriko.helpers.extensions.sin
import com.pandulapeter.kubriko.types.AngleRadians
import com.pandulapeter.kubriko.types.Scale
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneSize
import com.pandulapeter.kubriko.types.SceneUnit

class CircleCollisionMask(
    initialPosition: SceneOffset = SceneOffset.Zero,
    initialRadius: SceneUnit = SceneUnit.Zero,
    initialPivot: SceneOffset = SceneOffset(initialRadius, initialRadius),
    initialScale: Scale = Scale.Unit,
    initialRotation: AngleRadians = AngleRadians.Zero
) : PointCollisionMask(
    initialPosition = initialPosition,
), ComplexCollisionMask {
    var radius = initialRadius.clamp(min = SceneUnit.Zero)
        set(value) {
            val newValue = value.clamp(min = SceneUnit.Zero)
            if (field != newValue) {
                field = newValue
                pivot = pivot.clamp(min = SceneOffset.Zero, max = size.bottomRight)
                isAxisAlignedBoundingBoxDirty = true
            }
        }
    override val size get() = SceneSize(radius * 2, radius * 2)
    override var pivot = initialPivot.clamp(SceneOffset.Zero, size.bottomRight)
        set(value) {
            val newValue = value.clamp(SceneOffset.Zero, size.bottomRight)
            if (field != newValue) {
                field = newValue
                isAxisAlignedBoundingBoxDirty = true
            }
        }
    override var scale = initialScale
        set(value) {
            if (field != value) {
                field = value
                isAxisAlignedBoundingBoxDirty = true
            }
        }
    override var rotation = initialRotation
        set(value) {
            if (field != value) {
                field = value
                if (pivot != size.center || scale.horizontal != scale.vertical) {
                    isAxisAlignedBoundingBoxDirty = true
                }
            }
        }

    // TODO: Not precise while rotating
    override fun createAxisAlignedBoundingBox() = if (scale.horizontal == scale.vertical && pivot == size.center) {
        AxisAlignedBoundingBox(
            min = SceneOffset.Zero - pivot * scale + position,
            max = SceneOffset(size.width, size.height) * scale - pivot * scale + position,
        )
    } else arrayOf(
        transformPoint(SceneOffset.Zero, rotation),
        transformPoint(SceneOffset.Right * size.width, rotation),
        transformPoint(SceneOffset.Down * size.height, rotation),
        transformPoint(size.bottomRight, rotation),
    ).let { corners ->
        AxisAlignedBoundingBox(
            min = SceneOffset(corners.minOf { it.x }, corners.minOf { it.y }) - pivot + position,
            max = SceneOffset(corners.maxOf { it.x }, corners.maxOf { it.y }) - pivot + position,
        )
    }

    private fun transformPoint(point: SceneOffset, rotation: AngleRadians): SceneOffset {
        val scaled = (point - pivot) * scale
        val rotated = if (rotation == AngleRadians.Zero) scaled else SceneOffset(
            x = scaled.x * rotation.cos - scaled.y * rotation.sin,
            y = scaled.x * rotation.sin + scaled.y * rotation.cos,
        )
        return rotated + pivot
    }

    override fun DrawScope.drawDebugBounds(color: Color, stroke: Stroke) = this@CircleCollisionMask.size.raw.let { size ->
        drawCircle(
            color = color,
            radius = radius.raw,
            center = size.center,
            style = stroke,
        )
        drawLine(
            color = color,
            start = Offset(pivot.x.raw, 0f),
            end = Offset(pivot.x.raw, size.height),
            strokeWidth = stroke.width,
        )
        drawLine(
            color = color,
            start = Offset(0f, pivot.y.raw),
            end = Offset(size.width, pivot.y.raw),
            strokeWidth = stroke.width,
        )
    }
}