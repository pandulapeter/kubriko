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
import com.pandulapeter.kubrikoCreator.actors.BlockDefinition
import com.pandulapeter.kubrikoCreator.actors.EventDefinition
import com.pandulapeter.kubrikoCreator.actors.EventType
import com.pandulapeter.kubrikoCreator.actors.NameProblem
import com.pandulapeter.kubrikoCreator.actors.SpriteFiles
import com.pandulapeter.kubrikoCreator.commands.CommandContext
import com.pandulapeter.kubrikoCreator.commands.CommandProblem
import com.pandulapeter.kubrikoCreator.commands.Invocation
import com.pandulapeter.kubrikoCreator.commands.LookupProblem
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.commands.assertRoundTrip
import com.pandulapeter.kubrikoCreator.commands.commandContext
import com.pandulapeter.kubrikoCreator.commands.usagesOf
import com.pandulapeter.kubrikoCreator.context.RelationSubject
import com.pandulapeter.kubrikoCreator.testing.testDirectory
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AudioFolderCommandsTest {
    private val wav = "RIFF\u0000\u0000\u0000\u0000WAVEfmt ".toByteArray(Charsets.ISO_8859_1)
    private val mp3 = "ID3\u0004rest".toByteArray(Charsets.ISO_8859_1)
    private val common = "src/commonMain/composeResources"
    private val desktop = "src/desktopMain/composeResources"

    private val player = ActorTypeDefinition(
        name = "Player",
        events = listOf(
            EventDefinition(
                EventType.EVERY_FRAME,
                blocks = listOf(BlockDefinition.PlaySound("files/sounds/enemies/hit.wav"), BlockDefinition.PlayMusic("files/sounds/enemies/boss.mp3")),
            ),
        ),
    )

    private fun TestScope.context(
        vararg types: ActorTypeDefinition,
        sounds: List<String> = emptyList(),
        music: List<String> = emptyList(),
        resources: String = common,
    ): CommandContext {
        val root = testDirectory()
        sounds.forEach { File(root, "$resources/$it").apply { parentFile.mkdirs() }.writeBytes(wav) }
        music.forEach { File(root, "$resources/$it").apply { parentFile.mkdirs() }.writeBytes(mp3) }
        return commandContext(*types, rootDirectory = root, spriteFiles = SpriteFiles(emptyMap()), audioFiles = AudioFiles(sounds, music))
    }

    private fun CommandContext.directory(path: String) = File(project.rootDirectory, path)

    private fun CommandContext.files() = project.rootDirectory.walkTopDown()
        .filter { it.isFile || (it.isDirectory && it.listFiles().isNullOrEmpty()) }
        .associate { it.relativeTo(project.rootDirectory).invariantSeparatorsPath to if (it.isFile) it.readBytes().toList() else null }

    @Test
    fun aFolderIsMadeInTheFilesFolderAndUndoneAgain() = runTest {
        val context = context(sounds = listOf("files/sounds/hit.wav"))
        context.assertRoundTrip(Invocation(CreateFolder, CreateFolder.Input(FolderGroup.SOUNDS, "sounds/enemies"))) { context.directory("$common/files/sounds/enemies").isDirectory }
        CreateFolder.apply(context, CreateFolder.Input(FolderGroup.SOUNDS, "sounds/enemies"))
        assertTrue("sounds/enemies" in context.foldersOf(FolderGroup.SOUNDS))
    }

    @Test
    fun aNewFolderKeepsToTheNamingRulesOfAudioFiles() = runTest {
        val context = context(sounds = listOf("files/sounds/hit.wav"))
        fun rejection(path: String) = CreateFolder.validate(context, CreateFolder.Input(FolderGroup.SOUNDS, path))
        assertEquals(Rejection(NameProblem.RESOURCE_CHARACTERS), rejection("sounds/bad name"))
        assertEquals(Rejection(NameProblem.RESOURCE_FIRST_CHARACTER), rejection("_hidden"))
        assertEquals(Rejection(CommandProblem.AUDIO_FOLDER_RESERVED, "scenes/levels"), rejection("scenes/levels"))
        assertEquals(Rejection(CommandProblem.FOLDER_EXISTS, "Sounds"), rejection("Sounds"))
        assertEquals(LookupProblem.FOLDER_MISSING, rejection("effects/enemies")?.reason)
        assertNull(rejection("effects"))
    }

    @Test
    fun eachGroupShowsTheFoldersOfItsOwnFilesAndEmptyOnesButNotTheScenes() = runTest {
        val context = context(sounds = listOf("files/sounds/hit.wav", "files/audio/both.wav"), music = listOf("files/music/theme.mp3", "files/audio/both.mp3"))
        context.directory("$common/files/empty").mkdirs()
        context.directory("$common/files/scenes/levels").mkdirs()
        context.directory("$desktop/files/sounds/desktop-only").mkdirs()
        assertEquals(setOf("audio", "empty", "sounds", "sounds/desktop-only"), context.foldersOf(FolderGroup.SOUNDS))
        assertEquals(setOf("audio", "empty", "music"), context.foldersOf(FolderGroup.MUSIC))
        assertEquals(listOf("files/sounds/hit.wav" to "sounds", "files/audio/both.wav" to "audio"), context.itemsOf(FolderGroup.SOUNDS))
    }

    @Test
    fun renamingAFolderMovesEverySourceSetsFilesAndWhatPlaysThemFollowsUntilUndone() = runTest {
        val context = context(player, sounds = listOf("files/sounds/enemies/hit.wav", "files/sounds/enemies/deep/roar.wav"), music = listOf("files/sounds/enemies/boss.mp3"))
        context.directory("$desktop/files/sounds/enemies").mkdirs()
        File(context.directory("$desktop/files/sounds/enemies"), "hit.wav").writeBytes(wav)
        context.directory("$common/files/sounds/enemies/empty").mkdirs()
        val after = context.assertRoundTrip(Invocation(RenameFolder, RenameFolder.Input(FolderGroup.SOUNDS, "sounds/enemies", "foes"))) { context.files() to context.blockTypes!!.types.value }
        val files = after.first.keys
        assertTrue("$common/files/sounds/foes/hit.wav" in files)
        assertTrue("$common/files/sounds/foes/deep/roar.wav" in files)
        assertTrue("$common/files/sounds/foes/boss.mp3" in files)
        assertTrue("$common/files/sounds/foes/empty" in files)
        assertTrue("$desktop/files/sounds/foes/hit.wav" in files)
        assertTrue(files.none { "enemies" in it })
        val blocks = (after.second.single() as ActorTypeDefinition).events.single().blocks
        assertEquals(listOf(BlockDefinition.PlaySound("files/sounds/foes/hit.wav"), BlockDefinition.PlayMusic("files/sounds/foes/boss.mp3")), blocks)
    }

    @Test
    fun deletingAFolderDeletesItsSoundsAndMusicAndListsThemUntilUndone() = runTest {
        val context = context(player, sounds = listOf("files/sounds/enemies/hit.wav", "files/sounds/keep.wav"), music = listOf("files/sounds/enemies/boss.mp3"))
        val usages = context.usagesOf(RelationSubject.Folder(FolderGroup.SOUNDS, "sounds/enemies")).map { it.toString() }
        assertTrue(usages.any { it.startsWith("files/sounds/enemies/hit.wav is in folder sounds/enemies") }, usages.toString())
        assertTrue(usages.any { it.startsWith("files/sounds/enemies/boss.mp3 is in folder sounds/enemies") }, usages.toString())
        val after = context.assertRoundTrip(Invocation(DeleteFolder, DeleteFolder.Input(FolderGroup.SOUNDS, "sounds/enemies"))) { context.files() to context.blockTypes!!.types.value }
        assertEquals(setOf("$common/files/sounds/keep.wav"), after.first.keys.filter { it.startsWith("$common/files") }.toSet())
        assertEquals(emptyList(), (after.second.single() as ActorTypeDefinition).events.single().blocks)
    }

    @Test
    fun anEmptyFolderHoldingAFileNothingListsKeepsItsDirectory() = runTest {
        val context = context(sounds = listOf("files/sounds/hit.wav"))
        val folder = context.directory("$common/files/sounds/empty").apply { mkdirs() }
        File(folder, ".DS_Store").writeText("")
        DeleteFolder.apply(context, DeleteFolder.Input(FolderGroup.SOUNDS, "sounds/empty", keepsContents = true))
        assertTrue(folder.isDirectory)
        File(folder, ".DS_Store").delete()
        DeleteFolder.apply(context, DeleteFolder.Input(FolderGroup.SOUNDS, "sounds/empty", keepsContents = true))
        assertFalse(folder.exists())
    }

    @Test
    fun aFolderCalledLikeABuildFolderIsMovedAndKeptLikeAnyOther() = runTest {
        val context = context(sounds = listOf("files/sfx/build/hit.wav"))
        context.directory("$common/files/empty/out").mkdirs()
        File(context.directory("$common/files/empty/out"), "data.json").writeText("{}")
        assertTrue("sfx/build" in context.foldersOf(FolderGroup.SOUNDS))
        assertNull(RenameFolder.apply(context, RenameFolder.Input(FolderGroup.SOUNDS, "sfx", "fx")).failure)
        assertTrue(context.directory("$common/files/fx/build/hit.wav").isFile)
        DeleteFolder.apply(context, DeleteFolder.Input(FolderGroup.SOUNDS, "empty", keepsContents = true))
        assertTrue(context.directory("$common/files/empty/out/data.json").isFile)
    }

    @Test
    fun aFolderTheGroupDoesNotShowIsStillTaken() = runTest {
        val context = context(sounds = listOf("files/sfx/hit.wav"), music = listOf("files/music/theme.mp3"))
        assertEquals(Rejection(CommandProblem.FOLDER_EXISTS, "music"), RenameFolder.validate(context, RenameFolder.Input(FolderGroup.SOUNDS, "sfx", "music")))
        assertEquals(Rejection(CommandProblem.FOLDER_EXISTS, "Music"), CreateFolder.validate(context, CreateFolder.Input(FolderGroup.SOUNDS, "Music")))
    }

    @Test
    fun theScenesFolderIsReservedInAnyCapitals() = runTest {
        val context = context(sounds = listOf("files/sfx/hit.wav"))
        assertEquals(Rejection(CommandProblem.AUDIO_FOLDER_RESERVED, "Scenes"), CreateFolder.validate(context, CreateFolder.Input(FolderGroup.SOUNDS, "Scenes")))
        assertEquals(Rejection(CommandProblem.AUDIO_FOLDER_RESERVED, "SCENES"), RenameFolder.validate(context, RenameFolder.Input(FolderGroup.SOUNDS, "sfx", "SCENES")))
    }
}
