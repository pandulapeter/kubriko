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

import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoCreator.ui.components.CreatorDialog
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.action_cancel
import kubriko.creator.generated.resources.action_open_in_browser
import kubriko.creator.generated.resources.page_address_title
import org.jetbrains.compose.resources.stringResource

/**
 * Shows the web [address] a page's link points at before it is opened, since a page's text may say one thing and link
 * to another.
 */
@Composable
internal fun AddressConfirmation(
    address: String,
    onOpen: () -> Unit,
    onDismiss: () -> Unit,
) = CreatorDialog(
    title = stringResource(Res.string.page_address_title),
    maxWidth = 520.dp,
    onDismiss = onDismiss,
    confirmButton = { Button(onClick = onOpen) { Text(stringResource(Res.string.action_open_in_browser)) } },
    dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.action_cancel)) } },
) {
    SelectionContainer {
        Text(text = address, style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace))
    }
}
