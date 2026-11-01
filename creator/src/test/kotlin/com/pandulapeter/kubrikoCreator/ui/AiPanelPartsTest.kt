/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.agentclientprotocol.model.ToolCallId
import com.agentclientprotocol.model.ToolCallStatus
import com.pandulapeter.kubrikoCreator.ai.assistant.TranscriptItem
import com.pandulapeter.kubrikoCreator.commands.ChangeRecord
import com.pandulapeter.kubrikoCreator.commands.CommandTarget
import com.pandulapeter.kubrikoCreator.context.HealthFinding
import com.pandulapeter.kubrikoCreator.context.ProjectDigest
import com.pandulapeter.kubrikoCreator.context.RelationSubject
import com.pandulapeter.kubrikoCreator.ui.assistant.AssistantTranscriptList
import com.pandulapeter.kubrikoCreator.ui.gameMap.GameMapPanel
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.command_edit_block
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The AI panel's header, a conversation's transcript, and the map, rendered offscreen as the app shows them.
 */
@OptIn(ExperimentalTestApi::class)
class AiPanelPartsTest {

    @Test
    fun theHeaderSwitchesBetweenTheAssistantAndTheUsersOwnToolWithTheMapBehindAButton() = uiTest {
        val selected = mutableListOf<AiPanelView>()
        showing { AiPanelHeader(view = AiPanelView.ASSISTANT, onViewSelected = { selected += it }) }
        onNodeWithText("Assistant").assertIsSelected()
        onNodeWithText("Your own AI tool").performClick()
        onNodeWithContentDescription("What the AI knows about your game", substring = true).performClick()
        assertEquals(listOf(AiPanelView.OWN_TOOL, AiPanelView.GAME_MAP), selected)
        saveSnapshot("ai-panel-header")
    }

    @Test
    fun aTranscriptShowsMessagesCreatorsToolCallsAndWhatATurnChanged() = uiTest {
        val kept = mutableListOf<Int>()
        showing {
            AssistantTranscriptList(
                items = listOf(
                    TranscriptItem.UserMessage("Make the Ball faster"),
                    TranscriptItem.ToolCall(ToolCallId("1"), "kubriko-creator_edit_block", status = ToolCallStatus.COMPLETED),
                    TranscriptItem.AgentMessage("Done: the Ball now starts at **360**."),
                    TranscriptItem.TurnChanges(4, listOf(ChangeRecord(5, Res.string.command_edit_block, CommandTarget.BlockType("Ball", 0, 0)))),
                ),
                isBusy = false,
                steps = emptyMap(),
                onShowStep = {},
                onKeepChanges = { kept += it },
            )
        }
        onNodeWithText("Make the Ball faster").assertExists()
        assertEquals(2, onAllNodesWithText("Edit Block", substring = true).fetchSemanticsNodes().size)
        onNodeWithText("kubriko-creator_edit_block").assertDoesNotExist()
        onNodeWithText("Done: the Ball now starts at 360.").assertExists()
        onNodeWithText("Keep").performClick()
        assertEquals(listOf(4), kept)
        saveSnapshot("transcript")
    }

    @Test
    fun theMapListsWhatIsWorthALookFirst() = uiTest {
        val opened = mutableListOf<String>()
        showing {
            GameMapPanel(
                digest = ProjectDigest(
                    projectName = "breakout",
                    actorTypes = listOf(ProjectDigest.Type("Ball", "", "the ball", null, listOf("speed"), listOf("When added to the Scene"))),
                    managerTypes = emptyList(),
                    scenes = listOf(ProjectDigest.Scene("level-1", null, mapOf("Ball" to 1))),
                    relations = emptyList(),
                ),
                onTypeSelected = { opened += it },
                onSceneSelected = { opened += it },
                findings = listOf(
                    HealthFinding(HealthFinding.Kind.UNREAD_PROPERTY, RelationSubject.Property("Ball", "speed")),
                    HealthFinding(HealthFinding.Kind.TIMER_NEVER_STARTED, RelationSubject.Type("Ball"), "spawn"),
                ),
            )
        }
        onNodeWithText("Worth a look").assertExists()
        onNodeWithText("the Timer spawn never starts", substring = true).performClick()
        assertEquals(listOf("Ball"), opened)
        saveSnapshot("map-worth-a-look")
    }
}
