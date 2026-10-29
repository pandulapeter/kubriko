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
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoCreator.ai.assistant.SignInMethod
import com.pandulapeter.kubrikoCreator.ui.components.WithTooltip
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.assistant_account_use_api_key
import kubriko.creator.generated.resources.assistant_sign_in_note
import kubriko.creator.generated.resources.assistant_sign_in_title
import org.jetbrains.compose.resources.stringResource

/**
 * The agent isn't signed in: its sign-in methods, each a button described by the agent, and giving an API key instead.
 * Signing in always completes in the agent's own flow, so Kubriko Creator never sees the account's credentials.
 */
@Composable
internal fun SignInCard(
    methods: List<SignInMethod>,
    canKeepApiKey: Boolean,
    onSignIn: (SignInMethod) -> Unit,
    onUseApiKey: () -> Unit,
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
        text = stringResource(Res.string.assistant_sign_in_title),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSecondaryContainer,
    )
    Text(
        text = stringResource(Res.string.assistant_sign_in_note),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSecondaryContainer,
    )
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        methods.forEach { method ->
            WithTooltip(method.description?.trim().orEmpty()) {
                Button(onClick = { onSignIn(method) }) {
                    Text(method.name)
                }
            }
        }
        if (canKeepApiKey) {
            TextButton(onClick = onUseApiKey) {
                Text(stringResource(Res.string.assistant_account_use_api_key))
            }
        }
    }
}
