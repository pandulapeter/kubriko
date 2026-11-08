/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.screenplay

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import com.pandulapeter.kubrikoCreator.ai.assistant.AssistantChat
import com.pandulapeter.kubrikoCreator.ai.assistant.AssistantTranscript
import com.pandulapeter.kubrikoCreator.ai.assistant.TranscriptItem
import com.pandulapeter.kubrikoCreator.commands.CommandHistory
import com.pandulapeter.kubrikoCreator.commands.screenplay.pageBeforeTurn
import com.pandulapeter.kubrikoCreator.ui.LocalAiActivitySettings
import com.pandulapeter.kubrikoCreator.ui.components.LocalIsAiEnabled
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

/**
 * What the assistant changed on a page in a turn still offered for review: the page as it was before the turn ([before]),
 * and Keep and Undo for everything the turn changed, as its card in the conversation has them.
 */
internal class PageReview(
    val before: String,
    val onKeep: () -> Unit,
    val onUndo: () -> Unit,
)

/**
 * The review of the page named, or null while no turn offered for review changed it ([rememberPageReviews]).
 */
internal val LocalPageReviews = staticCompositionLocalOf<(page: String) -> PageReview?> { { null } }

/**
 * A turn of [chat] whose changes since [sinceRevision] are offered to keep or undo, made through the tool address
 * [conversationId].
 */
internal data class OfferedTurn(
    val chat: AssistantChat,
    val conversationId: String,
    val sinceRevision: Int,
)

/**
 * The turn of a conversation offered for review, if its last review is still offered.
 */
internal fun offeredTurnOf(chat: AssistantChat, transcript: AssistantTranscript, conversationId: String?): OfferedTurn? {
    val review = transcript.items.lastOrNull { it is TranscriptItem.TurnReview } as? TranscriptItem.TurnReview ?: return null
    return if (review.state == TranscriptItem.TurnReview.State.OFFERED && conversationId != null) OfferedTurn(chat, conversationId, review.sinceRevision) else null
}

/**
 * The review of each page the turns [chats] offer for review changed, read from the undo [history]; none while Keep and
 * Undo after a turn are turned off in the settings, or AI features are.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@Composable
internal fun rememberPageReviews(chats: StateFlow<List<AssistantChat>>, history: StateFlow<CommandHistory>): (page: String) -> PageReview? {
    val isShown = LocalAiActivitySettings.current.isTurnReviewShown && LocalIsAiEnabled.current
    val offered = remember(chats) {
        chats.flatMapLatest { open ->
            if (open.isEmpty()) {
                flowOf(emptyList())
            } else {
                combine(open.map { chat -> combine(chat.transcript, chat.toolsId) { transcript, id -> offeredTurnOf(chat, transcript, id) } }) { turns -> turns.filterNotNull() }
            }
        }.distinctUntilChanged()
    }.collectAsState(emptyList())
    val currentHistory = history.collectAsState()
    return remember(offered, currentHistory, isShown) { { page -> pageReviewOf(page, offered.value, currentHistory.value, isShown) } }
}

/**
 * The review of the page named [page] by the first of the [offered] turns that changed it, read from the undo [history],
 * or none while reviews aren't [isShown].
 */
internal fun pageReviewOf(page: String, offered: List<OfferedTurn>, history: CommandHistory, isShown: Boolean): PageReview? = if (!isShown) {
    null
} else {
    offered.firstNotNullOfOrNull { turn ->
        history.pageBeforeTurn(turn.conversationId, turn.sinceRevision, page)?.let { before ->
            PageReview(before, onKeep = { turn.chat.keepChanges(turn.sinceRevision) }, onUndo = { turn.chat.undoChanges(turn.sinceRevision) })
        }
    }
}
