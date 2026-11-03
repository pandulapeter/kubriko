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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoCreator.ui.components.IconActionButton
import com.pandulapeter.kubrikoCreator.ui.components.Popover
import com.pandulapeter.kubrikoCreator.ui.components.rememberMenuAnchor
import com.pandulapeter.kubrikoCreator.ui.components.rememberRightClickMenuState
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.action_assistant_setup
import kubriko.creator.generated.resources.action_close_conversation
import kubriko.creator.generated.resources.action_new_conversation
import kubriko.creator.generated.resources.ic_add
import kubriko.creator.generated.resources.ic_close
import kubriko.creator.generated.resources.ic_settings
import org.jetbrains.compose.resources.stringResource

/**
 * The assistant's one header line: the [conversation] it shows, then a button opening its [setup] (which agent it runs, as
 * whom, and with what options, which rarely change, so they stay out of sight; [setupSummary] names them in the button's
 * tooltip; a choice that goes on in the panel, such as signing in, closes it), and starting or closing a conversation.
 */
@Composable
internal fun AssistantHeader(
    setupSummary: String,
    canClose: Boolean,
    onNewConversation: () -> Unit,
    onCloseConversation: () -> Unit,
    conversation: @Composable (Modifier) -> Unit,
    setup: @Composable ColumnScope.(close: () -> Unit) -> Unit,
) = Row(
    modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, top = 4.dp),
    verticalAlignment = Alignment.CenterVertically,
) {
    conversation(Modifier.weight(1f))
    val menu = rememberRightClickMenuState()
    val anchor = rememberMenuAnchor(menu)
    IconActionButton(
        modifier = anchor.modifier,
        icon = Res.drawable.ic_settings,
        label = stringResource(Res.string.action_assistant_setup, setupSummary),
        onClick = anchor::open,
    )
    Popover(
        state = menu,
        modifier = Modifier.width(SETUP_WIDTH),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            setup(menu::close)
        }
    }
    IconActionButton(
        icon = Res.drawable.ic_add,
        label = Res.string.action_new_conversation,
        onClick = onNewConversation,
    )
    if (canClose) {
        IconActionButton(
            icon = Res.drawable.ic_close,
            label = Res.string.action_close_conversation,
            onClick = onCloseConversation,
        )
    }
}

private val SETUP_WIDTH = 320.dp
