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
import com.agentclientprotocol.model.ToolKind
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class PermissionHandlerTest {
    private val options = listOf(
        PermissionOption(PermissionOptionId("allow"), "Allow", PermissionOptionKind.ALLOW_ONCE),
        PermissionOption(PermissionOptionId("reject"), "Reject", PermissionOptionKind.REJECT_ONCE),
    )
    private val asked = mutableListOf<SessionUpdate.ToolCallUpdate>()
    private val recording = PermissionHandler { toolCall, _ ->
        asked += toolCall
        RequestPermissionOutcome.Cancelled
    }

    private fun call(title: String, kind: ToolKind? = null) = SessionUpdate.ToolCallUpdate(ToolCallId("1"), title = title, kind = kind)

    @Test
    fun aCreatorToolThatChangesNothingIsAllowedOnce() = runTest {
        val handler = PermissionHandler.allowingCreatorTools(recording)
        listOf("kubriko-creator_read_scene", "mcp__kubriko-creator__add_block").forEach { title ->
            listOf(null, ToolKind.READ, ToolKind.OTHER, ToolKind.FETCH, ToolKind.SEARCH, ToolKind.THINK).forEach { kind ->
                assertEquals(RequestPermissionOutcome.Selected(PermissionOptionId("allow")), handler.answer(call(title, kind), options))
            }
        }
        assertEquals(emptyList(), asked)
    }

    @Test
    fun aCreatorTitledCallThatEditsOrRunsSomethingIsLeftToTheNextHandler() = runTest {
        val changingKinds = listOf(ToolKind.EDIT, ToolKind.DELETE, ToolKind.MOVE, ToolKind.EXECUTE)
        changingKinds.forEach { kind ->
            assertEquals(RequestPermissionOutcome.Cancelled, PermissionHandler.allowingCreatorTools(recording).answer(call("kubriko-creator_read_scene", kind), options))
        }
        assertEquals(changingKinds, asked.map { it.kind })
    }

    @Test
    fun otherToolsAreLeftToTheNextHandler() = runTest {
        val handler = PermissionHandler.allowingCreatorTools(recording)
        assertEquals(RequestPermissionOutcome.Selected(PermissionOptionId("allow")), handler.answer(call("kubriko-creator_add_block"), options))
        assertEquals(RequestPermissionOutcome.Cancelled, handler.answer(call("run_command"), options))
        assertEquals(listOf<String?>("run_command"), asked.map { it.title })
    }
}
