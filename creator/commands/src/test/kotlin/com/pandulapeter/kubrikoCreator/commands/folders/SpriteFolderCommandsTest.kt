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

import com.pandulapeter.kubrikoCreator.actors.ActorTypeDefinition
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
import com.pandulapeter.kubrikoCreator.commands.resources.DeleteResource
import com.pandulapeter.kubrikoCreator.commands.resources.ListResources
import com.pandulapeter.kubrikoCreator.commands.resources.RenameResource
import com.pandulapeter.kubrikoCreator.sprites.SpriteFolderStore
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
import kotlin.test.assertNull

class SpriteFolderCommandsTest {
    private val png = byteArrayOf(0x89.toByte(), 'P'.code.toByte(), 'N'.code.toByte(), 'G'.code.toByte(), 13, 10, 26, 10, 1, 2, 3)
    private val drawable = "src/commonMain/composeResources/drawable"

    private fun TestScope.context(vararg sprites: String, types: List<ActorTypeDefinition> = emptyList()): CommandContext {
        val root = testDirectory()
        val files = sprites.associateWith { path -> File(root, "src/commonMain/composeResources/$path").also { it.parentFile.mkdirs(); it.writeBytes(png) } }
        return commandContext(*types.toTypedArray(), rootDirectory = root, spriteFiles = SpriteFiles(files), audioFiles = AudioFiles()).also { it.spriteFolders.refresh() }
    }

    private fun CommandContext.files() = project.rootDirectory.walkTopDown()
        .filter { it.isFile || (it.isDirectory && it.listFiles().isNullOrEmpty()) }
        .associate { it.relativeTo(project.rootDirectory).invariantSeparatorsPath to if (it.isFile) it.readBytes().toList() else null }

    private fun CommandContext.snapshot() = files() to spriteFolders.state.value

    @Test
    fun aSpriteMovesIntoAFolderOnlyInTheSidebar() = runTest {
        val context = context("drawable/bat.png", "drawable/ball.png")
        CreateFolder.apply(context, CreateFolder.Input(FolderGroup.SPRITES, "enemies/flying"))
        val after = context.assertRoundTrip(Invocation(MoveSpriteToFolder, MoveSpriteToFolder.Input("drawable/bat.png", "enemies/flying"))) { context.snapshot() }
        assertEquals("{}\n".toByteArray().toList(), after.first[".kubriko/sprites/enemies/flying/bat.json"])
        assertEquals(listOf("drawable/ball.png" to "", "drawable/bat.png" to "enemies/flying"), context.itemsOf(FolderGroup.SPRITES).sortedBy { it.first })
        assertEquals(setOf("enemies", "enemies/flying"), context.foldersOf(FolderGroup.SPRITES))
    }

    @Test
    fun onlyASpriteMovesIntoAFolderThatExists() = runTest {
        val context = context("drawable/bat.png")
        assertEquals(LookupProblem.FOLDER_MISSING, MoveSpriteToFolder.validate(context, MoveSpriteToFolder.Input("drawable/bat.png", "nowhere"))?.reason)
        assertEquals(Rejection(LookupProblem.RESOURCE_MISSING, "files/sounds/hit.wav"), MoveSpriteToFolder.validate(context, MoveSpriteToFolder.Input("files/sounds/hit.wav", "")))
    }

    @Test
    fun renamingAFolderKeepsItsSpritesInIt() = runTest {
        val context = context("drawable/bat.png")
        CreateFolder.apply(context, CreateFolder.Input(FolderGroup.SPRITES, "enemies"))
        MoveSpriteToFolder.apply(context, MoveSpriteToFolder.Input("drawable/bat.png", "enemies"))
        context.assertRoundTrip(Invocation(RenameFolder, RenameFolder.Input(FolderGroup.SPRITES, "enemies", "foes"))) { context.snapshot() }
        assertEquals(listOf("drawable/bat.png" to "foes"), context.itemsOf(FolderGroup.SPRITES))
    }

    @Test
    fun movingAFolderKeepsItsSpritesInIt() = runTest {
        val context = context("drawable/bat.png")
        CreateFolder.apply(context, CreateFolder.Input(FolderGroup.SPRITES, "enemies"))
        CreateFolder.apply(context, CreateFolder.Input(FolderGroup.SPRITES, "cave"))
        MoveSpriteToFolder.apply(context, MoveSpriteToFolder.Input("drawable/bat.png", "enemies"))
        context.assertRoundTrip(Invocation(MoveFolder, MoveFolder.Input(FolderGroup.SPRITES, "enemies", "cave"))) { context.snapshot() }
        assertEquals(listOf("drawable/bat.png" to "cave/enemies"), context.itemsOf(FolderGroup.SPRITES))
    }

