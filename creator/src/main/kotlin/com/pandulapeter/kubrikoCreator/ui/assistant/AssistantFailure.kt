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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoCreator.ai.assistant.ChatState
import com.pandulapeter.kubrikoCreator.ai.assistant.FailureReason
import com.pandulapeter.kubrikoCreator.ui.components.FailureDetails
import com.pandulapeter.kubrikoCreator.ui.components.openInBrowser
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.action_download_node
import kubriko.creator.generated.resources.action_install_node
import kubriko.creator.generated.resources.action_install_uv
import kubriko.creator.generated.resources.action_open_agent_log
import kubriko.creator.generated.resources.action_try_again
import org.jetbrains.compose.resources.stringResource

/**
 * Why the conversation couldn't start or go on, with its details and what can be done: installing what is missing (or
 * downloading Node.js for Creator alone), opening the agent's log, and trying again.
 */
@Composable
internal fun AssistantFailure(
    state: ChatState.Failed,
    onTryAgain: (() -> Unit)?,
    onOpenLog: (() -> Unit)?,
    modifier: Modifier = Modifier,
    onInstallNode: (() -> Unit)? = null,
    nodeInstallFailure: String? = null,
) = Column(
    modifier = modifier.verticalScroll(rememberScrollState()).padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
) {
    Text(
        text = state.reason.description(),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.error,
    )
    if (state.details.isNotEmpty()) FailureDetails(state.details.joinToString("\n"))
    if (state.reason == FailureReason.NODE_MISSING) {
        onInstallNode?.let { install ->
            FilledTonalButton(onClick = install) {
                Text(stringResource(Res.string.action_download_node))
            }
        }
        nodeInstallFailure?.let { FailureDetails(it) }
        TextButton(onClick = { openInBrowser(NODE_DOWNLOAD_URL) }) {
            Text(stringResource(Res.string.action_install_node))
        }
    }
    if (state.reason == FailureReason.UV_MISSING) {
        TextButton(onClick = { openInBrowser(UV_INSTALL_URL) }) {
            Text(stringResource(Res.string.action_install_uv))
        }
    }
    onOpenLog?.let { openLog ->
        TextButton(onClick = openLog) {
            Text(stringResource(Res.string.action_open_agent_log))
        }
    }
    onTryAgain?.let { tryAgain ->
        FilledTonalButton(onClick = tryAgain) {
            Text(stringResource(Res.string.action_try_again))
        }
    }
}

private const val NODE_DOWNLOAD_URL = "https://nodejs.org/en/download"
private const val UV_INSTALL_URL = "https://docs.astral.sh/uv/getting-started/installation/"
