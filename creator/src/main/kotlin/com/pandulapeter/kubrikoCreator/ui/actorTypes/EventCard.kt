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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoCreator.actors.BlockDefinition
import com.pandulapeter.kubrikoCreator.actors.BlockKind
import com.pandulapeter.kubrikoCreator.actors.EventDefinition
import com.pandulapeter.kubrikoCreator.actors.EventType
import com.pandulapeter.kubrikoCreator.actors.KeyChoice
import com.pandulapeter.kubrikoCreator.ui.components.IconActionButton
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.action_add_block
import kubriko.creator.generated.resources.action_remove
import kubriko.creator.generated.resources.event_blocks_empty
import kubriko.creator.generated.resources.event_collision_choose
import kubriko.creator.generated.resources.ic_add
import kubriko.creator.generated.resources.ic_delete
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * One Event of an Actor type, with its Blocks in the order they run.
 */
@Composable
internal fun EventCard(
    event: EventDefinition,
    actorTypeNames: List<String>,
    onKeyChange: (KeyChoice) -> Unit,
    onOtherActorTypeChange: (String) -> Unit,
    onRemove: () -> Unit,
    onBlockAdded: (BlockDefinition) -> Unit,
    onBlockChange: (blockIndex: Int, BlockDefinition) -> Unit,
    onBlockMoved: (blockIndex: Int, offset: Int) -> Unit,
    onBlockRemoved: (blockIndex: Int) -> Unit,
) = Surface(
    modifier = Modifier.fillMaxWidth(),
    color = MaterialTheme.colorScheme.surfaceContainer,
    shape = MaterialTheme.shapes.medium,
) {
    Column(
        modifier = Modifier.padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                modifier = Modifier.size(18.dp),
                painter = painterResource(event.type.icon),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = stringResource(event.type.title),
                style = MaterialTheme.typography.titleSmall,
            )
            event.key?.let { key ->
                MenuButton(
                    label = stringResource(key.title),
                    options = KeyChoice.entries,
                    optionText = { stringResource(it.title) },
                    onSelected = onKeyChange,
                )
            }
            if (event.type == EventType.COLLISION) {
                MenuButton(
                    label = event.otherActorType ?: stringResource(Res.string.event_collision_choose),
                    options = actorTypeNames,
                    optionText = { it },
                    onSelected = onOtherActorTypeChange,
                )
            }
            Spacer(Modifier.weight(1f))
            IconActionButton(
                icon = Res.drawable.ic_delete,
                label = Res.string.action_remove,
                onClick = onRemove,
            )
        }
        HorizontalDivider(modifier = Modifier.padding(end = 8.dp))
        if (event.blocks.isEmpty()) {
            Text(
                modifier = Modifier.padding(start = 24.dp, top = 4.dp),
                text = stringResource(Res.string.event_blocks_empty),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        event.blocks.forEachIndexed { index, block ->
            BlockRow(
                block = block,
                isFirst = index == 0,
                isLast = index == event.blocks.lastIndex,
                onBlockChange = { onBlockChange(index, it) },
                onMoveUp = { onBlockMoved(index, -1) },
                onMoveDown = { onBlockMoved(index, 1) },
                onRemove = { onBlockRemoved(index) },
            )
        }
        MenuButton(
            label = stringResource(Res.string.action_add_block),
            icon = Res.drawable.ic_add,
            options = BlockKind.entries.filter { it.isAvailableIn(event.type) },
            optionText = { stringResource(it.title) },
            optionIcon = { it.icon },
            onSelected = { onBlockAdded(it.createDefault()) },
        )
    }
}
