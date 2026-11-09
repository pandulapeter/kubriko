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
import androidx.compose.ui.unit.Dp
import com.pandulapeter.kubrikoCreator.console.ConsoleLines
import com.pandulapeter.kubrikoCreator.ui.components.PanelLayout
import com.pandulapeter.kubrikoCreator.ui.components.PanelSplitter
import com.pandulapeter.kubrikoCreator.ui.components.PanelWidths.Panel
import com.pandulapeter.kubrikoCreator.ui.components.rememberPanelHeight
import kotlinx.coroutines.flow.StateFlow

/**
 * The workspace's console under a splitter that resizes it (opening it when it is closed), in a column [availableHeight]
 * tall. It opens by itself for lines worth seeing that arrive while it is shown ([ConsoleOpening]), what the game logs
 * once per play session.
 */
@Composable
internal fun WorkspaceConsole(
    lines: StateFlow<ConsoleLines>,
    onClear: () -> Unit,
    panelLayout: PanelLayout,
    availableHeight: Dp,
) {
    val console by lines.collectAsState()
    var isExpanded by remember { mutableStateOf(false) }
    val opening = remember(lines) { ConsoleOpening(lines.value) }
    LaunchedEffect(console.revision) {
        if (opening.opensFor(console)) isExpanded = true
    }
    val height = rememberPanelHeight(Panel.CONSOLE, panelLayout)
    PanelSplitter(
        state = height,
        availableHeight = availableHeight,
        onDrag = { isExpanded = true },
    )
    Console(
        lines = console,
        isExpanded = isExpanded,
        onExpandedChange = { isExpanded = it },
        onClear = onClear,
        listHeight = height.heightIn(availableHeight),
    )
}
