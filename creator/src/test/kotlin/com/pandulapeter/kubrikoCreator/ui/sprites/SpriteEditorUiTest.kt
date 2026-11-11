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

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.MouseButton
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performTextReplacement
import com.pandulapeter.kubrikoCreator.commands.Invocation
import com.pandulapeter.kubrikoCreator.commands.resources.EditSprite
import com.pandulapeter.kubrikoCreator.commands.resources.EditSpriteFrames
import com.pandulapeter.kubrikoCreator.commands.resources.SaveSprite
import com.pandulapeter.kubrikoCreator.sprites.SpriteSheet
import com.pandulapeter.kubrikoCreator.sprites.pixels.Pixel
import com.pandulapeter.kubrikoCreator.sprites.pixels.PixelImage
import com.pandulapeter.kubrikoCreator.testing.saveSnapshot
import com.pandulapeter.kubrikoCreator.testing.showing
import com.pandulapeter.kubrikoCreator.testing.testDirectory
import com.pandulapeter.kubrikoCreator.testing.uiTest
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

@OptIn(ExperimentalTestApi::class)
class SpriteEditorUiTest {
    private val red = 0xFFFF0000.toInt()
    private val blue = 0xFF0000FF.toInt()

    @Test
    fun aStrokeWithEitherButtonDrawsInItsColorWhenItIsLetGo() = uiTest {
        val drawn = mutableListOf<List<Pixel>>()
        val state = SpriteEditorState().apply {
            primaryColor = blue
            secondaryColor = red
        }
        showing { SpriteCanvas(frame = PixelImage(4, 4), state = state, onDraw = { drawn += it }, modifier = Modifier.fillMaxSize()) }
        // The frame is 32 by 32 pixels at the default zoom, in the middle, so its first row is 12 pixels above it.
        onRoot().performMouseInput {
            moveTo(center + Offset(-12f, -12f))
            press()
            moveTo(center + Offset(12f, -12f))
            release()
        }
        assertEquals(listOf((0..3).map { Pixel(it, 0, blue) }), drawn)
        onRoot().performMouseInput {
            moveTo(center + Offset(-12f, 12f))
            press(MouseButton.Secondary)
            release(MouseButton.Secondary)
        }
        assertEquals(listOf(Pixel(0, 3, red)), drawn.last())
        val strokes = drawn.size
        onRoot().performMouseInput {
            moveTo(center)
            scroll(-3f)
            scroll(2f)
        }
        assertEquals(strokes, drawn.size, "scrolling draws nothing")
    }

    @Test
    fun theEditorShowsTheSpritesFrameAndChangesItThroughCommands() = uiTest(width = 900, height = 560) {
        val file = File(testDirectory(), "hero.png").apply {
            writeBytes(PixelImage(8, 4).with((0 until 4).map { Pixel(it, it, red) } + (4 until 8).map { Pixel(it, 0, blue) }).toPng())
        }
        val commands = mutableListOf<Invocation<*>>()
        val state = SpriteEditorState().apply { tool = SpriteTool.RECTANGLE }
        showing {
            SpriteEditor(
                path = "drawable/hero.png",
                file = file,
                sheet = SpriteSheet(frameWidth = 4, frameHeight = 4, frameCount = 2, framesPerRow = 2),
                revision = 0,
                state = state,
                editing = SpriteEditing(palette = listOf(0xFF00FF00), onCommand = { invocation, onSuccess, _ -> null.also { commands += invocation; onSuccess() } }),
                modifier = Modifier.fillMaxSize(),
            )
        }
        waitUntil(timeoutMillis = 5_000) { state.draft != null }
        onNodeWithText("Filled").assertExists()
        saveSnapshot("sprite-editor")
        onNodeWithText("2").performClick()
        assertEquals(1, state.frameIndex)
        onNodeWithText("Save").assertIsNotEnabled()
        onNodeWithText("Image").performClick()
        onNodeWithText("Flip vertically").performClick()
        assertEquals(emptyList(), commands, "the editor keeps its changes until they are saved")
        assertEquals(blue, state.draft!!.current.frames[1][0, 3])
        onNodeWithText("Save").performClick()
        val saved = commands.single().input as SaveSprite.Input
        assertEquals(blue, saved.frames.frames[1][0, 3])
        assertEquals(red, saved.frames.frames[0][0, 0], "the other frame as it was")
        onNodeWithText("Save").assertIsNotEnabled()
    }

