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

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import com.pandulapeter.kubrikoCreator.sprites.pixels.Pixel
import com.pandulapeter.kubrikoCreator.sprites.pixels.PixelImage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SpriteEditorStateTest {
    private val red = 0xFFFF0000.toInt()
    private val blue = 0xFF0000FF.toInt()
    private val frame = PixelImage(4, 4).with(listOf(Pixel(0, 0, red), Pixel(1, 0, red)))
    private val drawn = mutableListOf<List<Pixel>>()
    private val state = SpriteEditorState().apply {
        primaryColor = blue
        secondaryColor = red
    }

    private fun press(at: IntOffset, isSecondary: Boolean = false, moves: List<IntOffset> = emptyList()) {
        val gesture = SpriteGesture.start(state, frame, at, isSecondary) { drawn += it }
        moves.forEach(gesture::moveTo)
        gesture.finish { drawn += it }
    }

    @Test
    fun aPencilStrokeIsOneStepOfThePixelsItChangedInTheButtonsColor() {
        press(IntOffset(0, 1), isSecondary = false, listOf(IntOffset(2, 1), IntOffset(2, 1)))
        assertEquals(listOf(listOf(Pixel(0, 1, blue), Pixel(1, 1, blue), Pixel(2, 1, blue))), drawn.filter { it.isNotEmpty() })
        drawn.clear()
        press(IntOffset(0, 0), isSecondary = true)
        assertEquals(emptyList(), drawn.filter { it.isNotEmpty() }, "red over red changes nothing")
    }

    @Test
    fun shapesFillsAndTheEraserDrawWhatTheirToolsCover() {
        state.tool = SpriteTool.RECTANGLE
        state.isShapeFilled = true
        assertEquals(4, state.also { it.startStroke(frame, IntOffset(2, 2), false); it.continueStroke(IntOffset(3, 3)) }.finishStroke().size)
        state.tool = SpriteTool.FILL
        state.startStroke(frame, IntOffset(0, 0), false)
        assertEquals(listOf(Pixel(0, 0, blue), Pixel(1, 0, blue)), state.finishStroke().sortedBy { it.x })
        state.tool = SpriteTool.ERASER
        state.startStroke(frame, IntOffset(1, 0), false)
        assertEquals(listOf(Pixel(1, 0, PixelImage.TRANSPARENT)), state.finishStroke())
    }

    @Test
    fun thePickerTakesAColorForEitherButton() {
        state.tool = SpriteTool.PICKER
        press(IntOffset(0, 0), isSecondary = false)
        assertEquals(red, state.primaryColor)
        press(IntOffset(3, 3), isSecondary = true)
        assertEquals(PixelImage.TRANSPARENT, state.secondaryColor)
    }

    @Test
    fun aSelectionIsDraggedMovedAsOneStepAndCleared() {
        state.tool = SpriteTool.SELECT
        press(IntOffset(0, 0), false, listOf(IntOffset(1, 0)))
        assertEquals(IntRect(0, 0, 2, 1), state.selection)
        press(IntOffset(0, 0), false, listOf(IntOffset(0, 2)))
        assertEquals(IntOffset(0, 2), state.floating?.position)
        assertEquals(emptyList(), drawn.filter { it.isNotEmpty() }, "nothing is drawn while it floats")
        assertTrue(state.onKey(Key.Enter, isShortcut = false, frame) { drawn += it })
        val moved = drawn.single { it.isNotEmpty() }
        assertEquals(Pixel(0, 0, PixelImage.TRANSPARENT), moved.first())
        assertEquals(listOf(Pixel(0, 2, red), Pixel(1, 2, red)), moved.takeLast(2))
        assertEquals(IntRect(0, 2, 2, 3), state.selection)
    }

    @Test
    fun copiedPixelsPasteFloatingAndKeysLetGoOfThem() {
        state.selection = IntRect(0, 0, 2, 1)
        assertTrue(state.onKey(Key.C, isShortcut = true, frame) { drawn += it })
        assertTrue(state.onKey(Key.V, isShortcut = true, frame) { drawn += it })
        assertEquals(SpriteTool.SELECT, state.tool)
        assertEquals(IntOffset(0, 0), state.floating?.position)
        assertTrue(state.onKey(Key.Escape, isShortcut = false, frame) { drawn += it })
        assertNull(state.floating)
        state.selection = IntRect(0, 0, 2, 1)
        assertTrue(state.onKey(Key.Delete, isShortcut = false, frame) { drawn += it })
        assertEquals(listOf(Pixel(0, 0, PixelImage.TRANSPARENT), Pixel(1, 0, PixelImage.TRANSPARENT)), drawn.last())
        assertFalse(state.onKey(Key.Z, isShortcut = true, frame) { drawn += it }, "undo goes on to the workspace")
        assertTrue(state.onKey(Key.G, isShortcut = false, frame) { drawn += it })
        assertEquals(SpriteTool.FILL, state.tool)
    }

    @Test
    fun theZoomGoesBySteps() {
        repeat(20) { state.zoomIn() }
        assertEquals(32, state.zoom)
        state.zoomToFit(16, 8, 300f, 300f)
        assertEquals(16, state.zoom)
        state.showSprite("drawable/a.png")
        state.frameIndex = 3
        state.showSprite("drawable/b.png")
        assertEquals(0, state.frameIndex)
        assertEquals(blue, state.primaryColor, "the colors go from one sprite to the next")
    }

    @Test
    fun theCanvasMapsItsPositionsToPixels() {
        val geometry = SpriteCanvasGeometry(androidx.compose.ui.geometry.Size(100f, 100f), 4, 2, 10, androidx.compose.ui.geometry.Offset(0f, 5f))
        assertEquals(androidx.compose.ui.geometry.Offset(30f, 45f), geometry.topLeft)
        assertEquals(IntOffset(0, 0), geometry.pixelAt(androidx.compose.ui.geometry.Offset(31f, 46f)))
        assertEquals(IntOffset(-1, 1), geometry.pixelAt(androidx.compose.ui.geometry.Offset(29f, 55f)))
    }

    @Test
    fun drawnPixelsShowUntilTheyAreReadBackOrTheirWriteFails() {
        val pixels = listOf(Pixel(0, 0, blue))
        var failure: () -> Unit = {}
        state.pendingPixels = pixels
        state.draw(pixels, "drawable/ship.png", 0) { invocation, onFailure ->
            assertEquals(com.pandulapeter.kubrikoCreator.commands.resources.DrawPixels.Input("drawable/ship.png", 0, listOf(com.pandulapeter.kubrikoCreator.commands.resources.DrawPixels.DrawnPixel(0, 0, 0xFF0000FF))), invocation.input)
            failure = onFailure
            null
        }
        assertEquals(pixels, state.pendingPixels)
        failure()
        assertEquals(emptyList(), state.pendingPixels)
        state.pendingPixels = pixels
        state.draw(pixels, "drawable/ship.png", 0) { _, _ -> com.pandulapeter.kubrikoCreator.commands.Rejection(com.pandulapeter.kubrikoCreator.commands.CommandProblem.INTERNAL, "") }
        assertEquals(emptyList(), state.pendingPixels, "a refused stroke stops showing at once")
    }

    @Test
    fun floatingPixelsArePutDownOnTheirFrameBeforeTheFrameOrTheImageChanges() {
        state.selection = IntRect(0, 0, 2, 1)
        state.liftSelection(frame)
        state.floating = state.floating?.copy(position = IntOffset(1, 1))
        val putDown = state.settle(frame)
        assertEquals(Pixel(1, 1, red), putDown.first { it.color == red })
        assertNull(state.floating)
        assertNull(state.selection)
        assertEquals(emptyList(), state.settle(frame))
    }

    @Test
    fun aLongPencilStrokeCoversEachPixelOnceAsItGoes() {
        state.startStroke(frame, IntOffset(0, 3), isSecondary = false)
        (1..3).forEach { state.continueStroke(IntOffset(it, 3)) }
        state.continueStroke(IntOffset(0, 3))
        assertEquals((0..3).map { Pixel(it, 3, blue) }, state.strokePreview)
        assertEquals((0..3).map { Pixel(it, 3, blue) }, state.finishStroke())
        assertEquals(emptyList(), state.strokePreview)
    }

    @Test
    fun whatIsDrawnButNotReadBackIsOneOverlayWithErasedPixelsShownAsSuch() {
        val overlay = overlayOf(2, 1, listOf(Pixel(0, 0, blue), Pixel(1, 0, PixelImage.TRANSPARENT)))!!
        assertEquals(blue, overlay[0, 0])
        assertEquals(0xFFCCCCCC.toInt(), overlay[1, 0])
        assertNull(overlayOf(2, 1, emptyList()))
    }

    @Test
    fun anImageIsDecodedAgainOnlyOnceItsFileChanged() {
        val file = java.io.File(com.pandulapeter.kubrikoCreator.testing.testDirectory(), "ship.png").apply { writeBytes(frame.toPng()) }
        val reader = SpriteImageReader()
        val first = reader.read(file)
        kotlin.test.assertSame(first, reader.read(file))
        val stamp = file.lastModified()
        val changed = frame.with(listOf(Pixel(3, 3, blue))).toPng()
        file.writeBytes(changed)
        file.setLastModified(stamp)
        assertEquals(frame.with(listOf(Pixel(3, 3, blue))), reader.read(file), "a rewrite within the same moment is read")
    }
}
