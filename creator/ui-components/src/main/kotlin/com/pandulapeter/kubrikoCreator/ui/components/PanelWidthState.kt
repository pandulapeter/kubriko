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
public data class PanelRow(
    public val availableWidth: Dp,
    public val panelsInRow: Int,
)

/**
 * The width of a side [panel], kept by the [layout] and changed through its [PanelSplitter]: the width being dragged stays
 * here and is kept only once the drag stops, so Creator's settings are written once per drag, not once per pixel.
 */
@Stable
public class PanelWidthState(
    public val panel: Panel,
    private val layout: PanelLayout,
) {
    private var draggedWidth by mutableStateOf<Dp?>(null)

    /**
     * The width to draw the panel at in [row], within the limits of [clampedPanelWidth].
     */
    public fun widthIn(row: PanelRow): Dp = clampedPanelWidth(draggedWidth ?: layout.sizeOf(panel), row.availableWidth, row.panelsInRow)

    /**
     * Follows a drag of the splitter that grows the panel by [growthInDp] in [row].
     */
    public fun onDrag(growthInDp: Float, row: PanelRow) {
        draggedWidth = clampedPanelWidth(widthIn(row) + growthInDp.dp, row.availableWidth, row.panelsInRow)
    }

    /**
     * Keeps the dragged width in the [layout] once the drag ends.
     */
    public fun onDragStopped() {
        draggedWidth?.let { layout.onSizeChange(panel, it) }
    }

    /**
     * Gives the panel its default width again.
     */
    public fun onReset() {
        draggedWidth = null
        layout.onSizeReset(panel)
    }
}

/**
 * The [PanelWidthState] of [panel], started again whenever the [layout] keeps another width for it.
 */
@Composable
public fun rememberPanelWidth(panel: Panel, layout: PanelLayout): PanelWidthState = remember(panel, layout.sizeOf(panel)) { PanelWidthState(panel, layout) }
