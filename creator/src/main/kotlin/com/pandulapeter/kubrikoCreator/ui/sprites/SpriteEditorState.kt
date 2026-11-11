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

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import com.pandulapeter.kubrikoCreator.sprites.pixels.Pixel
import com.pandulapeter.kubrikoCreator.sprites.pixels.PixelImage

/**
 * What the sprite editor keeps besides the sprite itself: the tool and its options, the two colors (the left mouse
 * button's and the right one's, as in GameMaker), the view, the frame shown, the stroke being drawn, the selection, and
 * what was copied. One lives as long as the workspace, so the colors, the tool, and what was copied go from one sprite to
 * the next ([showSprite]), while the rest starts over.
 */
internal class SpriteEditorState {
    var tool by mutableStateOf(SpriteTool.PENCIL)
    var primaryColor by mutableIntStateOf(0xFF000000.toInt())
    var secondaryColor by mutableIntStateOf(PixelImage.TRANSPARENT)
    var isShapeFilled by mutableStateOf(false)
    var isFillContiguous by mutableStateOf(true)

    /**
     * Whether the whole-image operations change every frame rather than the one shown.
     */
    var isEveryFrame by mutableStateOf(false)

    var zoom by mutableIntStateOf(DEFAULT_ZOOM)
        private set

    /**
     * How far the image is moved from the middle of the canvas, in pixels of the screen.
     */
    var pan by mutableStateOf(Offset.Zero)

    var frameIndex by mutableIntStateOf(0)

    /**
     * The pixel the pointer is over, or null when it is outside the canvas.
     */
    var cursor by mutableStateOf<IntOffset?>(null)

    private val strokePoints = ArrayList<IntOffset>()
    private var strokeFrame: PixelImage? = null
    private var strokeColor = PixelImage.TRANSPARENT
    private val strokeCovered = HashSet<IntOffset>()

    /**
     * The pixels a pencil or eraser stroke being drawn covered so far, each once, growing in place as the pointer moves
     * (only along its newest part), so a long stroke never copies or redraws more than it adds.
     */
    val strokeTrail: SnapshotStateList<Pixel> = mutableStateListOf()

    /**
     * The pixels a shape or a fill being drawn covers: a shape's worked out again as the pointer moves, since it takes
     * another shape, a fill's once, since it depends only on where it started.
     */
    var strokeShape by mutableStateOf<List<Pixel>>(emptyList())
        private set

    var selection by mutableStateOf<IntRect?>(null)
    var floating by mutableStateOf<FloatingPixels?>(null)

    /**
     * What was copied or cut last, which pastes into any frame of any sprite.
     */
    var clipboard by mutableStateOf<PixelImage?>(null)
        private set

    /**
     * The pixels drawn but not shown by the frames read yet: they are drawn over them until the next read, so a stroke
     * doesn't flicker back while its write is under way.
     */
    var pendingPixels by mutableStateOf<List<Pixel>>(emptyList())

    private var shownSprite: String? = null

    /**
     * Whether the sprite just shown is still to be zoomed to fit the canvas, once its size and the canvas's are known.
     */
    var isFitPending: Boolean = false
        private set

    /**
     * Starts over for the sprite at [path] unless it is already shown: its first frame, in the middle, nothing selected.
     */
    fun showSprite(path: String) {
        if (path == shownSprite) return
        shownSprite = path
        isFitPending = true
        frameIndex = 0
        pan = Offset.Zero
        clearStroke()
        selection = null
        floating = null
        pendingPixels = emptyList()
    }

    fun zoomIn() {
        zoom = ZOOM_STEPS.firstOrNull { it > zoom } ?: ZOOM_STEPS.last()
    }

    fun zoomOut() {
        zoom = ZOOM_STEPS.lastOrNull { it < zoom } ?: ZOOM_STEPS.first()
    }

    /**
     * Zooms to the largest step that shows a frame of [width] by [height] pixels whole in [canvasWidth] by
     * [canvasHeight] screen pixels, in the middle.
     */
    fun zoomToFit(width: Int, height: Int, canvasWidth: Float, canvasHeight: Float) {
        zoom = ZOOM_STEPS.lastOrNull { it * width <= canvasWidth * FIT_MARGIN && it * height <= canvasHeight * FIT_MARGIN } ?: ZOOM_STEPS.first()
        pan = Offset.Zero
        isFitPending = false
    }

    /**
     * Starts a stroke of the tool on [frame] at [at], with the right button's color when [isSecondary].
     */
    fun startStroke(frame: PixelImage, at: IntOffset, isSecondary: Boolean) {
        clearStroke()
        strokeFrame = frame
        strokeColor = if (isSecondary) secondaryColor else primaryColor
        strokePoints += at
        updateStroke(listOf(at))
    }

    fun continueStroke(at: IntOffset) {
        if (strokeFrame == null || strokePoints.last() == at) return
        val previous = strokePoints.last()
        strokePoints += at
        updateStroke(listOf(previous, at))
    }

