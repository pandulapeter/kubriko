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

import com.pandulapeter.kubrikoCreator.commands.CommandExecutor
import com.pandulapeter.kubrikoCreator.commands.CommandOutcome
import com.pandulapeter.kubrikoCreator.commands.Invocation
import com.pandulapeter.kubrikoCreator.commands.assertRoundTrip
import com.pandulapeter.kubrikoCreator.commands.commandContext
import com.pandulapeter.kubrikoCreator.commands.testScope
import com.pandulapeter.kubrikoCreator.context.ConversationStart
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class GameNotesCommandsTest {

    @Test
    fun notesAreWrittenWithAClaudeFileImportingThemAndUndoneToWhatWasThere() = runTest {
        val context = commandContext()
        val root = context.project.rootDirectory
        val executor = CommandExecutor(context, testScope())
        assertEquals(JsonPrimitive("The game has no notes yet."), assertIs<CommandOutcome.Success>(executor.call("read_game_notes", JsonObject(emptyMap()))).result)
        assertIs<CommandOutcome.Success>(executor.call("set_game_notes", buildJsonObject { put("text", "Bricks give 10 points.") }))
        assertEquals("Bricks give 10 points.", File(root, "AGENTS.md").readText())
        assertEquals("@AGENTS.md\n", File(root, "CLAUDE.md").readText())
        assertTrue(ConversationStart.of(context).endsWith("The game's notes (AGENTS.md):\nBricks give 10 points."))
        executor.undo()
        assertFalse(File(root, "AGENTS.md").exists())
    }

    @Test
    fun anExistingClaudeFileIsLeftAlone() = runTest {
        val context = commandContext()
        val claude = File(context.project.rootDirectory, "CLAUDE.md").also { it.writeText("My own instructions") }
        context.assertRoundTrip(Invocation(SetGameNotes, SetGameNotes.Input("Notes"))) { File(context.project.rootDirectory, "AGENTS.md").takeIf { it.isFile }?.readText() }
        assertEquals("My own instructions", claude.readText())
    }
}
