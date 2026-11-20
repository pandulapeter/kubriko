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
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isCtrlPressed
import androidx.compose.ui.input.pointer.isMetaPressed
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubriko.helpers.extensions.sceneUnit
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubrikoCreator.sceneRuntime.ActorFrame
import com.pandulapeter.kubrikoCreator.sceneRuntime.ActorTypeLook
import com.pandulapeter.kubrikoCreator.sceneRuntime.FrameHandle
import com.pandulapeter.kubrikoCreator.sceneRuntime.SceneView
import com.pandulapeter.kubrikoCreator.scenes.SceneActorEntry
import com.pandulapeter.kubrikoCreator.scenes.SceneDocument
import com.pandulapeter.kubrikoCreator.ui.SidebarDragState
import com.pandulapeter.kubrikoCreator.ui.components.FLASH_MAXIMUM_ALPHA

/**
 * The Scene as the game draws it, with the selected Actors outlined along their boxes. The pointer selects, moves
 * ([onMove] when it is let go, so only Actors whose positions the Scene file saves, [canDragActor]), resizes, and
 * rotates them ([onTransform]), as [SceneCanvasGestures] says, scrolling moves the camera, and with Ctrl or Cmd,
 * zooms. While [isSnapping], the [grid] is drawn, and moving, resizing, placing, and the arrow keys' steps snap to it.
 * The keys remove, duplicate ([onDuplicate]), select, and move the selected Actors ([SceneKeyAction]), and an Actor type
 * dragged in from the sidebar is placed where it is dropped ([onPlace], in scene units).
 *
 * @param actors The Scene's Actors, which [view] loads again whenever they change.
 * @param previews The looks of the Actor types whose Actors [view] draws itself until they are compiled.
 * @param isSceneReadable Whether the Scene file can be changed at all, which one that can't be read can't.
 * @param pictureHost Offered a picture of the Scene as drawn, without the selection's outline, while the canvas shows it.
 * @param flash Actors to light up, each drawn over in the flash's color as it fades.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun SceneCanvas(
    view: SceneView,
    actors: List<SceneActorEntry>,
    previews: Map<String, ActorTypeLook>,
    isSceneReadable: Boolean,
    selectedIds: List<String>,
    onSelectionChange: (List<String>) -> Unit,
    onMove: (List<ActorMove>) -> Unit,
    onTransform: (actorId: String, handle: FrameHandle, after: ActorFrame) -> Unit,
    onRemove: (List<String>) -> Unit,
    onDuplicate: (List<String>) -> Unit,
    dragState: SidebarDragState,
    onPlace: (actorTypeName: String, x: Float, y: Float) -> Unit,
    grid: SceneGrid,
    isSnapping: Boolean,
    modifier: Modifier = Modifier,
    pictureHost: ScenePictureHost? = null,
    flash: SceneFlash? = null,
) {
    val currentOnPlace by rememberUpdatedState(onPlace)
    val placementSnap by rememberUpdatedState<(Offset) -> Offset>(if (isSnapping) grid::snap else { point -> point })
    DisposableEffect(dragState) {
        onDispose { dragState.sceneTarget = null }
    }
    LaunchedEffect(view, actors, previews) { view.load(actors, previews) }
    var dragRevision by remember { mutableIntStateOf(0) }
    val focusRequester = remember { FocusRequester() }
    val currentSelectedIds by rememberUpdatedState(selectedIds)
    val currentOnSelectionChange by rememberUpdatedState(onSelectionChange)
    val currentOnRemove by rememberUpdatedState(onRemove)
    val currentOnDuplicate by rememberUpdatedState(onDuplicate)
    val step by rememberUpdatedState(if (isSnapping) grid.size else 1f)
    val handleMetrics = rememberHandleMetrics()
    val callbacks by rememberUpdatedState(
        SceneCanvasGestures.Callbacks(
            selectedIds = selectedIds,
            canDrag = { actorId -> canDragActor(view.entryOf(actorId), isSceneReadable) },
            handleMetrics = handleMetrics,
            grid = grid,
            isSnapping = isSnapping,
            onSelectionChange = onSelectionChange,
            onMove = onMove,
            onTransform = onTransform,
            onDragged = { dragRevision++ },
        ),
    )
    val gestures = remember(view) { SceneCanvasGestures(view, focusRequester) { callbacks } }
    DisposableEffect(gestures) {
        onDispose { gestures.commitNudge() }
    }
    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .onGloballyPositioned { coordinates ->
                val bounds = coordinates.boundsInRoot()
                dragState.sceneTarget = SidebarDragState.SceneTarget(bounds) { actorTypeName, at ->
                    val position = placementSnap(gestures.toScene(at - bounds.topLeft))
                    currentOnPlace(actorTypeName, position.x, position.y)
                }
            }
            .then(if (dragState.isOverScene) Modifier.border(2.dp, MaterialTheme.colorScheme.primary) else Modifier)
            .clipToBounds()
            .focusRequester(focusRequester)
            .focusable()
            .onFocusChanged { if (!it.isFocused) gestures.commitNudge() }
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyUp) return@onKeyEvent gestures.commitNudge()
                if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                val action = SceneKeyAction.of(event.key, event.isShiftPressed, event.isCtrlPressed || event.isMetaPressed, step) ?: return@onKeyEvent false
                performSceneKeyAction(
                    action = action,
                    selectedIds = currentSelectedIds,
                    drawnIds = view::drawnActorIds,
                    onRemove = currentOnRemove,
                    onDuplicate = currentOnDuplicate,
                    onSelectionChange = currentOnSelectionChange,
                    nudge = gestures::nudge,
                )
            }
            .onPointerEvent(PointerEventType.Scroll) { event ->
                val delta = event.changes.first().scrollDelta
                if (event.keyboardModifiers.isCtrlPressed || event.keyboardModifiers.isMetaPressed) {
                    view.viewportManager.multiplyScaleFactor(1f - delta.y * ZOOM_PER_SCROLL)
                } else {
                    view.viewportManager.addToCameraPosition(delta * PAN_PER_SCROLL)
                }
            }
            .then(gestures.modifier),
    ) {
        SceneViewport(view.kubriko, pictureHost, background = MaterialTheme.colorScheme.surfaceContainerLowest)
        SceneOverlay(gestures = gestures, view = view, selectedIds = selectedIds, flash = flash, dragRevision = dragRevision, handleMetrics = handleMetrics, grid = grid.takeIf { isSnapping })
    }
}

/**
 * What the canvas draws over the Scene: the grid, the Actors [flash] lights up, the selected Actors' outlines (with
 * handles while only one is selected), and the box a drag selects in, following the camera, a drag ([dragRevision]),
 * and the Actors created from the Scene's content.
 */
