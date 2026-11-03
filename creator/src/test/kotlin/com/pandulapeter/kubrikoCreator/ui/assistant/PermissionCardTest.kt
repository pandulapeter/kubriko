/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.assistant

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.agentclientprotocol.model.PermissionOption
import com.agentclientprotocol.model.PermissionOptionId
import com.agentclientprotocol.model.PermissionOptionKind
import com.agentclientprotocol.model.SessionUpdate
import com.agentclientprotocol.model.ToolCallId
import com.pandulapeter.kubrikoCreator.ai.assistant.PermissionRequest
import com.pandulapeter.kubrikoCreator.testing.saveSnapshot
import com.pandulapeter.kubrikoCreator.testing.showing
import com.pandulapeter.kubrikoCreator.testing.uiTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class PermissionCardTest {
    private val allow = PermissionOption(PermissionOptionId("allow"), "Allow once", PermissionOptionKind.ALLOW_ONCE)
    private val reject = PermissionOption(PermissionOptionId("reject"), "Reject", PermissionOptionKind.REJECT_ONCE)

    @Test
    fun theAgentsQuestionNamesTheToolAndEachAnswerIsGivenBack() = uiTest {
        val answers = mutableListOf<PermissionOption>()
        showing {
            PermissionCard(
                request = PermissionRequest(SessionUpdate.ToolCallUpdate(ToolCallId("1"), title = "Run gradle build"), listOf(allow, reject)),
                onAnswer = { answers += it },
            )
        }
        onNodeWithText("The assistant asks to use Run gradle build.").assertExists()
        onNodeWithText("Reject").performClick()
        onNodeWithText("Allow once").performClick()
        assertEquals(listOf(reject, allow), answers)
        saveSnapshot("permission-card")
    }

    @Test
    fun aToolCallWithoutATitleIsNamedByItsId() = uiTest {
        showing { PermissionCard(request = PermissionRequest(SessionUpdate.ToolCallUpdate(ToolCallId("call-7")), listOf(allow)), onAnswer = {}) }
        onNodeWithText("The assistant asks to use call-7.").assertExists()
    }
}
