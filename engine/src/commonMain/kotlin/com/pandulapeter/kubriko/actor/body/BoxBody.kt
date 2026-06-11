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
import com.pandulapeter.kubriko.helpers.extensions.sceneUnit
import com.pandulapeter.kubriko.helpers.extensions.sin
import com.pandulapeter.kubriko.types.AngleRadians
import com.pandulapeter.kubriko.types.Scale
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneSize

/**
 * A body type that represents a rectangular box in the scene.
 * It supports size, pivot, scaling, and rotation.
 *
 * @param initialPosition The initial position of the body.
 * @param initialSize The initial size of the box.
 * @param initialPivot The point within the box that serves as the center for rotation and scaling.
 * @param initialScale The initial horizontal and vertical scale.
 * @param initialRotation The initial rotation in radians.
 */
class BoxBody(
    initialPosition: SceneOffset = SceneOffset.Zero,
    initialSize: SceneSize = SceneSize.Zero,
    initialPivot: SceneOffset = initialSize.center,
    initialScale: Scale = Scale.Unit,
    initialRotation: AngleRadians = AngleRadians.Zero
) : PointBody(
    initialPosition = initialPosition,
) {
    /**
     * The dimensions of the box.
     */
    var size = initialSize
        set(value) {
            if (field != value) {
                field = value
                pivot = pivot.clamp(min = SceneOffset.Zero, max = value.bottomRight)
                isAxisAlignedBoundingBoxDirty = true
            }
        }

    /**
     * The point around which rotation and scaling are applied, relative to the box's top-left corner.
     */
    var pivot = initialPivot.clamp(min = SceneOffset.Zero, max = size.bottomRight)
        set(value) {
            val newValue = value.clamp(min = SceneOffset.Zero, max = size.bottomRight)
            if (field != newValue) {
                field = newValue
                isAxisAlignedBoundingBoxDirty = true
            }
        }

    /**
     * The scale factor of the box.
     */
    var scale = initialScale
        set(value) {
            if (field != value) {
                field = value
                isAxisAlignedBoundingBoxDirty = true
            }
        }

    /**
     * The rotation of the box in radians.
     */
    var rotation = initialRotation
        set(value) {
            if (field != value) {
                field = value
                isAxisAlignedBoundingBoxDirty = true
            }
        }

    /**
     * Creates a copy of this [BoxBody] with optional new property values.
     */
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

    // Raw float math on the four corners; building an Array<SceneOffset> here would box every
    // value class element, and this runs once per frame for every moving/rotating/scaling body.
    override fun createAxisAlignedBoundingBox(): AxisAlignedBoundingBox {
        val pivotX = pivot.x.raw
        val pivotY = pivot.y.raw
        // Corner coordinates scaled around the pivot: (0,0), (w,0), (0,h), (w,h)
        val scaledLeft = -pivotX * scale.horizontal
        val scaledTop = -pivotY * scale.vertical
        val scaledRight = (size.width.raw - pivotX) * scale.horizontal
        val scaledBottom = (size.height.raw - pivotY) * scale.vertical
        val minX: Float
        val minY: Float
        val maxX: Float
        val maxY: Float
        if (rotation == AngleRadians.Zero) {
            minX = minOf(scaledLeft, scaledRight) + pivotX
            minY = minOf(scaledTop, scaledBottom) + pivotY
            maxX = maxOf(scaledLeft, scaledRight) + pivotX
            maxY = maxOf(scaledTop, scaledBottom) + pivotY
        } else {
            val cos = rotation.cos
            val sin = rotation.sin
            val x0 = scaledLeft * cos - scaledTop * sin + pivotX
            val y0 = scaledLeft * sin + scaledTop * cos + pivotY
            val x1 = scaledRight * cos - scaledTop * sin + pivotX
            val y1 = scaledRight * sin + scaledTop * cos + pivotY
            val x2 = scaledLeft * cos - scaledBottom * sin + pivotX
            val y2 = scaledLeft * sin + scaledBottom * cos + pivotY
            val x3 = scaledRight * cos - scaledBottom * sin + pivotX
            val y3 = scaledRight * sin + scaledBottom * cos + pivotY
            minX = minOf(x0, x1, x2, x3)
            minY = minOf(y0, y1, y2, y3)
            maxX = maxOf(x0, x1, x2, x3)
            maxY = maxOf(y0, y1, y2, y3)
        }
        val positionX = position.x.raw
        val positionY = position.y.raw
        return AxisAlignedBoundingBox(
            min = SceneOffset(
                x = (minX - pivotX + positionX).sceneUnit,
                y = (minY - pivotY + positionY).sceneUnit,
            ),
            max = SceneOffset(
                x = (maxX - pivotX + positionX).sceneUnit,
                y = (maxY - pivotY + positionY).sceneUnit,
            ),
        )
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