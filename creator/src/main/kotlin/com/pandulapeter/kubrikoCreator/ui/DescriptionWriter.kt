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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import com.pandulapeter.kubrikoCreator.ai.assistant.AssistantPanelModel
import com.pandulapeter.kubrikoCreator.ai.assistant.ChatState
import com.pandulapeter.kubrikoCreator.ui.components.LocalIsAiEnabled

/**
 * Has the in-app assistant write a description, by sending [write] the prompt asking for it.
 *
 * @param isBusy Whether the assistant is in the middle of a turn, which a prompt can't interrupt.
 */
@Immutable
internal class DescriptionWriter(
    val isBusy: Boolean,
    val write: (prompt: String) -> Unit,
)

/**
 * The [DescriptionWriter] of the workspace, or null while no conversation of the assistant takes prompts.
 */
internal val LocalDescriptionWriter = compositionLocalOf<DescriptionWriter?> { null }

/**
 * A [DescriptionWriter] sending its prompts to the conversation [assistant] shows, then calling [onSent], or null while
 * AI features are off or that conversation doesn't take prompts.
 */
@Composable
internal fun rememberDescriptionWriter(
    assistant: AssistantPanelModel,
    onSent: () -> Unit,
): DescriptionWriter? {
    val chat = assistant.currentChat.collectAsState().value
    val state = chat?.state?.collectAsState()?.value
    val isBusy = chat?.isBusy?.collectAsState()?.value == true
    val currentOnSent by rememberUpdatedState(onSent)
    val writer = remember(chat, isBusy) {
        chat?.let {
            DescriptionWriter(isBusy = isBusy) { prompt ->
                chat.send(prompt)
                currentOnSent()
            }
        }
    }
    return writer.takeIf { LocalIsAiEnabled.current && state == ChatState.Ready }
}
