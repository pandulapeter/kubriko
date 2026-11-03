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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties

/**
 * A dialog of Kubriko Creator's: as wide as its content needs, up to [maxWidth], with a [title] above its [text],
 * fading in rather than appearing at once.
 */
@Composable
public fun CreatorDialog(
    title: String,
    maxWidth: Dp,
    onDismiss: () -> Unit,
    confirmButton: @Composable () -> Unit,
    dismissButton: (@Composable () -> Unit)? = null,
    text: @Composable () -> Unit,
): Unit = AlertDialog(
    modifier = Modifier.widthIn(max = maxWidth).padding(24.dp).dialogEntrance(),
    properties = DialogProperties(usePlatformDefaultWidth = false),
    onDismissRequest = onDismiss,
    shape = MaterialTheme.shapes.extraLarge,
    title = { Text(title, style = MaterialTheme.typography.titleLarge) },
    text = text,
    confirmButton = confirmButton,
    dismissButton = dismissButton,
)
