/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.commands.resources

import com.pandulapeter.kubrikoCreator.actors.AudioFiles
import com.pandulapeter.kubrikoCreator.actors.SpriteFiles
import com.pandulapeter.kubrikoCreator.commands.CommandContext
import com.pandulapeter.kubrikoCreator.commands.CommandOutcome
import com.pandulapeter.kubrikoCreator.commands.CommandProblem
import com.pandulapeter.kubrikoCreator.commands.Invocation
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.commands.assertRoundTrip
import com.pandulapeter.kubrikoCreator.commands.commandContext
import com.pandulapeter.kubrikoCreator.foundation.EditorMutation
import com.pandulapeter.kubrikoCreator.sprites.SpriteSheet
import com.pandulapeter.kubrikoCreator.sprites.pixels.PixelImage
import com.pandulapeter.kubrikoCreator.testing.testDirectory
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull

@OptIn(EditorMutation::class)
class SpriteDrawingCommandsTest {
    private val red = 0xFFFF0000L
    private val blue = 0xFF0000FFL

    private fun TestScope.context(image: PixelImage = PixelImage(4, 2), sheet: SpriteSheet? = null): CommandContext {
        val root = testDirectory()
        val file = File(root, "src/commonMain/composeResources/drawable/ship.png").apply { parentFile.mkdirs(); writeBytes(image.toPng()) }
        return commandContext(rootDirectory = root, spriteFiles = SpriteFiles(mapOf("drawable/ship.png" to file)), audioFiles = AudioFiles()).also { context ->
            context.spriteFolders.refresh()
            sheet?.let { context.spriteFolders.setSheet("ship", it) }
        }
    }

    private fun CommandContext.image() = PixelImage.of(spriteImageFile("drawable/ship.png").readBytes())!!

    @Test
    fun pixelsAndRowsAreDrawnOnAFrameAndUndoneByteForByte() = runTest {
        val context = context(sheet = SpriteSheet(frameWidth = 2, frameHeight = 2, frameCount = 2, framesPerRow = 2))
        val input = DrawPixels.Input(
            sprite = "drawable/ship.png",
            frame = 1,
            pixels = listOf(DrawPixels.DrawnPixel(0, 0, red)),
            rows = listOf(".b", "b."),
            palette = listOf(DrawPixels.PaletteColor("b", blue)),
        )
        val drawn = context.assertRoundTrip(Invocation(DrawPixels, input)) { context.image() }
        assertEquals(red.toInt(), drawn[2, 0])
        assertEquals(blue.toInt(), drawn[3, 0])
        assertEquals(blue.toInt(), drawn[2, 1])
        assertEquals(PixelImage.TRANSPARENT, drawn[0, 0])
    }

    @Test
    fun drawingSaysWhatIsWrongWithItsPixels() = runTest {
        val context = context()
        fun rejection(input: DrawPixels.Input) = DrawPixels.validate(context, input)
        assertEquals(Rejection(SpriteProblem.PIXELS_NONE), rejection(DrawPixels.Input("drawable/ship.png")))
        assertEquals(Rejection(SpriteProblem.PIXELS_LETTER_UNKNOWN, "x"), rejection(DrawPixels.Input("drawable/ship.png", rows = listOf(".x"))))
        assertEquals(Rejection(SpriteProblem.PIXELS_PALETTE_KEY), rejection(DrawPixels.Input("drawable/ship.png", rows = listOf("."), palette = listOf(DrawPixels.PaletteColor(".", red)))))
        assertEquals(Rejection(SpriteProblem.SPRITE_FRAME_MISSING, "drawable/ship.png", "1", "0"), rejection(DrawPixels.Input("drawable/ship.png", frame = 1, rows = listOf("."))))
        val outside = DrawPixels.apply(context, DrawPixels.Input("drawable/ship.png", pixels = listOf(DrawPixels.DrawnPixel(4, 0, red))))
        assertEquals(Rejection(SpriteProblem.PIXELS_OUTSIDE_FRAME, "4, 0", "4", "2"), outside.failure)
        assertEquals(PixelImage(4, 2), context.image())
    }

    @Test
    fun aFrameIsFlippedAndEveryFrameScaledWithItsSheet() = runTest {
        val sheet = SpriteSheet(frameWidth = 2, frameHeight = 2, frameCount = 2, framesPerRow = 2, originX = 1)
        val context = context(PixelImage(4, 2).with(listOf(com.pandulapeter.kubrikoCreator.sprites.pixels.Pixel(2, 0, red.toInt()))), sheet)
        val flipped = context.assertRoundTrip(Invocation(EditSprite, EditSprite.Input("drawable/ship.png", EditSprite.Operation.Flip(), frame = 1))) { context.image() }
        assertEquals(red.toInt(), flipped[3, 0])
        val scaled = context.assertRoundTrip(Invocation(EditSprite, EditSprite.Input("drawable/ship.png", EditSprite.Operation.Scale(2)))) { context.image() to context.spriteFolders.state.value.sheets }
        assertEquals(8 to 4, scaled.first.width to scaled.first.height)
        assertEquals(mapOf("ship" to sheet.copy(frameWidth = 4, frameHeight = 4, originX = 2)), scaled.second)
        assertEquals(
            Rejection(SpriteProblem.SPRITE_FRAME_SIZE_ALONE),
            EditSprite.apply(context, EditSprite.Input("drawable/ship.png", EditSprite.Operation.ResizeCanvas(3, 2), frame = 0)).failure,
        )
        assertEquals(Rejection(SpriteProblem.SPRITE_SCALE_INVALID), EditSprite.validate(context, EditSprite.Input("drawable/ship.png", EditSprite.Operation.Scale(0))))
    }

