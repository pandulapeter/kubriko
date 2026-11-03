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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.agentclientprotocol.annotations.UnstableApi
import com.agentclientprotocol.model.SessionConfigOptionValue
import com.pandulapeter.kubrikoCreator.ai.assistant.AgentChoice
import com.pandulapeter.kubrikoCreator.ai.assistant.AgentDownload
import com.pandulapeter.kubrikoCreator.ai.assistant.AssistantChat
import com.pandulapeter.kubrikoCreator.ai.assistant.AssistantPanelModel
import com.pandulapeter.kubrikoCreator.ai.assistant.AttentionChip
import com.pandulapeter.kubrikoCreator.ai.assistant.ChatState
import com.pandulapeter.kubrikoCreator.ai.assistant.FailureReason
import com.pandulapeter.kubrikoCreator.commands.AiStep
import com.pandulapeter.kubrikoCreator.ui.components.openInSystemEditor
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.assistant_empty
import kubriko.creator.generated.resources.assistant_note
import kubriko.creator.generated.resources.assistant_note_untested
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Duration.Companion.milliseconds

/**
 * The in-app assistant: conversations about the open project with the agent chosen, which works through the same
 * commands as the editor.
 *
 * @param aiSteps The open project's AI steps, which the tool call cards tie into.
 * @param onShowStep Goes to what a step worked on, in the editor.
 * @param attention What the user is looking at, offered with the next message.
 * @param captureScreen Takes a screenshot of the running game, for [AttentionChip.GameScreen].
 */
