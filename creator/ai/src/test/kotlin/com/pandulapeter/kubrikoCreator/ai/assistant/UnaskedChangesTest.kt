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
import com.agentclientprotocol.model.ToolCallStatus
import com.agentclientprotocol.model.ToolKind
import kotlin.test.Test
import kotlin.test.assertEquals
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
    fun deletesAndMovesCompletedWithoutAskingAreUnaskedChanges() {
        started("Delete notes.txt", ToolKind.DELETE)
        assertTrue(ended())
        started("Move notes.txt", ToolKind.MOVE)
        assertTrue(ended())
    }

    @Test
    fun aCommandRunWithoutAskingIsnt() {
        started("ls", ToolKind.EXECUTE)
        assertFalse(ended())
    }

    @Test
    fun aCallTheUserWasAskedAboutIsnt() {
        started("Write notes.txt", ToolKind.EDIT)
        changes.asked(question())
        assertFalse(ended())
    }

    @Test
    fun aKindTheUserAllowedAlwaysIsntAnymore() {
        started("Write notes.txt", ToolKind.EDIT)
        val question = changes.asked(question())
        changes.answered(question, listOf(always, once), RequestPermissionOutcome.Selected(always.optionId))
        assertFalse(ended())
        started("Write more.txt", ToolKind.EDIT)
        assertFalse(ended())
        started("Delete notes.txt", ToolKind.DELETE)
        assertTrue(ended())
    }

    @Test
    fun allowingOnceStillCountsTheNextUnaskedCall() {
        started("Write notes.txt", ToolKind.EDIT)
        val question = changes.asked(question())
        changes.answered(question, listOf(always, once), RequestPermissionOutcome.Selected(once.optionId))
        assertFalse(ended())
        started("Write more.txt", ToolKind.EDIT)
        assertTrue(ended())
    }

    @Test
    fun aQuestionLeavingOutTheKindGetsTheOneTheCallWasStartedWith() {
        started("Write notes.txt", ToolKind.EDIT)
        val question = changes.asked(question())
        assertEquals(ToolKind.EDIT, question.kind)
        assertEquals("Write notes.txt", question.title)
        assertTrue(PermissionRequest(question, emptyList()).isOutsideCreator)
    }

    private fun question() = SessionUpdate.ToolCallUpdate(ID)

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
        val always = PermissionOption(PermissionOptionId("always"), "Always allow", PermissionOptionKind.ALLOW_ALWAYS)
        val once = PermissionOption(PermissionOptionId("once"), "Allow", PermissionOptionKind.ALLOW_ONCE)
    }
}
