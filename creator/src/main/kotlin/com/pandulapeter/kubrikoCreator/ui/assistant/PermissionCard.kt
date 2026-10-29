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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.agentclientprotocol.model.PermissionOption
import com.agentclientprotocol.model.PermissionOptionKind
import com.pandulapeter.kubrikoCreator.ai.assistant.PermissionRequest
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.assistant_permission
import org.jetbrains.compose.resources.stringResource

/**
 * The agent asking to run a tool call, with the answers it offers (allow once, always, or reject), as the agent names them.
 */
@Composable
internal fun PermissionCard(
    request: PermissionRequest,
    onAnswer: (PermissionOption) -> Unit,
    modifier: Modifier = Modifier,
) = Column(
    modifier = modifier
        .fillMaxWidth()
        .padding(8.dp)
        .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(8.dp))
        .padding(10.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp),
) {
    Text(
        text = stringResource(Res.string.assistant_permission, request.toolCall.title ?: request.toolCall.toolCallId.value),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSecondaryContainer,
    )
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        request.options.forEach { option ->
            if (option.kind == PermissionOptionKind.ALLOW_ONCE || option.kind == PermissionOptionKind.ALLOW_ALWAYS) {
                FilledTonalButton(onClick = { onAnswer(option) }) { Text(option.name) }
            } else {
                OutlinedButton(onClick = { onAnswer(option) }) { Text(option.name) }
            }
        }
    }
}
