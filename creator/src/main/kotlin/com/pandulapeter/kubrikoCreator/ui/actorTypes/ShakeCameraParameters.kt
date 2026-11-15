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
import kubriko.creator.generated.resources.block_milliseconds_unit
import kubriko.creator.generated.resources.block_parameter_duration
import kubriko.creator.generated.resources.block_parameter_strength
import kubriko.creator.generated.resources.block_units_unit

/**
 * How far a Shake the camera Block moves the camera at most, and for how long, neither of which is an edge.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ShakeCameraParameters(
    block: BlockDefinition.ShakeCamera,
    scope: BlockScope,
    onBlockChange: (BlockDefinition) -> Unit,
) = FlowRow(
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalArrangement = Arrangement.spacedBy(4.dp),
    itemVerticalAlignment = Alignment.CenterVertically,
) {
    NumberValueField(
        label = Res.string.block_parameter_strength,
        value = block.strength,
        scope = scope,
        onValueChange = { onBlockChange(block.copy(strength = it)) },
        offersEdges = false,
        accepts = { it >= 0f },
    )
    UnitText(Res.string.block_units_unit)
    NumberValueField(
        label = Res.string.block_parameter_duration,
        value = block.durationInMilliseconds,
        scope = scope,
        onValueChange = { onBlockChange(block.copy(durationInMilliseconds = it)) },
        offersEdges = false,
        accepts = { it >= 0f },
    )
    UnitText(Res.string.block_milliseconds_unit)
}
