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
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.block_direction_unit
import kubriko.creator.generated.resources.block_parameter_direction
import kubriko.creator.generated.resources.block_parameter_speed
import kubriko.creator.generated.resources.block_velocity_unit

/**
 * The direction and the speed of the Move in a direction Block, each with its unit.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun MoveInDirectionParameters(
    block: BlockDefinition.MoveInDirection,
    scope: BlockScope,
    onBlockChange: (BlockDefinition) -> Unit,
) = FlowRow(
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalArrangement = Arrangement.spacedBy(4.dp),
    itemVerticalAlignment = Alignment.CenterVertically,
) {
    NumberValueField(
        label = Res.string.block_parameter_direction,
        value = block.direction,
        scope = scope,
        onValueChange = { onBlockChange(block.copy(direction = it)) },
        offersEdges = false,
    )
    UnitText(Res.string.block_direction_unit)
    NumberValueField(
        label = Res.string.block_parameter_speed,
        value = block.speed,
        scope = scope,
        onValueChange = { onBlockChange(block.copy(speed = it)) },
        offersEdges = false,
    )
    UnitText(Res.string.block_velocity_unit)
}
