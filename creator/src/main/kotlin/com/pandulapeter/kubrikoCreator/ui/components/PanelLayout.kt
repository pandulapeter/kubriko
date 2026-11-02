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
 * The side panels' widths as the user left them, and how to keep a new one.
 *
 * @param widths The kept widths in dp, by [Panel.id]; a panel without one has its default width.
 * @param onWidthChange Keeps a panel's new width.
 * @param onWidthReset Gives a panel its default width again.
 */
internal class PanelLayout(
    private val widths: Map<String, Float>,
    val onWidthChange: (Panel, Dp) -> Unit,
    val onWidthReset: (Panel) -> Unit,
) {
    fun widthOf(panel: Panel) = widths[panel.id]?.dp ?: panel.default
}
