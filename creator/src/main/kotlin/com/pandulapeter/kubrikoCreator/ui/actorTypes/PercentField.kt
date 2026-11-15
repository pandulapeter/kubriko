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
import com.pandulapeter.kubrikoCreator.actors.BlockScope
import com.pandulapeter.kubrikoCreator.actors.NumberValue
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.block_percent_unit
import org.jetbrains.compose.resources.StringResource

/**
 * A percent a Block takes (an opacity, a change of one, or a volume), which takes no edge.
 *
 * @param accepts Which numbers the field takes: from 0 to 100, or any change of one.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun PercentField(
    label: StringResource,
    value: NumberValue,
    scope: BlockScope,
    onValueChange: (NumberValue) -> Unit,
    accepts: (Float) -> Boolean = { true },
) = FlowRow(
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalArrangement = Arrangement.spacedBy(4.dp),
    itemVerticalAlignment = Alignment.CenterVertically,
) {
    NumberValueField(
        label = label,
        value = value,
        scope = scope,
        onValueChange = onValueChange,
        offersEdges = false,
        accepts = accepts,
    )
    UnitText(Res.string.block_percent_unit)
}
