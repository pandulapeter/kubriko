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

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.isShiftPressed
import androidx.compose.ui.input.pointer.pointerInput
import com.pandulapeter.kubriko.actor.body.BoxBody
import com.pandulapeter.kubriko.helpers.extensions.sceneUnit
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubrikoCreator.sceneRuntime.ActorFrame
import com.pandulapeter.kubrikoCreator.sceneRuntime.FrameHandle
import com.pandulapeter.kubrikoCreator.sceneRuntime.SceneView

/**
 * What the Scene canvas does with the pointer: a press on a handle of the selected Actor resizes or rotates it (Shift
 * keeping a corner's proportions, or turning in steps of [ROTATION_STEP_IN_DEGREES]), one on an Actor selects it and
 * dragging moves it, and dragging elsewhere (or with another button, or on an Actor that can't be moved) moves the
 * camera. What changes the Scene is reported only when the pointer is let go.
 */
internal class SceneCanvasGestures(
    private val view: SceneView,
    private val focusRequester: FocusRequester,
    private val callbacks: () -> Callbacks,
) {
    /**
     * What the gestures read and report, read again for every gesture so that each sees the latest.
     */
    class Callbacks(
        val selectedId: String?,
        val canDrag: (actorId: String) -> Boolean,
        val handleMetrics: HandleMetrics,
        val onSelect: (String?) -> Unit,
        val onMove: (actorId: String, x: Float, y: Float) -> Unit,
        val onTransform: (actorId: String, handle: FrameHandle, after: ActorFrame) -> Unit,
        val onDragged: () -> Unit,
    )

    val modifier: Modifier = Modifier.pointerInput(view) {
        awaitEachGesture {
            val press = awaitPointerEvent(PointerEventPass.Initial)
            val down = press.changes.firstOrNull { it.pressed && !it.previousPressed } ?: return@awaitEachGesture
            focusRequester.requestFocus()
            val current = callbacks()
            val isPrimary = press.buttons.isPrimaryPressed
            val handle = if (isPrimary) transformableHandleAt(current, down.position) else null
            when {
                handle != null -> transform(current, checkNotNull(current.selectedId), handle, down)
                isPrimary -> {
                    val pickedId = view.actorIdAt(down.position)
                    current.onSelect(pickedId)
                    val movable = pickedId?.takeIf { !view.isBackground(it) && current.canDrag(it) }
                    if (movable == null) pan(down) else move(current, movable, down)
                }
                else -> pan(down)
            }
        }
    }

    private fun transformableHandleAt(current: Callbacks, position: Offset): FrameHandle? {
        val selectedId = current.selectedId ?: return null
        val frame = transformableFrameOf(selectedId, current) ?: return null
        return handleAt(frame, position, current.handleMetrics, ::toScreen)
    }

    /**
     * The frame of the Actor [actorId], if it can be resized and rotated: one that can be dragged, with a box that isn't
     * scaled to nothing.
     */
    fun transformableFrameOf(actorId: String, current: Callbacks = callbacks()): ActorFrame? = actorId
        .takeIf { !view.isBackground(it) && current.canDrag(it) }
        ?.let(view::frameOf)
        ?.takeIf { it.isInvertible }

    private suspend fun AwaitPointerEventScope.transform(current: Callbacks, actorId: String, handle: FrameHandle, down: PointerInputChange) {
        val body = view.actorOf(actorId)?.body as? BoxBody ?: return
        val start = ActorFrame.of(body)
        val from = toScene(down.position)
        var after = start
        val hasMoved = dragging(down.id, down.position) { change, isShiftPressed ->
            val pointer = toScene(change.position)
            after = if (handle == FrameHandle.ROTATE) {
                start.rotated(from, pointer, ROTATION_STEP_IN_DEGREES.takeIf { isShiftPressed })
            } else {
                start.resized(handle, pointer, keepsProportions = isShiftPressed)
            }
            after.applyTo(body)
            current.onDragged()
        }
        if (hasMoved && after != start) current.onTransform(actorId, handle, after)
    }

    private suspend fun AwaitPointerEventScope.move(current: Callbacks, actorId: String, down: PointerInputChange) {
        val actor = view.actorOf(actorId) ?: return pan(down)
        val startPosition = actor.body.position
        val hasMoved = dragging(down.id, down.position) { change, _ ->
            actor.body.position = startPosition + (view.toScene(change.position) - view.toScene(down.position))
            current.onDragged()
        }
        if (hasMoved) current.onMove(actorId, actor.body.position.x.raw, actor.body.position.y.raw)
    }

    private suspend fun AwaitPointerEventScope.pan(down: PointerInputChange) {
        var lastPosition = down.position
        dragging(down.id, down.position) { change, _ ->
            view.viewportManager.addToCameraPosition(lastPosition - change.position)
            lastPosition = change.position
        }
    }

    /**
     * Follows the pointer [pointerId] until it is let go, calling [onDrag] for each move once it went further than the
     * touch slop from [start]; whether it did.
     */
    private suspend fun AwaitPointerEventScope.dragging(pointerId: PointerId, start: Offset, onDrag: (PointerInputChange, isShiftPressed: Boolean) -> Unit): Boolean {
        var isDragging = false
        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            val change = event.changes.firstOrNull { it.id == pointerId } ?: break
            if (!change.pressed) break
            if (!isDragging && (change.position - start).getDistance() > viewConfiguration.touchSlop) isDragging = true
            if (isDragging) onDrag(change, event.keyboardModifiers.isShiftPressed)
            change.consume()
        }
        return isDragging
    }

    private fun toScene(screenOffset: Offset) = view.toScene(screenOffset).let { Offset(it.x.raw, it.y.raw) }

    /**
     * Where [scenePoint], in scene units, is on the screen.
     */
    fun toScreen(scenePoint: Offset): Offset = view.toScreen(SceneOffset(scenePoint.x.sceneUnit, scenePoint.y.sceneUnit))

    private companion object {
        const val ROTATION_STEP_IN_DEGREES = 15f
    }
}
