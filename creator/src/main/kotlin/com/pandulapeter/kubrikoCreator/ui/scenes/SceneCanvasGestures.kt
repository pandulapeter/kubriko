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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerKeyboardModifiers
import androidx.compose.ui.input.pointer.isAltPressed
import androidx.compose.ui.input.pointer.isCtrlPressed
import androidx.compose.ui.input.pointer.isMetaPressed
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
 * What the Scene canvas does with the pointer: a press on a handle of the only selected Actor resizes or rotates it
 * (Shift keeping a corner's proportions, or turning in steps of [ROTATION_STEP_IN_DEGREES]); one on an Actor selects it
 * (with Shift, Ctrl, or Cmd, adds it to the selection or takes it out), and dragging moves every selected Actor with it
 * (both snapping to the grid while [Callbacks.isSnapping] is on, or with Alt held while it is off); dragging with Shift
 * from where no Actor is selects those its box touches ([marquee]); and dragging elsewhere (or with another button, or
 * on an Actor that can't be moved) moves the camera. What changes the Scene is reported only when the pointer is let go.
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
        val selectedIds: List<String>,
        val canDrag: (actorId: String) -> Boolean,
        val handleMetrics: HandleMetrics,
        val grid: SceneGrid,
        val isSnapping: Boolean,
        val onSelectionChange: (List<String>) -> Unit,
        val onMove: (List<ActorMove>) -> Unit,
        val onTransform: (actorId: String, handle: FrameHandle, after: ActorFrame) -> Unit,
        val onDragged: () -> Unit,
    )

    /**
     * The box a drag with Shift selects the Actors in, in pixels, while it lasts.
     */
    var marquee by mutableStateOf<Rect?>(null)
        private set

    val modifier: Modifier = Modifier.pointerInput(view) {
        awaitEachGesture {
            val press = awaitPointerEvent(PointerEventPass.Initial)
            val down = press.changes.firstOrNull { it.pressed && !it.previousPressed } ?: return@awaitEachGesture
            focusRequester.requestFocus()
            val current = callbacks()
            val isPrimary = press.buttons.isPrimaryPressed
            val handle = if (isPrimary) transformableHandleAt(current, down.position) else null
            when {
                handle != null -> transform(current, current.selectedIds.single(), handle, down)
                isPrimary -> pick(current, press.keyboardModifiers, down)
                else -> pan(down)
            }
        }
    }

    private suspend fun AwaitPointerEventScope.pick(current: Callbacks, modifiers: PointerKeyboardModifiers, down: PointerInputChange) {
        val pickedId = view.actorIdAt(down.position)?.takeUnless(view::isBackground)
        val isToggling = modifiers.isShiftPressed || modifiers.isCtrlPressed || modifiers.isMetaPressed
        when {
            pickedId == null && modifiers.isShiftPressed -> selectWithin(current, down)
            pickedId == null -> {
                current.onSelectionChange(emptyList())
                pan(down)
            }
            isToggling -> {
                val toggled = if (pickedId in current.selectedIds) current.selectedIds - pickedId else current.selectedIds + pickedId
                current.onSelectionChange(toggled)
                if (pickedId in toggled) move(current, toggled, pickedId, down) else pan(down)
            }
            pickedId in current.selectedIds -> {
                if (!move(current, current.selectedIds, pickedId, down)) current.onSelectionChange(listOf(pickedId))
            }
            else -> {
                current.onSelectionChange(listOf(pickedId))
                move(current, listOf(pickedId), pickedId, down)
            }
        }
    }

    private suspend fun AwaitPointerEventScope.selectWithin(current: Callbacks, down: PointerInputChange) {
        dragging(down.id, down.position) { change, _ -> marquee = Rect(down.position, change.position).normalized() }
        val box = marquee ?: return
        marquee = null
        val topLeft = toScene(box.topLeft)
        val bottomRight = toScene(box.bottomRight)
        current.onSelectionChange((current.selectedIds + view.actorIdsWithin(topLeft.x, topLeft.y, bottomRight.x, bottomRight.y)).distinct())
    }

    private fun Rect.normalized() = Rect(minOf(left, right), minOf(top, bottom), maxOf(left, right), maxOf(top, bottom))

    private fun transformableHandleAt(current: Callbacks, position: Offset): FrameHandle? {
        val selectedId = current.selectedIds.singleOrNull() ?: return null
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
        val hasMoved = dragging(down.id, down.position) { change, modifiers ->
            val pointer = toScene(change.position)
            after = if (handle == FrameHandle.ROTATE) {
                start.rotated(from, pointer, ROTATION_STEP_IN_DEGREES.takeIf { modifiers.isShiftPressed })
            } else {
                start.resized(handle, pointer, keepsProportions = modifiers.isShiftPressed, snap = snapping(current, modifiers))
            }
            after.applyTo(body)
            current.onDragged()
        }
        if (hasMoved && after != start) current.onTransform(actorId, handle, after)
    }

    /**
     * Moves the Actors [actorIds] that can be moved along with [pressedId], which the pointer drags (and which alone snaps
     * to the grid, the others keeping their distance to it); whether the pointer dragged. A press on an Actor that can't
     * be moved pans instead.
     */
    private suspend fun AwaitPointerEventScope.move(current: Callbacks, actorIds: List<String>, pressedId: String, down: PointerInputChange): Boolean {
        val pressed = view.actorOf(pressedId)?.takeIf { current.canDrag(pressedId) } ?: return pan(down)
        val moved = actorIds.filter { !view.isBackground(it) && current.canDrag(it) }.mapNotNull { id -> view.actorOf(id)?.let { id to it } }
        val starts = moved.map { (_, actor) -> actor.body.position }
        val pressedStart = pressed.body.position
        val hasMoved = dragging(down.id, down.position) { change, modifiers ->
            val target = pressedStart + (view.toScene(change.position) - view.toScene(down.position))
            val snapped = snapping(current, modifiers)(Offset(target.x.raw, target.y.raw))
            val delta = SceneOffset(snapped.x.sceneUnit, snapped.y.sceneUnit) - pressedStart
            moved.forEachIndexed { index, (_, actor) -> actor.body.position = starts[index] + delta }
            current.onDragged()
        }
        if (hasMoved) current.onMove(moved.map { (id, actor) -> ActorMove(id, actor.body.position.x.raw, actor.body.position.y.raw) })
        return hasMoved
    }

    /**
     * Moves the selected Actors that can be moved by [dx] and [dy] scene units at once, as the arrow keys do.
     */
    fun nudge(dx: Float, dy: Float) {
        val current = callbacks()
        val moved = current.selectedIds.filter { !view.isBackground(it) && current.canDrag(it) }.mapNotNull { id -> view.actorOf(id)?.let { id to it } }
        if (moved.isEmpty()) return
        val delta = SceneOffset(dx.sceneUnit, dy.sceneUnit)
        moved.forEach { (_, actor) -> actor.body.position += delta }
        current.onDragged()
        current.onMove(moved.map { (id, actor) -> ActorMove(id, actor.body.position.x.raw, actor.body.position.y.raw) })
    }

    /**
     * Rounds to [Callbacks.grid] while snapping is on, which Alt turns the other way while it is held.
     */
    private fun snapping(current: Callbacks, modifiers: PointerKeyboardModifiers): (Float) -> Float =
        if (current.isSnapping != modifiers.isAltPressed) current.grid::snap else { value -> value }

    private operator fun ((Float) -> Float).invoke(point: Offset) = Offset(this(point.x), this(point.y))

    private suspend fun AwaitPointerEventScope.pan(down: PointerInputChange): Boolean {
        var lastPosition = down.position
        dragging(down.id, down.position) { change, _ ->
            view.viewportManager.addToCameraPosition(lastPosition - change.position)
            lastPosition = change.position
        }
        return false
    }

    /**
     * Follows the pointer [pointerId] until it is let go, calling [onDrag] for each move once it went further than the
     * touch slop from [start]; whether it did.
     */
    private suspend fun AwaitPointerEventScope.dragging(pointerId: PointerId, start: Offset, onDrag: (PointerInputChange, PointerKeyboardModifiers) -> Unit): Boolean {
        var isDragging = false
        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            val change = event.changes.firstOrNull { it.id == pointerId } ?: break
            if (!change.pressed) break
            if (!isDragging && (change.position - start).getDistance() > viewConfiguration.touchSlop) isDragging = true
            if (isDragging) onDrag(change, event.keyboardModifiers)
            change.consume()
        }
        return isDragging
    }

    /**
     * Where [screenOffset], in pixels, is in the Scene, in scene units.
     */
    fun toScene(screenOffset: Offset): Offset = view.toScene(screenOffset).let { Offset(it.x.raw, it.y.raw) }

    /**
     * Where [scenePoint], in scene units, is on the screen.
     */
    fun toScreen(scenePoint: Offset): Offset = view.toScreen(SceneOffset(scenePoint.x.sceneUnit, scenePoint.y.sceneUnit))

    private companion object {
        const val ROTATION_STEP_IN_DEGREES = 15f
    }
}
