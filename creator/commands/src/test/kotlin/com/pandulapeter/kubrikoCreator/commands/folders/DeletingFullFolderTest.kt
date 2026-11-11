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
import com.pandulapeter.kubrikoCreator.actors.BlockDefinition
import com.pandulapeter.kubrikoCreator.actors.BlockTypeKind
import com.pandulapeter.kubrikoCreator.actors.EventDefinition
import com.pandulapeter.kubrikoCreator.actors.EventType
import com.pandulapeter.kubrikoCreator.commands.CommandContext
import com.pandulapeter.kubrikoCreator.commands.CommandExecutor
import com.pandulapeter.kubrikoCreator.commands.CommandOutcome
import com.pandulapeter.kubrikoCreator.commands.CommandProblem
import com.pandulapeter.kubrikoCreator.commands.Invocation
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.commands.assertRoundTrip
import com.pandulapeter.kubrikoCreator.commands.commandContext
import com.pandulapeter.kubrikoCreator.commands.project.CheckChange
import com.pandulapeter.kubrikoCreator.commands.screenplay.CreatePage
import com.pandulapeter.kubrikoCreator.commands.testScope
import com.pandulapeter.kubrikoCreator.commands.usagesOf
import com.pandulapeter.kubrikoCreator.context.RelationSubject
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class DeletingFullFolderTest {
    private val monster = ActorTypeDefinition(name = "Monster", folder = "enemies")
    private val bat = ActorTypeDefinition(
        name = "Bat",
        parent = "Monster",
        folder = "enemies/flying",
        events = listOf(EventDefinition(EventType.EVERY_FRAME, blocks = listOf(BlockDefinition.AddActor("Monster")))),
    )
    private val ball = ActorTypeDefinition(
        name = "Ball",
        events = listOf(
            EventDefinition(
                EventType.EVERY_FRAME,
                blocks = listOf(BlockDefinition.AddActor("Bat"), BlockDefinition.Stop, BlockDefinition.Kotlin("println(Bat::class)"), BlockDefinition.GoToScene("levels/1")),
            ),
            EventDefinition(EventType.COLLISION, otherActorType = "Monster"),
        ),
    )

    private fun CommandContext.writeScene(name: String, text: String = """[{"typeId":"Bat","state":"{}"},{"typeId":"Ball","state":"{}"}]""") {
        File(project.rootDirectory, "src/commonMain/composeResources/files/scenes/$name.json").apply { parentFile.mkdirs() }.writeText(text)
        scenes.refresh()
    }

    private fun CommandContext.startingWith(firstScene: String) {
        File(project.rootDirectory, "build.gradle.kts").writeText("val gameFirstScene = providers.gradleProperty(\"game.firstScene\")\n")
        File(project.rootDirectory, "gradle.properties").writeText("game.name=Game\ngame.firstScene=$firstScene\n")
    }

    private fun CommandContext.files() = project.rootDirectory.walkTopDown().filter { it.isFile }.associate { it.relativeTo(project.rootDirectory).invariantSeparatorsPath to it.readText() }

    private fun CommandContext.snapshot() = Triple(blockTypes!!.types.value, files(), blockTypes!!.folders.value[BlockTypeKind.ACTOR].orEmpty())

    private fun deleting(group: FolderGroup, path: String) = Invocation(DeleteFolder, DeleteFolder.Input(group, path))

    @Test
    fun aFolderOfActorTypesGoesWithItsFoldersAndWhatRefersToItAndIsUndoneByteForByte() = runTest {
        val context = commandContext(monster, bat, ball)
        context.writeScene("level")
        val after = context.assertRoundTrip(deleting(FolderGroup.ACTOR_TYPES, "enemies")) { context.snapshot() }
        val ballAfter = after.first.single()
        assertEquals(listOf(BlockDefinition.Stop, BlockDefinition.Kotlin("println(Bat::class)"), BlockDefinition.GoToScene("levels/1")), ballAfter.events.single().blocks)
        assertEquals(emptySet(), after.third)
        DeleteFolder.apply(context, DeleteFolder.Input(FolderGroup.ACTOR_TYPES, "enemies"))
        assertEquals(listOf("Ball"), context.scenes.actors("level")!!.map { it.typeId })
    }

    @Test
    fun theUsagesListWhatIsInTheFolderAndWhatOutsideItRefersToItButNotWhatGoesWithIt() = runTest {
        val context = commandContext(monster, bat, ball)
        context.writeScene("level")
        val expected = listOf(
            "Bat is in folder enemies: it is deleted with the folder.",
            "Monster is in folder enemies: it is deleted with the folder.",
            "Ball#0.0 adds Bat: the Block is removed.",
            "Ball#0.2 names in Kotlin Bat: left as it is, so the build fails until it is changed.",
            "Scene level places Bat: its Actors are removed.",
            "Ball#1 collides with Monster: the Event is removed with its Blocks.",
        )
        assertEquals(expected, context.usagesOf(RelationSubject.Folder(FolderGroup.ACTOR_TYPES, "enemies")).map { it.toString() })
        val check = CheckChange.read(context, CheckChange.Input("delete_folder", buildJsonObject { put("group", "actorTypes"); put("path", "enemies") }))
        val text = assertIs<CommandOutcome.Success>(check).result.jsonPrimitive.content
        expected.forEach { assertTrue(it in text, "check_change misses $it") }
        val executor = CommandExecutor(context = context, scope = testScope())
        val result = assertIs<CommandOutcome.Success>(executor.call("delete_folder", buildJsonObject { put("group", "actorTypes"); put("path", "enemies") })).result.jsonObject
        assertEquals(expected.map { it.replace("Ball#0.2 names", "Ball#0.1 names") }, result.getValue("usages").jsonArray.map { it.jsonPrimitive.content })
    }

    @Test
    fun aFolderWhoseActorTypeHasAChildOutsideItIsRefused() = runTest {
        val context = commandContext(monster, bat.copy(folder = ""), ball)
        assertEquals(Rejection(CommandProblem.ACTOR_TYPE_HAS_CHILDREN, "Monster", "Bat"), DeleteFolder.validate(context, DeleteFolder.Input(FolderGroup.ACTOR_TYPES, "enemies")))
        assertEquals(Rejection(CommandProblem.FOLDER_TOP), DeleteFolder.validate(context, DeleteFolder.Input(FolderGroup.SCENES, "")))
    }

    @Test
    fun aFolderOfScenesGoesWithItsFoldersAndTheBlocksGoingToThem() = runTest {
        val context = commandContext(ball)
        listOf("levels/1", "levels/deep/2", "main").forEach { context.writeScene(it, "[]") }
        val after = context.assertRoundTrip(deleting(FolderGroup.SCENES, "levels")) {
            Triple(context.blockTypes!!.types.value, context.files(), context.foldersOf(FolderGroup.SCENES))
        }
        assertEquals(listOf("main"), context.scenes.names())
        assertEquals(emptySet(), after.third)
        assertTrue(BlockDefinition.GoToScene("levels/1") !in after.first.single().events.first().blocks)
    }

    @Test
    fun aFolderHoldingTheFirstSceneIsntDeletedAndLeavesEverythingAsItWas() = runTest {
        val context = commandContext(ball)
        listOf("levels/1", "levels/deep/2", "main").forEach { context.writeScene(it, "[]") }
        context.startingWith("levels/deep/2")
        assertTrue("The game's settings starts with Scene levels/deep/2: prevents deleting it." in context.usagesOf(RelationSubject.Folder(FolderGroup.SCENES, "levels")).map { it.toString() })
        val before = context.blockTypes!!.types.value to context.files()
        assertEquals(Rejection(CommandProblem.SCENE_IS_FIRST, "levels/deep/2"), DeleteFolder.apply(context, DeleteFolder.Input(FolderGroup.SCENES, "levels")).failure)
        assertEquals(before, context.blockTypes!!.types.value to context.files())
        assertEquals(listOf("levels/1", "levels/deep/2", "main"), context.scenes.names())
    }

    @Test
    fun aFolderOfPagesGoesWithItsImagesAndTheMentionsOfItsPagesBecomeText() = runTest {
        val context = commandContext()
        CreatePage.apply(context, CreatePage.Input("levels/cave", "# Cave\n\nDark.\n\n![Bats](cave-1.png)\n"))
        CreatePage.apply(context, CreatePage.Input("levels/deep/pit", "# Pit\n\nDeep.\n"))
        CreatePage.apply(context, CreatePage.Input("intro", "# Intro\n\nStarts in the [cave](levels/cave.md).\n"))
        File(context.project.rootDirectory, "screenplay/levels/cave-1.png").writeText("png")
        val after = context.assertRoundTrip(deleting(FolderGroup.PAGES, "levels")) { context.files() to context.screenplay.scanFolders() }
        assertEquals(mapOf("screenplay/intro.md" to "# Intro\n\nStarts in the cave.\n"), after.first)
        assertEquals(emptySet(), after.second)
    }

    @Test
    fun aFolderLeftOnDiskByAFileNothingListsIsStillUndone() = runTest {
        val context = commandContext(ball)
        listOf("levels/1", "levels/deep/2", "main").forEach { context.writeScene(it, "[]") }
        File(context.project.rootDirectory, "src/commonMain/composeResources/files/scenes/levels/.DS_Store").writeText("")
        val after = context.assertRoundTrip(deleting(FolderGroup.SCENES, "levels")) { context.scenes.names() }
        assertEquals(listOf("main"), after)
    }

    @Test
    fun aSceneDeletedOutsideCreatorJustBeforeIsntInTheWay() = runTest {
        val context = commandContext(ball)
        listOf("levels/1", "levels/deep/2", "main").forEach { context.writeScene(it, "[]") }
        File(context.project.rootDirectory, "src/commonMain/composeResources/files/scenes/levels/1.json").delete()
        assertEquals(null, DeleteFolder.apply(context, DeleteFolder.Input(FolderGroup.SCENES, "levels")).failure)
        assertEquals(listOf("main"), context.scenes.names())
    }

    @Test
    fun anEmptyFolderIsDeletedWithTheFoldersInIt() = runTest {
        val context = commandContext(ball)
        CreateFolder.apply(context, CreateFolder.Input(FolderGroup.ACTOR_TYPES, "props"))
        CreateFolder.apply(context, CreateFolder.Input(FolderGroup.ACTOR_TYPES, "props/walls"))
        val after = context.assertRoundTrip(deleting(FolderGroup.ACTOR_TYPES, "props")) { context.blockTypes!!.folders.value[BlockTypeKind.ACTOR].orEmpty() }
        assertEquals(emptySet(), after)
    }
}
