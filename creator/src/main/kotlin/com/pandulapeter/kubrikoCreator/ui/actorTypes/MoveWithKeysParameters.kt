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
import com.pandulapeter.kubrikoCreator.actors.SteeringKeys
import com.pandulapeter.kubrikoCreator.ui.components.LabeledSwitch
import com.pandulapeter.kubrikoCreator.ui.components.MenuButton
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.block_move_with_keys_arrows
import kubriko.creator.generated.resources.block_move_with_keys_arrows_and_wasd
import kubriko.creator.generated.resources.block_move_with_keys_diagonally
import kubriko.creator.generated.resources.block_move_with_keys_wasd
import kubriko.creator.generated.resources.block_parameter_speed
import kubriko.creator.generated.resources.block_velocity_unit
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * The keys, the speed, and whether the Move with keys Block moves diagonally too.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun MoveWithKeysParameters(
    block: BlockDefinition.MoveWithKeys,
    scope: BlockScope,
    onBlockChange: (BlockDefinition) -> Unit,
) = FlowRow(
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalArrangement = Arrangement.spacedBy(4.dp),
    itemVerticalAlignment = Alignment.CenterVertically,
) {
    MenuButton(
        label = stringResource(block.keys.title),
        options = SteeringKeys.entries,
        optionText = { stringResource(it.title) },
        onSelected = { onBlockChange(block.copy(keys = it)) },
    )
    NumberValueField(
        label = Res.string.block_parameter_speed,
        value = block.speed,
        scope = scope,
        onValueChange = { onBlockChange(block.copy(speed = it)) },
        offersEdges = false,
    )
    UnitText(Res.string.block_velocity_unit)
    LabeledSwitch(
        label = stringResource(Res.string.block_move_with_keys_diagonally),
        checked = block.movesDiagonally,
        onCheckedChange = { onBlockChange(block.copy(movesDiagonally = it)) },
    )
}

private val SteeringKeys.title: StringResource
    get() = when (this) {
        SteeringKeys.ARROWS -> Res.string.block_move_with_keys_arrows
        SteeringKeys.WASD -> Res.string.block_move_with_keys_wasd
        SteeringKeys.ARROWS_AND_WASD -> Res.string.block_move_with_keys_arrows_and_wasd
    }
