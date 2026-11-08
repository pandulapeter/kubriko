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
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import kotlinx.coroutines.flow.map

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
 * The turns offered for review and what they changed ([rememberPageReviews]), which [rememberReviewOf] reads a page's
 * review from.
 *
 * @param isShown Whether reviews are shown at all: Keep and Undo after a turn and AI features are both on.
 */
internal data class PageReviews(
    val offered: List<OfferedTurn> = emptyList(),
    val history: CommandHistory = CommandHistory(),
    val isShown: Boolean = false,
)

internal val LocalPageReviews = compositionLocalOf { PageReviews() }

/**
 * The review of the page named [page], read again only when the turns offered or the history change.
 */
@Composable
internal fun PageReviews.rememberReviewOf(page: String): PageReview? = remember(this, page) { pageReviewOf(page, offered, history, isShown) }

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
 * The last review of a conversation's [transcript], if any.
 */
internal fun lastReviewOf(transcript: AssistantTranscript): TranscriptItem.TurnReview? = transcript.items.lastOrNull { it is TranscriptItem.TurnReview } as? TranscriptItem.TurnReview

/**
 * The turn of [chat] offered for review, if its last [review] is still offered and no later turn is running ([isBusy]),
 * which the review's Undo would undo too.
 */
internal fun offeredTurnOf(chat: AssistantChat, review: TranscriptItem.TurnReview?, conversationId: String?, isBusy: Boolean): OfferedTurn? =
    if (review?.state == TranscriptItem.TurnReview.State.OFFERED && conversationId != null && !isBusy) OfferedTurn(chat, conversationId, review.sinceRevision) else null

/**
 * The turns of [chats] offered for review and the undo [history] they are read from, for [LocalPageReviews].
 */
@OptIn(ExperimentalCoroutinesApi::class)
@Composable
internal fun rememberPageReviews(chats: StateFlow<List<AssistantChat>>, history: StateFlow<CommandHistory>): PageReviews {
    val isShown = LocalAiActivitySettings.current.isTurnReviewShown && LocalIsAiEnabled.current
    val offered by remember(chats) {
        chats.flatMapLatest { open ->
            if (open.isEmpty()) {
                flowOf(emptyList())
            } else {
                combine(
                    open.map { chat ->
                        // A transcript changes with every streamed word, its last review far more rarely.
                        val review = chat.transcript.map(::lastReviewOf).distinctUntilChanged()
                        combine(review, chat.toolsId, chat.isBusy) { last, id, isBusy -> offeredTurnOf(chat, last, id, isBusy) }
                    },
                ) { turns -> turns.filterNotNull() }
            }
        }.distinctUntilChanged()
    }.collectAsState(emptyList())
    val currentHistory by history.collectAsState()
    return PageReviews(offered, currentHistory, isShown)
}

/**
 * The review of the page named [page] by the first of the [offered] turns whose undoing would change it, read from the
 * undo [history], or none while reviews aren't [isShown].
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
