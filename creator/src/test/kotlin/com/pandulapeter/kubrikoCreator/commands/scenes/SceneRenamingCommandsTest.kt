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
import com.pandulapeter.kubrikoCreator.commands.EditorMutation
import com.pandulapeter.kubrikoCreator.commands.Invocation
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.commands.assertRoundTrip
import com.pandulapeter.kubrikoCreator.commands.commandContext
import com.pandulapeter.kubrikoCreator.commands.folders.CreateFolder
import com.pandulapeter.kubrikoCreator.commands.folders.DeleteFolder
import com.pandulapeter.kubrikoCreator.commands.folders.FolderGroup
import com.pandulapeter.kubrikoCreator.commands.folders.RenameFolder
import com.pandulapeter.kubrikoCreator.commands.folders.foldersOf
import com.pandulapeter.kubrikoCreator.workspace.ConsoleEntry
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.error_command_scene_missing
import kubriko.creator.generated.resources.error_folder_not_empty
import kubriko.creator.generated.resources.error_scene_name_invalid
import kubriko.creator.generated.resources.error_scene_name_taken
import kubriko.creator.generated.resources.warning_scene_named_in_code
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals

class SceneRenamingCommandsTest {
    private val door = ActorTypeDefinition(name = "Door", events = listOf(EventDefinition(EventType.ADDED_TO_SCENE, blocks = listOf(BlockDefinition.GoToScene("main")))))
    private val renames = mutableListOf<Pair<String, String>>()
    private val reports = mutableListOf<ConsoleEntry>()

    @OptIn(EditorMutation::class)
    private fun TestScope.context(): CommandContext {
        val root = createTempDirectory().toFile()
        File(root, "src/commonMain/kotlin/Game.kt").apply { parentFile.mkdirs() }.writeText("""createManagers(firstScene = "main")""")
        return commandContext(door, rootDirectory = root, onSceneRenamed = { old, new -> renames += old to new }, report = { reports += it }).apply {
            scenes.write("main", "[]")
            scenes.write("level-2", "[]")
        }
    }

    private fun CommandContext.goToScene() = (blockTypes!!.actorType("Door")!!.events.single().blocks.single() as BlockDefinition.GoToScene).scene

    @Test
    fun aRenamedSceneTakesItsGoToSceneBlocksAlongAndSaysWhereCodeStillNamesIt() = runTest {
        val context = context()
        val moved = context.assertRoundTrip(Invocation(RenameScene, RenameScene.Input("main", "levels/1-1"))) { context.scenes.names() to context.goToScene() }
        assertEquals(listOf("level-2", "levels/1-1") to "levels/1-1", moved)
        assertEquals(listOf("main" to "levels/1-1", "levels/1-1" to "main", "main" to "levels/1-1"), renames)
        assertEquals(
            ConsoleEntry.Status(Res.string.warning_scene_named_in_code, isError = true, arguments = listOf("src/commonMain/kotlin/Game.kt", "main", "levels/1-1")),
            reports.first(),
        )
        assertEquals(Rejection(Res.string.error_scene_name_taken), RenameScene.validate(context, RenameScene.Input("level-2", "Levels/1-1")))
        assertEquals(Rejection(Res.string.error_scene_name_invalid), RenameScene.validate(context, RenameScene.Input("level-2", "levels//2")))
        assertEquals(Rejection(Res.string.error_command_scene_missing, "ghost"), RenameScene.validate(context, RenameScene.Input("ghost", "x")))
    }

    @Test
    fun sceneFoldersAreCreatedRenamedWithTheirScenesAndDeletedWhenEmpty() = runTest {
        val context = context()
        context.assertRoundTrip(Invocation(CreateFolder, CreateFolder.Input(FolderGroup.SCENES, "levels"))) { context.foldersOf(FolderGroup.SCENES) }
        assertEquals(Rejection(Res.string.error_scene_name_invalid), CreateFolder.validate(context, CreateFolder.Input(FolderGroup.SCENES, "two words")))
        RenameScene.apply(context, RenameScene.Input("level-2", "levels/2"))
        assertEquals(Rejection(Res.string.error_folder_not_empty, "levels"), DeleteFolder.validate(context, DeleteFolder.Input(FolderGroup.SCENES, "levels")))
        val renamed = context.assertRoundTrip(Invocation(RenameFolder, RenameFolder.Input(FolderGroup.SCENES, "levels", "world"))) { context.scenes.names() }
        assertEquals(listOf("main", "world/2"), renamed)
    }
}