    @Test
    fun aNewSpriteIsASeeThroughImageInItsFolderAndUndoingDeletesBoth() = runTest {
        val context = context()
        context.spriteFolders.createFolder("player")
        val input = CreateSprite.Input("drawable/hero.png", width = 3, height = 5, folder = "player")
        val heroFile = File(context.scenes.resourcesDirectory, "drawable/hero.png")
        val created = context.assertRoundTrip(Invocation(CreateSprite, input)) { heroFile.takeIf { it.isFile }?.readBytes()?.let(PixelImage::of) to context.spriteFolders.state.value.folderOf["hero"] }
        assertEquals(PixelImage(3, 5) to "player", created)
        assertEquals(Rejection(SpriteProblem.SPRITE_PATH_INVALID, "drawable/hero.jpg"), CreateSprite.validate(context, input.copy(path = "drawable/hero.jpg")))
        assertEquals(Rejection(SpriteProblem.SPRITE_SIZE_INVALID, "4096"), CreateSprite.validate(context, input.copy(width = 0)))
    }

    @Test
    fun aSpriteIsReadAsAnImageItsColorsAndRows() = runTest {
        val context = context(PixelImage(2, 1, intArrayOf(red.toInt(), PixelImage.TRANSPARENT)))
        val outcome = assertIs<CommandOutcome.Success>(ReadSprite.read(context, ReadSprite.Input("drawable/ship.png", asRows = true)))
        val result = outcome.result.jsonObject
        assertEquals(2, result.getValue("width").jsonPrimitive.int)
        assertEquals(listOf("#FF0000"), result.getValue("colors").jsonArray.map { it.jsonPrimitive.content })
        assertEquals(listOf("a."), result.getValue("rows").jsonArray.map { it.jsonPrimitive.content })
        assertEquals("#FF0000", result.getValue("palette").jsonArray.single().jsonObject.getValue("color").jsonPrimitive.content)
        val preview = assertNotNull(PixelImage.of(assertNotNull(outcome.image)))
        assertEquals(32 to 16, preview.width to preview.height)
    }

    @Test
    fun thePaletteIsSetAndUndoneAndHoldsAtMostSixtyFourColors() = runTest {
        val context = context()
        val colors = context.assertRoundTrip(Invocation(SetPalette, SetPalette.Input(listOf(red, blue, red)))) { context.palette.colors.value }
        assertEquals(listOf(red, blue), colors)
        assertEquals(Rejection(SpriteProblem.PALETTE_TOO_LARGE, "64"), SetPalette.validate(context, SetPalette.Input(List(65) { it.toLong() })))
    }

    @Test
    fun imagesBecomeTheFramesOfANewSpriteAndUndoingDeletesItAndItsSheet() = runTest {
        val context = context()
        val frames = (0 until 3).map { index ->
            File(testDirectory(), "walk-$index.png").apply { writeBytes(PixelImage(2, 2).with(listOf(com.pandulapeter.kubrikoCreator.sprites.pixels.Pixel(index % 2, 0, red.toInt()))).toPng()) }
        }
        val file = File(context.scenes.resourcesDirectory, "drawable/walk.png")
        val input = ImportSpriteFrames.Input(frames.map { it.path }, "drawable/walk.png")
        val imported = context.assertRoundTrip(Invocation(ImportSpriteFrames, input)) { file.takeIf { it.isFile }?.readBytes()?.let(PixelImage::of) to context.spriteFolders.state.value.sheets["walk"] }
        assertEquals(6 to 2, imported.first!!.width to imported.first!!.height)
        assertEquals(red.toInt(), imported.first!![2 + 1, 0])
        assertEquals(SpriteSheet(frameWidth = 2, frameHeight = 2, frameCount = 3, framesPerRow = 3), imported.second)
        val wide = File(testDirectory(), "wide.png").apply { writeBytes(PixelImage(3, 2).toPng()) }
        assertEquals(
            Rejection(SpriteProblem.FRAMES_DIFFER_IN_SIZE, wide.path, "2", "2"),
            ImportSpriteFrames.apply(context, input.copy(files = listOf(frames.first().path, wide.path), path = "drawable/run.png")).failure,
        )
        assertEquals(Rejection(SpriteProblem.FRAMES_NONE), ImportSpriteFrames.validate(context, input.copy(files = emptyList())))
    }
}
