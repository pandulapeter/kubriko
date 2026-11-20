/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.scenes

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import com.pandulapeter.kubrikoCreator.commands.scenes.SetActorValues
import com.pandulapeter.kubrikoCreator.sceneRuntime.ActorFrame
import com.pandulapeter.kubrikoCreator.sceneRuntime.FrameHandle

/**
 * How big the selected Actor's handles are on the screen, in pixels.
 *
 * @param size The side of a resizing handle's square, and the diameter of the rotating one's circle.
 * @param reach How far from a handle's middle a press still takes it.
 * @param rotateDistance How far the rotating handle stands out from the middle of the top edge.
 */
internal data class HandleMetrics(
    val size: Float,
    val reach: Float,
    val rotateDistance: Float,
    val strokeWidth: Float,
)

/**
 * Where [handle] of [frame] is on the screen, with [toScreen] turning Scene units into pixels: the rotating handle
 * stands out from the middle of the top edge, away from the box's middle, by [HandleMetrics.rotateDistance].
 */
internal fun screenPointOf(frame: ActorFrame, handle: FrameHandle, metrics: HandleMetrics, toScreen: (Offset) -> Offset): Offset {
    val point = toScreen(frame.pointOf(handle))
    if (handle != FrameHandle.ROTATE) return point
    val middle = toScreen(frame.toScene(Offset(frame.size.x / 2, frame.size.y / 2)))
    val outwards = point - middle
    val length = outwards.getDistance()
    val direction = if (length > 0f) outwards / length else Offset(0f, -1f)
    return point + direction * metrics.rotateDistance
}

/**
 * The handle of [frame] a press at [pointer] (in pixels) takes, the nearest within reach, or null.
 */
internal fun handleAt(frame: ActorFrame, pointer: Offset, metrics: HandleMetrics, toScreen: (Offset) -> Offset): FrameHandle? = FrameHandle.entries
    .map { it to (screenPointOf(frame, it, metrics, toScreen) - pointer).getDistance() }
    .filter { (_, distance) -> distance <= metrics.reach }
    .minByOrNull { (_, distance) -> distance }
    ?.first

/**
 * What `set_actor_values` takes to give the Actor [actorId] of [sceneName] the frame [after], which dragging [handle] made:
 * its position and size for a resizing handle, its rotation for the rotating one.
 */
internal fun transformInputOf(sceneName: String, actorId: String, handle: FrameHandle, after: ActorFrame): SetActorValues.Input = if (handle == FrameHandle.ROTATE) {
    SetActorValues.Input(sceneName, actorId, rotation = ActorFrame.degreesOf(after.rotation))
} else {
    SetActorValues.Input(sceneName, actorId, x = after.position.x, y = after.position.y, width = after.size.x, height = after.size.y)
}

/**
 * Draws the outline of [frame], following its rotation, filled or in [style].
 */
internal fun DrawScope.drawFrameOutline(frame: ActorFrame, color: Color, toScreen: (Offset) -> Offset, style: DrawStyle = Fill) {
    val path = Path().apply {
        moveTo(toScreen(frame.pointOf(FrameHandle.TOP_LEFT)))
        lineTo(toScreen(frame.pointOf(FrameHandle.TOP_RIGHT)))
        lineTo(toScreen(frame.pointOf(FrameHandle.BOTTOM_RIGHT)))
        lineTo(toScreen(frame.pointOf(FrameHandle.BOTTOM_LEFT)))
        close()
    }
    drawPath(path = path, color = color, style = style)
}

/**
 * Draws the handles of [frame]: a square at each corner and in the middle of each edge, and a circle on a line standing
 * out from the top edge, each filled with [fill] and outlined in [color].
 */
internal fun DrawScope.drawFrameHandles(frame: ActorFrame, color: Color, fill: Color, metrics: HandleMetrics, toScreen: (Offset) -> Offset) {
    val stroke = Stroke(width = metrics.strokeWidth)
    val half = metrics.size / 2
    val rotate = screenPointOf(frame, FrameHandle.ROTATE, metrics, toScreen)
    drawLine(color = color, start = toScreen(frame.pointOf(FrameHandle.TOP)), end = rotate, strokeWidth = metrics.strokeWidth)
    drawCircle(color = fill, radius = half, center = rotate)
    drawCircle(color = color, radius = half, center = rotate, style = stroke)
    FrameHandle.entries.forEach { handle ->
        if (handle == FrameHandle.ROTATE) return@forEach
        val topLeft = screenPointOf(frame, handle, metrics, toScreen) - Offset(half, half)
        drawRect(color = fill, topLeft = topLeft, size = Size(metrics.size, metrics.size))
        drawRect(color = color, topLeft = topLeft, size = Size(metrics.size, metrics.size), style = stroke)
    }
}

private fun Path.moveTo(point: Offset) = moveTo(point.x, point.y)

private fun Path.lineTo(point: Offset) = lineTo(point.x, point.y)
