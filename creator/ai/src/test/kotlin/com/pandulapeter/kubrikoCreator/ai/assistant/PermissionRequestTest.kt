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

import com.agentclientprotocol.model.ContentBlock
import com.agentclientprotocol.model.SessionUpdate
import com.agentclientprotocol.model.ToolCallContent
import com.agentclientprotocol.model.ToolCallId
import com.agentclientprotocol.model.ToolKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PermissionRequestTest {
    private fun request(title: String?, kind: ToolKind?, content: List<ToolCallContent>? = null) =
        PermissionRequest(SessionUpdate.ToolCallUpdate(ToolCallId("1"), title = title, kind = kind, content = content), emptyList())

    @Test
    fun theAgentsOwnEditsAndCommandsAreOutsideCreator() {
        listOf(ToolKind.EDIT, ToolKind.DELETE, ToolKind.MOVE, ToolKind.EXECUTE).forEach { kind ->
            assertTrue(request("Edit Game.kt", kind).isOutsideCreator, kind.toString())
        }
    }

    @Test
    fun readsAndCreatorsOwnToolsArentOutsideCreator() {
        listOf(null, ToolKind.READ, ToolKind.SEARCH, ToolKind.FETCH, ToolKind.THINK, ToolKind.OTHER).forEach { kind ->
            assertFalse(request("Read Game.kt", kind).isOutsideCreator, kind.toString())
        }
        assertFalse(request("mcp__kubriko-creator__add_block", ToolKind.EDIT).isOutsideCreator)
    }

    @Test
    fun theDiffsAreTheCallsFileChangesOnly() {
        val diff = ToolCallContent.Diff("Game.kt", newText = "b", oldText = "a")
        val request = request("Edit Game.kt", ToolKind.EDIT, listOf(ToolCallContent.Content(ContentBlock.Text("Editing")), diff))
        assertEquals(listOf(diff), request.diffs)
        assertEquals(emptyList(), request("Edit Game.kt", ToolKind.EDIT).diffs)
    }
}