    @Test
    fun aRenamedSpriteStaysInItsFolder() = runTest {
        val context = context("drawable/bat.png")
        CreateFolder.apply(context, CreateFolder.Input(FolderGroup.SPRITES, "enemies"))
        MoveSpriteToFolder.apply(context, MoveSpriteToFolder.Input("drawable/bat.png", "enemies"))
        val renamed = context.assertRoundTrip(Invocation(RenameResource, RenameResource.Input("drawable/bat.png", "drawable/owl.png"))) { context.snapshot() }
        assertEquals("enemies", renamed.second.folderOf["owl"])
    }

    @Test
    fun aDeletedSpriteComesBackToItsFolder() = runTest {
        val context = context("drawable/bat.png")
        CreateFolder.apply(context, CreateFolder.Input(FolderGroup.SPRITES, "enemies"))
        MoveSpriteToFolder.apply(context, MoveSpriteToFolder.Input("drawable/bat.png", "enemies"))
        val deleted = context.assertRoundTrip(Invocation(DeleteResource, DeleteResource.Input("drawable/bat.png"))) { context.snapshot() }
        assertNull(deleted.second.folderOf["bat"])
    }

    @Test
    fun deletingAFolderDeletesItsSpritesUntilUndone() = runTest {
        val context = context("drawable/bat.png", "drawable/ball.png")
        CreateFolder.apply(context, CreateFolder.Input(FolderGroup.SPRITES, "enemies"))
        MoveSpriteToFolder.apply(context, MoveSpriteToFolder.Input("drawable/bat.png", "enemies"))
        val after = context.assertRoundTrip(Invocation(DeleteFolder, DeleteFolder.Input(FolderGroup.SPRITES, "enemies"))) { context.snapshot() }
        assertEquals(setOf("$drawable/ball.png"), after.first.keys.filter { it.startsWith(drawable) || it.startsWith(".kubriko/sprites/") }.toSet())
    }

    private fun CommandContext.withLeftoverFile() = apply {
        File(project.rootDirectory, ".kubriko/sprites/enemies").mkdirs()
        File(project.rootDirectory, ".kubriko/sprites/enemies/bat.json").writeText("{}\n")
        spriteFolders.refresh()
    }

    @Test
    fun aFolderShownEmptyButHoldingTheFileOfAnImageDeletedOutsideIsDeletedUntilUndone() = runTest {
        val context = context("drawable/ball.png").withLeftoverFile()
        val after = context.assertRoundTrip(Invocation(DeleteFolder, DeleteFolder.Input(FolderGroup.SPRITES, "enemies", keepsContents = true))) { context.snapshot() }
        assertEquals(SpriteFolderStore.State(), after.second)
    }

    @Test
    fun aFullFolderTakesTheFileOfAnImageDeletedOutsideWithIt() = runTest {
        val context = context("drawable/ball.png").withLeftoverFile()
        val after = context.assertRoundTrip(Invocation(DeleteFolder, DeleteFolder.Input(FolderGroup.SPRITES, "enemies"))) { context.snapshot() }
        assertEquals(SpriteFolderStore.State(), after.second)
    }

    @Test
    fun theResourcesAreListedWithTheirFolders() = runTest {
        val context = context("drawable/bat.png", "drawable/ball.png")
        CreateFolder.apply(context, CreateFolder.Input(FolderGroup.SPRITES, "enemies"))
        MoveSpriteToFolder.apply(context, MoveSpriteToFolder.Input("drawable/bat.png", "enemies"))
        val listed = assertIs<CommandOutcome.Success>(ListResources.read(context, ListResources.Input(FolderGroup.SPRITES, folder = "enemies"))).result.jsonObject
        assertEquals(1, listed.getValue("total").jsonPrimitive.int)
        assertEquals("drawable/bat.png", listed.getValue("resources").jsonArray.single().jsonObject.getValue("path").jsonPrimitive.content)
        assertEquals(Rejection(CommandProblem.NOT_A_RESOURCE_GROUP), assertIs<CommandOutcome.Rejected>(ListResources.read(context, ListResources.Input(FolderGroup.SCENES))).rejection)
    }
}
