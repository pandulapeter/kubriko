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
 * The widths of the type editor's fixed columns: the appearance and Properties, the Events, and the Block palette. The
 * selected Event's Blocks take the rest.
 */
internal data class BlockTypeEditorColumns(
    val properties: Dp,
    val events: Dp,
    val palette: Dp,
) {
    companion object {
        private val roomy = BlockTypeEditorColumns(properties = 250.dp, events = 280.dp, palette = 220.dp)
        private val narrowest = BlockTypeEditorColumns(properties = 220.dp, events = 220.dp, palette = 180.dp)
        private val minimumBlocksWidth = 320.dp

        /**
         * The columns for an editor [available] wide: as roomy as they can be while the Blocks keep [minimumBlocksWidth],
         * narrowing evenly down to their narrowest when a side panel takes the room.
         */
        fun of(available: Dp): BlockTypeEditorColumns {
            val shortfall = roomy.total + minimumBlocksWidth - available
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
