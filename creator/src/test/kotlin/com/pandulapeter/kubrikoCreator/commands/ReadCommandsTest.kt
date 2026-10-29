/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.commands

import com.pandulapeter.kubrikoCreator.actors.ActorTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.BlockDefinition
import com.pandulapeter.kubrikoCreator.actors.BlockTypeKind
import com.pandulapeter.kubrikoCreator.actors.EventDefinition
import com.pandulapeter.kubrikoCreator.actors.EventType
import com.pandulapeter.kubrikoCreator.actors.ManagerTypeDefinition
import com.pandulapeter.kubrikoCreator.commands.actorTypes.ListActorTypes
import com.pandulapeter.kubrikoCreator.commands.scenes.ReadScene
import com.pandulapeter.kubrikoCreator.git.ChangeKind
import com.pandulapeter.kubrikoCreator.git.FakeGitRepository
import com.pandulapeter.kubrikoCreator.git.FileChange
import com.pandulapeter.kubrikoCreator.git.RepositoryStatus
import com.pandulapeter.kubrikoCreator.scenes.SceneActorEntry
import com.pandulapeter.kubrikoCreator.scenes.SceneDocument
import com.pandulapeter.kubrikoCreator.workspace.ConsoleEntry
import com.pandulapeter.kubrikoCreator.workspace.FakeBlockTypeRepository
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.error_command_actor_type_missing
import kubriko.creator.generated.resources.error_command_no_actor_types
import kubriko.creator.generated.resources.error_command_page_invalid
import kubriko.creator.generated.resources.error_command_scene_actor_missing
import kubriko.creator.generated.resources.error_command_scene_missing
import kubriko.creator.generated.resources.error_folder_missing
import kubriko.creator.generated.resources.status_scene_opened
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ReadCommandsTest {
    private val ball = ActorTypeDefinition(name = "Ball", color = 0xFF112233, events = listOf(EventDefinition(EventType.EDGE_TOUCHED, blocks = listOf(BlockDefinition.BounceOffEdge))))
    private val scene = """[{"typeId":"Ball","state":"{\"body\":{\"position\":{\"x\":1.0,\"y\":2.0}}}"},{"typeId":"Music","state":"{}"}]"""

    private suspend fun CommandExecutor.read(name: String, arguments: JsonObject = JsonObject(emptyMap())): JsonElement = assertIs<CommandOutcome.Success>(call(name, arguments)).result

    private fun TestScope.executor(context: CommandContext = commandContext(ball)) = CommandExecutor(context, testScope())

    @Test
    fun readingChangesNothingAndIsNotRecorded() = runTest {
        val repository = FakeBlockTypeRepository(listOf(ball))
        val executor = executor(commandContext(blockTypes = blockTypesController(repository = repository)))
        CommandRegistry.editor.commands.filterIsInstance<ReadCommand<*>>().forEach { executor.call(it.name, JsonObject(emptyMap())) }
        assertEquals(listOf("regenerate"), repository.writes)
        assertEquals(CommandHistory(), executor.history.value)
    }

    @Test
    fun theOverviewNamesTheTypesAndScenes() = runTest {
        val context = commandContext(ball, ManagerTypeDefinition("Game"))
        @OptIn(EditorMutation::class)
        context.scenes.write("main", "[]")
        assertEquals(
            Json.parseToJsonElement("""{"name":"game","usesBlockBuiltActorTypes":true,"actorTypes":["Ball"],"managerTypes":["Game"],"scenes":["main"],"needsUpgrade":false}"""),
            executor(context).read("get_project_overview"),
        )
        assertEquals(
            Json.parseToJsonElement("""{"name":"game","usesBlockBuiltActorTypes":false,"scenes":[],"needsUpgrade":false}"""),
            executor(commandContext(blockTypes = null)).read("get_project_overview"),
        )
    }

    @Test
    fun theBlockCatalogSaysWhichEventsAllowWhichBlocks() = runTest {
        val catalog = executor().read("get_block_catalog").jsonObject
        val edge = catalog.getValue("events").jsonArray.map { it.jsonObject }.single { it.getValue("type").jsonPrimitive.content == "edgeTouched" }
        assertTrue("bounceOffEdge" in edge.getValue("blocks").jsonArray.map { it.jsonPrimitive.content })
        val removeOther = catalog.getValue("blocks").jsonArray.map { it.jsonObject }.single { it.getValue("type").jsonPrimitive.content == "removeOther" }
        assertEquals(listOf("collision"), removeOther.getValue("allowedIn").jsonArray.map { it.jsonPrimitive.content })
        assertTrue("description" in removeOther.getValue("schema").jsonObject)
        assertEquals(EventType.entries.size, catalog.getValue("events").jsonArray.size)
    }

    @Test
    fun actorTypesAreListedAndReadAsTheirDefinitions() = runTest {
        val executor = executor()
        assertEquals(
            Json.parseToJsonElement("""{"total":1,"actorTypes":[{"name":"Ball","events":["edgeTouched"],"isChangedByHand":false}],"folders":[]}"""),
            executor.read("list_actor_types"),
        )
        val definition = executor.read("read_actor_type", buildJsonObject { put("actorType", "Ball") }).jsonObject
        assertEquals("#112233", definition.getValue("color").jsonPrimitive.content)
        assertEquals("bounceOffEdge", definition.getValue("events").jsonArray[0].jsonObject.getValue("blocks").jsonArray[0].jsonObject.getValue("type").jsonPrimitive.content)
        assertEquals(
            CommandOutcome.Rejected(Rejection(Res.string.error_command_actor_type_missing, "Wall")),
            executor.call("read_actor_type", buildJsonObject { put("actorType", "Wall") }),
        )
        assertEquals(CommandOutcome.Rejected(Rejection(Res.string.error_command_no_actor_types)), executor(commandContext(blockTypes = null)).call("list_actor_types", JsonObject(emptyMap())))
    }

    @Test
    fun scenesAreListedAndTheirActorsReadWithTheirPositions() = runTest {
        val context = commandContext(ball)
        @OptIn(EditorMutation::class)
        context.scenes.write("main", scene)
        @OptIn(EditorMutation::class)
        context.scenes.write("broken", "nope")
        val executor = executor(context)
        assertEquals(Json.parseToJsonElement("""[{"name":"broken","isValid":false},{"name":"main","actorCount":2}]"""), executor.read("list_scenes"))
        val read = executor.read("read_scene", buildJsonObject { put("scene", "main") }).jsonObject
        assertEquals(
            Json.parseToJsonElement("""[{"actorId":"i0","index":0,"typeId":"Ball","x":1.0,"y":2.0,"state":"{\"body\":{\"position\":{\"x\":1.0,\"y\":2.0}}}"},{"actorId":"i1","index":1,"typeId":"Music","state":"{}"}]"""),
            read.getValue("actors"),
        )
        assertEquals(SceneDocument.format(SceneDocument.parse(scene)), read.getValue("content").jsonPrimitive.content)
        assertEquals(2, read.getValue("total").jsonPrimitive.int)
        assertEquals(CommandOutcome.Rejected(Rejection(Res.string.error_command_scene_missing, "other")), executor.call("read_scene", buildJsonObject { put("scene", "other") }))
    }

    @Test
    fun largeScenesAreReadAPageAtATimeFilteredOrSummarized() = runTest {
        val context = commandContext(ball)
        val bricks = List(250) { SceneActorEntry("Brick", SceneDocument.blockBuiltState(it % 10 * 10f, it / 10 * 10f, 8f, 8f), "b$it") }
        @OptIn(EditorMutation::class)
        context.scenes.write("main", SceneDocument.format(bricks + SceneActorEntry("Ball", SceneDocument.blockBuiltState(-5f, 300f, 4f, 4f), "ball") + SceneActorEntry("Music", "{}", "music")))
        val executor = executor(context)
        suspend fun read(arguments: JsonObjectBuilder.() -> Unit) = executor.read("read_scene", buildJsonObject { put("scene", "main"); arguments() }).jsonObject
        fun JsonObject.ids() = getValue("actors").jsonArray.map { it.jsonObject.getValue("actorId").jsonPrimitive.content }

        val first = read {}
        assertEquals(252, first.getValue("total").jsonPrimitive.int)
        assertEquals(ReadScene.DEFAULT_PAGE_SIZE, first.ids().size)
        assertEquals(ReadScene.DEFAULT_PAGE_SIZE, first.getValue("nextOffset").jsonPrimitive.int)
        assertFalse("content" in first)
        val last = read { put("offset", 240) }
        assertEquals((240 until 250).map { "b$it" } + listOf("ball", "music"), last.ids())
        assertEquals(251, last.getValue("actors").jsonArray[11].jsonObject.getValue("index").jsonPrimitive.int)
        assertFalse("nextOffset" in last)
        assertEquals(emptyList(), read { put("offset", 300) }.ids())

        assertEquals(listOf("ball"), read { put("actorType", "Ball") }.ids())
        val rectangle = read { put("left", 15f); put("top", 0f); put("right", 30f); put("bottom", 10f) }
        assertEquals(listOf("b2", "b3", "b12", "b13"), rectangle.ids())
        assertEquals(listOf("b0", "b10"), read { put("right", 0f); put("top", 0f); put("bottom", 10f); put("count", 5) }.ids())

        assertEquals(
            Json.parseToJsonElement("""{"total":252,"actorTypes":[{"typeId":"Ball","count":1},{"typeId":"Brick","count":250},{"typeId":"Music","count":1}],"bounds":{"left":-5.0,"top":0.0,"right":90.0,"bottom":300.0}}"""),
            read { put("summary", true) },
        )
        assertEquals(Json.parseToJsonElement("""{"total":1,"actorTypes":[{"typeId":"Music","count":1}]}"""), read { put("summary", true); put("actorType", "Music") })
        assertEquals(CommandOutcome.Rejected(Rejection(Res.string.error_command_page_invalid)), executor.call("read_scene", buildJsonObject { put("scene", "main"); put("count", 0) }))
        assertEquals(CommandOutcome.Rejected(Rejection(Res.string.error_command_page_invalid)), executor.call("read_scene", buildJsonObject { put("scene", "main"); put("offset", -1) }))
    }

    @Test
    fun actorTypesAreListedByFolderAPageAtATime() = runTest {
        val types = List(150) { ActorTypeDefinition(name = "Enemy$it", folder = if (it < 120) "enemies/flying" else "enemies") } +
            ActorTypeDefinition(name = "Ball") +
            ActorTypeDefinition(name = "Bat", parent = "Enemy0", folder = "enemies/flying")
        val repository = FakeBlockTypeRepository(types).apply { folders[BlockTypeKind.ACTOR] = mutableSetOf("empty") }
        val executor = executor(commandContext(blockTypes = blockTypesController(repository = repository)))
        suspend fun list(arguments: JsonObjectBuilder.() -> Unit) = executor.read("list_actor_types", buildJsonObject(arguments)).jsonObject
        fun JsonObject.names() = getValue("actorTypes").jsonArray.map { it.jsonObject.getValue("name").jsonPrimitive.content }

        val all = list {}
        assertEquals(152, all.getValue("total").jsonPrimitive.int)
        assertEquals(ListActorTypes.DEFAULT_PAGE_SIZE, all.names().size)
        assertEquals(listOf("empty", "enemies", "enemies/flying"), all.getValue("folders").jsonArray.map { it.jsonPrimitive.content })
        val flying = list { put("folder", "enemies/flying") }
        assertEquals(121, flying.getValue("total").jsonPrimitive.int)
        assertEquals(emptyList(), flying.getValue("folders").jsonArray)
        val bat = list { put("folder", "enemies/flying"); put("count", 1) }.getValue("actorTypes").jsonArray.single().jsonObject
        assertEquals(Json.parseToJsonElement("""{"name":"Bat","folder":"enemies/flying","parent":"Enemy0","events":[],"isChangedByHand":false}"""), bat)
        val enemies = list { put("folder", "enemies"); put("count", 200) }
        assertEquals(150 + 1, enemies.names().size)
        assertEquals(listOf("enemies/flying"), enemies.getValue("folders").jsonArray.map { it.jsonPrimitive.content })
        assertEquals(CommandOutcome.Rejected(Rejection(Res.string.error_folder_missing, "ghosts")), executor.call("list_actor_types", buildJsonObject { put("folder", "ghosts") }))
        assertEquals(CommandOutcome.Rejected(Rejection(Res.string.error_command_page_invalid)), executor.call("list_actor_types", buildJsonObject { put("count", 0) }))
    }

    @Test
    fun theConsoleIsReadAsTextNewestLast() = runTest {
        val console = listOf(
            ConsoleEntry.Status(Res.string.status_scene_opened, arguments = listOf("1.5")),
            ConsoleEntry.Status(Res.string.error_command_scene_actor_missing, isError = true, arguments = listOf("main", "3")),
            ConsoleEntry.Problem("Unresolved reference: speed", "src/Ball.kt:3:5"),
            ConsoleEntry.Log("Hello", source = "Game", isImportant = false),
        )
        val executor = executor(commandContext(ball, console = console))
        assertEquals(
            Json.parseToJsonElement(
                """[{"kind":"error","text":"main has no Actor with the ID 3."},{"kind":"problem","text":"Unresolved reference: speed","location":"src/Ball.kt:3:5"},{"kind":"log","text":"Hello","source":"Game"}]""",
            ),
            executor.read("read_console", buildJsonObject { put("limit", 3) }),
        )
        assertEquals("The Scene is open, in 1.5 seconds.", executor.read("read_console").jsonArray[0].jsonObject.getValue("text").jsonPrimitive.content)
    }

    @Test
    fun theVersionControlStatusListsChangesAndHistory() = runTest {
        assertEquals(Json.parseToJsonElement("""{"isUnderVersionControl":false}"""), executor().read("get_version_control_status"))
        val repository = FakeGitRepository(RepositoryStatus("main", listOf(FileChange("README.md", ChangeKind.ADDED)))).apply { commits += "First" }
        assertEquals(
            Json.parseToJsonElement(
                """{"isUnderVersionControl":true,"branch":"main","changes":[{"title":"README.md","kind":"added","isBlockType":false,"paths":["README.md"]}],"history":[{"id":"0","message":"First","author":"Me"}]}""",
            ),
            executor(commandContext(git = gitController(repository))).read("get_version_control_status"),
        )
    }
}
