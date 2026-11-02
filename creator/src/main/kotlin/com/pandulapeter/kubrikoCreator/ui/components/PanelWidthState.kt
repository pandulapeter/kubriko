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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoCreator.ui.components.PanelWidths.Panel

/**
 * The row a resizable side panel is in: how wide it is, and how many side panels it shows now, which share half of it.
 */
internal data class PanelRow(
    val availableWidth: Dp,
    val panelsInRow: Int,
)

/**
 * The width of a side [panel], kept by the [layout] and changed through its [PanelSplitter]: the width being dragged stays
 * here and is kept only once the drag stops, so Creator's settings are written once per drag, not once per pixel.
 */
@Stable
internal class PanelWidthState(
    val panel: Panel,
    private val layout: PanelLayout,
) {
    private var draggedWidth by mutableStateOf<Dp?>(null)

    /**
     * The width to draw the panel at in [row], within the limits of [clampedPanelWidth].
     */
    fun widthIn(row: PanelRow) = clampedPanelWidth(draggedWidth ?: layout.widthOf(panel), row.availableWidth, row.panelsInRow)

    fun onDrag(growthInDp: Float, row: PanelRow) {
        draggedWidth = clampedPanelWidth(widthIn(row) + growthInDp.dp, row.availableWidth, row.panelsInRow)
    }

    fun onDragStopped() {
        draggedWidth?.let { layout.onWidthChange(panel, it) }
    }

    fun onReset() {
        draggedWidth = null
        layout.onWidthReset(panel)
    }
}

/**
 * The [PanelWidthState] of [panel], started again whenever the [layout] keeps another width for it.
 */
@Composable
internal fun rememberPanelWidth(panel: Panel, layout: PanelLayout) = remember(panel, layout.widthOf(panel)) { PanelWidthState(panel, layout) }
