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
import com.pandulapeter.kubrikoCreator.actors.EventDefinition
import com.pandulapeter.kubrikoCreator.actors.EventType
import com.pandulapeter.kubrikoCreator.git.ChangeKind
import com.pandulapeter.kubrikoCreator.git.FakeGitRepository
import com.pandulapeter.kubrikoCreator.git.FileChange
import com.pandulapeter.kubrikoCreator.git.RepositoryStatus
import com.pandulapeter.kubrikoCreator.workspace.ConsoleEntry
import com.pandulapeter.kubrikoCreator.workspace.FakeBlockTypeRepository
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.error_command_actor_type_missing
import kubriko.creator.generated.resources.error_command_no_actor_types
import kubriko.creator.generated.resources.error_command_scene_actor_missing
import kubriko.creator.generated.resources.error_command_scene_missing
import kubriko.creator.generated.resources.status_scene_opened
import kotlin.test.Test
import kotlin.test.assertEquals
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
    fun theOverviewNamesTheActorTypesAndScenes() = runTest {
        val context = commandContext(ball)
        @OptIn(EditorMutation::class)
        context.scenes.write("main", "[]")
        assertEquals(
            Json.parseToJsonElement("""{"name":"game","usesBlockBuiltActorTypes":true,"actorTypes":["Ball"],"scenes":["main"]}"""),
            executor(context).read("get_project_overview"),
        )
        assertEquals(
            Json.parseToJsonElement("""{"name":"game","usesBlockBuiltActorTypes":false,"scenes":[]}"""),
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
        assertEquals(Json.parseToJsonElement("""[{"name":"Ball","events":["edgeTouched"],"isChangedByHand":false}]"""), executor.read("list_actor_types"))
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
            Json.parseToJsonElement("""[{"actorIndex":0,"typeId":"Ball","x":1.0,"y":2.0,"state":"{\"body\":{\"position\":{\"x\":1.0,\"y\":2.0}}}"},{"actorIndex":1,"typeId":"Music","state":"{}"}]"""),
            read.getValue("actors"),
        )
        assertEquals(scene, read.getValue("content").jsonPrimitive.content)
        assertEquals(CommandOutcome.Rejected(Rejection(Res.string.error_command_scene_missing, "other")), executor.call("read_scene", buildJsonObject { put("scene", "other") }))
    }

    @Test
    fun theConsoleIsReadAsTextNewestLast() = runTest {
        val console = listOf(
            ConsoleEntry.Status(Res.string.status_scene_opened),
            ConsoleEntry.Status(Res.string.error_command_scene_actor_missing, isError = true, arguments = listOf("main", "3")),
            ConsoleEntry.Problem("Unresolved reference: speed", "src/Ball.kt:3:5"),
            ConsoleEntry.Log("Hello", source = "Game", isImportant = false),
        )
        val executor = executor(commandContext(ball, console = console))
        assertEquals(
            Json.parseToJsonElement(
                """[{"kind":"error","text":"main has no Actor at index 3."},{"kind":"problem","text":"Unresolved reference: speed","location":"src/Ball.kt:3:5"},{"kind":"log","text":"Hello","source":"Game"}]""",
            ),
            executor.read("read_console", buildJsonObject { put("limit", 3) }),
        )
        assertEquals("The Scene is open.", executor.read("read_console").jsonArray[0].jsonObject.getValue("text").jsonPrimitive.content)
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
