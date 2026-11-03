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

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoCreator.ui.components.PanelWidths.Panel

/**
 * The panels' sizes as the user left them (the side panels' widths and the console's height), and how to keep a new one.
 *
 * @param sizes The kept sizes in dp, by [Panel.id]; a panel without one has its default size.
 * @param onSizeChange Keeps a panel's new size.
 * @param onSizeReset Gives a panel its default size again.
 */
public class PanelLayout(
    private val sizes: Map<String, Float>,
    public val onSizeChange: (Panel, Dp) -> Unit,
    public val onSizeReset: (Panel) -> Unit,
) {
    public fun sizeOf(panel: Panel): Dp = sizes[panel.id]?.dp ?: panel.default
}
