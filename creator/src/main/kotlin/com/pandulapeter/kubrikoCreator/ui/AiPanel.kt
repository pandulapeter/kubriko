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

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.pandulapeter.kubrikoCreator.ai.assistant.AssistantPanelModel
import com.pandulapeter.kubrikoCreator.ai.assistant.AttentionChip
import com.pandulapeter.kubrikoCreator.commands.AiStep
import com.pandulapeter.kubrikoCreator.commands.UserQuestions
import com.pandulapeter.kubrikoCreator.context.HealthFinding
import com.pandulapeter.kubrikoCreator.context.PageSummary
import com.pandulapeter.kubrikoCreator.context.ProjectDigest
import com.pandulapeter.kubrikoCreator.mcp.McpController
import com.pandulapeter.kubrikoCreator.mcp.clients.AiClientsController
import com.pandulapeter.kubrikoCreator.mcp.clients.AiConfigFileClient
import com.pandulapeter.kubrikoCreator.ui.assistant.AssistantForms
import com.pandulapeter.kubrikoCreator.ui.assistant.AssistantHeaderActions
import com.pandulapeter.kubrikoCreator.ui.assistant.AssistantPanel
import com.pandulapeter.kubrikoCreator.ui.assistant.QuestionCards
import com.pandulapeter.kubrikoCreator.ui.assistant.ScreenplayOfferCard
import com.pandulapeter.kubrikoCreator.ui.assistant.rememberAssistantForms
import com.pandulapeter.kubrikoCreator.ui.gameMap.GameMapPanel
import com.pandulapeter.kubrikoCreator.ui.mcp.AiConnectionPanel
import com.pandulapeter.kubrikoCreator.ui.theme.CreatorMotion

/**
 * Everything about working with AI in one panel, since the two ways of doing it solve the same problem: the in-app
 * assistant, or an AI tool of the user's own connected over MCP. What the AI is told about the game sits behind a button.
 *
 * @param digest What the AI is told about the game, read only while that view is shown.
 * @param findings What about the game is worth a look, shown with it.
 * @param attention What the user is looking at, which the assistant offers with the next message.
 * @param captureScreenshot Takes the picture an [AttentionChip.Screenshot] sends.
 */
@Composable
internal fun AiPanel(
    view: AiPanelView,
    onViewSelected: (AiPanelView) -> Unit,
    assistant: AssistantPanelModel,
    mcp: McpController,
    aiClients: AiClientsController,
    aiSteps: List<AiStep>,
    onShowStep: (AiStep) -> Unit,
    digest: ProjectDigest?,
    findings: List<HealthFinding>,
    pages: List<PageSummary>,
    onTypeSelected: (String) -> Unit,
    onSceneSelected: (String) -> Unit,
    onPageSelected: (String) -> Unit,
    attention: List<AttentionChip>,
    captureScreenshot: suspend (AttentionChip.Screenshot) -> ByteArray?,
    modifier: Modifier = Modifier,
    onGameSettingsSelected: () -> Unit = {},
    isScreenplayOffered: Boolean = false,
    onScreenplayOfferAnswered: () -> Unit = {},
    questions: List<UserQuestions.Question> = emptyList(),
    onAnswer: (id: Long, answer: String) -> Unit = { _, _ -> },
) = Column(
    modifier = modifier,
) {
    val assistantForms = rememberAssistantForms()
    AiPanelHeader(
        view = view,
        onViewSelected = onViewSelected,
        assistantActions = { AssistantHeaderActions(model = assistant, forms = assistantForms) },
    )
    HorizontalDivider()
    ScreenplayOfferCard(isShown = isScreenplayOffered && view == AiPanelView.ASSISTANT, onAnswered = onScreenplayOfferAnswered)
    QuestionCards(questions = remember(questions) { questions.filter { it.origin == null } }, onAnswer = onAnswer)
    var shownDigest by remember { mutableStateOf(digest) }
    var shownFindings by remember { mutableStateOf(findings) }
    if (view == AiPanelView.GAME_MAP) {
        shownDigest = digest
        shownFindings = findings
    }
    AnimatedContent(
        modifier = Modifier.fillMaxWidth().weight(1f),
        targetState = view,
        transitionSpec = { fadeIn(CreatorMotion.medium()) togetherWith fadeOut(CreatorMotion.short()) },
    ) { shownView ->
        AiPanelContent(
            view = shownView,
            assistant = assistant,
            assistantForms = assistantForms,
            mcp = mcp,
            aiClients = aiClients,
            aiSteps = aiSteps,
            onShowStep = onShowStep,
            digest = shownDigest,
            findings = shownFindings,
            pages = pages,
            onTypeSelected = onTypeSelected,
            onSceneSelected = onSceneSelected,
            onPageSelected = onPageSelected,
            onGameSettingsSelected = onGameSettingsSelected,
            attention = attention,
            captureScreenshot = captureScreenshot,
            questions = questions,
            onAnswer = onAnswer,
        )
    }
}

@Composable
private fun AiPanelContent(
    view: AiPanelView,
    assistant: AssistantPanelModel,
    assistantForms: AssistantForms,
    mcp: McpController,
    aiClients: AiClientsController,
    aiSteps: List<AiStep>,
    onShowStep: (AiStep) -> Unit,
    digest: ProjectDigest?,
    findings: List<HealthFinding>,
    pages: List<PageSummary>,
    onTypeSelected: (String) -> Unit,
    onSceneSelected: (String) -> Unit,
    onPageSelected: (String) -> Unit,
    onGameSettingsSelected: () -> Unit,
    attention: List<AttentionChip>,
    captureScreenshot: suspend (AttentionChip.Screenshot) -> ByteArray?,
    questions: List<UserQuestions.Question>,
    onAnswer: (id: Long, answer: String) -> Unit,
) {
    val content = Modifier.fillMaxSize()
    when (view) {
        AiPanelView.ASSISTANT -> AssistantPanel(
            modifier = content,
            model = assistant,
            forms = assistantForms,
            aiSteps = aiSteps,
            onShowStep = onShowStep,
            attention = attention,
            captureScreenshot = captureScreenshot,
            questions = questions,
            onAnswer = onAnswer,
        )
        AiPanelView.OWN_TOOL -> AiConnectionPanel(
            modifier = content,
            settings = mcp.settings.collectAsState().value,
            state = mcp.state.collectAsState().value,
            clientSetups = aiClients.setups.collectAsState().value,
            configFileClients = AiConfigFileClient.entries,
            clientStatuses = aiClients.statuses.collectAsState().value,
            calls = mcp.calls.collectAsState().value,
            onEnabledChange = mcp::setEnabled,
            onRefreshClients = aiClients::refresh,
            onConnect = aiClients::connect,
            onDisconnect = aiClients::disconnect,
            onConnectConfigFile = aiClients::connectConfigFile,
            onDisconnectConfigFile = aiClients::disconnectConfigFile,
        )
        AiPanelView.GAME_MAP -> GameMapPanel(
            modifier = content,
            digest = digest,
            findings = findings,
            pages = pages,
            onTypeSelected = onTypeSelected,
            onSceneSelected = onSceneSelected,
            onPageSelected = onPageSelected,
            onGameSettingsSelected = onGameSettingsSelected,
        )
    }
}
