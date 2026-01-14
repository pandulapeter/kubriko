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

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.graphics.drawscope.Stroke
import com.pandulapeter.kubriko.helpers.extensions.bottomRight
import com.pandulapeter.kubriko.helpers.extensions.clamp
import com.pandulapeter.kubriko.helpers.extensions.cos
import com.pandulapeter.kubriko.helpers.extensions.sin
import com.pandulapeter.kubriko.types.AngleRadians
import com.pandulapeter.kubriko.types.Scale
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneSize

class BoxBody(
    initialPosition: SceneOffset = SceneOffset.Zero,
    initialSize: SceneSize = SceneSize.Zero,
    initialPivot: SceneOffset = initialSize.center,
    initialScale: Scale = Scale.Unit,
    initialRotation: AngleRadians = AngleRadians.Zero
) : PointBody(
    initialPosition = initialPosition,
) {
    var size = initialSize
        set(value) {
            if (field != value) {
                field = value
                pivot = pivot.clamp(min = SceneOffset.Zero, max = value.bottomRight)
                isAxisAlignedBoundingBoxDirty = true
            }
        }
    var pivot = initialPivot.clamp(min = SceneOffset.Zero, max = size.bottomRight)
        set(value) {
            val newValue = value.clamp(min = SceneOffset.Zero, max = size.bottomRight)
            if (field != newValue) {
                field = newValue
                isAxisAlignedBoundingBoxDirty = true
            }
        }
    var scale = initialScale
        set(value) {
            if (field != value) {
                field = value
                isAxisAlignedBoundingBoxDirty = true
            }
        }
    var rotation = initialRotation
        set(value) {
            if (field != value) {
                field = value
                isAxisAlignedBoundingBoxDirty = true
            }
        }

    fun copyAsBoxBody(
        position: SceneOffset = this.position,
        size: SceneSize = this.size,
        pivot: SceneOffset = this.pivot,
        scale: Scale = this.scale,
        rotation: AngleRadians = this.rotation
    ) = BoxBody(
        initialPosition = position,
        initialSize = size,
        initialPivot = pivot,
        initialScale = scale,
        initialRotation = rotation,
    )

    override fun createAxisAlignedBoundingBox() = arrayOf(
        transformPoint(SceneOffset.Zero),
        transformPoint(SceneOffset.Right * size.width),
        transformPoint(SceneOffset.Down * size.height),
        transformPoint(size.bottomRight),
    ).let { corners ->
        AxisAlignedBoundingBox(
            min = SceneOffset(corners.minOf { it.x }, corners.minOf { it.y }) - pivot + position,
            max = SceneOffset(corners.maxOf { it.x }, corners.maxOf { it.y }) - pivot + position,
        )
    }

    private fun transformPoint(point: SceneOffset): SceneOffset {
        val scaled = (point - pivot) * scale
        val rotated = if (rotation == AngleRadians.Zero) scaled else SceneOffset(
            x = scaled.x * rotation.cos - scaled.y * rotation.sin,
            y = scaled.x * rotation.sin + scaled.y * rotation.cos,
        )
        return rotated + pivot
    }

    override fun DrawScope.drawDebugBounds(color: Color, style: DrawStyle) = this@BoxBody.size.raw.let { size ->
        drawRect(
            color = color,
            size = size,
            style = style,
        )
        if (style is Stroke) {
            drawLine(
                color = color,
                start = Offset(pivot.x.raw, 0f),
                end = Offset(pivot.x.raw, size.height),
                strokeWidth = style.width,
            )
            drawLine(
                color = color,
                start = Offset(0f, pivot.y.raw),
                end = Offset(size.width, pivot.y.raw),
                strokeWidth = style.width,
            )
        }
    }
}