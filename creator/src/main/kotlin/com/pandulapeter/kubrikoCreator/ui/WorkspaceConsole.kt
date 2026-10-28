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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.pandulapeter.kubrikoCreator.workspace.ConsoleEntry
import kotlinx.coroutines.flow.StateFlow

/**
 * The workspace's [Console], collecting its entries itself, so that a line the game logs recomposes only the console.
 * It opens on its own for entries that need attention.
 */
@Composable
internal fun WorkspaceConsole(
    entries: StateFlow<List<ConsoleEntry>>,
    onClear: () -> Unit,
) {
    val console by entries.collectAsState()
    var isExpanded by remember { mutableStateOf(false) }
    LaunchedEffect(console.lastOrNull()) {
        if (console.lastOrNull()?.opensConsole == true) isExpanded = true
    }
    Console(
        entries = console,
        isExpanded = isExpanded,
        onExpandedChange = { isExpanded = it },
        onClear = onClear,
    )
}
