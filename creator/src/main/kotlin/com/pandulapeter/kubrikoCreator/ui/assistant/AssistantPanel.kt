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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.agentclientprotocol.model.SessionConfigOptionValue
import com.pandulapeter.kubrikoCreator.ai.assistant.AssistantChat
import com.pandulapeter.kubrikoCreator.ai.assistant.AssistantController
import com.pandulapeter.kubrikoCreator.ai.assistant.ChatState
import com.pandulapeter.kubrikoCreator.ai.assistant.FailureReason
import com.pandulapeter.kubrikoCreator.ui.components.IconActionButton
import com.pandulapeter.kubrikoCreator.ui.components.MenuButton
import com.pandulapeter.kubrikoCreator.ui.components.PanelHeader
import com.pandulapeter.kubrikoCreator.ui.components.openInBrowser
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.action_close_conversation
import kubriko.creator.generated.resources.action_install_node
import kubriko.creator.generated.resources.action_new_conversation
import kubriko.creator.generated.resources.action_try_again
import kubriko.creator.generated.resources.assistant_agent
import kubriko.creator.generated.resources.assistant_empty
import kubriko.creator.generated.resources.assistant_note
import kubriko.creator.generated.resources.assistant_starting
import kubriko.creator.generated.resources.assistant_title
import kubriko.creator.generated.resources.ic_add
import kubriko.creator.generated.resources.ic_chat
import kubriko.creator.generated.resources.ic_close
import org.jetbrains.compose.resources.stringResource

/**
 * The in-app assistant: conversations about the open project with Claude Agent, which works through the same
 * commands as the editor.
 */
@Composable
internal fun AssistantPanel(
    controller: AssistantController,
    modifier: Modifier = Modifier,
) = Column(
    modifier = modifier,
) {
    val chats by controller.chats.collectAsState()
    val chat by controller.currentChat.collectAsState()
    LaunchedEffect(Unit) {
        controller.findConfigurations()
        if (controller.currentChat.value == null) controller.newChat()
    }
    PanelHeader(
        icon = Res.drawable.ic_chat,
        title = stringResource(Res.string.assistant_title),
    ) {
        IconActionButton(
            icon = Res.drawable.ic_add,
            label = Res.string.action_new_conversation,
            onClick = controller::newChat,
        )
        chat?.let { current ->
            IconActionButton(
                icon = Res.drawable.ic_close,
                label = Res.string.action_close_conversation,
                onClick = { controller.close(current) },
            )
        }
    }
    if (chats.size > 1) {
        chat?.let { current ->
            Box(Modifier.padding(horizontal = 12.dp)) {
                MenuButton(
                    label = current.title(),
                    options = chats.asReversed(),
                    optionText = { it.title() },
                    onSelected = controller::select,
                )
            }
        }
    }
    AssistantOptionsRow(
        configuration = controller.configuration.collectAsState().value,
        configurations = controller.configurations.collectAsState().value,
        configOptions = chat?.configOptions?.collectAsState()?.value.orEmpty(),
        onConfigurationSelected = controller::selectConfiguration,
        onConfigOptionSelected = { option, choice -> chat?.setConfigOption(option.id, SessionConfigOptionValue.of(choice.value.value)) },
    )
    HorizontalDivider()
    chat?.let { current ->
        ChatContent(
            modifier = Modifier.weight(1f),
            chat = current,
        )
    }
}

@Composable
private fun ChatContent(
    chat: AssistantChat,
    modifier: Modifier = Modifier,
) = Column(
    modifier = modifier,
) {
    val state by chat.state.collectAsState()
    val transcript by chat.transcript.collectAsState()
    val isBusy by chat.isBusy.collectAsState()
    val permissionRequest by chat.permissionRequest.collectAsState()
    Box(
        modifier = Modifier.weight(1f).fillMaxWidth(),
    ) {
        when {
            transcript.items.isNotEmpty() -> AssistantTranscriptList(
                modifier = Modifier.fillMaxSize(),
                items = transcript.items,
                isBusy = isBusy,
            )
            state is ChatState.Ready -> Hint(stringResource(Res.string.assistant_empty))
            state is ChatState.Starting -> Starting()
        }
        (state as? ChatState.Failed)?.takeIf { transcript.items.isEmpty() || it.reason == FailureReason.CONVERSATION_LOST }?.let { failed ->
            Failure(
                modifier = Modifier.fillMaxSize(),
                state = failed,
                onTryAgain = chat::retry.takeIf { failed.reason != FailureReason.CONVERSATION_LOST },
            )
        }
    }
    if (transcript.plan.isNotEmpty()) {
        HorizontalDivider()
        PlanChecklist(transcript.plan)
    }
    permissionRequest?.let { request ->
        PermissionCard(
            request = request,
            onAnswer = chat::answer,
        )
    }
    HorizontalDivider()
    AssistantComposer(
        commands = chat.availableCommands.collectAsState().value,
        isEnabled = state == ChatState.Ready,
        isBusy = isBusy,
        onSend = chat::send,
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
        text = stringResource(Res.string.assistant_agent),
        style = MaterialTheme.typography.titleSmall,
    )
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
    )
    Text(
        text = stringResource(Res.string.assistant_note),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun Starting() = Column(
    modifier = Modifier.padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
) {
    CircularProgressIndicator(
        modifier = Modifier.size(20.dp),
        strokeWidth = 2.dp,
    )
    Text(
        text = stringResource(Res.string.assistant_starting),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun Failure(
    state: ChatState.Failed,
    onTryAgain: (() -> Unit)?,
    modifier: Modifier = Modifier,
) = Column(
    modifier = modifier.verticalScroll(rememberScrollState()).padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
) {
    Text(
        text = state.reason.description(),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.error,
    )
    if (state.details.isNotEmpty()) {
        Text(
            text = state.details.joinToString("\n"),
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    if (state.reason == FailureReason.NODE_MISSING) {
        TextButton(onClick = { openInBrowser(NODE_DOWNLOAD_URL) }) {
            Text(stringResource(Res.string.action_install_node))
        }
    }
    onTryAgain?.let { tryAgain ->
        FilledTonalButton(onClick = tryAgain) {
            Text(stringResource(Res.string.action_try_again))
        }
    }
}

@Composable
private fun AssistantChat.title() = transcript.collectAsState().value.title?.let(::conversationTitle)
    ?: stringResource(Res.string.action_new_conversation)

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
private const val NODE_DOWNLOAD_URL = "https://nodejs.org/en/download"
