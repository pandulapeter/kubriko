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
 * The height of a [PanelSide.BOTTOM] [panel] as its splitter is dragged, kept in [layout] once the drag ends.
 */
@Stable
public class PanelHeightState(
    public val panel: Panel,
    private val layout: PanelLayout,
) {
    private var draggedHeight by mutableStateOf<Dp?>(null)

    /**
     * The height to draw the panel at in a column [availableHeight] tall, within the limits of [clampedPanelHeight].
     */
    public fun heightIn(availableHeight: Dp): Dp = clampedPanelHeight(draggedHeight ?: layout.sizeOf(panel), availableHeight)

    /**
     * Follows a drag of the splitter that grows the panel by [growthInDp] in a column [availableHeight] tall.
     */
    public fun onDrag(growthInDp: Float, availableHeight: Dp) {
        draggedHeight = clampedPanelHeight(heightIn(availableHeight) + growthInDp.dp, availableHeight)
    }

    /**
     * Keeps the dragged height in the [layout] once the drag ends.
     */
    public fun onDragStopped() {
        draggedHeight?.let { layout.onSizeChange(panel, it) }
    }

    /**
     * Gives the panel its default height again.
     */
    public fun onReset() {
        draggedHeight = null
        layout.onSizeReset(panel)
    }
}

/**
 * Remembers the [PanelHeightState] of [panel] in [layout].
 */
@Composable
public fun rememberPanelHeight(panel: Panel, layout: PanelLayout): PanelHeightState = remember(panel, layout.sizeOf(panel)) { PanelHeightState(panel, layout) }
