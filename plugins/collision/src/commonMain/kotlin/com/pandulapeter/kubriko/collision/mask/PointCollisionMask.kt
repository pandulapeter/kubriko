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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.DrawStyle
import com.pandulapeter.kubriko.actor.body.AxisAlignedBoundingBox
import com.pandulapeter.kubriko.types.SceneOffset

/**
 * A collision mask representing a single point in the scene.
 *
 * @param initialPosition The initial position of the point in scene units.
 */
open class PointCollisionMask internal constructor(
    initialPosition: SceneOffset,
) : CollisionMask {
    /**
     * Whether the cached bounding box is out of date. A subclass sets it whenever something that affects the box
     * changes, and [updateAxisAlignedBoundingBox] is then called on the next read of [axisAlignedBoundingBox].
     */
    protected var isAxisAlignedBoundingBoxDirty = true
    private val cachedAxisAlignedBoundingBox = AxisAlignedBoundingBox(
        min = initialPosition,
        max = initialPosition,
    )

    /**
     * A single instance mutated in place as the mask moves - read through this property instead of storing the box.
     */
    override val axisAlignedBoundingBox: AxisAlignedBoundingBox
        get() {
            if (isAxisAlignedBoundingBoxDirty) {
                updateAxisAlignedBoundingBox(cachedAxisAlignedBoundingBox)
                // Without clearing the flag the cache never takes effect and every read recomputes
                // the bounding box (a full vertex transform for polygon masks).
                isAxisAlignedBoundingBoxDirty = false
            }
            return cachedAxisAlignedBoundingBox
        }
    override var position = initialPosition
        set(value) {
            if (field != value) {
                field = value
                isAxisAlignedBoundingBoxDirty = true
            }
        }

    /**
     * Writes the current bounding box of the mask into [target] in place. A subclass with a shape overrides it.
     *
     * @param target The cached bounding box to update.
     */
    protected open fun updateAxisAlignedBoundingBox(target: AxisAlignedBoundingBox) = target.update(
        min = position,
        max = position,
    )

    override fun DrawScope.drawDebugBounds(color: Color, style: DrawStyle) = drawCircle(
        color = color,
        radius = 2f,
        center = size.center,
        style = style,
    )

    companion object {
        /**
         * Creates a [PointCollisionMask].
         *
         * @param initialPosition The position of the point in scene units, the scene origin by default.
         */
        operator fun invoke(
            initialPosition: SceneOffset = SceneOffset.Zero,
        ) = PointCollisionMask(
            initialPosition = initialPosition,
        )
    }
}