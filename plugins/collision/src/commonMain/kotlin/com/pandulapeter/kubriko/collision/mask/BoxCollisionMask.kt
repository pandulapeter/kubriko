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

import com.pandulapeter.kubriko.helpers.extensions.sceneUnit
import com.pandulapeter.kubriko.types.AngleRadians
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneSize
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
    private var requestedWidth = abs(initialSize.width.raw)
    private var requestedHeight = abs(initialSize.height.raw)

    /**
     * The width and height of the box in scene units. Setting it resizes the box around its center in place, exactly
     * as a box made with that size would be, and increments [shapeRevision] if that changes it; a negative width or
     * height is read as its absolute value. A size with a positive, finite width and height is applied without
     * allocating, so a box can follow a scaling body every frame. Like the rest of the mask, it is changed on the thread
     * the mask is used on.
     */
    override var size: SceneSize
        get() = super.size
        set(value) {
            val width = abs(value.width.raw)
            val height = abs(value.height.raw)
            // Raw bits, so that setting the same NaN again is no change either.
            if (width.toRawBits() == requestedWidth.toRawBits() && height.toRawBits() == requestedHeight.toRawBits()) return
            requestedWidth = width
            requestedHeight = height
            if (width > 0f && height > 0f && width.isFinite() && height.isFinite()) {
                replaceWithScaledVertices(CORNER_SIGNS_X, CORNER_SIGNS_Y, width / 2f, height / 2f)
            } else {
                replaceShape(cornersOf(value))
            }
        }

    private companion object {

        /**
         * The corners of a box two units wide and high, in the order the convex hull puts a rectangle's in, which scaling
         * either axis keeps: the shape every box is made of, built or resized.
         */
        val CORNER_SIGNS_X = floatArrayOf(-1f, 1f, 1f, -1f)
        val CORNER_SIGNS_Y = floatArrayOf(-1f, -1f, 1f, 1f)

        fun cornersOf(size: SceneSize) = CORNER_SIGNS_X.indices.map { i ->
            // Adding 0 turns the -0 of a side without length into the 0 a box has always had there.
            SceneOffset(size.width * (CORNER_SIGNS_X[i] / 2f) + 0f.sceneUnit, size.height * (CORNER_SIGNS_Y[i] / 2f) + 0f.sceneUnit)
        }
    }
}