    /**
     * Ends the stroke, returning the pixels it draws, none when there was no stroke.
     */
    fun finishStroke(): List<Pixel> = (if (tool == SpriteTool.PENCIL || tool == SpriteTool.ERASER) strokeTrail.toList() else strokeShape).also { clearStroke() }

    private fun updateStroke(newPart: List<IntOffset>) {
        val frame = strokeFrame ?: return
        when (tool) {
            SpriteTool.PENCIL, SpriteTool.ERASER -> strokePixels(tool, newPart, frame, strokeColor).forEach { pixel ->
                if (strokeCovered.add(IntOffset(pixel.x, pixel.y))) strokeTrail += pixel
            }
            SpriteTool.FILL -> if (strokePoints.size == 1) strokeShape = strokePixels(tool, strokePoints, frame, strokeColor, isContiguous = isFillContiguous)
            SpriteTool.LINE, SpriteTool.RECTANGLE, SpriteTool.ELLIPSE ->
                strokeShape = strokePixels(tool, listOf(strokePoints.first(), strokePoints.last()), frame, strokeColor, isShapeFilled)
            SpriteTool.PICKER, SpriteTool.SELECT -> Unit
        }
    }

    private fun clearStroke() {
        strokePoints.clear()
        strokeCovered.clear()
        strokeFrame = null
        strokeTrail.clear()
        strokeShape = emptyList()
    }

    fun pickColor(frame: PixelImage, at: IntOffset, isSecondary: Boolean) {
        if (!frame.contains(at.x, at.y)) return
        if (isSecondary) secondaryColor = frame[at.x, at.y] else primaryColor = frame[at.x, at.y]
    }

    /**
     * Copies what is selected on [frame], or the floating pixels, so they can be pasted.
     *
     * @return Whether there was anything to copy.
     */
    fun copy(frame: PixelImage): Boolean {
        val copied = floating?.image ?: selection?.let { frame.cropped(it.left, it.top, it.width, it.height) } ?: return false
        clipboard = copied
        return true
    }

    /**
     * Puts what was copied on the frame as floating pixels, at the top left of the selection or of the frame, with the
     * selection tool chosen to move them.
     *
     * @return Whether there was anything to paste.
     */
    fun paste(): Boolean {
        val copied = clipboard ?: return false
        floating = FloatingPixels(copied, selection?.topLeft ?: IntOffset.Zero)
        selection = null
        tool = SpriteTool.SELECT
        return true
    }

    /**
     * Lifts the selected pixels of [frame] off it as floating pixels, to be moved.
     */
    fun liftSelection(frame: PixelImage) {
        val area = selection ?: return
        floating = FloatingPixels(frame.cropped(area.left, area.top, area.width, area.height), area.topLeft, liftedFrom = area)
        selection = null
    }

    /**
     * Puts the floating pixels down on [frame], returning the pixels that draws, none when nothing floats.
     */
    fun putDown(frame: PixelImage): List<Pixel> {
        val placed = floating ?: return emptyList()
        floating = null
        selection = placed.bounds.intersect(IntRect(0, 0, frame.width, frame.height)).takeIf { it.width > 0 && it.height > 0 }
        return placed.pixelsOn(frame)
    }

    /**
     * Gets [frame] ready for what changes the frame shown or the whole image: puts the floating pixels down, returning the
     * pixels that draws, and lets go of the selection, which belongs to the frame as it is now.
     */
    fun settle(frame: PixelImage): List<Pixel> = putDown(frame).also { selection = null }

    /**
     * Drops the floating pixels, returning the pixels that clear where they were lifted from on [frame] (none for pasted
     * ones), which leaves them deleted.
     */
    fun dropFloating(frame: PixelImage): List<Pixel> {
        val area = floating?.liftedFrom
        floating = null
        return area?.let { clearedPixels(frame, it) }.orEmpty()
    }

    /**
     * The pixels clearing the selection of [frame] draws, none when nothing is selected.
     */
    fun clearedSelection(frame: PixelImage): List<Pixel> {
        val area = selection ?: return emptyList()
        return clearedPixels(frame, area)
    }

    private fun clearedPixels(frame: PixelImage, area: IntRect) = (area.top until area.bottom).flatMap { y -> (area.left until area.right).map { x -> Pixel(x, y, PixelImage.TRANSPARENT) } }
        .filter { frame.contains(it.x, it.y) && frame[it.x, it.y] != PixelImage.TRANSPARENT }

    companion object {
        val ZOOM_STEPS = listOf(1, 2, 3, 4, 6, 8, 12, 16, 24, 32)
        private const val DEFAULT_ZOOM = 8
        private const val FIT_MARGIN = 0.9f

        /**
         * The zoom from which the pixel grid shows.
         */
        const val GRID_ZOOM = 4
    }
}
