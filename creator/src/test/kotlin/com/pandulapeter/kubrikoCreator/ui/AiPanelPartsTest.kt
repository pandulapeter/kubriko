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
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.agentclientprotocol.model.ToolCallId
import com.agentclientprotocol.model.ToolCallStatus
import com.pandulapeter.kubrikoCreator.ai.assistant.TranscriptItem
import com.pandulapeter.kubrikoCreator.commands.AiStep
import com.pandulapeter.kubrikoCreator.commands.AiStepOrigin
import com.pandulapeter.kubrikoCreator.commands.ChangeRecord
import com.pandulapeter.kubrikoCreator.commands.CommandTarget
import com.pandulapeter.kubrikoCreator.context.HealthFinding
import com.pandulapeter.kubrikoCreator.context.PageSummary
import com.pandulapeter.kubrikoCreator.context.ProjectDigest
import com.pandulapeter.kubrikoCreator.context.RelationSubject
import com.pandulapeter.kubrikoCreator.testing.saveSnapshot
import com.pandulapeter.kubrikoCreator.testing.showing
import com.pandulapeter.kubrikoCreator.testing.uiTest
import com.pandulapeter.kubrikoCreator.ui.assistant.AssistantTranscriptList
import com.pandulapeter.kubrikoCreator.ui.gameMap.GameMapPanel
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The AI panel's header, a conversation's transcript, and the map, rendered offscreen as the app shows them.
 */
@OptIn(ExperimentalTestApi::class)
class AiPanelPartsTest {

    @Test
    fun theHeaderNamesTheAssistantAndKeepsTheUsersOwnToolAndTheMapInItsMenu() = uiTest {
        val selected = mutableListOf<AiPanelView>()
        showing { AiPanelHeader(view = AiPanelView.ASSISTANT, onViewSelected = { selected += it }) }
        onNodeWithText("Assistant").assertExists()
        onNodeWithText("Your own AI tool").assertDoesNotExist()
        saveSnapshot("ai-panel-header")
        onNodeWithText("What the AI knows about your game").assertDoesNotExist()
        onNodeWithContentDescription("More AI options").performClick()
        saveSnapshot("ai-panel-menu")
        onNodeWithText("Use your own AI tool instead").performClick()
        onNodeWithContentDescription("More AI options").performClick()
        onNodeWithText("What the AI knows about your game").performClick()
        assertEquals(listOf(AiPanelView.OWN_TOOL, AiPanelView.GAME_MAP), selected)
    }

    @Test
    fun theUsersOwnToolLeadsBackToTheAssistant() = uiTest {
        val selected = mutableListOf<AiPanelView>()
        showing { AiPanelHeader(view = AiPanelView.OWN_TOOL, onViewSelected = { selected += it }) }
        onNodeWithText("Your own AI tool").assertExists()
        onNodeWithContentDescription("Back to the Assistant").performClick()
        assertEquals(listOf(AiPanelView.ASSISTANT), selected)
        saveSnapshot("ai-panel-header-own-tool")
    }

    @Test
    fun aTranscriptShowsMessagesCreatorsToolCallsAndWhatATurnChanged() = uiTest {
        val kept = mutableListOf<Int>()
        showing {
            AssistantTranscriptList(
                items = listOf(
                    TranscriptItem.UserMessage("Make the Ball faster"),
                    TranscriptItem.ToolCall(ToolCallId("1"), "kubriko-creator_edit_block", status = ToolCallStatus.COMPLETED),
                    TranscriptItem.TurnChanges(4, listOf(ChangeRecord(5, "edit_block", CommandTarget.BlockType("Ball", 0, 0)))),
                    TranscriptItem.AgentMessage("Done: the Ball now starts at **360**."),
                    TranscriptItem.TurnReview(4),
                ),
                isBusy = false,
                steps = emptyMap(),
                onShowStep = {},
                isTurnReviewShown = true,
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
    fun keepAndUndoAreLeftOutUnlessTurnedOn() = uiTest {
        showing {
            AssistantTranscriptList(
                items = listOf(
                    TranscriptItem.UserMessage("Make the Ball faster"),
                    TranscriptItem.TurnChanges(4, listOf(ChangeRecord(5, "edit_block", CommandTarget.BlockType("Ball", 0, 0)))),
                    TranscriptItem.AgentMessage("Done."),
                    TranscriptItem.TurnReview(4),
                ),
                isBusy = false,
                steps = emptyMap(),
                onShowStep = {},
            )
        }
        onNodeWithText("What changed").assertExists()
        onNodeWithText("Keep").assertDoesNotExist()
        onNodeWithText("Undo").assertDoesNotExist()
    }

    @Test
    fun aToolCallNamesWhatItsStepWorkedOnAndGoesThereWhenClicked() = uiTest {
        val step = AiStep(id = 7, toolName = "add_block", target = CommandTarget.BlockType("Ball", 1, null), isRunning = false, origin = AiStepOrigin("c", "1"))
        val shown = mutableListOf<AiStep>()
        showing {
            AssistantTranscriptList(
                items = listOf(TranscriptItem.ToolCall(ToolCallId("1"), "kubriko-creator_add_block", status = ToolCallStatus.COMPLETED)),
                isBusy = false,
                steps = mapOf(ToolCallId("1") to step),
                onShowStep = { shown += it },
            )
        }
        onNodeWithText("Ball, Event 2", substring = true).performClick()
        assertEquals(listOf(step), shown)
        onNodeWithText("Ball, Event 2", substring = true).performClick()
        assertEquals(listOf(step, step), shown)
        saveSnapshot("transcript-tool-call-target")
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

    @Test
    fun theMapListsThePagesFirstAndAPageFindingOpensItsPage() = uiTest {
        val opened = mutableListOf<String>()
        showing {
            GameMapPanel(
                digest = ProjectDigest(projectName = "breakout", actorTypes = emptyList(), managerTypes = emptyList(), scenes = emptyList(), relations = emptyList()),
                onTypeSelected = {},
                onSceneSelected = {},
                findings = listOf(HealthFinding(HealthFinding.Kind.PAGE_IMAGE_MISSING, RelationSubject.Page("hero"), usage = "screenplay/hero-1.png")),
                pages = listOf(PageSummary("README", "Breakout", "Bounce the ball off the paddle."), PageSummary("hero", null, null)),
                onPageSelected = { opened += it },
            )
        }
        onNodeWithText("The screenplay").assertExists()
        onNodeWithText("README: Breakout").performClick()
        onNodeWithText("hero shows screenplay/hero-1.png, which is gone.").performClick()
        assertEquals(listOf("README", "hero"), opened)
        saveSnapshot("map-pages")
    }
}
