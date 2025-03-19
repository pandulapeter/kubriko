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
import kotlin.math.atan2


open class PolygonCollisionMask internal constructor(
    unprocessedVertices: List<SceneOffset> = emptyList(),
    initialPosition: SceneOffset = SceneOffset.Zero,
    initialRotation: AngleRadians = AngleRadians.Zero
) : PointCollisionMask(
    initialPosition = initialPosition,
), ComplexCollisionMask {
    val vertices = generateConvexHull(unprocessedVertices)
    internal var rotationMatrix = RotationMatrix(initialRotation)
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

    private fun generateConvexHull(vertices: List<SceneOffset>): List<SceneOffset> {
        if (vertices.size < 3) return vertices // Convex hull is not defined for fewer than 3 points

        // Step 1: Find the leftmost point (lowest x, then lowest y)
        var pivot = vertices[0]
        for (vertex in vertices) {
            if (vertex.x < pivot.x || (vertex.x == pivot.x && vertex.y < pivot.y)) {
                pivot = vertex
            }
        }

        // Step 2: Sort the points based on polar angle relative to the pivot
        val sortedVertices = vertices
            .filter { it != pivot } // Remove the pivot from the list
            .sortedBy { atan2((it.y - pivot.y).raw.toDouble(), (it.x - pivot.x).raw.toDouble()) }

        // Step 3: Add the pivot to the sorted list
        val sortedPoints = listOf(pivot) + sortedVertices

        // Step 4: Construct the convex hull using a stack
        val hull = mutableListOf<SceneOffset>()
        for (point in sortedPoints) {
            while (hull.size >= 2 && crossProduct(hull[hull.size - 2], hull[hull.size - 1], point) <= 0) {
                hull.removeAt(hull.size - 1) // Remove the last point from the hull if it's a clockwise turn
            }
            hull.add(point)
        }

        // Step 5: Return the center-aligned convex hull
        return hull.center.let { centerPoint ->
            hull.map {
                it - centerPoint
            }
        }
    }

    private fun crossProduct(o: SceneOffset, a: SceneOffset, b: SceneOffset): Float {
        return ((a.x - o.x) * (b.y - o.y) - (a.y - o.y) * (b.x - o.x)).raw
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