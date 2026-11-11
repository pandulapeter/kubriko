/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ai.assistant

import com.pandulapeter.kubrikoCreator.actors.ActorTypeDefinition
import com.pandulapeter.kubrikoCreator.commands.AiStepOrigin
import com.pandulapeter.kubrikoCreator.commands.CommandExecutor
import com.pandulapeter.kubrikoCreator.commands.commandContext
import com.pandulapeter.kubrikoCreator.commands.project.RememberForNextTime
import com.pandulapeter.kubrikoCreator.commands.testScope
import com.pandulapeter.kubrikoCreator.context.ConversationStart
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

class OpenProjectAccessTest {

    @Test
    fun withoutAnOpenProjectNothingIsReadOrUndone() = runTest {
        val access = OpenProjectAccess.of { null }

        assertNull(access.digest())
        assertNull(access.changeLog())
        assertEquals(0, access.undoTurn("conversation", sinceRevision = 0))
        assertNull(access.takeSummary("conversation"))
    }

    @Test
    fun theOpenProjectIsReadThroughItsCommands() = runTest {
        val executor = CommandExecutor(commandContext(ActorTypeDefinition("Ball")), testScope())
        val access = OpenProjectAccess.of { executor }

        assertSame(executor.changeLog, access.changeLog())
        assertEquals(ConversationStart.of(executor.context), access.digest())
    }

    @Test
    fun theSummaryAConversationNotedThroughItsToolsIsTakenOnce() = runTest {
        val executor = CommandExecutor(commandContext(), testScope())
        val access = OpenProjectAccess.of { executor }
        executor.call(RememberForNextTime.name, buildJsonObject { put("summary", "Made the Ball faster.") }, AiStepOrigin("conversation"))

        assertEquals("Made the Ball faster.", access.takeSummary("conversation"))
        assertNull(access.takeSummary("conversation"))
    }
}
