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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoCreator.ai.assistant.SignInSession
import com.pandulapeter.kubrikoCreator.ui.components.CompactTextField
import com.pandulapeter.kubrikoCreator.ui.components.openInBrowser
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.action_close
import kubriko.creator.generated.resources.action_open_sign_in_page
import kubriko.creator.generated.resources.action_send
import kubriko.creator.generated.resources.assistant_sign_in_code_hint
import kubriko.creator.generated.resources.assistant_sign_in_failed
import kubriko.creator.generated.resources.assistant_sign_in_running
import org.jetbrains.compose.resources.stringResource

/**
 * The sign-in command's terminal: what it writes, the page it links to (which it usually opens in the browser by itself),
 * and a field for the code that page may give to paste back.
 */
@Composable
internal fun SignInTerminal(
    session: SignInSession,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) = Column(
    modifier = modifier
        .fillMaxWidth()
        .padding(8.dp)
        .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(8.dp))
        .padding(10.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp),
) {
    val output by session.output.collectAsState()
    val exitCode by session.exitCode.collectAsState()
    Text(
        text = exitCode?.let { stringResource(Res.string.assistant_sign_in_failed, session.method.name, it) }
            ?: stringResource(Res.string.assistant_sign_in_running, session.method.name),
        style = MaterialTheme.typography.titleSmall,
        color = if (exitCode == null) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.error,
    )
    TerminalOutput(output.text)
    if (exitCode == null) {
        CodeInput(onSend = session::send)
    }
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        output.links.firstOrNull()?.let { link ->
            Button(onClick = { openInBrowser(link) }) {
                Text(stringResource(Res.string.action_open_sign_in_page))
            }
        }
        TextButton(onClick = onClose) {
            Text(stringResource(Res.string.action_close))
        }
    }
}

@Composable
private fun TerminalOutput(
    text: String,
) {
    val scrollState = rememberScrollState()
    LaunchedEffect(text) { scrollState.scrollTo(scrollState.maxValue) }
    SelectionContainer {
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 160.dp)
                .verticalScroll(scrollState),
            text = text.trimEnd(),
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }
}

@Composable
private fun CodeInput(
    onSend: (String) -> Unit,
) = Row(
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalAlignment = Alignment.CenterVertically,
) {
    var code by remember { mutableStateOf("") }
    val send = {
        if (code.isNotBlank()) {
            onSend(code.trim())
            code = ""
        }
    }
    CompactTextField(
        modifier = Modifier
            .weight(1f)
            .onPreviewKeyEvent { event -> (event.key == Key.Enter || event.key == Key.NumPadEnter).also { if (it) send() } },
        value = code,
        onValueChange = { code = it },
        placeholder = stringResource(Res.string.assistant_sign_in_code_hint),
    )
    TextButton(onClick = send, enabled = code.isNotBlank()) {
        Text(stringResource(Res.string.action_send))
    }
}
