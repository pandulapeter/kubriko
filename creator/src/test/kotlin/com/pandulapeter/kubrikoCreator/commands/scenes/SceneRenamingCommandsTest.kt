/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.commands.scenes

import com.pandulapeter.kubrikoCreator.actors.ActorTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.BlockDefinition
import com.pandulapeter.kubrikoCreator.actors.EventDefinition
import com.pandulapeter.kubrikoCreator.actors.EventType
import com.pandulapeter.kubrikoCreator.commands.CommandContext
import com.pandulapeter.kubrikoCreator.commands.CommandProblem
import com.pandulapeter.kubrikoCreator.commands.Invocation
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.commands.assertRoundTrip
import com.pandulapeter.kubrikoCreator.commands.commandContext
import com.pandulapeter.kubrikoCreator.commands.folders.CreateFolder
import com.pandulapeter.kubrikoCreator.commands.folders.DeleteFolder
import com.pandulapeter.kubrikoCreator.commands.folders.FolderGroup
import com.pandulapeter.kubrikoCreator.commands.folders.RenameFolder
import com.pandulapeter.kubrikoCreator.commands.folders.foldersOf
import com.pandulapeter.kubrikoCreator.console.ConsoleEntry
import com.pandulapeter.kubrikoCreator.foundation.EditorMutation
import com.pandulapeter.kubrikoCreator.scenes.SceneChange
import com.pandulapeter.kubrikoCreator.testing.testDirectory
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assume.assumeFalse
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

class SceneRenamingCommandsTest {
    private val door = ActorTypeDefinition(name = "Door", events = listOf(EventDefinition(EventType.ADDED_TO_SCENE, blocks = listOf(BlockDefinition.GoToScene("main")))))
    private val reports = mutableListOf<ConsoleEntry>()

    @OptIn(EditorMutation::class)
    private fun TestScope.context(): CommandContext {
        val root = testDirectory()
        File(root, "src/commonMain/kotlin/Game.kt").apply { parentFile.mkdirs() }.writeText("""createManagers(firstScene = "main")""")
        return commandContext(door, rootDirectory = root, report = { reports += it }).apply {
            scenes.write("main", "[]")
            scenes.write("level-2", "[]")
            scenes.takeChanges()
        }
    }

    private fun CommandContext.renames() = scenes.takeChanges().filterIsInstance<SceneChange.Moved>().map { it.oldName to it.newName }

    private fun CommandContext.goToScene() = (blockTypes!!.actorType("Door")!!.events.single().blocks.single() as BlockDefinition.GoToScene).scene

    @Test
    fun aRenamedSceneTakesItsGoToSceneBlocksAlongAndSaysWhereCodeStillNamesIt() = runTest {
        val context = context()
        val moved = context.assertRoundTrip(Invocation(RenameScene, RenameScene.Input("main", "levels/1-1"))) { context.scenes.names() to context.goToScene() }
        assertEquals(listOf("level-2", "levels/1-1") to "levels/1-1", moved)
        assertEquals(listOf("main" to "levels/1-1", "levels/1-1" to "main", "main" to "levels/1-1"), context.renames())
        assertEquals(
            ConsoleEntry.Status(CommandProblem.SCENE_NAMED_IN_CODE, isError = true, arguments = listOf("src/commonMain/kotlin/Game.kt", "main", "levels/1-1")),
            reports.first(),
        )
        assertEquals(Rejection(CommandProblem.SCENE_NAME_TAKEN), RenameScene.validate(context, RenameScene.Input("level-2", "Levels/1-1")))
        assertEquals(Rejection(CommandProblem.SCENE_NAME_INVALID), RenameScene.validate(context, RenameScene.Input("level-2", "levels//2")))
        assertEquals(Rejection(CommandProblem.SCENE_MISSING, "ghost"), RenameScene.validate(context, RenameScene.Input("ghost", "x")))
        assertEquals(Rejection(CommandProblem.SCENE_FOLDER_RESERVED, "node_modules"), RenameScene.validate(context, RenameScene.Input("level-2", "node_modules/2")))
    }

    @Test
    fun sceneFoldersAreCreatedRenamedWithTheirScenesAndDeletedWhenEmpty() = runTest {
        val context = context()
        context.assertRoundTrip(Invocation(CreateFolder, CreateFolder.Input(FolderGroup.SCENES, "levels"))) { context.foldersOf(FolderGroup.SCENES) }
        assertEquals(Rejection(CommandProblem.SCENE_NAME_INVALID), CreateFolder.validate(context, CreateFolder.Input(FolderGroup.SCENES, "two words")))
        assertEquals(Rejection(CommandProblem.SCENE_FOLDER_RESERVED, "build"), CreateFolder.validate(context, CreateFolder.Input(FolderGroup.SCENES, "levels/build")))
        RenameScene.apply(context, RenameScene.Input("level-2", "levels/2"))
        assertEquals(Rejection(CommandProblem.FOLDER_NOT_EMPTY, "levels"), DeleteFolder.validate(context, DeleteFolder.Input(FolderGroup.SCENES, "levels")))
        val renamed = context.assertRoundTrip(Invocation(RenameFolder, RenameFolder.Input(FolderGroup.SCENES, "levels", "world"))) { context.scenes.names() }
        assertEquals(listOf("main", "world/2"), renamed)
    }

    @Test
    fun aSceneThatCantBeMovedIsRenamedNowhereAndSaysSo() = runTest {
        val context = context()
        val folder = context.scenes.fileOf("main").parentFile
        folder.setWritable(false)
        try {
            assumeFalse(folder.canWrite())
            val applied = RenameScene.apply(context, RenameScene.Input("main", "start"))
            assertEquals(Rejection(CommandProblem.SCENE_NOT_MOVED, "main"), applied.failure)
            assertEquals(listOf("level-2", "main"), context.scenes.names())
            assertEquals("main", context.goToScene())
            assertEquals(emptyList(), context.renames())
        } finally {
            folder.setWritable(true)
        }
    }

    @Test
    fun aFolderWhoseSceneCantBeMovedMovesTheOthersBack() = runTest {
        val context = context()
        RenameScene.apply(context, RenameScene.Input("main", "levels/1"))
        RenameScene.apply(context, RenameScene.Input("level-2", "levels/deep/2"))
        val deep = context.scenes.fileOf("levels/deep/2").parentFile
        deep.setWritable(false)
        try {
            assumeFalse(deep.canWrite())
            val applied = RenameFolder.apply(context, RenameFolder.Input(FolderGroup.SCENES, "levels", "world"))
            assertEquals(Rejection(CommandProblem.SCENE_NOT_MOVED, "levels/deep/2"), applied.failure)
            assertEquals(listOf("levels/1", "levels/deep/2"), context.scenes.names())
            assertEquals("levels/1", context.goToScene())
            assertEquals(listOf("levels", "levels/deep"), context.foldersOf(FolderGroup.SCENES).sorted())
        } finally {
            deep.setWritable(true)
        }
    }
}
