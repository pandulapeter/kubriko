/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.sprites

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isCtrlPressed
import androidx.compose.ui.input.pointer.isMetaPressed
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.isTertiaryPressed
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.pandulapeter.kubrikoCreator.sprites.pixels.Pixel
import com.pandulapeter.kubrikoCreator.sprites.pixels.PixelImage
import com.pandulapeter.kubrikoCreator.window.LocalTrackpadPinches

/**
 * The frame being edited, zoomed with its pixels kept sharp over a checkerboard, a grid between its pixels from
 * [SpriteEditorState.GRID_ZOOM], and what is being drawn, selected, or moved over it. A press draws with the left
 * button's color and the right button's, and only a press of either starts one; the middle button, or scrolling, pans;
 * scrolling with Ctrl or Cmd zooms, as does pinching a trackpad ([LocalTrackpadPinches]; on Windows a pinch arrives as
 * such a scroll), a step at a time once their small movements add up to one ([SpriteZoomGestures]).
 *
 * @param onDraw Draws the given pixels on the frame, as one step.
 * @param onionSkin The frames around it, drawn under it ([com.pandulapeter.kubrikoCreator.sprites.pixels.OnionSkin]), if shown.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun SpriteCanvas(
    frame: PixelImage,
    state: SpriteEditorState,
    onDraw: (List<Pixel>) -> Unit,
    modifier: Modifier = Modifier,
    onionSkin: PixelImage? = null,
) {
    val bitmap = remember(frame) { frame.toImageBitmap() }
    val onionSkinBitmap = remember(onionSkin) { onionSkin?.toImageBitmap() }
    val overlay = remember(frame.width, frame.height, state.strokeShape) { overlayOf(frame.width, frame.height, state.strokeShape)?.toImageBitmap() }
    val floatingImage = state.floating?.image
    val floatingBitmap = remember(floatingImage) { floatingImage?.toImageBitmap() }
    val currentFrame by rememberUpdatedState(frame)
    val currentOnDraw by rememberUpdatedState(onDraw)
    val selectionColor = MaterialTheme.colorScheme.primary
    val canvasSize = remember { arrayOf(Size.Zero) }
    val zoom = remember(state) { SpriteZoomGestures(state) }
    val pinches = LocalTrackpadPinches.current
    LaunchedEffect(pinches, zoom) { pinches.collect(zoom::onPinch) }
    fun geometry() = SpriteCanvasGeometry(canvasSize[0], currentFrame.width, currentFrame.height, state.zoom, state.pan)
    fun draw(pixels: List<Pixel>) = currentOnDraw(pixels)
    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .clipToBounds()
            .onPointerEvent(PointerEventType.Move) { event -> state.cursor = geometry().pixelAt(event.changes.first().position).takeIf { currentFrame.contains(it.x, it.y) } }
            .onPointerEvent(PointerEventType.Enter) { zoom.isPointerOver = true }
            .onPointerEvent(PointerEventType.Exit) {
                state.cursor = null
                zoom.isPointerOver = false
            }
            .onPointerEvent(PointerEventType.Scroll) { event ->
                val delta = event.changes.first().scrollDelta
                if (event.keyboardModifiers.isCtrlPressed || event.keyboardModifiers.isMetaPressed) {
                    zoom.onZoomScroll(delta.y)
                } else {
                    state.pan -= delta * PAN_PER_SCROLL
                }
            }
            .pointerInput(state) {
                awaitEachGesture {
                    val press = awaitPointerEvent()
                    // Only a button pressed starts something: never a scroll, nor a touch a mouse's surface takes for one.
                    if (press.type != PointerEventType.Press) return@awaitEachGesture
                    if (!press.buttons.isPrimaryPressed && !press.buttons.isSecondaryPressed && !press.buttons.isTertiaryPressed) return@awaitEachGesture
                    val down = press.changes.firstOrNull { it.pressed && !it.previousPressed } ?: return@awaitEachGesture
                    val isPanning = press.buttons.isTertiaryPressed
                    val isSecondary = press.buttons.isSecondaryPressed
                    val gesture = if (isPanning) null else SpriteGesture.start(state, currentFrame, geometry().pixelAt(down.position), isSecondary, ::draw)
                    var last = down.position
                    while (true) {
                        val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) break
                        if (isPanning) state.pan += change.position - last else gesture?.moveTo(geometry().pixelAt(change.position))
                        last = change.position
                        change.consume()
                    }
                    gesture?.finish(::draw)
                }
            },
    ) {
        Canvas(Modifier.matchParentSize()) {
            canvasSize[0] = size
            val geometry = SpriteCanvasGeometry(size, frame.width, frame.height, state.zoom, state.pan)
            val imageBounds = Rect(geometry.topLeft, geometry.imageSize)
            drawCheckerboard(imageBounds)
            onionSkinBitmap?.let {
                drawImage(
                    image = it,
                    dstOffset = IntOffset(geometry.topLeft.x.toInt(), geometry.topLeft.y.toInt()),
                    dstSize = IntSize(geometry.imageSize.width.toInt(), geometry.imageSize.height.toInt()),
                    filterQuality = FilterQuality.None,
                )
            }
            drawImage(
                image = bitmap,
                dstOffset = IntOffset(geometry.topLeft.x.toInt(), geometry.topLeft.y.toInt()),
                dstSize = IntSize(geometry.imageSize.width.toInt(), geometry.imageSize.height.toInt()),
                filterQuality = FilterQuality.None,
            )
            clipRect(imageBounds.left, imageBounds.top, imageBounds.right, imageBounds.bottom) {
                drawTrail(geometry, state.strokeTrail, state.zoom)
                overlay?.let {
                    drawImage(
                        image = it,
                        dstOffset = IntOffset(geometry.topLeft.x.toInt(), geometry.topLeft.y.toInt()),
                        dstSize = IntSize(geometry.imageSize.width.toInt(), geometry.imageSize.height.toInt()),
                        filterQuality = FilterQuality.None,
                    )
                }
                val floating = state.floating
                if (floating != null && floatingBitmap != null) {
                    floating.liftedFrom?.let { area -> geometry.boundsOf(area).let { (topLeft, areaSize) -> drawCheckerboard(Rect(topLeft, areaSize)) } }
                    val (topLeft, floatingSize) = geometry.boundsOf(floating.bounds)
                    drawCheckerboard(Rect(topLeft, floatingSize))
                    drawImage(
                        image = floatingBitmap,
                        dstOffset = IntOffset(topLeft.x.toInt(), topLeft.y.toInt()),
                        dstSize = IntSize(floatingSize.width.toInt(), floatingSize.height.toInt()),
                        filterQuality = FilterQuality.None,
                    )
                }
            }
            if (state.zoom >= SpriteEditorState.GRID_ZOOM) drawGrid(geometry, frame.width, frame.height, state.zoom, GRID_COLOR)
            (state.floating?.bounds ?: state.selection)?.let { area ->
                val (topLeft, areaSize) = geometry.boundsOf(area)
                drawRect(selectionColor, topLeft, areaSize, style = Stroke(width = 1.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f))))
            }
            state.cursor?.let { cursor -> drawRect(selectionColor.copy(alpha = 0.6f), geometry.positionOf(cursor.x, cursor.y), Size(state.zoom.toFloat(), state.zoom.toFloat()), style = Stroke(1f)) }
        }
    }
}

/**
 * The pixels a pencil or an eraser stroke covered so far, each a square, the erased ones in the checkerboard's light gray.
 */
private fun DrawScope.drawTrail(geometry: SpriteCanvasGeometry, trail: List<Pixel>, zoom: Int) {
    val pixelSize = Size(zoom.toFloat(), zoom.toFloat())
    for (index in trail.indices) {
        val pixel = trail[index]
        drawRect(if (pixel.color == PixelImage.TRANSPARENT) CHECKERBOARD_LIGHT else Color(pixel.color), geometry.positionOf(pixel.x, pixel.y), pixelSize)
    }
}

private fun DrawScope.drawGrid(geometry: SpriteCanvasGeometry, width: Int, height: Int, zoom: Int, color: Color) {
    for (x in 1 until width) {
        val start = geometry.positionOf(x, 0)
        drawLine(color, start, start + Offset(0f, (height * zoom).toFloat()))
    }
    for (y in 1 until height) {
        val start = geometry.positionOf(0, y)
        drawLine(color, start, start + Offset((width * zoom).toFloat(), 0f))
    }
}

private const val PAN_PER_SCROLL = 24f
private val GRID_COLOR = Color(0x40000000)
