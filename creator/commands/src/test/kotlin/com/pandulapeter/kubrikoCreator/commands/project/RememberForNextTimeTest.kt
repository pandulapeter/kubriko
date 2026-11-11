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

import com.pandulapeter.kubrikoCreator.commands.AiStepOrigin
import com.pandulapeter.kubrikoCreator.commands.CommandExecutor
import com.pandulapeter.kubrikoCreator.commands.CommandOutcome
import com.pandulapeter.kubrikoCreator.commands.CommandProblem
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.commands.commandContext
import com.pandulapeter.kubrikoCreator.commands.testScope
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class RememberForNextTimeTest {
    @Test
    fun `the latest summary is kept for the conversation that noted it until it is taken`() = runTest {
        val context = commandContext()
        val executor = CommandExecutor(context, testScope())
        suspend fun remember(summary: String, conversationId: String) =
            executor.call(RememberForNextTime.name, buildJsonObject { put("summary", summary) }, AiStepOrigin(conversationId, "toolu_1"))

        assertIs<CommandOutcome.Success>(remember("Made the Ball faster.", "first"))
        assertIs<CommandOutcome.Success>(remember(" Made the Ball faster; the Paddle is still slow. ", "first"))
        assertIs<CommandOutcome.Success>(remember("Added lives.", "second"))
        assertEquals("Made the Ball faster; the Paddle is still slow.", context.summaries.take("first"))
        assertNull(context.summaries.take("first"))
        assertEquals("Added lives.", context.summaries.take("second"))
    }

    @Test
    fun `an outside client's summary is accepted and kept for nobody`() = runTest {
        val context = commandContext()
        assertIs<CommandOutcome.Success>(RememberForNextTime.run(context, RememberForNextTime.Input("Made the Ball faster.")))
        assertNull(context.summaries.take(""))
    }

    @Test
    fun `a summary has to say something briefly`() = runTest {
        val context = commandContext()
        val origin = AiStepOrigin("first")
        val invalid = CommandOutcome.Rejected(Rejection(CommandProblem.SUMMARY_INVALID, "600"))
        assertEquals(invalid, RememberForNextTime.run(context, RememberForNextTime.Input("  "), origin))
        assertEquals(invalid, RememberForNextTime.run(context, RememberForNextTime.Input("x".repeat(601)), origin))
        assertNull(context.summaries.take("first"))
    }
}
