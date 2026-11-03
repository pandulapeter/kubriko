/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.actorTypes

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The widths of the type editor's columns beside its Blocks, and which of them are [folded] into tabs of one column
 * (then [events] wide) because the editor is too narrow to show them side by side.
 */
internal data class BlockTypeEditorColumns(
    val properties: Dp,
    val events: Dp,
    val palette: Dp,
    val folded: Folded = Folded.NONE,
) {
    /**
     * Which columns share the tabbed column with the Events.
     */
    enum class Folded {
        NONE,
        DETAILS,
        DETAILS_AND_PALETTE,
    }

    companion object {
        private val roomy = BlockTypeEditorColumns(properties = 250.dp, events = 280.dp, palette = 220.dp)
        private val narrowest = BlockTypeEditorColumns(properties = 220.dp, events = 220.dp, palette = 180.dp)
        private val minimumBlocksWidth = 320.dp
        private val tabbedWidth = 240.dp
        private val minimumTabbedWidth = 180.dp
        private const val TABBED_SHARE = 0.4f

        /**
         * The columns for an editor [available] wide: as roomy as they can be while the Blocks keep [minimumBlocksWidth],
         * narrowing evenly down to their narrowest when a side panel takes the room, then folding the details, and at
         * last the palette too, into tabs beside the Events (a column that gives up some of its width too, down to
         * [minimumTabbedWidth]), so the Blocks never lose their room.
         */
        fun of(available: Dp): BlockTypeEditorColumns {
            val shortfall = roomy.total + minimumBlocksWidth - available
            val folded = when {
                shortfall <= roomy.total - narrowest.total -> Folded.NONE
                available >= tabbedWidth + narrowest.palette + minimumBlocksWidth -> Folded.DETAILS
                else -> Folded.DETAILS_AND_PALETTE
            }
            if (folded != Folded.NONE) {
                val tabbed = (available * TABBED_SHARE).coerceIn(minimumTabbedWidth, tabbedWidth)
                return BlockTypeEditorColumns(properties = tabbed, events = tabbed, palette = narrowest.palette, folded = folded)
            }
            val fraction = (shortfall / (roomy.total - narrowest.total)).coerceIn(0f, 1f)
            return BlockTypeEditorColumns(
                properties = lerp(roomy.properties, narrowest.properties, fraction),
                events = lerp(roomy.events, narrowest.events, fraction),
                palette = lerp(roomy.palette, narrowest.palette, fraction),
            )
        }

        private val BlockTypeEditorColumns.total get() = properties + events + palette

        private fun lerp(from: Dp, to: Dp, fraction: Float) = from + (to - from) * fraction
    }
}
