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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoCreator.actors.BlockDefinition
import com.pandulapeter.kubrikoCreator.actors.BlockScope
import com.pandulapeter.kubrikoCreator.actors.NumberLimits
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.block_parameter_by
import kubriko.creator.generated.resources.block_parameter_height
import kubriko.creator.generated.resources.block_parameter_scale
import kubriko.creator.generated.resources.block_parameter_width
import kubriko.creator.generated.resources.block_units_unit

/**
 * The numbers of the Set the size, Set the scale, and Change the scale Blocks, none of which is an edge: a width and a
 * height in scene units, or a scale, where 1 is the Actor's size.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SizeBlockParameters(
    block: BlockDefinition,
    scope: BlockScope,
    onBlockChange: (BlockDefinition) -> Unit,
) = FlowRow(
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalArrangement = Arrangement.spacedBy(4.dp),
    itemVerticalAlignment = Alignment.CenterVertically,
) {
    when (block) {
        is BlockDefinition.SetSize -> {
            NumberValueField(
                label = Res.string.block_parameter_width,
                value = block.width,
                scope = scope,
                onValueChange = { onBlockChange(block.copy(width = it)) },
                offersEdges = false,
                limits = NumberLimits.NON_NEGATIVE,
            )
            NumberValueField(
                label = Res.string.block_parameter_height,
                value = block.height,
                scope = scope,
                onValueChange = { onBlockChange(block.copy(height = it)) },
                offersEdges = false,
                limits = NumberLimits.NON_NEGATIVE,
            )
            UnitText(Res.string.block_units_unit)
        }
        is BlockDefinition.SetScale -> NumberValueField(
            label = Res.string.block_parameter_scale,
            value = block.scale,
            scope = scope,
            onValueChange = { onBlockChange(block.copy(scale = it)) },
            offersEdges = false,
            limits = NumberLimits.NON_NEGATIVE,
        )
        is BlockDefinition.ChangeScale -> NumberValueField(
            label = Res.string.block_parameter_by,
            value = block.by,
            scope = scope,
            onValueChange = { onBlockChange(block.copy(by = it)) },
            offersEdges = false,
        )
        else -> Unit
    }
}
