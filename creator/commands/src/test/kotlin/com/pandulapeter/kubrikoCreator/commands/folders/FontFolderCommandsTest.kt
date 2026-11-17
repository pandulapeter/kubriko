/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.commands.folders

import com.pandulapeter.kubrikoCreator.actors.AudioFiles
import com.pandulapeter.kubrikoCreator.actors.FontFiles
import com.pandulapeter.kubrikoCreator.actors.SpriteFiles
import com.pandulapeter.kubrikoCreator.commands.CommandContext
import com.pandulapeter.kubrikoCreator.commands.CommandOutcome
import com.pandulapeter.kubrikoCreator.commands.CommandProblem
import com.pandulapeter.kubrikoCreator.commands.Invocation
import com.pandulapeter.kubrikoCreator.commands.LookupProblem
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.commands.assertRoundTrip
import com.pandulapeter.kubrikoCreator.commands.commandContext
import com.pandulapeter.kubrikoCreator.commands.resources.DeleteResource
import com.pandulapeter.kubrikoCreator.commands.resources.ListResources
import com.pandulapeter.kubrikoCreator.commands.resources.RenameResource
import com.pandulapeter.kubrikoCreator.commands.resources.validateNewResource
import com.pandulapeter.kubrikoCreator.project.FontFolderStore
import com.pandulapeter.kubrikoCreator.testing.testDirectory
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class FontFolderCommandsTest {
    private val ttf = byteArrayOf(0, 1, 0, 0, 0, 16, 1, 2, 3)
    private val fontFolder = "src/commonMain/composeResources/font"

    private fun TestScope.context(vararg fonts: String): CommandContext {
        val root = testDirectory()
        val files = fonts.associateWith { path -> File(root, "src/commonMain/composeResources/$path").also { it.parentFile.mkdirs(); it.writeBytes(ttf) } }
        return commandContext(rootDirectory = root, spriteFiles = SpriteFiles(), fontFiles = FontFiles(files), audioFiles = AudioFiles()).also { it.fontFolders.refresh() }
    }

    private fun CommandContext.files() = project.rootDirectory.walkTopDown()
        .filter { it.isFile || (it.isDirectory && it.listFiles().isNullOrEmpty()) }
        .associate { it.relativeTo(project.rootDirectory).invariantSeparatorsPath to if (it.isFile) it.readBytes().toList() else null }

    private fun CommandContext.snapshot() = files() to fontFolders.state.value

    @Test
    fun aFontMovesIntoAFolderOnlyInTheSidebarApartFromTheSprites() = runTest {
        val context = context("font/pixel.ttf", "font/serif.otf")
        CreateFolder.apply(context, CreateFolder.Input(FolderGroup.FONTS, "titles"))
        val after = context.assertRoundTrip(Invocation(MoveFontToFolder, MoveFontToFolder.Input("font/pixel.ttf", "titles"))) { context.snapshot() }
        assertEquals("{}\n".toByteArray().toList(), after.first[".kubriko/fonts/titles/pixel.json"])
        assertEquals(listOf("font/pixel.ttf" to "titles", "font/serif.otf" to ""), context.itemsOf(FolderGroup.FONTS).sortedBy { it.first })
        assertEquals(setOf("titles"), context.foldersOf(FolderGroup.FONTS))
        assertEquals(emptySet(), context.foldersOf(FolderGroup.SPRITES))
    }

    @Test
    fun onlyAFontMovesIntoAFontFolderThatExists() = runTest {
        val context = context("font/pixel.ttf")
        assertEquals(LookupProblem.FOLDER_MISSING, MoveFontToFolder.validate(context, MoveFontToFolder.Input("font/pixel.ttf", "nowhere"))?.reason)
        assertEquals(Rejection(LookupProblem.RESOURCE_MISSING, "drawable/ball.png"), MoveFontToFolder.validate(context, MoveFontToFolder.Input("drawable/ball.png", "")))
    }

    @Test
    fun aFolderKeepsItsFontsThroughRenamesAndMoves() = runTest {
        val context = context("font/pixel.ttf")
        CreateFolder.apply(context, CreateFolder.Input(FolderGroup.FONTS, "titles"))
        CreateFolder.apply(context, CreateFolder.Input(FolderGroup.FONTS, "menus"))
        MoveFontToFolder.apply(context, MoveFontToFolder.Input("font/pixel.ttf", "titles"))
        context.assertRoundTrip(Invocation(RenameFolder, RenameFolder.Input(FolderGroup.FONTS, "titles", "headings"))) { context.snapshot() }
        context.assertRoundTrip(Invocation(MoveFolder, MoveFolder.Input(FolderGroup.FONTS, "headings", "menus"))) { context.snapshot() }
        assertEquals(listOf("font/pixel.ttf" to "menus/headings"), context.itemsOf(FolderGroup.FONTS))
    }

    @Test
    fun aRenamedFontStaysInItsFolderAndADeletedOneComesBackToIt() = runTest {
        val context = context("font/pixel.ttf")
        CreateFolder.apply(context, CreateFolder.Input(FolderGroup.FONTS, "titles"))
        MoveFontToFolder.apply(context, MoveFontToFolder.Input("font/pixel.ttf", "titles"))
        val renamed = context.assertRoundTrip(Invocation(RenameResource, RenameResource.Input("font/pixel.ttf", "font/blocky.ttf"))) { context.snapshot() }
        assertEquals("titles", renamed.second.folderOf["blocky"])
        val other = context("font/serif.otf")
        CreateFolder.apply(other, CreateFolder.Input(FolderGroup.FONTS, "titles"))
        MoveFontToFolder.apply(other, MoveFontToFolder.Input("font/serif.otf", "titles"))
        val deleted = other.assertRoundTrip(Invocation(DeleteResource, DeleteResource.Input("font/serif.otf"))) { other.snapshot() }
        assertNull(deleted.second.folderOf["serif"])
    }

    @Test
    fun deletingAFolderDeletesItsFontsUntilUndoneAndTakesLeftoverEntries() = runTest {
        val context = context("font/pixel.ttf", "font/serif.otf")
        CreateFolder.apply(context, CreateFolder.Input(FolderGroup.FONTS, "titles"))
        MoveFontToFolder.apply(context, MoveFontToFolder.Input("font/pixel.ttf", "titles"))
        File(context.project.rootDirectory, ".kubriko/fonts/titles/gone.json").writeText("{}\n")
        context.fontFolders.refresh()
        val after = context.assertRoundTrip(Invocation(DeleteFolder, DeleteFolder.Input(FolderGroup.FONTS, "titles"))) { context.snapshot() }
        assertEquals(setOf("$fontFolder/serif.otf"), after.first.keys.filter { it.startsWith(fontFolder) || it.startsWith(".kubriko/fonts/") }.toSet())
        assertEquals(FontFolderStore.State(), after.second)
    }

    @Test
    fun fontsAreListedAndTheirAccessorsMayNotClash() = runTest {
        val context = context("font/pixel-1.ttf")
        val listed = assertIs<CommandOutcome.Success>(ListResources.read(context, ListResources.Input(FolderGroup.FONTS))).result.jsonObject
        assertEquals("font/pixel-1.ttf", listed.getValue("resources").jsonArray.single().jsonObject.getValue("path").jsonPrimitive.content)
        assertEquals(Rejection(CommandProblem.RESOURCE_TAKEN, "font/pixel_1.otf"), context.validateNewResource("font/pixel_1.otf"))
        assertNull(context.validateNewResource("drawable/pixel_1.png"))
    }
}
