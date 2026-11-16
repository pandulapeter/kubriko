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

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.DrawStyle
import com.pandulapeter.kubriko.types.AngleRadians
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneSize
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * An oval collision mask: an ellipse as wide and high as its [size], which a [CircleCollisionMask] can't be.
 *
 * It is a [PolygonCollisionMask] of [VERTEX_COUNT] vertices, so it collides with every other mask, is hit by raycasts,
 * and gives a physics body its mass and inertia as a polygon does. The vertices lie just outside the ellipse, so the
 * polygon has the ellipse's area, and its outline is never more than about 1.3% of a radius off. Where width and height
 * are always the same, a [CircleCollisionMask] is exact and cheaper.
 *
 * @param initialPosition The center position of the ellipse in scene units.
 * @param initialSize The width and height of the ellipse in scene units.
 * @param initialRotation The rotation of the ellipse in radians.
 */
class EllipseCollisionMask(
    initialPosition: SceneOffset = SceneOffset.Zero,
    initialSize: SceneSize = SceneSize.Zero,
    initialRotation: AngleRadians = AngleRadians.Zero,
) : PolygonCollisionMask(
    unprocessedVertices = emptyList(),
    initialPosition = initialPosition,
    initialRotation = initialRotation,
) {
    private val requested = RequestedSize(initialSize)

    init {
        applySize()
    }

    /**
     * The width and height of the ellipse in scene units: the oval's, which its polygon reaches about 1.3% beyond at its
     * vertices, as its bounding box shows. Setting it resizes the ellipse around its center in place and increments
     * [shapeRevision] if that changes it; a negative width or height is read as its absolute value. A size with a
     * positive, finite width and height is applied without allocating, so an ellipse can follow a scaling body every
     * frame. Like the rest of the mask, it is changed on the thread the mask is used on.
     */
    override var size: SceneSize
        get() = requested.size
        set(value) {
            if (requested.update(value)) applySize()
        }

    private fun applySize() = replaceWithScaledShape(UNIT_XS, UNIT_YS, requested.width / 2f, requested.height / 2f)

    override fun DrawScope.drawDebugBounds(color: Color, style: DrawStyle) = drawOval(
        color = color,
        topLeft = Offset.Zero,
        size = this@EllipseCollisionMask.size.raw,
        style = style,
    )

    companion object {
        /**
         * How many vertices the polygon standing in for the ellipse has.
         */
        const val VERTEX_COUNT: Int = 16

        /**
         * How far outside a unit circle the vertices lie, so the polygon has the circle's area: a regular polygon of n
         * vertices at radius r has an area of n r² sin(2π / n) / 2.
         */
        private val AREA_RADIUS = sqrt(2.0 * PI / (VERTEX_COUNT * sin(2.0 * PI / VERTEX_COUNT))).toFloat()

        private val UNIT_XS = FloatArray(VERTEX_COUNT)
        private val UNIT_YS = FloatArray(VERTEX_COUNT)

        init {
            // Built from one quarter mirrored, so opposite vertices are exact negatives and the shape is centered exactly,
            // in the order the convex hull of the others keeps.
            val quarter = VERTEX_COUNT / 4
            for (index in 0 until VERTEX_COUNT) {
                val step = index % quarter
                val angle = step * 2.0 * PI / VERTEX_COUNT
                val c = if (step == 0) 1f else cos(angle).toFloat()
                val s = if (step == 0) 0f else sin(angle).toFloat()
                val (x, y) = when (index / quarter) {
                    0 -> c to s
                    1 -> -s to c
                    2 -> -c to -s
                    else -> s to -c
                }
                UNIT_XS[index] = x * AREA_RADIUS
                UNIT_YS[index] = y * AREA_RADIUS
            }
        }
    }
}
