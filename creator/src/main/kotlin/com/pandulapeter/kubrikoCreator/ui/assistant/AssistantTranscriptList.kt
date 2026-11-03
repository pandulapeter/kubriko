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

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.agentclientprotocol.model.StopReason
import com.agentclientprotocol.model.ToolCallId
import com.agentclientprotocol.model.ToolCallStatus
import com.pandulapeter.kubrikoCreator.ai.assistant.FailureReason
import com.pandulapeter.kubrikoCreator.ai.assistant.TranscriptItem
import com.pandulapeter.kubrikoCreator.commands.AiStep
import com.pandulapeter.kubrikoCreator.ui.canBeShown
import com.pandulapeter.kubrikoCreator.ui.commandTitle
import com.pandulapeter.kubrikoCreator.ui.components.FailureDetails
import com.pandulapeter.kubrikoCreator.ui.description
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.assistant_failure_agent_not_started
import kubriko.creator.generated.resources.assistant_failure_agent_stopped
import kubriko.creator.generated.resources.assistant_failure_auth_required
import kubriko.creator.generated.resources.assistant_failure_conversation_lost
import kubriko.creator.generated.resources.assistant_failure_node_missing
import kubriko.creator.generated.resources.assistant_failure_prompt_failed
import kubriko.creator.generated.resources.assistant_failure_uv_missing
import kubriko.creator.generated.resources.assistant_thinking
import kubriko.creator.generated.resources.assistant_tool_failed
import kubriko.creator.generated.resources.assistant_turn_cancelled
import kubriko.creator.generated.resources.assistant_turn_max_tokens
import kubriko.creator.generated.resources.assistant_turn_max_turn_requests
import kubriko.creator.generated.resources.assistant_turn_refused
import kubriko.creator.generated.resources.assistant_working
import kubriko.creator.generated.resources.ic_ai
import kubriko.creator.generated.resources.ic_check
import kubriko.creator.generated.resources.ic_chevron
import kubriko.creator.generated.resources.ic_code
import kubriko.creator.generated.resources.ic_warning
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * A conversation's messages, thoughts, and tool calls, following the newest as it streams in.
 *
 * @param isBusy Whether the agent works on a turn, shown at the end until it writes something.
 * @param steps The AI step each of Kubriko Creator's tool calls made, by the call's ID ([aiStepsOf]).
 * @param onShowStep Goes to what a step worked on, in the editor.
 * @param onKeepChanges Keeps what the turn that began at a revision changed.
 * @param onUndoChanges Reverts what the turn that began at a revision changed.
 */
@Composable
internal fun AssistantTranscriptList(
    items: List<TranscriptItem>,
    isBusy: Boolean,
    steps: Map<ToolCallId, AiStep>,
    onShowStep: (AiStep) -> Unit,
    modifier: Modifier = Modifier,
    onKeepChanges: (sinceRevision: Int) -> Unit = {},
    onUndoChanges: (sinceRevision: Int) -> Unit = {},
) {
    val listState = rememberLazyListState()
    val last = items.lastOrNull()
    var isFollowing by remember { mutableStateOf(true) }
    LaunchedEffect(listState) {
        var previous = TranscriptScroll(0, 0)
        snapshotFlow { TranscriptScroll(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset) to listState.canScrollForward }
            .collect { (current, canScrollForward) ->
                isFollowing = followsEnd(isFollowing, previous, current, canScrollForward)
                previous = current
            }
    }
    LaunchedEffect(items.size, (last as? TranscriptItem.AgentMessage)?.text?.length, isBusy) {
        if (isFollowing && items.isNotEmpty()) listState.scrollToItem(items.lastIndex + if (isBusy) 1 else 0, Int.MAX_VALUE)
    }
    LazyColumn(
        modifier = modifier,
        state = listState,
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // The list only grows at its end or changes items in place, so their indices are stable keys.
        itemsIndexed(items, contentType = { _, item -> item::class }) { _, item ->
            when (item) {
                is TranscriptItem.UserMessage -> UserMessage(item.text)
                is TranscriptItem.AgentMessage -> SelectionContainer { MarkdownText(item.text) }
                is TranscriptItem.Thought -> Thought(item.text)
                is TranscriptItem.ToolCall -> ToolCallCard(
                    toolCall = item,
                    step = steps[item.id],
                    onShowStep = onShowStep,
                )
                is TranscriptItem.TurnChanges -> TurnChangesCard(
                    changes = item,
                    onKeep = { onKeepChanges(item.sinceRevision) },
                    onUndo = { onUndoChanges(item.sinceRevision) },
                )
                is TranscriptItem.TurnEnded -> Notice(stringResource(item.reason.description()))
                is TranscriptItem.Failure -> FailureNotice(
                    reason = item.reason,
                    details = item.details,
                )
            }
        }
        if (isBusy && last !is TranscriptItem.AgentMessage) {
            item { Working() }
        }
    }
}

