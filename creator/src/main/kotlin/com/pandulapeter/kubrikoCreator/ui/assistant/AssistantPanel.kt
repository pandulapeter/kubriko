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
import androidx.compose.runtime.rememberCoroutineScope
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
import com.pandulapeter.kubrikoCreator.ai.assistant.AuthStatus
import com.pandulapeter.kubrikoCreator.ai.assistant.ChatState
import com.pandulapeter.kubrikoCreator.ai.assistant.FailureReason
import com.pandulapeter.kubrikoCreator.ai.assistant.SignInMethod
import com.pandulapeter.kubrikoCreator.commands.AiStep
import com.pandulapeter.kubrikoCreator.commands.UserQuestions
import com.pandulapeter.kubrikoCreator.ui.LocalAiActivitySettings
import com.pandulapeter.kubrikoCreator.ui.components.openInSystemEditor
import kotlinx.coroutines.launch
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.assistant_empty
import kubriko.creator.generated.resources.assistant_note
import kubriko.creator.generated.resources.assistant_note_untested
import kubriko.creator.generated.resources.assistant_question_late_answer
import kubriko.creator.generated.resources.assistant_setup_agent
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Duration.Companion.milliseconds

/**
 * The in-app assistant: conversations about the open project with the agent chosen, which works through the same
 * commands as the editor.
 *
 * @param aiSteps The open project's AI steps, which the tool call cards tie into.
 * @param onShowStep Goes to what a step worked on, in the editor.
 * @param attention What the user is looking at, offered with the next message.
 * @param captureScreenshot Takes the picture an [AttentionChip.Screenshot] sends.
 * @param questions The questions waiting for an answer, which the conversation that asked shows.
 * @param onAnswer Answers a question.
 */
@Composable
internal fun AssistantPanel(
    model: AssistantPanelModel,
    aiSteps: List<AiStep>,
    onShowStep: (AiStep) -> Unit,
    modifier: Modifier = Modifier,
    attention: List<AttentionChip> = emptyList(),
    captureScreenshot: suspend (AttentionChip.Screenshot) -> ByteArray? = { null },
    forms: AssistantForms = rememberAssistantForms(),
    questions: List<UserQuestions.Question> = emptyList(),
    onAnswer: (id: Long, answer: String) -> Unit = { _, _ -> },
) {
    val agent by model.agent.collectAsState()
    CompositionLocalProvider(LocalAgent provides agent) {
        AssistantPanelContent(
            model = model,
            forms = forms,
            aiSteps = aiSteps,
            onShowStep = onShowStep,
            attention = attention,
            captureScreenshot = captureScreenshot,
            modifier = modifier,
            questions = questions,
            onAnswer = onAnswer,
        )
    }
}