@Composable
internal fun AssistantPanel(
    model: AssistantPanelModel,
    aiSteps: List<AiStep>,
    onShowStep: (AiStep) -> Unit,
    modifier: Modifier = Modifier,
    attention: List<AttentionChip> = emptyList(),
    captureScreen: suspend () -> ByteArray? = { null },
) {
    val agent by model.agent.collectAsState()
    CompositionLocalProvider(LocalAgent provides agent) {
        AssistantPanelContent(
            model = model,
            aiSteps = aiSteps,
            onShowStep = onShowStep,
            attention = attention,
            captureScreen = captureScreen,
            modifier = modifier,
        )
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun AssistantPanelContent(
    model: AssistantPanelModel,
    aiSteps: List<AiStep>,
    onShowStep: (AiStep) -> Unit,
    attention: List<AttentionChip>,
    captureScreen: suspend () -> ByteArray?,
    modifier: Modifier = Modifier,
) = Column(
    modifier = modifier,
) {
    val chats by model.chats.collectAsState()
    val chat by model.currentChat.collectAsState()
    val authStatus by model.authStatus.collectAsState()
    val signInMethods by model.signInMethods.collectAsState()
    val hasApiKey by model.hasApiKey.collectAsState()
    var isEnteringApiKey by remember { mutableStateOf(false) }
    var isEnteringCustomAgent by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        model.findConfigurations()
        if (model.currentChat.value == null) model.newChat()
    }
    AgentRow(
        agent = LocalAgent.current,
        agents = model.agents.collectAsState().value,
        customAgentCommand = model.customAgentCommand,
        canClose = chat != null,
        onAgentSelected = model::selectAgent,
        onEnterCustomAgent = { isEnteringCustomAgent = true },
        onOpenLog = model.errorLogFile?.let { file -> { if (file.isFile) openInSystemEditor(file) } },
        onNewConversation = model::newChat,
        onCloseConversation = { chat?.let(model::close) },
    )
    ConversationMenu(
        chats = chats,
        current = chat,
        savedConversations = model.savedConversations.collectAsState().value,
        onSelected = model::select,
        onResume = model::resume,
    )
    AssistantOptionsRow(
        configuration = model.configuration.collectAsState().value,
        configurations = model.configurations.collectAsState().value,
        configOptions = chat?.configOptions?.collectAsState()?.value.orEmpty(),
        onConfigurationSelected = model::selectConfiguration,
        onConfigOptionSelected = { option, choice -> chat?.setConfigOption(option.id, SessionConfigOptionValue.of(choice.value.value)) },
    ) {
        AssistantAccountMenu(
            status = authStatus,
            methods = signInMethods,
            hasApiKey = hasApiKey,
            canKeepApiKey = model.canKeepApiKey,
            onSignIn = model::signIn,
            onUseApiKey = { isEnteringApiKey = true },
            onRemoveApiKey = model::removeApiKey,
        )
    }
    if (isEnteringCustomAgent) {
        CustomAgentForm(
            initialCommand = model.customAgentCommand,
            onSave = { command ->
                isEnteringCustomAgent = false
                model.selectAgent(AgentChoice.Custom(command))
            },
            onCancel = { isEnteringCustomAgent = false },
        )
    }
    HorizontalDivider()
    val session by model.signIn.collectAsState()
    val needsSignIn by model.needsSignIn.collectAsState()
    val hasApiKeyFailed by model.apiKeyFailed.collectAsState()
    chat?.let { current ->
        ChatContent(
            modifier = Modifier.weight(1f),
            chat = current,
            download = model.download.collectAsState().value,
            attention = attention,
            captureScreen = captureScreen,
            onInstallNode = model::installNode.takeIf { model.canInstallNode },
            nodeInstallFailure = model.nodeInstallFailure.collectAsState().value,
            isBilledToApiKey = hasApiKey,
            onOpenLog = model.errorLogFile?.let { file -> { if (file.isFile) openInSystemEditor(file) } },
            aiSteps = aiSteps,
            onShowStep = onShowStep,
        ) { isPromptRefused ->
            AssistantSignInArea(
                session = session,
                isEnteringApiKey = isEnteringApiKey,
                needsSignIn = needsSignIn || isPromptRefused,
                methods = signInMethods,
                canKeepApiKey = model.canKeepApiKey,
                hasApiKeyFailed = hasApiKeyFailed,
                onSignIn = model::signIn,
                onCloseSignIn = model::closeSignIn,
                onUseApiKey = { isEnteringApiKey = true },
                onSaveApiKey = { key -> model.setApiKey(key) { isEnteringApiKey = false } },
                onCancelApiKey = { isEnteringApiKey = false },
                variablesForm = model.variablesForm.collectAsState().value,
                hasVariablesFailed = model.variablesFailed.collectAsState().value,
                onSaveVariables = model::setVariables,
                onCancelVariables = model::closeVariablesForm,
            )
        }
    }
}

@Composable
private fun ChatContent(
    chat: AssistantChat,
    onInstallNode: (() -> Unit)?,
    nodeInstallFailure: String?,
    attention: List<AttentionChip>,
    captureScreen: suspend () -> ByteArray?,
    download: AgentDownload?,
    isBilledToApiKey: Boolean,
    onOpenLog: (() -> Unit)?,
    aiSteps: List<AiStep>,
    onShowStep: (AiStep) -> Unit,
    modifier: Modifier = Modifier,
    signInArea: @Composable (isPromptRefused: Boolean) -> Unit,
) = Column(
    modifier = modifier,
) {
    val state by chat.state.collectAsState()
    val transcript by remember(chat) { chat.transcript.throttled(TRANSCRIPT_PERIOD) }.collectAsState(chat.transcript.value)
    val isBusy by chat.isBusy.collectAsState()
    val permissionRequest by chat.permissionRequest.collectAsState()
    val toolsId by chat.toolsId.collectAsState()
    val steps = remember(transcript.items, aiSteps, toolsId) { aiStepsOf(transcript.items, aiSteps, toolsId) }
    Box(
        modifier = Modifier.weight(1f).fillMaxWidth(),
    ) {
        when {
            transcript.items.isNotEmpty() -> AssistantTranscriptList(
                modifier = Modifier.fillMaxSize(),
                items = transcript.items,
                isBusy = isBusy,
                steps = steps,
                onShowStep = onShowStep,
                onKeepChanges = chat::keepChanges,
                onUndoChanges = chat::undoChanges,
            )
            state is ChatState.Ready -> Hint(stringResource(Res.string.assistant_empty))
            state is ChatState.Starting || download != null -> AssistantStarting(download)
        }
        (state as? ChatState.Failed)?.takeIf { download == null && (transcript.items.isEmpty() || it.reason == FailureReason.CONVERSATION_LOST) }?.let { failed ->
            AssistantFailure(
                modifier = Modifier.fillMaxSize(),
                state = failed,
                onTryAgain = chat::retry.takeIf { failed.reason != FailureReason.CONVERSATION_LOST },
                onOpenLog = onOpenLog,
                onInstallNode = onInstallNode,
                nodeInstallFailure = nodeInstallFailure,
            )
        }
    }
    if (transcript.plan.isNotEmpty()) {
        HorizontalDivider()
        PlanChecklist(transcript.plan)
    }
    signInArea(transcript.needsSignIn)
    permissionRequest?.let { request ->
        PermissionCard(
            request = request,
            onAnswer = chat::answer,
        )
    }
    var leftOut by remember(chat) { mutableStateOf(emptySet<String>()) }
    AttentionChips(
        chips = attention,
        leftOut = leftOut,
        onToggle = { chip -> leftOut = if (chip.key in leftOut) leftOut - chip.key else leftOut + chip.key },
    )
    ConversationUsageLine(
        usage = transcript.usage,
        isBilledToApiKey = isBilledToApiKey,
    )
    HorizontalDivider()
    AssistantComposer(
        commands = chat.availableCommands.collectAsState().value,
        isEnabled = state == ChatState.Ready,
        isBusy = isBusy,
        onSend = { text ->
            val chips = attention.filter { it.key !in leftOut }
            chat.send(text) { AttentionChip.contentOf(chips, captureScreen) }
        },
        onStop = chat::stop,
    )
}

@Composable
private fun Hint(
    text: String,
) = Column(
    modifier = Modifier.padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
) {
    Text(
        text = LocalAgent.current.name,
        style = MaterialTheme.typography.titleSmall,
    )
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
    )
    Text(
        text = if (LocalAgent.current.isTested) stringResource(Res.string.assistant_note) else stringResource(Res.string.assistant_note_untested, LocalAgent.current.name),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/**
 * The first line of what the user first asked, cut short at a word, as the conversation's name in the menu.
 */
internal fun conversationTitle(firstMessage: String): String {
    val line = firstMessage.lineSequence().first().trim()
    if (line.length <= TITLE_LENGTH) return line
    val cut = line.take(TITLE_LENGTH).substringBeforeLast(' ').ifEmpty { line.take(TITLE_LENGTH) }
    return cut.trimEnd('.', ',', ';', ':') + ELLIPSIS
}

private const val TITLE_LENGTH = 40
private const val ELLIPSIS = "…"

/**
 * How often a streaming reply is drawn again at most, rather than for each chunk.
 */
private val TRANSCRIPT_PERIOD = 50.milliseconds
