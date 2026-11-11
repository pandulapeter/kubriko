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
import com.pandulapeter.kubrikoCreator.actors.BlockTypeKind
import com.pandulapeter.kubrikoCreator.actors.ManagerTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.NameProblem
import com.pandulapeter.kubrikoCreator.commands.CommandContext
import com.pandulapeter.kubrikoCreator.commands.CommandProblem
import com.pandulapeter.kubrikoCreator.commands.Invocation
import com.pandulapeter.kubrikoCreator.commands.LookupProblem
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.commands.WriteCommand
import com.pandulapeter.kubrikoCreator.commands.assertRoundTrip
import com.pandulapeter.kubrikoCreator.commands.blockTypesController
import com.pandulapeter.kubrikoCreator.commands.commandContext
import com.pandulapeter.kubrikoCreator.testing.FakeBlockTypeRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FolderCommandsTest {
    private val bat = ActorTypeDefinition(name = "Bat", folder = "enemies/flying")
    private val wall = ActorTypeDefinition(name = "Wall")
    private val game = ManagerTypeDefinition(name = "Game")

    private fun <I> CommandContext.rejection(command: WriteCommand<I>, input: I) = command.validate(this, input)

    private fun CommandContext.actorFolders() = blockTypes!!.folders.value[BlockTypeKind.ACTOR].orEmpty()

    @Test
    fun foldersComeFromTheTypesInThemAndFromDisk() = runTest {
        val repository = FakeBlockTypeRepository(listOf(bat, wall, game)).apply { folders[BlockTypeKind.MANAGER] = mutableSetOf("rules") }
        val context = commandContext(blockTypes = blockTypesController(repository = repository))
        assertEquals(setOf("enemies", "enemies/flying"), context.actorFolders())
        assertEquals(setOf("rules"), context.blockTypes!!.folders.value[BlockTypeKind.MANAGER])
    }

    @Test
    fun foldersAreCreatedAndDeleted() = runTest {
        val context = commandContext(bat, wall, game)
        val created = context.assertRoundTrip(Invocation(CreateFolder, CreateFolder.Input(FolderGroup.ACTOR_TYPES, "enemies/walking"))) { context.actorFolders() }
        assertEquals(setOf("enemies", "enemies/flying", "enemies/walking"), created)
        assertEquals(Rejection(CommandProblem.FOLDER_EXISTS, "Enemies"), context.rejection(CreateFolder, CreateFolder.Input(FolderGroup.ACTOR_TYPES, "Enemies")))
        assertEquals(Rejection(LookupProblem.FOLDER_MISSING, "props"), context.rejection(CreateFolder, CreateFolder.Input(FolderGroup.ACTOR_TYPES, "props/walls")))
        assertEquals(Rejection(NameProblem.PATH_PART_EMPTY), context.rejection(CreateFolder, CreateFolder.Input(FolderGroup.ACTOR_TYPES, "a//b")))
        assertEquals(Rejection(CommandProblem.FOLDER_NOT_EMPTY, "enemies"), context.rejection(DeleteFolder, DeleteFolder.Input(FolderGroup.ACTOR_TYPES, "enemies", keepsContents = true)))
        assertEquals(Rejection(CommandProblem.FOLDER_TOP), context.rejection(DeleteFolder, DeleteFolder.Input(FolderGroup.ACTOR_TYPES, "")))
        assertEquals(setOf("enemies", "enemies/flying"), context.assertRoundTrip(Invocation(DeleteFolder, DeleteFolder.Input(FolderGroup.ACTOR_TYPES, "enemies/walking"))) { context.actorFolders() })
        assertEquals(Rejection(CommandProblem.NO_ACTOR_TYPES), commandContext(blockTypes = null).rejection(CreateFolder, CreateFolder.Input(FolderGroup.ACTOR_TYPES, "x")))
    }

    @Test
    fun typesMoveBetweenFoldersAndFoldersAreRenamedWithEverythingInThem() = runTest {
        val context = commandContext(bat, wall, game)
        assertEquals("enemies", context.assertRoundTrip(Invocation(MoveToFolder, MoveToFolder.Input("Wall", "enemies"))) { context.blockTypes!!["Wall"]!!.folder })
        assertEquals(Rejection(LookupProblem.FOLDER_MISSING, "rules"), context.rejection(MoveToFolder, MoveToFolder.Input("Game", "rules")))
        val renamed = context.assertRoundTrip(Invocation(RenameFolder, RenameFolder.Input(FolderGroup.ACTOR_TYPES, "enemies", "monsters"))) {
            context.actorFolders() to context.blockTypes!!.types.value.map { it.name to it.folder }
        }
        assertEquals(setOf("monsters", "monsters/flying"), renamed.first)
        assertEquals(listOf("Bat" to "monsters/flying", "Game" to "", "Wall" to "monsters"), renamed.second)
        assertEquals(Rejection(NameProblem.FOLDER_CHARACTERS), context.rejection(RenameFolder, RenameFolder.Input(FolderGroup.ACTOR_TYPES, "monsters", "a/b")))
        assertEquals(Rejection(CommandProblem.FOLDER_TOP), context.rejection(RenameFolder, RenameFolder.Input(FolderGroup.ACTOR_TYPES, "", "monsters")))
        assertNull(context.rejection(MoveToFolder, MoveToFolder.Input("Bat", "")))
    }
}
