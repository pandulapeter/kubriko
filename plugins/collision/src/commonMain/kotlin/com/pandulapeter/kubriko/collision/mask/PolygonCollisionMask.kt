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
import com.pandulapeter.kubriko.helpers.extensions.sceneUnit
import com.pandulapeter.kubriko.types.AngleRadians
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneSize
import com.pandulapeter.kubriko.types.SceneUnit

/**
 * A collision mask representing a convex polygon.
 *
 * The vertices are automatically processed into a convex hull.
 *
 * @param unprocessedVertices The initial list of vertices to form the polygon.
 * @param initialPosition The center position of the mask in scene units.
 * @param initialRotation The initial rotation of the polygon in radians.
 *
 */
open class PolygonCollisionMask internal constructor(
    unprocessedVertices: List<SceneOffset> = emptyList(),
    initialPosition: SceneOffset = SceneOffset.Zero,
    initialRotation: AngleRadians = AngleRadians.Zero
) : PointCollisionMask(
    initialPosition = initialPosition,
), ComplexCollisionMask {
    private var vertexXs = FloatArray(0)
    private var vertexYs = FloatArray(0)
    private var normalXs = FloatArray(0)
    private var normalYs = FloatArray(0)
    private var shapeSize = SceneSize.Zero
    private var cachedVertices: List<SceneOffset> = emptyList()
    private var cachedVerticesRevision = 0

    /**
     * The number of vertices, which [vertexAt] reads.
     */
    var vertexCount = 0
        private set

    /**
     * Incremented whenever the vertices change, such as when a [BoxCollisionMask] is resized.
     */
    final override var shapeRevision = 0
        private set

    /**
     * The vertices relative to the center of the mask, in the order the faces go around it.
     *
     * The same list is returned until the shape changes, when the next read builds a new one. Code that runs every
     * frame reads them through [vertexCount] and [vertexAt] instead, which never allocate.
     */
    val vertices: List<SceneOffset>
        get() {
            if (cachedVerticesRevision != shapeRevision) {
                cachedVertices = List(vertexCount) { vertexAt(it) }
                cachedVerticesRevision = shapeRevision
            }
            return cachedVertices
        }

    /**
     * The matrix of the current [rotation], updated in place whenever the rotation changes.
     */
    var rotationMatrix = RotationMatrix(initialRotation)
        private set
    internal val transposedRotationMatrix = RotationMatrix()

    init {
        rotationMatrix.transposeInto(transposedRotationMatrix)
        val hull = generateConvexHull(unprocessedVertices)
        storeShape(hull)
        cachedVertices = hull
    }

    override val size: SceneSize get() = shapeSize

    /**
     * The rotation of the polygon in radians.
     */
    var rotation = initialRotation
        set(value) {
            if (field != value) {
                field = value
                rotationMatrix.set(rotation)
                rotationMatrix.transposeInto(transposedRotationMatrix)
                isAxisAlignedBoundingBoxDirty = true
            }
        }

    /**
     * Returns the vertex at [index], relative to the center of the mask, without allocating.
     *
     * @param index From 0 until [vertexCount].
     */
    fun vertexAt(index: Int) = SceneOffset(vertexXs[index].sceneUnit, vertexYs[index].sceneUnit)

    /**
     * The inward normal of the face that starts at the vertex at [index], in object space.
     */
    internal fun normalAt(index: Int) = SceneOffset(normalXs[index].sceneUnit, normalYs[index].sceneUnit)

    /**
     * Replaces the shape with the convex hull of [points], as the constructor builds it.
     */
    private fun replaceShape(points: List<SceneOffset>) {
        storeShape(generateConvexHull(points))
        onShapeChanged()
    }

    /**
     * Replaces the shape with the unit shape [unitXs] and [unitYs] scaled by [scaleX] and [scaleY], the shape every box
     * and ellipse is made of: in place and without allocating when both are positive and finite, and through the convex
     * hull otherwise, which a shape without area needs.
     */
    internal fun replaceWithScaledShape(unitXs: FloatArray, unitYs: FloatArray, scaleX: Float, scaleY: Float) {
        if (scaleX > 0f && scaleY > 0f && scaleX.isFinite() && scaleY.isFinite()) {
            replaceWithScaledVertices(unitXs, unitYs, scaleX, scaleY)
        } else {
            // Adding 0 turns the -0 of a side without length into a 0.
            replaceShape(unitXs.indices.map { i -> SceneOffset((unitXs[i] * scaleX + 0f).sceneUnit, (unitYs[i] * scaleY + 0f).sceneUnit) })
        }
    }

    /**
     * Replaces the shape in place, without allocating, with the vertices [unitXs] and [unitYs] scaled by [scaleX] and
     * [scaleY]: a convex hull centered on the origin, in the order the constructor would put the scaled vertices in,
     * which scaling each axis by a positive number keeps.
     */
    private fun replaceWithScaledVertices(unitXs: FloatArray, unitYs: FloatArray, scaleX: Float, scaleY: Float) {
        prepareVertices(unitXs.size)
        for (i in unitXs.indices) {
            vertexXs[i] = unitXs[i] * scaleX
            vertexYs[i] = unitYs[i] * scaleY
        }
        storeNormalsAndSize()
        onShapeChanged()
    }

    /**
     * Makes room for [count] vertices. The arrays are never smaller than a box needs, so a box resized in place never
     * swaps them, which a reader on another thread would otherwise find out of step with [vertexCount].
     */
    private fun prepareVertices(count: Int) {
        if (vertexXs.size < count) {
            val capacity = maxOf(count, MINIMUM_VERTEX_CAPACITY)
            vertexXs = FloatArray(capacity)
            vertexYs = FloatArray(capacity)
            normalXs = FloatArray(capacity)
            normalYs = FloatArray(capacity)
        }
        vertexCount = count
    }

    private fun onShapeChanged() {
        shapeRevision++
        isAxisAlignedBoundingBoxDirty = true
    }

    private fun storeShape(hull: List<SceneOffset>) {
        prepareVertices(hull.size)
        for (i in hull.indices) {
            vertexXs[i] = hull[i].x.raw
            vertexYs[i] = hull[i].y.raw
        }
        storeNormalsAndSize()
    }

    private fun storeNormalsAndSize() {
        for (i in 0 until vertexCount) {
            val face = vertexAt(if (i + 1 == vertexCount) 0 else i + 1).minus(vertexAt(i))
            val normal = face.normal().normalized().unaryMinus()
            normalXs[i] = normal.x.raw
            normalYs[i] = normal.y.raw
        }
        shapeSize = if (vertexCount < 2) {
            SceneSize.Zero
        } else {
            var minX = vertexXs[0]
            var maxX = vertexXs[0]
            var minY = vertexYs[0]
            var maxY = vertexYs[0]
            for (i in 1 until vertexCount) {
                // compareTo orders NaN as SceneUnit's comparisons do, so a bad vertex sizes the shape as it always has.
                if (vertexXs[i].compareTo(minX) < 0) minX = vertexXs[i]
                if (vertexXs[i].compareTo(maxX) > 0) maxX = vertexXs[i]
                if (vertexYs[i].compareTo(minY) < 0) minY = vertexYs[i]
                if (vertexYs[i].compareTo(maxY) > 0) maxY = vertexYs[i]
            }
            SceneSize(width = (maxX - minX).sceneUnit, height = (maxY - minY).sceneUnit)
        }
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
        // A polygon without area (no vertices, or all of them coincident) has zero normals that would contain everything.
        if (size == SceneSize.Zero) {
            return sceneOffset == position
        }
        for (i in 0 until vertexCount) {
            val objectPoint = sceneOffset - (position + rotationMatrix.times(vertexAt(i)))
            if (objectPoint.dot(rotationMatrix.times(normalAt(i))) > SceneUnit.Zero) {
                return false
            }
        }
        return true
    }

    override fun updateAxisAlignedBoundingBox(target: AxisAlignedBoundingBox) {
        if (vertexCount == 0) {
            super.updateAxisAlignedBoundingBox(target)
            return
        }
        val firstPoint = rotationMatrix.times(vertexAt(0))
        var minX = firstPoint.x
        var maxX = firstPoint.x
        var minY = firstPoint.y
        var maxY = firstPoint.y
        for (i in 1 until vertexCount) {
            val point = rotationMatrix.times(vertexAt(i))
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
        target.update(
            min = SceneOffset(minX, minY) + position,
            max = SceneOffset(maxX, maxY) + position,
        )
    }

    override fun DrawScope.drawDebugBounds(color: Color, style: DrawStyle) = this@PolygonCollisionMask.size.raw.let { size ->
        if (vertexCount == 0) return@let
        val path = Path().apply {
            moveTo(vertexXs[0] + size.center.x, vertexYs[0] + size.center.y)
            for (i in 1 until vertexCount) {
                lineTo(vertexXs[i] + size.center.x, vertexYs[i] + size.center.y)
            }
            close()
        }
        drawPath(path = path, color = color, style = style)
    }

    companion object {
        private const val MINIMUM_VERTEX_CAPACITY = 4

        /**
         * Creates a [PolygonCollisionMask] from the convex hull of [vertices], centered on its centroid.
         *
         * @param vertices The vertices of the polygon, empty by default (the mask then behaves like a point).
         * @param initialPosition The center position of the mask in scene units, the scene origin by default.
         * @param initialRotation The initial rotation of the polygon in radians, zero by default.
         */
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
