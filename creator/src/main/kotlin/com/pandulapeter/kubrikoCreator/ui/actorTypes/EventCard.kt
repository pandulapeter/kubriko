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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoCreator.actors.BlockDefinition
import com.pandulapeter.kubrikoCreator.actors.BlockKind
import com.pandulapeter.kubrikoCreator.actors.EventDefinition
import com.pandulapeter.kubrikoCreator.actors.KeyChoice
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.action_add_block
import kubriko.creator.generated.resources.action_remove
import kubriko.creator.generated.resources.event_blocks_empty
import org.jetbrains.compose.resources.stringResource

/**
 * One Event of an Actor type, with its Blocks in the order they run.
 */
@Composable
internal fun EventCard(
    event: EventDefinition,
    onKeyChange: (KeyChoice) -> Unit,
    onRemove: () -> Unit,
    onBlockAdded: (BlockDefinition) -> Unit,
    onBlockChange: (blockIndex: Int, BlockDefinition) -> Unit,
    onBlockMoved: (blockIndex: Int, offset: Int) -> Unit,
    onBlockRemoved: (blockIndex: Int) -> Unit,
) = OutlinedCard(
    modifier = Modifier.fillMaxWidth(),
) {
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(event.type.title),
                style = MaterialTheme.typography.titleMedium,
            )
            event.key?.let { key ->
                MenuButton(
                    label = stringResource(key.title),
                    options = KeyChoice.entries,
                    optionTitle = { it.title },
                    onSelected = onKeyChange,
                )
            }
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onRemove) {
                Text(stringResource(Res.string.action_remove))
            }
        }
        if (event.blocks.isEmpty()) {
            Text(
                text = stringResource(Res.string.event_blocks_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        event.blocks.forEachIndexed { index, block ->
            if (index > 0) HorizontalDivider()
            BlockRow(
                block = block,
                onBlockChange = { onBlockChange(index, it) },
                onMoveUp = { onBlockMoved(index, -1) },
                onMoveDown = { onBlockMoved(index, 1) },
                onRemove = { onBlockRemoved(index) },
            )
        }
        MenuButton(
            label = stringResource(Res.string.action_add_block),
            options = BlockKind.entries.filter { it.isAvailableIn(event.type) },
            optionTitle = { it.title },
            onSelected = { onBlockAdded(it.createDefault()) },
        )
    }
}