    @Test
    fun leavingASpriteWithChangesAsksWhetherToSaveThem() = uiTest(width = 900, height = 560) {
        val file = File(testDirectory(), "hero.png").apply { writeBytes(PixelImage(4, 4).toPng()) }
        val commands = mutableListOf<Invocation<*>>()
        var guard: ((() -> Unit) -> Unit)? = null
        val state = SpriteEditorState()
        showing {
            SpriteEditor(
                path = "drawable/hero.png",
                file = file,
                sheet = null,
                revision = 0,
                state = state,
                editing = SpriteEditing(onCommand = { invocation, onSuccess, _ -> null.also { commands += invocation; onSuccess() } }, onGuardLeaving = { guard = it }),
                modifier = Modifier.fillMaxSize(),
            )
        }
        waitUntil(timeoutMillis = 5_000) { state.draft != null }
        assertNull(guard, "nothing to ask about yet")
        runOnIdle { state.draw(0, listOf(Pixel(0, 0, red))) }
        waitForIdle()
        var left = 0
        runOnIdle { guard!! { left++ } }
        onNodeWithText("Save the changes to hero.png?").assertExists()
        onNodeWithText("Cancel").performClick()
        assertEquals(0, left)
        runOnIdle { guard!! { left++ } }
        onNode(hasText("Save") and androidx.compose.ui.test.hasAnyAncestor(androidx.compose.ui.test.isDialog())).performClick()
        assertEquals(1, left)
        assertIs<SaveSprite.Input>(commands.single().input)
        waitForIdle()
        assertNull(guard, "saved: nothing left to ask about")
        runOnIdle { state.draw(0, listOf(Pixel(1, 0, red))) }
        waitForIdle()
        runOnIdle { guard!! { left++ } }
        onNodeWithText("Don't save").performClick()
        assertEquals(2, left)
        assertEquals(1, commands.size)
        assertEquals(PixelImage.TRANSPARENT, state.draft!!.current.frames.single()[1, 0])
    }

    @Test
    fun anOperationTheCommandWouldRefuseCantBeApplied() = uiTest(width = 600, height = 400) {
        val applied = mutableListOf<EditSprite.Operation>()
        showing { SpriteOperationDialog(SpriteImageAction.SCALE, frameWidth = 8, frameHeight = 8, onApply = { applied += it }, onDismiss = {}) }
        onNode(androidx.compose.ui.test.hasSetTextAction() and hasText("2")).performTextReplacement("17")
        onNodeWithText("Apply").assertIsNotEnabled()
        onNode(androidx.compose.ui.test.hasSetTextAction() and hasText("17")).performTextReplacement("3")
        onNodeWithText("Apply").performClick()
        assertEquals(listOf<EditSprite.Operation>(EditSprite.Operation.Scale(3)), applied)
    }

    @Test
    fun theFrameStripShowsAddsDuplicatesDeletesAndDragsFrames() = uiTest(width = 700, height = 200) {
        val frames = listOf(PixelImage(2, 2).with(listOf(Pixel(0, 0, red))), PixelImage(2, 2).with(listOf(Pixel(0, 0, blue))))
        val edits = mutableListOf<EditSpriteFrames.Operation>()
        val shown = mutableListOf<Int>()
        showing { SpriteFrameStrip(frames = frames, shownIndex = 0, onShow = { shown += it }, onEdit = { edits += it }) }
        onNodeWithText("2").performClick()
        assertEquals(listOf(1), shown)
        onNodeWithContentDescription("Add a frame after this one").performClick()
        onNodeWithContentDescription("Duplicate the frame").performClick()
        onNodeWithContentDescription("Delete the frame").performClick()
        onNodeWithText("1").performMouseInput {
            moveTo(center)
            press()
            moveBy(Offset(30f, 0f))
            moveBy(Offset(40f, 0f))
            release()
        }
        assertEquals(
            listOf(EditSpriteFrames.Operation.Add(at = 1), EditSpriteFrames.Operation.Duplicate(0), EditSpriteFrames.Operation.Delete(0), EditSpriteFrames.Operation.Move(0, to = 1)),
            edits,
        )
        saveSnapshot("sprite-frame-strip")
    }

    @Test
    fun theLastFrameCantBeDeleted() = uiTest(width = 700, height = 200) {
        showing { SpriteFrameStrip(frames = listOf(PixelImage(2, 2)), shownIndex = 0, onShow = {}, onEdit = {}) }
        onNodeWithContentDescription("Delete the frame").assertIsNotEnabled()
    }
}
