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

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.pandulapeter.kubrikoCreator.ai.assistant.ConversationMatch

/**
 * The assistant panel's search of its saved conversations: whether it is shown in place of the conversation, what is
 * typed into it, the message chosen that the conversation shown scrolls to, and the one of another agent being read.
 */
internal class ConversationSearchState {
    var isOpen by mutableStateOf(false)
        private set
    var query by mutableStateOf("")
    var jump: ConversationMatch? by mutableStateOf(null)
        private set
    var reading: ConversationMatch? by mutableStateOf(null)
        private set

    fun open() {
        isOpen = true
    }

    fun close() {
        isOpen = false
    }

    /**
     * Goes to [match]: shown with [show] and scrolled to, when its conversation's agent is the one chosen; otherwise read
     * from its kept messages, since only its own agent can continue it.
     */
    fun choose(match: ConversationMatch, show: (ConversationMatch) -> Unit) {
        if (match.isHeldByAnotherAgent) {
            reading = match
        } else {
            jump = match
            isOpen = false
            show(match)
        }
    }

    /**
     * Forgets the message to scroll to, once the conversation has scrolled to it.
     */
    fun onJumped() {
        jump = null
    }

    /**
     * Forgets the message to scroll to when the conversation shown, [shownId], is known and isn't the one it is in: its
     * conversation didn't open (the assistant was off), or the user went to another, so it never scrolls later.
     */
    fun onConversationShown(shownId: String?) {
        if (shownId != null && shownId != jump?.conversation?.id) jump = null
    }

    fun stopReading() {
        reading = null
    }
}
