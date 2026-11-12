/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.commands.project

import com.pandulapeter.kubrikoCreator.actors.ActorTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.BlockDefinition
import com.pandulapeter.kubrikoCreator.actors.EventDefinition
import com.pandulapeter.kubrikoCreator.actors.EventType
import com.pandulapeter.kubrikoCreator.commands.CommandExecutor
import com.pandulapeter.kubrikoCreator.commands.CommandOutcome
import com.pandulapeter.kubrikoCreator.commands.commandContext
import com.pandulapeter.kubrikoCreator.commands.testScope
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class SearchProjectTest {
    private val ball = ActorTypeDefinition(
        name = "Ball",
        events = listOf(EventDefinition(EventType.EDGE_TOUCHED, blocks = List(45) { BlockDefinition.Log("bounce $it") })),
    )

    @Test
    fun theHitsAreListedWithHowManyMoreThereAre() = runTest {
        val context = commandContext(ball)
        java.io.File(context.project.rootDirectory, "screenplay").mkdirs()
        val executor = CommandExecutor(context, testScope())
        val result = assertIs<CommandOutcome.Success>(executor.call("search_project", buildJsonObject { put("query", "bounce 1") })).result
        assertEquals(
            Json.parseToJsonElement(
                """{"hits":["Ball#0.1 (Block text): bounce 1",${(10..19).joinToString(",") { "\"Ball#0.$it (Block text): bounce $it\"" }}]}""",
            ),
            result,
        )
        val many = assertIs<CommandOutcome.Success>(executor.call("search_project", buildJsonObject { put("query", "bounce") })).result
        assertEquals("5", (many as kotlinx.serialization.json.JsonObject)["more"].toString())
    }

    @Test
    fun aBlankQueryIsRejected() = runTest {
        val executor = CommandExecutor(commandContext(ball), testScope())
        assertIs<CommandOutcome.Rejected>(executor.call("search_project", buildJsonObject { put("query", " ") }))
    }

    @Test
    fun theUserSearchesTextsOnly() = runTest {
        val executor = CommandExecutor(commandContext(ball), testScope())
        assertEquals(listOf("Ball#0.0 (Block text): bounce 0"), executor.searchTexts("bounce 0", limit = 10).hits.map { it.toString() })
        assertEquals(emptyList(), executor.searchTexts("Ball", limit = 10).hits)
    }
}
