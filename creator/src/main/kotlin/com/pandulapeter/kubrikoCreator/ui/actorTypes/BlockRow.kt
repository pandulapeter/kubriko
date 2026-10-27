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

import androidx.compose.foundation.background
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoCreator.actors.BlockDefinition
import com.pandulapeter.kubrikoCreator.actors.BlockKind
import com.pandulapeter.kubrikoCreator.actors.BlockNesting
import com.pandulapeter.kubrikoCreator.actors.PropertyDefinition
import com.pandulapeter.kubrikoCreator.ui.components.CompactTextField
import com.pandulapeter.kubrikoCreator.ui.components.IconActionButton
import com.pandulapeter.kubrikoCreator.ui.components.rememberFlashColor
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.action_drag_block
import kubriko.creator.generated.resources.action_remove
import kubriko.creator.generated.resources.block_else_stray
import kubriko.creator.generated.resources.block_end_stray
import kubriko.creator.generated.resources.block_parameter_code
import kubriko.creator.generated.resources.block_parameter_message
import kubriko.creator.generated.resources.block_parameter_relative
import kubriko.creator.generated.resources.block_parameter_repeating
import kubriko.creator.generated.resources.block_parameter_seconds
import kubriko.creator.generated.resources.block_parameter_speed
import kubriko.creator.generated.resources.block_parameter_times
import kubriko.creator.generated.resources.block_parameter_x
import kubriko.creator.generated.resources.block_parameter_y
import kubriko.creator.generated.resources.block_velocity_unit
import kubriko.creator.generated.resources.ic_close
import kubriko.creator.generated.resources.ic_drag_handle
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * One Block of an Event: its icon and name with the buttons that rearrange it, and its parameters under them.
 */
