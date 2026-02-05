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

import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.DrawStyle
import com.pandulapeter.kubriko.actor.body.AxisAlignedBoundingBox
import com.pandulapeter.kubriko.collision.implementation.RotationMatrix
import com.pandulapeter.kubriko.helpers.extensions.center
import com.pandulapeter.kubriko.helpers.extensions.dot
import com.pandulapeter.kubriko.helpers.extensions.normal
import com.pandulapeter.kubriko.helpers.extensions.normalized
import com.pandulapeter.kubriko.types.AngleRadians
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneSize
import com.pandulapeter.kubriko.types.SceneUnit


open class PolygonCollisionMask internal constructor(
    unprocessedVertices: List<SceneOffset> = emptyList(),
    initialPosition: SceneOffset = SceneOffset.Zero,
    initialRotation: AngleRadians = AngleRadians.Zero
) : PointCollisionMask(
    initialPosition = initialPosition,
), ComplexCollisionMask {
    val vertices = generateConvexHull(unprocessedVertices)
    var rotationMatrix = RotationMatrix(initialRotation)
        private set
    override val size = when {
        vertices.size < 2 -> SceneSize.Zero
        else -> {
            val minX = vertices.minOf { it.x }
            val maxX = vertices.maxOf { it.x }
            val minY = vertices.minOf { it.y }
            val maxY = vertices.maxOf { it.y }
            SceneSize(
                width = maxX - minX,
                height = maxY - minY
            )
        }
    }
    var rotation = initialRotation
        set(value) {
            if (field != value) {
                field = value
                rotationMatrix.set(rotation)
                isAxisAlignedBoundingBoxDirty = true
            }
        }
    internal val normals = (0..vertices.lastIndex).map { i ->
        val face = vertices[if (i + 1 == vertices.size) 0 else i + 1].minus(vertices[i])
        face.normal().normalized().unaryMinus()
    }

    private fun cross(o: SceneOffset, a: SceneOffset, b: SceneOffset) = (a.x - o.x).raw * (b.y - o.y).raw - (a.y - o.y).raw * (b.x - o.x).raw

    private fun generateConvexHull(points: List<SceneOffset>): List<SceneOffset> {
        if (points.size <= 1) return points

        val sorted = points.sortedWith(compareBy<SceneOffset> { it.x }.thenBy { it.y })

        // Build lower hull
        val lower = mutableListOf<SceneOffset>()
        for (p in sorted) {
            while (
                lower.size >= 2 &&
                cross(lower[lower.size - 2], lower.last(), p) <= 0f
            ) {
                lower.removeAt(lower.lastIndex)
            }
            lower.add(p)
        }

        // Build upper hull
        val upper = mutableListOf<SceneOffset>()
        for (p in sorted.asReversed()) {
            while (
                upper.size >= 2 &&
                cross(upper[upper.size - 2], upper.last(), p) <= 0f
            ) {
                upper.removeAt(upper.lastIndex)
            }
            upper.add(p)
        }

        // Remove duplicate endpoints
        lower.removeAt(lower.lastIndex)
        upper.removeAt(upper.lastIndex)

        val hull = lower + upper

        // Center the hull around its centroid
        val center = hull.center
        return hull.map { it - center }
    }

    override fun isSceneOffsetInside(sceneOffset: SceneOffset): Boolean {
        for (i in vertices.indices) {
            val objectPoint = sceneOffset - (position + rotationMatrix.times(vertices[i]))
            if (objectPoint.dot(rotationMatrix.times(normals[i])) > SceneUnit.Zero) {
                return false
            }
        }
        return true
    }

    override fun updateAxisAlignedBoundingBox(): AxisAlignedBoundingBox {
        val firstPoint = rotationMatrix.times(vertices[0])
        var minX = firstPoint.x
        var maxX = firstPoint.x
        var minY = firstPoint.y
        var maxY = firstPoint.y
        for (i in 1 until vertices.size) {
            val point = rotationMatrix.times(vertices[i])
            val px = point.x
            val py = point.y
            if (px < minX) {
                minX = px
            } else if (px > maxX) {
                maxX = px
            }
            if (py < minY) {
                minY = py
            } else if (py > maxY) {
                maxY = py
            }
        }
        return AxisAlignedBoundingBox(
            min = SceneOffset(minX, minY) + position,
            max = SceneOffset(maxX, maxY) + position,
        )
    }


    override fun DrawScope.drawDebugBounds(color: Color, style: DrawStyle) = this@PolygonCollisionMask.size.raw.let { size ->
        val path = Path().apply {
            moveTo(vertices[0].x.raw + size.center.x, vertices[0].y.raw + size.center.y)
            for (i in 1 until vertices.size) {
                lineTo(vertices[i].x.raw + size.center.x, vertices[i].y.raw + size.center.y)
            }
            close()
        }
        drawPath(path = path, color = color, style = style)
    }

    companion object {
        operator fun invoke(
            vertices: List<SceneOffset> = emptyList(),
            initialPosition: SceneOffset = SceneOffset.Zero,
            initialRotation: AngleRadians = AngleRadians.Zero
        ) = PolygonCollisionMask(
            unprocessedVertices = vertices,
            initialPosition = initialPosition,
            initialRotation = initialRotation,
        )
    }
}