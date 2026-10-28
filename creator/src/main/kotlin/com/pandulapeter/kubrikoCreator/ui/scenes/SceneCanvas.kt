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

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isCtrlPressed
import androidx.compose.ui.input.pointer.isMetaPressed
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubriko.KubrikoViewport
import com.pandulapeter.kubriko.helpers.extensions.sceneUnit
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubrikoCreator.scenes.SceneView

/**
 * The Scene as the game draws it, with the selected Actor outlined. A click selects the Actor on top, dragging it moves
 * it ([onMove] when it is let go), dragging elsewhere (or with the middle button) moves the camera, scrolling moves it
 * too, and with Ctrl or Cmd, zooms. Delete or Backspace removes the selected Actor, and an Actor type dragged in from the
 * sidebar is placed where it is dropped ([onPlace], in scene units).
 *
 * @param content The Scene file's content, which the Actors are created from again whenever it changes.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun SceneCanvas(
    view: SceneView,
    content: String,
    selectedIndex: Int?,
    onSelect: (Int?) -> Unit,
    onMove: (index: Int, x: Float, y: Float) -> Unit,
    onRemove: (Int) -> Unit,
    dragState: ActorTypeDragState,
    onPlace: (actorTypeName: String, x: Float, y: Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentOnPlace by rememberUpdatedState(onPlace)
    DisposableEffect(dragState) {
        onDispose { dragState.dropTarget = null }
    }
    LaunchedEffect(view, content) { view.load(content) }
    val loadRevision by view.loadRevision.collectAsState()
    val cameraPosition by view.viewportManager.cameraPosition.collectAsState()
    val scaleFactor by view.viewportManager.scaleFactor.collectAsState()
    val viewportSize by view.viewportManager.size.collectAsState()
    var dragRevision by remember { mutableIntStateOf(0) }
    val focusRequester = remember { FocusRequester() }
    val currentSelectedIndex by rememberUpdatedState(selectedIndex)
    val currentOnSelect by rememberUpdatedState(onSelect)
    val currentOnMove by rememberUpdatedState(onMove)
    val selectionColor = MaterialTheme.colorScheme.primary
    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .onGloballyPositioned { coordinates ->
                val bounds = coordinates.boundsInRoot()
                dragState.dropTarget = ActorTypeDragState.DropTarget(bounds) { actorTypeName, at ->
                    val position = view.toScene(at - bounds.topLeft)
                    currentOnPlace(actorTypeName, position.x.raw, position.y.raw)
                }
            }
            .then(if (dragState.isOverDropTarget) Modifier.border(2.dp, MaterialTheme.colorScheme.primary) else Modifier)
            .clipToBounds()
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { event ->
                val index = currentSelectedIndex
                if (event.type == KeyEventType.KeyDown && (event.key == Key.Delete || event.key == Key.Backspace) && index != null) {
                    onRemove(index)
                    true
                } else {
                    false
                }
            }
            .onPointerEvent(PointerEventType.Scroll) { event ->
                val delta = event.changes.first().scrollDelta
                if (event.keyboardModifiers.isCtrlPressed || event.keyboardModifiers.isMetaPressed) {
                    view.viewportManager.multiplyScaleFactor(1f - delta.y * ZOOM_PER_SCROLL)
                } else {
                    view.viewportManager.addToCameraPosition(delta * PAN_PER_SCROLL)
                }
            }
            .pointerInput(view) {
                awaitEachGesture {
                    val press = awaitPointerEvent(PointerEventPass.Initial)
                    val down = press.changes.firstOrNull { it.pressed && !it.previousPressed } ?: return@awaitEachGesture
                    focusRequester.requestFocus()
                    val isPrimary = press.buttons.isPrimaryPressed
                    val pickedIndex = if (isPrimary) view.actorIndexAt(down.position) else null
                    if (isPrimary) currentOnSelect(pickedIndex)
                    val actor = pickedIndex?.takeIf { !view.isBackground(it) }?.let { view.actors.getOrNull(it) }
                    val startPosition = actor?.body?.position ?: SceneOffset.Zero
                    var lastPosition = down.position
                    var isDragging = false
                    while (true) {
                        val change = awaitPointerEvent(PointerEventPass.Initial).changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) break
                        if (!isDragging && (change.position - down.position).getDistance() > viewConfiguration.touchSlop) isDragging = true
                        if (isDragging) {
                            if (actor != null) {
                                actor.body.position = startPosition + (view.toScene(change.position) - view.toScene(down.position))
                                dragRevision++
                            } else {
                                view.viewportManager.addToCameraPosition(lastPosition - change.position)
                            }
                        }
                        lastPosition = change.position
                        change.consume()
                    }
                    if (actor != null && isDragging && pickedIndex != null) {
                        currentOnMove(pickedIndex, actor.body.position.x.raw, actor.body.position.y.raw)
                    }
                }
            },
    ) {
        KubrikoViewport(
            modifier = Modifier.fillMaxSize(),
            kubriko = view.kubriko,
            windowInsets = WindowInsets(0),
        )
        Canvas(
            modifier = Modifier.fillMaxSize(),
        ) {
            // Read so that the outline follows the camera, the drag, and the Actors created from the Scene's content.
            cameraPosition
            scaleFactor
            viewportSize
            dragRevision
            loadRevision
            val bounds = selectedIndex?.let { view.boundsOf(it) } ?: return@Canvas
            val topLeft = view.toScreen(SceneOffset(bounds.left.sceneUnit, bounds.top.sceneUnit))
            val bottomRight = view.toScreen(SceneOffset(bounds.right.sceneUnit, bounds.bottom.sceneUnit))
            drawRect(
                color = selectionColor,
                topLeft = topLeft,
                size = Size(bottomRight.x - topLeft.x, bottomRight.y - topLeft.y),
                style = Stroke(width = SELECTION_STROKE_WIDTH.toPx()),
            )
        }
    }
}

private const val ZOOM_PER_SCROLL = 0.05f
private const val PAN_PER_SCROLL = 20f
private val SELECTION_STROKE_WIDTH = 2.dp
