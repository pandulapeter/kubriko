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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoCreator.ai.assistant.AgentChoice
import com.pandulapeter.kubrikoCreator.ai.assistant.RegistryAgent
import com.pandulapeter.kubrikoCreator.ui.components.IconActionButton
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.action_close_conversation
import kubriko.creator.generated.resources.action_new_conversation
import kubriko.creator.generated.resources.ic_add
import kubriko.creator.generated.resources.ic_close

/**
 * The assistant's first line: the agent it runs, and a new conversation or closing the one shown.
 */
@Composable
internal fun AgentRow(
    agent: AgentChoice,
    agents: List<RegistryAgent>,
    customAgentCommand: String?,
    canClose: Boolean,
    onAgentSelected: (AgentChoice) -> Unit,
    onEnterCustomAgent: () -> Unit,
    onOpenLog: (() -> Unit)?,
    onNewConversation: () -> Unit,
    onCloseConversation: () -> Unit,
) = Row(
    modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 4.dp, top = 4.dp),
    verticalAlignment = Alignment.CenterVertically,
) {
    Box(Modifier.weight(1f)) {
        AgentMenu(
            agent = agent,
            agents = agents,
            customAgentCommand = customAgentCommand,
            onSelected = onAgentSelected,
            onEnterCustomAgent = onEnterCustomAgent,
            onOpenLog = onOpenLog,
        )
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
