/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.sceneRuntime

import androidx.compose.ui.geometry.Offset
import com.pandulapeter.kubriko.actor.body.BoxBody
import com.pandulapeter.kubriko.helpers.extensions.rad
import com.pandulapeter.kubriko.helpers.extensions.sceneUnit
import com.pandulapeter.kubriko.types.Scale
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneSize
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Where a box body is in the Scene, as Kubriko places it: its [pivot] (relative to its top left corner) is at
 * [position], and the box is scaled and then rotated around it. Every point is in scene units; a point "in the box" is
 * relative to the box's top left corner, before scaling and rotation.
 *
 * @param rotation In radians, clockwise.
 */
public data class ActorFrame(
    val position: Offset,
    val size: Offset,
    val pivot: Offset,
    val scaleHorizontal: Float = 1f,
    val scaleVertical: Float = 1f,
    val rotation: Float = 0f,
) {
    /**
     * Whether points of the Scene can be turned into points in the box, which a box scaled to nothing can't.
     */
    public val isInvertible: Boolean get() = scaleHorizontal != 0f && scaleVertical != 0f && size.x > 0f && size.y > 0f

    /**
     * Where the point [inBox] of the box is in the Scene.
     */
    public fun toScene(inBox: Offset): Offset {
        val scaledX = (inBox.x - pivot.x) * scaleHorizontal
        val scaledY = (inBox.y - pivot.y) * scaleVertical
        val cos = cos(rotation)
        val sin = sin(rotation)
        return Offset(position.x + scaledX * cos - scaledY * sin, position.y + scaledX * sin + scaledY * cos)
    }

    /**
     * Where the point [inScene] is in the box: the inverse of [toScene], for a box that [isInvertible].
     */
    public fun toBox(inScene: Offset): Offset {
        val x = inScene.x - position.x
        val y = inScene.y - position.y
        val cos = cos(rotation)
        val sin = sin(rotation)
        return Offset((x * cos + y * sin) / scaleHorizontal + pivot.x, (-x * sin + y * cos) / scaleVertical + pivot.y)
    }

    /**
     * Where [handle] is in the Scene (for [FrameHandle.ROTATE], the middle of the edge it stands out from).
     */
    public fun pointOf(handle: FrameHandle): Offset = toScene(Offset(size.x * handle.fractionX, size.y * handle.fractionY))

    /**
     * The box resized by dragging [handle] to [pointer] (in the Scene), with the opposite edge or corner staying where it
     * is, every side at least [minimumSize] long, and the pivot where it was in proportion to the size. [keepsProportions]
     * keeps a corner's width and height in proportion. [snap] rounds the new width and height (to a grid, say).
     */
    public fun resized(
        handle: FrameHandle,
        pointer: Offset,
        keepsProportions: Boolean = false,
        minimumSize: Float = MINIMUM_SIZE,
        snap: (Float) -> Float = { it },
    ): ActorFrame {
        val inBox = toBox(pointer)
        var width = edgeLength(handle.fractionX, inBox.x, size.x)
        var height = edgeLength(handle.fractionY, inBox.y, size.y)
        if (keepsProportions && handle.isCorner) {
            val factor = max(width / size.x, height / size.y)
            width = size.x * factor
            height = size.y * factor
        }
        width = max(minimumSize, if (handle.fractionX == 0.5f) width else snap(width))
        height = max(minimumSize, if (handle.fractionY == 0.5f) height else snap(height))
        val left = if (handle.fractionX == 0f) size.x - width else 0f
        val top = if (handle.fractionY == 0f) size.y - height else 0f
        val pivotFraction = Offset(pivot.x / size.x, pivot.y / size.y)
        val nextPivot = Offset(width * pivotFraction.x, height * pivotFraction.y)
        return copy(position = toScene(Offset(left + nextPivot.x, top + nextPivot.y)), size = Offset(width, height), pivot = nextPivot)
    }

    /**
     * The box turned around its pivot by as much as a pointer moved from [from] to [to] (in the Scene) goes around it,
     * rounded to multiples of [stepInDegrees] when it is given.
     */
    public fun rotated(from: Offset, to: Offset, stepInDegrees: Float? = null): ActorFrame {
        val turn = angleAround(to) - angleAround(from)
        val next = rotation + turn
        return copy(rotation = stepInDegrees?.let { step -> radiansOf((degreesOf(next) / step).roundToInt() * step) } ?: next)
    }

    /**
     * Gives [body] this frame's position, size, pivot, scale, and rotation.
     */
    public fun applyTo(body: BoxBody) {
        body.size = SceneSize(size.x.sceneUnit, size.y.sceneUnit)
        body.pivot = SceneOffset(pivot.x.sceneUnit, pivot.y.sceneUnit)
        body.scale = Scale(scaleHorizontal, scaleVertical)
        body.rotation = rotation.rad
        body.position = SceneOffset(position.x.sceneUnit, position.y.sceneUnit)
    }

    private fun angleAround(point: Offset) = atan2(point.y - position.y, point.x - position.x)

    /**
     * How long an edge of the box becomes when the handle at [fraction] of it goes to [pointer] (both along it), the far
     * end staying; [length] for a handle in the middle, which doesn't move along it.
     */
    private fun edgeLength(fraction: Float, pointer: Float, length: Float) = when (fraction) {
        0f -> length - pointer
        1f -> pointer
        else -> length
    }

    public companion object {
        /**
         * The shortest an edge gets by dragging a handle, in scene units.
         */
        public const val MINIMUM_SIZE: Float = 1f

        /**
         * Where [body] is now.
         */
        public fun of(body: BoxBody): ActorFrame = ActorFrame(
            position = Offset(body.position.x.raw, body.position.y.raw),
            size = Offset(body.size.width.raw, body.size.height.raw),
            pivot = Offset(body.pivot.x.raw, body.pivot.y.raw),
            scaleHorizontal = body.scale.horizontal,
            scaleVertical = body.scale.vertical,
            rotation = body.rotation.normalized,
        )

        /**
         * [radians] in degrees, between 0 and 360, rounded as the inspector shows them.
         */
        public fun degreesOf(radians: Float): Float {
            val degrees = ActorBodyValues.degreesOf(radians) % FULL_TURN
            return if (degrees < 0f) degrees + FULL_TURN else if (abs(degrees - FULL_TURN) < 0.0005f) 0f else degrees
        }

        private fun radiansOf(degrees: Float) = (degrees * Math.PI / 180).toFloat()

        private const val FULL_TURN = 360f
    }
}

/**
 * A handle of the selected Actor's outline in the Scene editor: one at each corner and in the middle of each edge,
 * resizing it, and [ROTATE], standing out above the middle of its top edge, rotating it.
 *
 * @param fractionX Where the handle is along the box's width, from its left edge.
 * @param fractionY Where the handle is along the box's height, from its top edge.
 */
public enum class FrameHandle(public val fractionX: Float, public val fractionY: Float) {
    TOP_LEFT(0f, 0f),
    TOP(0.5f, 0f),
    TOP_RIGHT(1f, 0f),
    RIGHT(1f, 0.5f),
    BOTTOM_RIGHT(1f, 1f),
    BOTTOM(0.5f, 1f),
    BOTTOM_LEFT(0f, 1f),
    LEFT(0f, 0.5f),
    ROTATE(0.5f, 0f),
    ;

    internal val isCorner get() = fractionX != 0.5f && fractionY != 0.5f
}
