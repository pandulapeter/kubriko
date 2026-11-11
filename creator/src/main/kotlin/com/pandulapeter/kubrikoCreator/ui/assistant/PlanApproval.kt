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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.pandulapeter.kubrikoCreator.ai.assistant.AssistantChat
import com.pandulapeter.kubrikoCreator.ai.assistant.ChatState
import com.pandulapeter.kubrikoCreator.ai.assistant.SmallerModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * What the plan's checklist offers: the [smallerModel] to approve it with, if the agent has one, and [onApprove], which
 * sends the approval, or null while the plan isn't to be approved.
 */
internal class PlanApproval(
    val smallerModel: SmallerModel?,
    val onApprove: ((String, SmallerModel?) -> Unit)?,
)

/**
 * The [chat]'s [PlanApproval]. Whether its plan awaits approval is read from the transcript as it is, not as the panel
 * shows it a moment later, so an approval just sent never offers the button again.
 */
@Composable
internal fun rememberPlanApproval(chat: AssistantChat, state: ChatState, isBusy: Boolean, isAnyQuestionOpen: Boolean): PlanApproval {
    val isPlanAwaitingApproval by remember(chat) { chat.transcript.map { it.isPlanAwaitingApproval }.distinctUntilChanged() }
        .collectAsState(chat.transcript.value.isPlanAwaitingApproval)
    val configOptions by chat.configOptions.collectAsState()
    val approve = remember(chat) { { text: String, model: SmallerModel? -> chat.send(text, model = model) } }
    val isOffered = isPlanApprovalOffered(isPlanAwaitingApproval, state, isBusy, isAnyQuestionOpen)
    return remember(configOptions, isOffered, approve) { PlanApproval(SmallerModel.of(configOptions), approve.takeIf { isOffered }) }
}

/**
 * Whether the plan's Approve button shows: the agent laid out a plan in its last turn and started none of it, and it
 * takes prompts again. While a question of the conversation is open, even one whose call stopped waiting, its answer goes
 * first, through the question's card or the composer, which an approval sent past it would leave unanswered.
 */
internal fun isPlanApprovalOffered(isPlanAwaitingApproval: Boolean, state: ChatState, isBusy: Boolean, isAnyQuestionOpen: Boolean) =
    isPlanAwaitingApproval && state == ChatState.Ready && !isBusy && !isAnyQuestionOpen
