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

import com.pandulapeter.kubriko.types.AngleRadians
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneSize
import com.pandulapeter.kubriko.types.SceneUnit
import kotlin.math.abs

/**
 * A rectangular collision mask.
 *
 * @param initialPosition The center position of the box in scene units.
 * @param initialSize The width and height of the box in scene units.
 * @param initialRotation The rotation of the box in radians.
 */
class BoxCollisionMask(
    initialPosition: SceneOffset = SceneOffset.Zero,
    initialSize: SceneSize = SceneSize.Zero,
    initialRotation: AngleRadians = AngleRadians.Zero
) : PolygonCollisionMask(
    unprocessedVertices = cornersOf(initialSize),
    initialPosition = initialPosition,
    initialRotation = initialRotation,
) {
    /**
     * The width and height of the box in scene units. Setting it resizes the box around its center in place, exactly
     * as a box made with that size would be, and increments [shapeRevision]; a negative width or height is read as its
     * absolute value. A size with a positive, finite width and height is applied without allocating, so a box can
     * follow a scaling body every frame.
     */
    override var size: SceneSize
        get() = super.size
        set(value) {
            val width = abs(value.width.raw)
            val height = abs(value.height.raw)
            if (width == super.size.width.raw && height == super.size.height.raw && vertexCount == CORNER_COUNT) return
            if (width > 0f && height > 0f && width.isFinite() && height.isFinite()) {
                val halfWidth = width / 2f
                val halfHeight = height / 2f
                prepareVertices(CORNER_COUNT)
                for (i in 0 until CORNER_COUNT) {
                    setVertex(i, CORNER_SIGNS_X[i] * halfWidth, CORNER_SIGNS_Y[i] * halfHeight)
                }
                storeNormalsAndSize()
                onShapeChanged()
            } else {
                replaceShape(cornersOf(value))
            }
        }

    private companion object {
        const val CORNER_COUNT = 4

        /**
         * The corners in the order the convex hull puts a rectangle's in, which scaling either axis keeps.
         */
        val CORNER_SIGNS_X = floatArrayOf(-1f, 1f, 1f, -1f)
        val CORNER_SIGNS_Y = floatArrayOf(-1f, -1f, 1f, 1f)

        fun cornersOf(size: SceneSize) = listOf(
            SceneOffset.Zero - size.center,
            SceneOffset(size.width, SceneUnit.Zero) - size.center,
            SceneOffset(size.width, size.height) - size.center,
            SceneOffset(SceneUnit.Zero, size.height) - size.center,
        )
    }
}