@Composable
internal fun BlockRow(
    block: BlockDefinition,
    nesting: BlockNesting,
    properties: List<PropertyDefinition>,
    actorTypeNames: List<String>,
    index: Int,
    dragState: BlockDragState,
    onBlockChange: (BlockDefinition) -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
    flashKey: Any? = null,
) = Column(
    modifier = modifier
        .fillMaxWidth()
        .padding(start = (nesting.depth * NESTING_INDENT).dp)
        .alpha(if ((dragState.dragged as? DraggedBlock.Existing)?.index == index) 0.4f else 1f)
        .clip(MaterialTheme.shapes.small)
        .background(if (nesting.role == BlockNesting.Role.PLAIN) MaterialTheme.colorScheme.surfaceContainer else MaterialTheme.colorScheme.surfaceContainerHigh)
        .background(rememberFlashColor(flashKey))
        .padding(start = 4.dp, top = 2.dp, bottom = 6.dp),
    verticalArrangement = Arrangement.spacedBy(2.dp),
) {
    val kind = BlockKind.of(block)
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            modifier = Modifier.size(18.dp).blockDragSource(dragState) { DraggedBlock.Existing(index, block) },
            painter = painterResource(Res.drawable.ic_drag_handle),
            contentDescription = stringResource(Res.string.action_drag_block),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
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
        IconActionButton(icon = Res.drawable.ic_close, label = Res.string.action_remove, onClick = onRemove)
    }
    if (kind !in kindsWithoutParameters) {
        Row(
            modifier = Modifier.padding(start = 46.dp),
        ) {
            InlineParameters(block, properties, actorTypeNames, onBlockChange)
        }
    }
    if (nesting.role == BlockNesting.Role.STRAY) {
        Text(
            modifier = Modifier.padding(start = 46.dp, bottom = 4.dp),
            text = stringResource(if (block == BlockDefinition.Else) Res.string.block_else_stray else Res.string.block_end_stray),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
        )
    }
    if (block is BlockDefinition.Kotlin) {
        CompactTextField(
            modifier = Modifier.fillMaxWidth().padding(start = 46.dp, end = 8.dp, bottom = 4.dp).heightIn(min = 72.dp),
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
    properties: List<PropertyDefinition>,
    actorTypeNames: List<String>,
    onBlockChange: (BlockDefinition) -> Unit,
) = when (block) {
    is BlockDefinition.AddActor -> Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ActorTypeChoice(block.actorType, actorTypeNames) { onBlockChange(block.copy(actorType = it)) }
        OffsetParameters(block.x, block.y, isVelocity = false) { x, y -> onBlockChange(block.copy(x = x, y = y)) }
        Text(
            text = stringResource(Res.string.block_parameter_relative),
            style = MaterialTheme.typography.bodySmall,
        )
        Switch(
            checked = block.isRelative,
            onCheckedChange = { onBlockChange(block.copy(isRelative = it)) },
        )
    }
    is BlockDefinition.MoveTowardsNearest -> Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ActorTypeChoice(block.actorType, actorTypeNames) { onBlockChange(block.copy(actorType = it)) }
        NumberField(
            modifier = Modifier.width(96.dp),
            label = Res.string.block_parameter_speed,
            value = block.speed,
            onValueChange = { onBlockChange(block.copy(speed = it)) },
        )
        Text(
            text = stringResource(Res.string.block_velocity_unit),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    is BlockDefinition.SetNumber, is BlockDefinition.ChangeNumber, is BlockDefinition.SetText, is BlockDefinition.SetToggle -> PropertyBlockParameters(block, properties, onBlockChange)
    is BlockDefinition.StartTimer -> Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TimerChoice(block.timer) { onBlockChange(block.copy(timer = it)) }
        NumberField(
            modifier = Modifier.width(96.dp),
            label = Res.string.block_parameter_seconds,
            value = block.seconds,
            onValueChange = { if (it >= 0f) onBlockChange(block.copy(seconds = it)) },
        )
        Text(
            text = stringResource(Res.string.block_parameter_repeating),
            style = MaterialTheme.typography.bodySmall,
        )
        Switch(
            checked = block.isRepeating,
            onCheckedChange = { onBlockChange(block.copy(isRepeating = it)) },
        )
    }
    is BlockDefinition.StopTimer -> TimerChoice(block.timer) { onBlockChange(block.copy(timer = it)) }
    is BlockDefinition.If -> ConditionParameters(block.condition, properties) { onBlockChange(block.copy(condition = it)) }
    is BlockDefinition.Repeat -> NumberField(
        modifier = Modifier.width(96.dp),
        label = Res.string.block_parameter_times,
        value = block.times.toFloat(),
        onValueChange = { if (it >= 0f && it == it.toInt().toFloat()) onBlockChange(block.copy(times = it.toInt())) },
    )
    is BlockDefinition.SetVelocity -> OffsetParameters(block.x, block.y, isVelocity = true) { x, y -> onBlockChange(block.copy(x = x, y = y)) }
    is BlockDefinition.AddToVelocity -> OffsetParameters(block.x, block.y, isVelocity = true) { x, y -> onBlockChange(block.copy(x = x, y = y)) }
    is BlockDefinition.JumpTo -> OffsetParameters(block.x, block.y, isVelocity = false) { x, y -> onBlockChange(block.copy(x = x, y = y)) }
    is BlockDefinition.Log -> CompactTextField(
        modifier = Modifier.fillMaxWidth().padding(end = 8.dp),
        value = block.message,
        onValueChange = { onBlockChange(block.copy(message = it)) },
        placeholder = stringResource(Res.string.block_parameter_message),
    )
    BlockDefinition.Stop, BlockDefinition.BounceOffEdge, BlockDefinition.RemoveSelf, BlockDefinition.BounceOffOther, BlockDefinition.RemoveOther,
    BlockDefinition.Else, BlockDefinition.End, is BlockDefinition.Kotlin -> Unit
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

/**
 * How far each section moves the Blocks in it to the right, in dp.
 */
private const val NESTING_INDENT = 20

/**
 * The Blocks whose title says it all; a Kotlin Block's code gets a field of its own below.
 */
private val kindsWithoutParameters = setOf(
    BlockKind.STOP,
    BlockKind.BOUNCE_OFF_EDGE,
    BlockKind.REMOVE_SELF,
    BlockKind.BOUNCE_OFF_OTHER,
    BlockKind.REMOVE_OTHER,
    BlockKind.ELSE,
    BlockKind.END,
    BlockKind.KOTLIN,
)
