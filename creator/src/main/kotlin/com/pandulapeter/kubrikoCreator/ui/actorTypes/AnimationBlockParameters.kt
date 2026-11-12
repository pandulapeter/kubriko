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
import com.pandulapeter.kubrikoCreator.ui.components.LabeledSwitch
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.block_parameter_frame
import kubriko.creator.generated.resources.block_parameter_looping
import kubriko.creator.generated.resources.block_parameter_speed
import org.jetbrains.compose.resources.stringResource

/**
 * The parameters of the Blocks that play a sprite's frames: the frame to show, or the speed and whether it loops.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun AnimationBlockParameters(
    block: BlockDefinition,
    scope: BlockScope,
    onBlockChange: (BlockDefinition) -> Unit,
) = FlowRow(
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalArrangement = Arrangement.spacedBy(4.dp),
    itemVerticalAlignment = Alignment.CenterVertically,
) {
    when (block) {
        is BlockDefinition.SetFrame -> NumberValueField(
            label = Res.string.block_parameter_frame,
            value = block.frame,
            scope = scope,
            onValueChange = { onBlockChange(block.copy(frame = it)) },
            isWhole = true,
            accepts = { it >= 0f },
        )
        is BlockDefinition.SetAnimation -> {
            NumberValueField(
                label = Res.string.block_parameter_speed,
                value = block.speed,
                scope = scope,
                onValueChange = { onBlockChange(block.copy(speed = it)) },
            )
            LabeledSwitch(
                label = stringResource(Res.string.block_parameter_looping),
                checked = block.isLooping,
                onCheckedChange = { onBlockChange(block.copy(isLooping = it)) },
            )
        }
        else -> Unit
    }
}