@Composable
private fun SceneOverlay(
    gestures: SceneCanvasGestures,
    view: SceneView,
    selectedIds: List<String>,
    flash: SceneFlash?,
    dragRevision: Int,
    handleMetrics: HandleMetrics,
    grid: SceneGrid?,
) {
    val loadRevision by view.loadRevision.collectAsState()
    val cameraPosition by view.viewportManager.cameraPosition.collectAsState()
    val scaleFactor by view.viewportManager.scaleFactor.collectAsState()
    val viewportSize by view.viewportManager.size.collectAsState()
    val flashColor = MaterialTheme.colorScheme.tertiary
    val selectionColor = MaterialTheme.colorScheme.primary
    val handleFill = MaterialTheme.colorScheme.surface
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = GRID_ALPHA)
    val minimumGridSpacing = with(LocalDensity.current) { MINIMUM_GRID_SPACING.toPx() }
    val selectionStrokeWidth = with(LocalDensity.current) { SELECTION_STROKE_WIDTH.toPx() }
    val selectionStroke = remember(selectionStrokeWidth) { Stroke(width = selectionStrokeWidth) }
    Canvas(
        modifier = Modifier.fillMaxSize(),
    ) {
        // Read so that what is drawn follows the camera, the drag, and the Actors created from the Scene's content.
        cameraPosition
        scaleFactor
        viewportSize
        dragRevision
        loadRevision
        grid?.let { drawSceneGrid(it, gridColor, minimumGridSpacing, gestures::toScene, gestures::toScreen) }
        val strength = flash?.strength?.value ?: 0f
        if (strength > 0f) {
            val color = flashColor.copy(alpha = FLASH_MAXIMUM_ALPHA * strength)
            val actorIds = flash?.actorIds.orEmpty()
            for (index in actorIds.indices) drawActorOutline(gestures, view, actorIds[index], color)
        }
        for (index in selectedIds.indices) drawActorOutline(gestures, view, selectedIds[index], selectionColor, selectionStroke)
        selectedIds.singleOrNull()?.let(gestures::transformableFrameOf)?.let { frame -> drawFrameHandles(frame, selectionColor, handleFill, handleMetrics, gestures::toScreen) }
        gestures.marquee?.let { box ->
            drawRect(color = selectionColor.copy(alpha = MARQUEE_ALPHA), topLeft = box.topLeft, size = box.size)
            drawRect(color = selectionColor, topLeft = box.topLeft, size = box.size, style = selectionStroke)
        }
    }
}

/**
 * Draws over the Actor called [actorId] where [view] shows it, if it does, in [color], filled or in [style]: along its
 * box, following its rotation, or around where it is for an Actor without one.
 */
private fun DrawScope.drawActorOutline(gestures: SceneCanvasGestures, view: SceneView, actorId: String, color: Color, style: DrawStyle = Fill) {
    view.frameOf(actorId)?.let { frame -> return drawFrameOutline(frame, color, gestures::toScreen, style) }
    val bounds = view.boundsOf(actorId) ?: return
    val topLeft = view.toScreen(SceneOffset(bounds.left.sceneUnit, bounds.top.sceneUnit))
    val bottomRight = view.toScreen(SceneOffset(bounds.right.sceneUnit, bounds.bottom.sceneUnit))
    drawRect(color = color, topLeft = topLeft, size = Size(bottomRight.x - topLeft.x, bottomRight.y - topLeft.y), style = style)
}

/**
 * Whether dragging the Actor of [entry] moves it: only when its state saves a position the move can change, in a Scene
 * file that can be changed, so that what the canvas shows and what the file says never disagree.
 */
internal fun canDragActor(entry: SceneActorEntry?, isSceneReadable: Boolean) = isSceneReadable && entry != null && SceneDocument.positionOf(entry.state) != null

@Composable
private fun rememberHandleMetrics(): HandleMetrics {
    val density = LocalDensity.current
    return remember(density) {
        with(density) { HandleMetrics(size = HANDLE_SIZE.toPx(), reach = HANDLE_REACH.toPx(), rotateDistance = ROTATE_HANDLE_DISTANCE.toPx(), strokeWidth = HANDLE_STROKE_WIDTH.toPx()) }
    }
}

private const val ZOOM_PER_SCROLL = 0.05f
private const val PAN_PER_SCROLL = 20f
private val SELECTION_STROKE_WIDTH = 2.dp
private val HANDLE_SIZE = 8.dp
private val MINIMUM_GRID_SPACING = 8.dp
private const val GRID_ALPHA = 0.2f
private const val MARQUEE_ALPHA = 0.12f
private val HANDLE_REACH = 8.dp
private val ROTATE_HANDLE_DISTANCE = 24.dp
private val HANDLE_STROKE_WIDTH = 1.5.dp
