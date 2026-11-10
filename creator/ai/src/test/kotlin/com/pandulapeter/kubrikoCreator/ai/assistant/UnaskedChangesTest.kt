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

import com.agentclientprotocol.model.SessionUpdate
import com.agentclientprotocol.model.ToolCallId
import com.agentclientprotocol.model.ToolCallStatus
import com.agentclientprotocol.model.ToolKind
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UnaskedChangesTest {
    private val changes = UnaskedChanges()
    private var isReadOnly = false

    private fun started(title: String, kind: ToolKind?) =
        changes.completesUnaskedChange(SessionUpdate.ToolCall(ID, title, kind = kind, status = ToolCallStatus.PENDING)) { isReadOnly }

    private fun ended(status: ToolCallStatus = ToolCallStatus.COMPLETED) =
        changes.completesUnaskedChange(SessionUpdate.ToolCallUpdate(ID, status = status)) { isReadOnly }

    @Test
    fun anEditCompletedWithoutAskingIsAnUnaskedChange() {
        assertFalse(started("Write notes.txt", ToolKind.EDIT))
        assertTrue(ended())
    }

    @Test
    fun aCommandCompletedWithoutAskingIsAnUnaskedChange() {
        started("rm notes.txt", ToolKind.EXECUTE)
        assertTrue(ended())
    }

    @Test
    fun aCallTheUserWasAskedAboutIsnt() {
        started("Write notes.txt", ToolKind.EDIT)
        changes.asked(ID)
        assertFalse(ended())
    }

    @Test
    fun aCallInTheReadOnlyModeIsnt() {
        started("ls", ToolKind.EXECUTE)
        isReadOnly = true
        assertFalse(ended())
    }

    @Test
    fun readsCreatorsToolsAndFailedCallsArent() {
        started("Read notes.txt", ToolKind.READ)
        assertFalse(ended())
        started("mcp__kubriko-creator__add_block", ToolKind.EDIT)
        assertFalse(ended())
        started("Write notes.txt", ToolKind.EDIT)
        assertFalse(ended(ToolCallStatus.FAILED))
    }

    @Test
    fun aCallReportedCompletedAtOnceCounts() {
        assertTrue(changes.completesUnaskedChange(SessionUpdate.ToolCall(ID, "Write notes.txt", kind = ToolKind.EDIT, status = ToolCallStatus.COMPLETED)) { isReadOnly })
    }

    private companion object {
        val ID = ToolCallId("call-1")
    }
}
