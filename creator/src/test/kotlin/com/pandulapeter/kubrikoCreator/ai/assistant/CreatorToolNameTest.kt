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

import com.agentclientprotocol.model.PermissionOption
import com.agentclientprotocol.model.PermissionOptionId
import com.agentclientprotocol.model.PermissionOptionKind
import com.agentclientprotocol.model.RequestPermissionOutcome
import com.agentclientprotocol.model.SessionUpdate
import com.agentclientprotocol.model.ToolCallId
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CreatorToolNameTest {
    private val options = listOf(
        PermissionOption(PermissionOptionId("allow"), "Allow", PermissionOptionKind.ALLOW_ONCE),
        PermissionOption(PermissionOptionId("reject"), "Reject", PermissionOptionKind.REJECT_ONCE),
    )

    @Test
    fun creatorsToolsAreRecognizedAsEachAgentNamesThem() {
        assertEquals("read_actor_type", CreatorToolName.of("mcp__kubriko-creator__read_actor_type"))
        assertEquals("read_actor_type", CreatorToolName.of("kubriko-creator_read_actor_type"))
        assertEquals("list_scenes", CreatorToolName.of("kubriko-creator/list_scenes"))
    }

    @Test
    fun otherToolsAndNamesCreatorDoesntHaveAreNot() {
        assertNull(CreatorToolName.of("Edit Game.kt"))
        assertNull(CreatorToolName.of("kubriko-creator_delete_everything"))
        assertNull(CreatorToolName.of("other-server_read_actor_type"))
        assertNull(CreatorToolName.of(null))
    }

    @Test
    fun creatorsToolsAreAllowedWithoutAskingAndTheRestAreLeftToTheUser() = runTest {
        val asked = mutableListOf<String?>()
        val handler = PermissionHandler.allowingCreatorTools { toolCall, _ ->
            asked += toolCall.title
            RequestPermissionOutcome.Cancelled
        }
        assertEquals(RequestPermissionOutcome.Selected(PermissionOptionId("allow")), handler.answer(SessionUpdate.ToolCallUpdate(ToolCallId("1"), title = "kubriko-creator_add_block"), options))
        assertEquals(RequestPermissionOutcome.Cancelled, handler.answer(SessionUpdate.ToolCallUpdate(ToolCallId("2"), title = "run_command"), options))
        assertEquals(listOf<String?>("run_command"), asked)
    }
}
