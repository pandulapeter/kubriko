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
import com.pandulapeter.kubrikoCreator.commands.LookupProblem
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.commands.assertRoundTrip
import com.pandulapeter.kubrikoCreator.commands.commandContext
import com.pandulapeter.kubrikoCreator.commands.folders.FolderGroup
import com.pandulapeter.kubrikoCreator.sprites.SpriteSheet
import com.pandulapeter.kubrikoCreator.sprites.SpriteSheetProblem
import com.pandulapeter.kubrikoCreator.testing.testDirectory
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assume.assumeFalse
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class SetSpriteSheetTest {
    private val sheet = SpriteSheet(frameWidth = 16, frameHeight = 16, frameCount = 4, framesPerRow = 4, framesPerSecond = 8f)

    private fun TestScope.context(width: Int = 64, height: Int = 16): CommandContext {
        val root = testDirectory()
        val image = File(root, "src/commonMain/composeResources/drawable/bat.png").also { it.parentFile.mkdirs() }
        ImageIO.write(BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB), "png", image)
        return commandContext(rootDirectory = root, spriteFiles = SpriteFiles(mapOf("drawable/bat.png" to image)), audioFiles = AudioFiles())
            .also { it.spriteFolders.refresh() }
    }

    @Test
    fun aSpriteIsCutIntoFramesAndShownWholeAgainByUndo() = runTest {
        val context = context()
        context.assertRoundTrip(Invocation(SetSpriteSheet, SetSpriteSheet.Input("drawable/bat.png", sheet))) { context.spriteFolders.state.value }
        assertEquals(mapOf("bat" to sheet), context.spriteFolders.state.value.sheets)
        context.assertRoundTrip(Invocation(SetSpriteSheet, SetSpriteSheet.Input("drawable/bat.png", sheet.copy(framesPerSecond = 2f)))) { context.spriteFolders.state.value }
        val shownWhole = context.assertRoundTrip(Invocation(SetSpriteSheet, SetSpriteSheet.Input("drawable/bat.png"))) { context.spriteFolders.state.value }
        assertEquals(emptyMap(), shownWhole.sheets)
    }

    @Test
    fun undoingBringsBackTheSpritesFileByteForByteEvenWithASheetThatCantBePlayed() = runTest {
        val context = context()
        val file = File(context.project.rootDirectory, ".kubriko/sprites/enemies/bat.json").apply { parentFile.mkdirs() }
        val handWritten = "{\"sheet\":{\"frameWidth\":16,\"frameHeight\":16,\"frameCount\":4,\"framesPerRow\":4,\"framesPerSecond\":0},\"later\":1}\r\n"
        file.writeText(handWritten)
        context.spriteFolders.refresh()
        val after = context.assertRoundTrip(Invocation(SetSpriteSheet, SetSpriteSheet.Input("drawable/bat.png", sheet))) { file.readText() }
        assertEquals(mapOf("bat" to sheet), context.spriteFolders.state.value.sheets, after)
    }

    @Test
    fun onlyASpriteThatExistsIsCutAndOnlyIntoFramesThatCanBePlayed() = runTest {
        val context = context()
        assertEquals(Rejection(LookupProblem.RESOURCE_MISSING, "files/sounds/hit.wav"), SetSpriteSheet.validate(context, SetSpriteSheet.Input("files/sounds/hit.wav", sheet)))
        assertEquals(LookupProblem.RESOURCE_MISSING, SetSpriteSheet.validate(context, SetSpriteSheet.Input("drawable/owl.png", sheet))?.reason)
        assertEquals(Rejection(SpriteSheetProblem.FRAME_COUNT), SetSpriteSheet.validate(context, SetSpriteSheet.Input("drawable/bat.png", sheet.copy(frameCount = 0))))
    }

    @Test
    fun framesThatDontFitTheImageAreRefusedAndNothingChanges() = runTest {
        val context = context(width = 48)
        val applied = SetSpriteSheet.apply(context, SetSpriteSheet.Input("drawable/bat.png", sheet))
        assertEquals(Rejection(CommandProblem.SPRITE_SHEET_DOES_NOT_FIT, "drawable/bat.png", "48", "16"), applied.failure)
        assertEquals(emptyMap(), context.spriteFolders.state.value.sheets)
    }

    @Test
    fun aSheetThatCantBeWrittenIsReportedAndNothingChanges() = runTest {
        val context = context()
        val directory = File(context.project.rootDirectory, ".kubriko/sprites").apply { mkdirs() }
        directory.setWritable(false)
        try {
            assumeFalse("the folder still takes new files", runCatching { File(directory, "probe").createNewFile() }.getOrDefault(false))
            assertEquals(CommandProblem.SPRITE_SHEET_NOT_WRITTEN, SetSpriteSheet.apply(context, SetSpriteSheet.Input("drawable/bat.png", sheet)).failure?.reason)
        } finally {
            directory.setWritable(true)
        }
        assertEquals(emptyMap(), context.spriteFolders.state.value.sheets)
    }

    @Test
    fun theSpritesListSaysHowEachIsCut() = runTest {
        val context = context()
        SetSpriteSheet.apply(context, SetSpriteSheet.Input("drawable/bat.png", sheet))
        val listed = assertIs<CommandOutcome.Success>(ListResources.read(context, ListResources.Input(FolderGroup.SPRITES))).result
        val bat = listed.jsonObject.getValue("resources").jsonArray.single().jsonObject
        assertEquals(4, bat.getValue("sheet").jsonObject.getValue("frameCount").jsonPrimitive.int)
        assertNull(SetSpriteSheet.validate(context, SetSpriteSheet.Input("drawable/bat.png")))
    }
}
