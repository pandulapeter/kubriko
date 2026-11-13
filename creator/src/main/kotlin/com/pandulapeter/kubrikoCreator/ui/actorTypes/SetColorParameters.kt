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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoCreator.actors.BlockDefinition
import com.pandulapeter.kubrikoCreator.ui.components.ColorField
import com.pandulapeter.kubrikoCreator.ui.components.IconActionButton
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.block_color_add
import kubriko.creator.generated.resources.block_color_remove
import kubriko.creator.generated.resources.ic_add
import kubriko.creator.generated.resources.ic_close

/**
 * The colors of the Set the color Block: one opaque field per color, each removable while there are several, and a button
 * adding another until there are [BlockDefinition.SetColor.MAXIMUM_COLORS].
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SetColorParameters(
    block: BlockDefinition.SetColor,
    onBlockChange: (BlockDefinition) -> Unit,
) = FlowRow(
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalArrangement = Arrangement.spacedBy(4.dp),
    itemVerticalAlignment = Alignment.CenterVertically,
) {
    block.colors.forEachIndexed { index, color ->
        Row(verticalAlignment = Alignment.CenterVertically) {
            ColorField(
                color = color,
                onColorChange = { changed -> onBlockChange(block.copy(colors = block.colors.toMutableList().also { it[index] = changed })) },
                isAlphaEnabled = false,
            )
            if (block.colors.size > 1) {
                IconActionButton(
                    icon = Res.drawable.ic_close,
                    label = Res.string.block_color_remove,
                    onClick = { onBlockChange(block.copy(colors = block.colors.filterIndexed { other, _ -> other != index })) },
                )
            }
        }
    }
    if (block.colors.size < BlockDefinition.SetColor.MAXIMUM_COLORS) {
        IconActionButton(
            icon = Res.drawable.ic_add,
            label = Res.string.block_color_add,
            onClick = { onBlockChange(block.copy(colors = block.colors + (block.colors.lastOrNull() ?: BlockDefinition.SetColor.DEFAULT_COLOR))) },
        )
    }
}
