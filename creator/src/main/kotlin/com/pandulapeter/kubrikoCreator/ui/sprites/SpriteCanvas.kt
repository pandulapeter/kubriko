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
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.isTertiaryPressed
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.pandulapeter.kubrikoCreator.sprites.pixels.Pixel
import com.pandulapeter.kubrikoCreator.sprites.pixels.PixelImage

/**
 * The frame being edited, zoomed with its pixels kept sharp over a checkerboard, a grid between its pixels from
 * [SpriteEditorState.GRID_ZOOM], and what is being drawn, selected, or moved over it. A press draws with the left
 * button's color and the right button's; the middle button, or scrolling, pans, and scrolling with Ctrl or Cmd zooms.
 *
 * @param onDraw Draws the given pixels on the frame, as one step.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun SpriteCanvas(
    frame: PixelImage,
    state: SpriteEditorState,
    onDraw: (List<Pixel>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val bitmap = remember(frame) { frame.toImageBitmap() }
    val currentFrame by rememberUpdatedState(frame)
    val currentOnDraw by rememberUpdatedState(onDraw)
    val selectionColor = MaterialTheme.colorScheme.primary
    val canvasSize = remember { arrayOf(Size.Zero) }
    fun geometry() = SpriteCanvasGeometry(canvasSize[0], currentFrame.width, currentFrame.height, state.zoom, state.pan)
    fun draw(pixels: List<Pixel>) {
        if (pixels.isEmpty()) return
        state.pendingPixels = pixels
        currentOnDraw(pixels)
    }
    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .clipToBounds()
            .onPointerEvent(PointerEventType.Move) { event -> state.cursor = geometry().pixelAt(event.changes.first().position).takeIf { currentFrame.contains(it.x, it.y) } }
            .onPointerEvent(PointerEventType.Exit) { state.cursor = null }
            .onPointerEvent(PointerEventType.Scroll) { event ->
                val delta = event.changes.first().scrollDelta
                if (event.keyboardModifiers.isCtrlPressed || event.keyboardModifiers.isMetaPressed) {
                    if (delta.y < 0) state.zoomIn() else if (delta.y > 0) state.zoomOut()
                } else {
                    state.pan -= delta * PAN_PER_SCROLL
                }
            }
            .pointerInput(state) {
                awaitEachGesture {
                    val press = awaitPointerEvent()
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
            drawImage(
                image = bitmap,
                dstOffset = IntOffset(geometry.topLeft.x.toInt(), geometry.topLeft.y.toInt()),
                dstSize = IntSize(geometry.imageSize.width.toInt(), geometry.imageSize.height.toInt()),
                filterQuality = FilterQuality.None,
            )
            clipRect(imageBounds.left, imageBounds.top, imageBounds.right, imageBounds.bottom) {
                drawPixels(geometry, state.pendingPixels + state.strokePreview(frame), state.zoom)
                state.floating?.let { floating ->
                    floating.liftedFrom?.let { area -> geometry.boundsOf(area).let { (topLeft, areaSize) -> drawCheckerboard(Rect(topLeft, areaSize)) } }
                    drawPixels(geometry, floating.pixelsOn(PixelImage(frame.width, frame.height)).filter { floating.bounds.containsPixel(IntOffset(it.x, it.y)) }, state.zoom)
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

private fun DrawScope.drawPixels(geometry: SpriteCanvasGeometry, pixels: List<Pixel>, zoom: Int) {
    val pixelSize = Size(zoom.toFloat(), zoom.toFloat())
    pixels.forEach { pixel ->
        val topLeft = geometry.positionOf(pixel.x, pixel.y)
        if (pixel.color == PixelImage.TRANSPARENT) drawCheckerboard(Rect(topLeft, pixelSize)) else drawRect(Color(pixel.color), topLeft, pixelSize)
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

/**
 * The gray squares that show through see-through pixels, in [bounds].
 */
private fun DrawScope.drawCheckerboard(bounds: Rect) {
    clipRect(bounds.left, bounds.top, bounds.right, bounds.bottom) {
        drawRect(CHECKERBOARD_LIGHT, bounds.topLeft, bounds.size)
        var y = bounds.top
        var row = 0
        while (y < bounds.bottom) {
            var x = bounds.left + if (row % 2 == 0) 0f else CHECKERBOARD_SQUARE
            while (x < bounds.right) {
                drawRect(CHECKERBOARD_DARK, Offset(x, y), Size(CHECKERBOARD_SQUARE, CHECKERBOARD_SQUARE))
                x += CHECKERBOARD_SQUARE * 2
            }
            y += CHECKERBOARD_SQUARE
            row++
        }
    }
}

private const val PAN_PER_SCROLL = 24f
private val GRID_COLOR = Color(0x40000000)
private const val CHECKERBOARD_SQUARE = 8f
private val CHECKERBOARD_LIGHT = Color(0xFFCCCCCC)
private val CHECKERBOARD_DARK = Color(0xFF999999)