@Composable
private fun AssistantPanelContent(
    model: AssistantPanelModel,
    forms: AssistantForms,
    aiSteps: List<AiStep>,
    onShowStep: (AiStep) -> Unit,
    attention: List<AttentionChip>,
    captureScreenshot: suspend (AttentionChip.Screenshot) -> ByteArray?,
    modifier: Modifier = Modifier,
    questions: List<UserQuestions.Question>,
    onAnswer: (id: Long, answer: String) -> Unit,
) = Column(
    modifier = modifier,
) {
    val chats by model.chats.collectAsState()
    val chat by model.currentChat.collectAsState()
    val signInMethods by model.signInMethods.collectAsState()
    val hasApiKey by model.hasApiKey.collectAsState()
    LaunchedEffect(Unit) {
        model.findConfigurations()
        if (model.currentChat.value == null) model.newChat()
    }
    ConversationBar(
        canDelete = chat?.isEmpty?.collectAsState()?.value == false,
        onDelete = { chat?.let(model::delete) },
    ) { modifier ->
        ConversationMenu(
            modifier = modifier,
            chats = chats,
            current = chat,
            savedConversations = model.savedConversations.collectAsState().value,
            onSelected = model::select,
            onResume = model::resume,
        )
    }
    if (forms.isEnteringCustomAgent) {
        CustomAgentForm(
            initialCommand = model.customAgentCommand,
            onSave = { command ->
                forms.isEnteringCustomAgent = false
                model.selectAgent(AgentChoice.Custom(command))
            },
            onCancel = { forms.isEnteringCustomAgent = false },
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
            captureScreenshot = captureScreenshot,
            onInstallNode = model::installNode.takeIf { model.canInstallNode },
            nodeInstallFailure = model.nodeInstallFailure.collectAsState().value,
            isBilledToApiKey = hasApiKey,
            isAgentChangingFilesUnasked = LocalAgent.current.identity in model.agentsChangingFilesUnasked.collectAsState().value,
            onOpenLog = model.errorLogFile?.let { file -> { if (file.isFile) openInSystemEditor(file) } },
            aiSteps = aiSteps,
            onShowStep = onShowStep,
            questions = questions,
            onAnswer = onAnswer,
        ) { isPromptRefused ->
            AssistantSignInArea(
                session = session,
                isEnteringApiKey = forms.isEnteringApiKey,
                needsSignIn = needsSignIn || isPromptRefused,
                methods = signInMethods,
                canKeepApiKey = model.canKeepApiKey,
                hasApiKeyFailed = hasApiKeyFailed,
                onSignIn = model::signIn,
                onCloseSignIn = model::closeSignIn,
                onUseApiKey = { forms.isEnteringApiKey = true },
                onSaveApiKey = { key -> model.setApiKey(key) { forms.isEnteringApiKey = false } },
                onCancelApiKey = { forms.isEnteringApiKey = false },
                variablesForm = model.variablesForm.collectAsState().value,
                hasVariablesFailed = model.variablesFailed.collectAsState().value,
                onSaveVariables = model::setVariables,
                onCancelVariables = model::closeVariablesForm,
                systemTerminal = rememberSystemTerminalOffer(model),
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
    captureScreenshot: suspend (AttentionChip.Screenshot) -> ByteArray?,
    download: AgentDownload?,
    isBilledToApiKey: Boolean,
    isAgentChangingFilesUnasked: Boolean,
    onOpenLog: (() -> Unit)?,
    aiSteps: List<AiStep>,
    onShowStep: (AiStep) -> Unit,
    questions: List<UserQuestions.Question>,
    onAnswer: (id: Long, answer: String) -> Unit,
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
    val transcriptQuestions = remember(transcript.items, questions, toolsId) { questionsOf(transcript.items, questions, toolsId) }
    val answering = rememberCoroutineScope()
    Box(
        modifier = Modifier.weight(1f).fillMaxWidth(),
    ) {
        when {
            transcript.items.isNotEmpty() || transcriptQuestions.trailing.isNotEmpty() -> AssistantTranscriptList(
                modifier = Modifier.fillMaxSize(),
                items = transcript.items,
                isBusy = isBusy,
                steps = steps,
                onShowStep = onShowStep,
                isTurnReviewShown = LocalAiActivitySettings.current.isTurnReviewShown,
                onKeepChanges = chat::keepChanges,
                onUndoChanges = chat::undoChanges,
                questions = transcriptQuestions,
                canSendMessage = state == ChatState.Ready && !isBusy,
                onAnswer = { question, answer, isLate ->
                    onAnswer(question.id, answer)
                    if (isLate) answering.launch { chat.send(getString(Res.string.assistant_question_late_answer, question.text, answer)) }
                },
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
    if (isAgentChangingFilesUnasked) {
        UnaskedChangesNotice()
    }
    var toggled by remember(chat) { mutableStateOf(emptySet<String>()) }
    AttentionLine(
        chips = attention,
        toggled = toggled,
        onToggle = { chip -> toggled = if (chip.key in toggled) toggled - chip.key else toggled + chip.key },
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
            val chips = attention.filter { it.isSentWith(toggled) }
            chat.send(text) { AttentionChip.contentOf(chips, captureScreenshot) }
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

private const val TITLE_LENGTH = 80
private const val ELLIPSIS = "…"

/**
 * How often a streaming reply is drawn again at most, rather than for each chunk.
 */
private val TRANSCRIPT_PERIOD = 50.milliseconds

@Composable
private fun rememberSystemTerminalOffer(model: AssistantPanelModel): SystemTerminalOffer? {
    if (!model.canSignInInSystemTerminal) return null
    val isOpening by model.isOpeningSystemTerminal.collectAsState()
    val hasFailed by model.systemTerminalFailed.collectAsState()
    val onContinue = remember(model) { model::signInInSystemTerminal }
    return SystemTerminalOffer(isOpening, hasFailed, onContinue)
}
