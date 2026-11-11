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
import com.pandulapeter.kubrikoCreator.commands.Invocation
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.commands.assertRoundTrip
import com.pandulapeter.kubrikoCreator.commands.commandContext
import com.pandulapeter.kubrikoCreator.foundation.EditorMutation
import com.pandulapeter.kubrikoCreator.sprites.SpriteSheet
import com.pandulapeter.kubrikoCreator.sprites.pixels.Pixel
import com.pandulapeter.kubrikoCreator.sprites.pixels.PixelImage
import com.pandulapeter.kubrikoCreator.sprites.pixels.SpriteFrames
import com.pandulapeter.kubrikoCreator.sprites.sheetOfSprite
import com.pandulapeter.kubrikoCreator.testing.testDirectory
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

@OptIn(EditorMutation::class)
class SpriteFrameCommandsTest {
    private val red = 0xFFFF0000.toInt()
    private val blue = 0xFF0000FF.toInt()
    private val path = "drawable/ship.png"

    /**
     * A strip of two 2 by 1 frames: red, then blue.
     */
    private val strip = PixelImage(4, 1).with(listOf(Pixel(0, 0, red), Pixel(1, 0, red), Pixel(2, 0, blue), Pixel(3, 0, blue)))
    private val stripSheet = SpriteSheet(frameWidth = 2, frameHeight = 1, frameCount = 2, framesPerRow = 2)

    private fun TestScope.context(image: PixelImage = strip, sheet: SpriteSheet? = stripSheet): CommandContext {
        val root = testDirectory()
        val file = File(root, "src/commonMain/composeResources/$path").apply { parentFile.mkdirs(); writeBytes(image.toPng()) }
        return commandContext(rootDirectory = root, spriteFiles = SpriteFiles(mapOf(path to file)), audioFiles = AudioFiles()).also { context ->
            context.spriteFolders.refresh()
            sheet?.let { context.spriteFolders.setSheet("ship", it) }
        }
    }

    private fun CommandContext.image() = PixelImage.of(spriteImageFile(path).readBytes())!!

    private fun CommandContext.sheet() = spriteFolders.state.value.sheets.sheetOfSprite(path)

    private fun edited(operation: EditSpriteFrames.Operation, frames: SpriteFrames = SpriteFrames.of(strip, stripSheet)) =
        (EditSpriteFrames.edited(frames, path, operation) as SpriteEdit.Edited).frames

    private fun SpriteFrames.colors() = frames.map { it[0, 0] }

    @Test
    fun framesAreAddedDuplicatedDeletedAndMovedWithTheStripGrowingInOneRow() {
        val added = edited(EditSpriteFrames.Operation.Add(at = 1))
        assertEquals(listOf(red, PixelImage.TRANSPARENT, blue), added.colors())
        assertEquals(stripSheet.copy(frameCount = 3, framesPerRow = 3), added.sheet)
        assertEquals(listOf(red, blue, PixelImage.TRANSPARENT), edited(EditSpriteFrames.Operation.Add()).colors())
        assertEquals(listOf(red, red, blue), edited(EditSpriteFrames.Operation.Duplicate(0)).colors())
        assertEquals(listOf(blue), edited(EditSpriteFrames.Operation.Delete(0)).colors())
        assertEquals(listOf(blue, red), edited(EditSpriteFrames.Operation.Move(0, to = 1)).colors())
        val grid = SpriteFrames.of(PixelImage(4, 2), SpriteSheet(frameWidth = 2, frameHeight = 1, frameCount = 4, framesPerRow = 2))
        assertEquals(2, edited(EditSpriteFrames.Operation.Add(), grid).sheet?.framesPerRow, "frames in rows keep their rows")
    }

    @Test
    fun theFrameShownFollowsWhatChanged() {
        assertEquals(1, EditSpriteFrames.Operation.Add(at = 1).frameShownAfter(3))
        assertEquals(2, EditSpriteFrames.Operation.Add().frameShownAfter(3))
        assertEquals(1, EditSpriteFrames.Operation.Duplicate(0).frameShownAfter(3))
        assertEquals(0, EditSpriteFrames.Operation.Delete(1).frameShownAfter(1))
        assertEquals(0, EditSpriteFrames.Operation.Move(1, to = 0).frameShownAfter(2))
    }

    @Test
    fun theLastFrameAndFramesThatDontExistAreRefused() {
        val single = SpriteFrames.of(PixelImage(2, 1), null)
        assertEquals(Rejection(SpriteProblem.SPRITE_FRAME_LAST), (EditSpriteFrames.edited(single, path, EditSpriteFrames.Operation.Delete(0)) as SpriteEdit.Rejected).rejection)
        assertIs<SpriteEdit.Rejected>(EditSpriteFrames.edited(SpriteFrames.of(strip, stripSheet), path, EditSpriteFrames.Operation.Move(0, to = 2)))
        assertIs<SpriteEdit.Edited>(EditSpriteFrames.edited(SpriteFrames.of(strip, stripSheet), path, EditSpriteFrames.Operation.Add(at = 2)))
        val tall = SpriteFrames.of(PixelImage(EditSprite.MAXIMUM_SIZE, EditSprite.MAXIMUM_SIZE), null)
        assertEquals(
            Rejection(SpriteProblem.SPRITE_SIZE_INVALID, EditSprite.MAXIMUM_SIZE.toString()),
            (EditSpriteFrames.edited(tall, path, EditSpriteFrames.Operation.Add()) as SpriteEdit.Rejected).rejection,
            "a frame as wide as the widest image wraps to a row the image has no room for",
        )
    }

    @Test
    fun theCommandWritesTheImageAndTheSheetAndIsUndoneByteForByte() = runTest {
        val context = context()
        val image = context.assertRoundTrip(Invocation(EditSpriteFrames, EditSpriteFrames.Input(path, EditSpriteFrames.Operation.Duplicate(1)))) { context.image() to context.sheet() }
        assertEquals(6, image.first.width)
        assertEquals(blue, image.first[5, 0])
        assertEquals(stripSheet.copy(frameCount = 3, framesPerRow = 3), image.second)
    }

    @Test
    fun aSpriteNotCutIntoFramesBecomesOneThatIs() = runTest {
        val context = context(PixelImage(2, 1).with(listOf(Pixel(0, 0, red))), sheet = null)
        assertNull(EditSpriteFrames.validate(context, EditSpriteFrames.Input(path, EditSpriteFrames.Operation.Add())))
        EditSpriteFrames.apply(context, EditSpriteFrames.Input(path, EditSpriteFrames.Operation.Add()))
        assertEquals(SpriteSheet(frameWidth = 2, frameHeight = 1, frameCount = 2, framesPerRow = 2), context.sheet())
        assertEquals(4, context.image().width)
        assertEquals(
            Rejection(SpriteProblem.SPRITE_FRAME_MISSING, path, "5", "1"),
            EditSpriteFrames.validate(context, EditSpriteFrames.Input(path, EditSpriteFrames.Operation.Delete(5))),
        )
    }
}
