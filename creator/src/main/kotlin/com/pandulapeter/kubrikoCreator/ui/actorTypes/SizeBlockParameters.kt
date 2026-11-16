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
 * The width and height a Set the size Block gives the Actor, in scene units, neither of which is an edge.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SizeBlockParameters(
    block: BlockDefinition.SetSize,
    scope: BlockScope,
    onBlockChange: (BlockDefinition) -> Unit,
) = FlowRow(
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalArrangement = Arrangement.spacedBy(4.dp),
    itemVerticalAlignment = Alignment.CenterVertically,
) {
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

/**
 * The scale a Set the scale Block gives the Actor, where 1 is its size, which isn't an edge.
 */
@Composable
internal fun SizeBlockParameters(
    block: BlockDefinition.SetScale,
    scope: BlockScope,
    onBlockChange: (BlockDefinition) -> Unit,
) = NumberValueField(
    label = Res.string.block_parameter_scale,
    value = block.scale,
    scope = scope,
    onValueChange = { onBlockChange(block.copy(scale = it)) },
    offersEdges = false,
    limits = NumberLimits.NON_NEGATIVE,
)

/**
 * What a Change the scale Block adds to the Actor's scale, which isn't an edge.
 */
@Composable
internal fun SizeBlockParameters(
    block: BlockDefinition.ChangeScale,
    scope: BlockScope,
    onBlockChange: (BlockDefinition) -> Unit,
) = NumberValueField(
    label = Res.string.block_parameter_by,
    value = block.by,
    scope = scope,
    onValueChange = { onBlockChange(block.copy(by = it)) },
    offersEdges = false,
)
