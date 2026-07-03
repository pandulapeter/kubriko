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

import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.DrawStyle
import com.pandulapeter.kubriko.types.SceneOffset

/**
 * A basic body type that represents a single point in the scene.
 * This is the simplest form of physical representation for an actor.
 */
open class PointBody internal constructor(
    initialPosition: SceneOffset,
) : Body {
    private val cachedAxisAlignedBoundingBox = AxisAlignedBoundingBox(
        min = initialPosition,
        max = initialPosition,
    )

    /**
     * The axis-aligned bounding box of the point body. A single instance mutated in place - read
     * through this property instead of storing the box.
     */
    override val axisAlignedBoundingBox: AxisAlignedBoundingBox
        get() {
            if (isAxisAlignedBoundingBoxDirty) {
                updateAxisAlignedBoundingBox(cachedAxisAlignedBoundingBox)
                isAxisAlignedBoundingBoxDirty = false
            }
            return cachedAxisAlignedBoundingBox
        }
    protected var isAxisAlignedBoundingBoxDirty = true

    /**
     * The current position of the body in the scene.
     */
    var position = initialPosition
        set(value) {
            if (field != value) {
                field = value
                isAxisAlignedBoundingBoxDirty = true
            }
        }

    /**
     * Creates a copy of this [PointBody] with an optional new position.
     */
    fun copyAsPointBody(
        position: SceneOffset = this.position,
    ) = PointBody(
        initialPosition = position,
    )

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
         * Creates a new [PointBody] instance.
         */
        operator fun invoke(
            initialPosition: SceneOffset = SceneOffset.Zero,
        ) = PointBody(
            initialPosition = initialPosition,
        )
    }
}