@Composable
private fun UserMessage(
    text: String,
) = Box(
    modifier = Modifier.fillMaxWidth(),
    contentAlignment = Alignment.CenterEnd,
) {
    SelectionContainer {
        Text(
            modifier = Modifier
                .padding(start = 32.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

@Composable
private fun Thought(
    text: String,
) = Column {
    var isExpanded by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth().clickable { isExpanded = !isExpanded }.padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            modifier = Modifier.size(16.dp).rotate(if (isExpanded) 0f else -90f),
            painter = painterResource(Res.drawable.ic_chevron),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            modifier = Modifier.padding(start = 4.dp),
            text = stringResource(Res.string.assistant_thinking),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    if (isExpanded) {
        Text(
            modifier = Modifier.padding(start = 20.dp, top = 4.dp),
            text = text,
            style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ToolCallCard(
    toolCall: TranscriptItem.ToolCall,
    step: AiStep?,
    onShowStep: (AiStep) -> Unit,
) = Row(
    modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(8.dp))
        .background(MaterialTheme.colorScheme.surfaceContainer)
        .then(if (step != null && step.canBeShown()) Modifier.clickable { onShowStep(step) } else Modifier)
        .padding(horizontal = 10.dp, vertical = 6.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalAlignment = Alignment.CenterVertically,
) {
    Icon(
        modifier = Modifier.size(16.dp),
        painter = painterResource(if (toolCall.creatorToolName != null) Res.drawable.ic_ai else Res.drawable.ic_code),
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
    )
    Column(
        modifier = Modifier.weight(1f),
    ) {
        val title = creatorToolTitleOf(toolCall)?.let { stringResource(it) } ?: toolCall.title
        Text(
            text = if (toolCall.status == ToolCallStatus.FAILED) stringResource(Res.string.assistant_tool_failed, title) else title,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        step?.target?.let { target ->
            Text(
                text = target.description(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
    when (toolCall.status) {
        ToolCallStatus.COMPLETED -> StatusIcon(Res.drawable.ic_check)
        ToolCallStatus.FAILED -> StatusIcon(Res.drawable.ic_warning, isError = true)
        else -> CircularProgressIndicator(
            modifier = Modifier.size(14.dp),
            strokeWidth = 2.dp,
        )
    }
}

@Composable
private fun StatusIcon(
    icon: DrawableResource,
    isError: Boolean = false,
) = Icon(
    modifier = Modifier.size(16.dp),
    painter = painterResource(icon),
    contentDescription = null,
    tint = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
)

@Composable
private fun Working() = Row(
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalAlignment = Alignment.CenterVertically,
) {
    CircularProgressIndicator(
        modifier = Modifier.size(14.dp),
        strokeWidth = 2.dp,
    )
    Text(
        text = stringResource(Res.string.assistant_working),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun Notice(
    text: String,
    isError: Boolean = false,
) = Text(
    text = text,
    style = MaterialTheme.typography.bodySmall,
    color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
)

/**
 * The title of the Kubriko Creator command [toolCall] runs, or null if it runs a tool of the agent's own.
 */
internal fun creatorToolTitleOf(toolCall: TranscriptItem.ToolCall) = toolCall.creatorToolName?.let(::commandTitle)

private fun StopReason.description() = when (this) {
    StopReason.REFUSAL -> Res.string.assistant_turn_refused
    StopReason.MAX_TOKENS -> Res.string.assistant_turn_max_tokens
    StopReason.MAX_TURN_REQUESTS -> Res.string.assistant_turn_max_turn_requests
    else -> Res.string.assistant_turn_cancelled
}

@Composable
private fun FailureNotice(
    reason: FailureReason,
    details: String?,
) = Column(
    verticalArrangement = Arrangement.spacedBy(2.dp),
) {
    Notice(
        text = reason.description(),
        isError = true,
    )
    details?.takeIf { it.isNotBlank() }?.let { FailureDetails(it) }
}

/**
 * What the panel says about [this] failure, in one plain sentence; the error's own words go under it ([FailureDetails]).
 */
@Composable
internal fun FailureReason.description(): String = stringResource(
    when (this) {
        FailureReason.NODE_MISSING -> Res.string.assistant_failure_node_missing
        FailureReason.UV_MISSING -> Res.string.assistant_failure_uv_missing
        FailureReason.AGENT_NOT_STARTED -> Res.string.assistant_failure_agent_not_started
        FailureReason.AGENT_STOPPED -> Res.string.assistant_failure_agent_stopped
        FailureReason.CONVERSATION_LOST -> Res.string.assistant_failure_conversation_lost
        FailureReason.AUTH_REQUIRED -> Res.string.assistant_failure_auth_required
        FailureReason.PROMPT_FAILED -> Res.string.assistant_failure_prompt_failed
    },
    LocalAgent.current.name,
)
