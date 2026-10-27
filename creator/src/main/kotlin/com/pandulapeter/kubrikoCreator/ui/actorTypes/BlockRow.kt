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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoCreator.actors.BlockDefinition
import com.pandulapeter.kubrikoCreator.actors.BlockKind
import com.pandulapeter.kubrikoCreator.ui.components.CompactTextField
import com.pandulapeter.kubrikoCreator.ui.components.IconActionButton
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.action_move_down
import kubriko.creator.generated.resources.action_move_up
import kubriko.creator.generated.resources.action_remove
import kubriko.creator.generated.resources.block_parameter_code
import kubriko.creator.generated.resources.block_parameter_x
import kubriko.creator.generated.resources.block_parameter_y
import kubriko.creator.generated.resources.block_velocity_unit
import kubriko.creator.generated.resources.ic_close
import kubriko.creator.generated.resources.ic_move_down
import kubriko.creator.generated.resources.ic_move_up
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * One Block of an Event: its icon and name with the buttons that rearrange it, and its parameters under them.
 */
@Composable
internal fun BlockRow(
    block: BlockDefinition,
    isFirst: Boolean,
    isLast: Boolean,
    onBlockChange: (BlockDefinition) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
) = Column(
    modifier = Modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(4.dp),
) {
    val kind = BlockKind.of(block)
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            modifier = Modifier.size(16.dp),
            painter = painterResource(kind.icon),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.tertiary,
        )
        Text(
            modifier = Modifier.weight(1f),
            text = stringResource(kind.title),
            style = MaterialTheme.typography.bodyMedium,
        )
        IconActionButton(icon = Res.drawable.ic_move_up, label = Res.string.action_move_up, onClick = onMoveUp, isEnabled = !isFirst)
        IconActionButton(icon = Res.drawable.ic_move_down, label = Res.string.action_move_down, onClick = onMoveDown, isEnabled = !isLast)
        IconActionButton(icon = Res.drawable.ic_close, label = Res.string.action_remove, onClick = onRemove)
    }
    Row(
        modifier = Modifier.padding(start = 24.dp),
    ) {
        InlineParameters(block, onBlockChange)
    }
    if (block is BlockDefinition.Kotlin) {
        CompactTextField(
            modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 8.dp, bottom = 4.dp).heightIn(min = 72.dp),
            value = block.code,
            onValueChange = { onBlockChange(block.copy(code = it)) },
            placeholder = stringResource(Res.string.block_parameter_code),
            isSingleLine = false,
            textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
        )
    }
}

@Composable
private fun InlineParameters(
    block: BlockDefinition,
    onBlockChange: (BlockDefinition) -> Unit,
) = when (block) {
    is BlockDefinition.SetVelocity -> OffsetParameters(block.x, block.y, isVelocity = true) { x, y -> onBlockChange(block.copy(x = x, y = y)) }
    is BlockDefinition.AddToVelocity -> OffsetParameters(block.x, block.y, isVelocity = true) { x, y -> onBlockChange(block.copy(x = x, y = y)) }
    is BlockDefinition.JumpTo -> OffsetParameters(block.x, block.y, isVelocity = false) { x, y -> onBlockChange(block.copy(x = x, y = y)) }
    BlockDefinition.Stop, BlockDefinition.BounceOffEdge, BlockDefinition.RemoveSelf, BlockDefinition.BounceOffOther, BlockDefinition.RemoveOther, is BlockDefinition.Kotlin -> Unit
}

@Composable
private fun OffsetParameters(
    x: Float,
    y: Float,
    isVelocity: Boolean,
    onChange: (x: Float, y: Float) -> Unit,
) = Row(
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalAlignment = Alignment.CenterVertically,
) {
    NumberField(
        modifier = Modifier.width(96.dp),
        label = Res.string.block_parameter_x,
        value = x,
        onValueChange = { onChange(it, y) },
    )
    NumberField(
        modifier = Modifier.width(96.dp),
        label = Res.string.block_parameter_y,
        value = y,
        onValueChange = { onChange(x, it) },
    )
    if (isVelocity) {
        Text(
            text = stringResource(Res.string.block_velocity_unit),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
