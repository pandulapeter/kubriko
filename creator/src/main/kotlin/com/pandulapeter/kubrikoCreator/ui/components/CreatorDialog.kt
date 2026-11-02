/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties

/**
 * A dialog of Kubriko Creator's: as wide as its content needs, up to [maxWidth], with a [title] above its [text].
 */
@Composable
internal fun CreatorDialog(
    title: String,
    maxWidth: Dp,
    onDismiss: () -> Unit,
    confirmButton: @Composable () -> Unit,
    dismissButton: (@Composable () -> Unit)? = null,
    text: @Composable () -> Unit,
) = AlertDialog(
    modifier = Modifier.widthIn(max = maxWidth).padding(24.dp),
    properties = DialogProperties(usePlatformDefaultWidth = false),
    onDismissRequest = onDismiss,
    title = { Text(title) },
    text = text,
    confirmButton = confirmButton,
    dismissButton = dismissButton,
)
