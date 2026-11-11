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
        val state = SpriteEditorState()
        showing {
            SpriteEditor(
                path = "drawable/hero.png",
                file = file,
                sheet = SpriteSheet(frameWidth = 4, frameHeight = 4, frameCount = 2, framesPerRow = 2),
                revision = 0,
                palette = listOf(0xFF00FF00),
                state = state,
                onCommand = { invocation, _ -> commands += invocation; null },
                modifier = Modifier.fillMaxSize(),
            )
        }
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("Frame 1 of 2")).fetchSemanticsNodes().isNotEmpty() }
        saveSnapshot("sprite-editor")
        onNodeWithContentDescription("Next frame").performClick()
        onNodeWithText("Frame 2 of 2").assertExists()
        onNodeWithText("Image").performClick()
        onNodeWithText("Flip horizontally").performClick()
        assertEquals(EditSprite.Input("drawable/hero.png", EditSprite.Operation.Flip(isVertical = false), frame = 1), commands.single().input)
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
}
