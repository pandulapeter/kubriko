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
import com.pandulapeter.kubrikoCreator.actors.Axis
import com.pandulapeter.kubrikoCreator.actors.BlockScope
import com.pandulapeter.kubrikoCreator.actors.NumberValue
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.block_parameter_x
import kubriko.creator.generated.resources.block_parameter_y
import kubriko.creator.generated.resources.block_velocity_unit

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun OffsetParameters(
    x: NumberValue,
    y: NumberValue,
    scope: BlockScope,
    isVelocity: Boolean,
    onChange: (x: NumberValue, y: NumberValue) -> Unit,
) = FlowRow(
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalArrangement = Arrangement.spacedBy(4.dp),
    itemVerticalAlignment = Alignment.CenterVertically,
) {
    NumberValueField(
        label = Res.string.block_parameter_x,
        value = x,
        scope = scope,
        axis = Axis.HORIZONTAL,
        onValueChange = { onChange(it, y) },
    )
    NumberValueField(
        label = Res.string.block_parameter_y,
        value = y,
        scope = scope,
        axis = Axis.VERTICAL,
        onValueChange = { onChange(x, it) },
    )
    if (isVelocity) UnitText(Res.string.block_velocity_unit)
}
