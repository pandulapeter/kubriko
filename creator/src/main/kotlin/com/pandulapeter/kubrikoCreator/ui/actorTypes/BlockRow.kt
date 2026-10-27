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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoCreator.actors.BlockDefinition
import com.pandulapeter.kubrikoCreator.actors.BlockKind
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.action_move_down
import kubriko.creator.generated.resources.action_move_up
import kubriko.creator.generated.resources.action_remove
import kubriko.creator.generated.resources.block_parameter_code
import kubriko.creator.generated.resources.block_parameter_x
import kubriko.creator.generated.resources.block_parameter_y
import kubriko.creator.generated.resources.block_velocity_unit
import org.jetbrains.compose.resources.stringResource

/**
 * One Block of an Event: its name, its parameters, and the buttons that rearrange it.
 */
@Composable
internal fun BlockRow(
    block: BlockDefinition,
    onBlockChange: (BlockDefinition) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
) = Column(
    modifier = Modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(4.dp),
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(BlockKind.of(block).title),
            style = MaterialTheme.typography.titleSmall,
        )
        Spacer(Modifier.weight(1f))
        TextButton(onClick = onMoveUp) { Text(stringResource(Res.string.action_move_up)) }
        TextButton(onClick = onMoveDown) { Text(stringResource(Res.string.action_move_down)) }
        TextButton(onClick = onRemove) { Text(stringResource(Res.string.action_remove)) }
    }
    BlockParameters(block, onBlockChange)
}

@Composable
private fun BlockParameters(
    block: BlockDefinition,
    onBlockChange: (BlockDefinition) -> Unit,
) = when (block) {
    is BlockDefinition.SetVelocity -> OffsetParameters(block.x, block.y, isVelocity = true) { x, y -> onBlockChange(block.copy(x = x, y = y)) }
    is BlockDefinition.AddToVelocity -> OffsetParameters(block.x, block.y, isVelocity = true) { x, y -> onBlockChange(block.copy(x = x, y = y)) }
    is BlockDefinition.JumpTo -> OffsetParameters(block.x, block.y, isVelocity = false) { x, y -> onBlockChange(block.copy(x = x, y = y)) }
    is BlockDefinition.Kotlin -> OutlinedTextField(
        modifier = Modifier.fillMaxWidth().heightIn(min = 96.dp),
        value = block.code,
        onValueChange = { onBlockChange(block.copy(code = it)) },
        label = { Text(stringResource(Res.string.block_parameter_code)) },
        textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
    )
    BlockDefinition.Stop, BlockDefinition.BounceOffEdge, BlockDefinition.RemoveSelf -> Unit
}

@Composable
private fun OffsetParameters(
    x: Float,
    y: Float,
    isVelocity: Boolean,
    onChange: (x: Float, y: Float) -> Unit,
) = Row(
    horizontalArrangement = Arrangement.spacedBy(12.dp),
    verticalAlignment = Alignment.CenterVertically,
) {
    NumberField(
        modifier = Modifier.width(140.dp),
        label = Res.string.block_parameter_x,
        value = x,
        onValueChange = { onChange(it, y) },
    )
    NumberField(
        modifier = Modifier.width(140.dp),
        label = Res.string.block_parameter_y,
        value = y,
        onValueChange = { onChange(x, it) },
    )
    if (isVelocity) {
        Text(
            text = stringResource(Res.string.block_velocity_unit),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}
