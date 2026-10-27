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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoCreator.actors.BlockDefinition
import com.pandulapeter.kubrikoCreator.actors.BlockNesting
import com.pandulapeter.kubrikoCreator.actors.EventDefinition
import com.pandulapeter.kubrikoCreator.actors.EventType
import com.pandulapeter.kubrikoCreator.actors.KeyChoice
import com.pandulapeter.kubrikoCreator.actors.PropertyDefinition
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.event_blocks_empty
import kubriko.creator.generated.resources.event_collision_choose
import kubriko.creator.generated.resources.event_none_selected
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * The Blocks of the selected Event, in the order they run, under a header that also chooses the Event's key or other
 * Actor type.
 */
@Composable
internal fun EventBlocks(
    event: EventDefinition?,
    properties: List<PropertyDefinition>,
    actorTypeNames: List<String>,
    onKeyChange: (KeyChoice) -> Unit,
    onOtherActorTypeChange: (String) -> Unit,
    onBlockChange: (blockIndex: Int, BlockDefinition) -> Unit,
    onBlockMoved: (blockIndex: Int, targetIndex: Int) -> Unit,
    onBlockRemoved: (blockIndex: Int) -> Unit,
    modifier: Modifier = Modifier,
    highlight: ActorTypeHighlight? = null,
) = Column(
    modifier = modifier,
) {
    if (event == null) {
        Text(
            modifier = Modifier.padding(16.dp),
            text = stringResource(Res.string.event_none_selected),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return@Column
    }
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 36.dp).padding(start = 12.dp, end = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            modifier = Modifier.size(16.dp),
            painter = painterResource(event.type.icon),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = stringResource(event.type.title),
            style = MaterialTheme.typography.titleSmall,
        )
        when (event.type) {
            EventType.KEY_PRESSED -> MenuButton(
                label = event.key?.let { stringResource(it.title) }.orEmpty(),
                options = KeyChoice.entries,
                optionText = { stringResource(it.title) },
                onSelected = onKeyChange,
            )
            EventType.COLLISION -> MenuButton(
                label = event.otherActorType ?: stringResource(Res.string.event_collision_choose),
                options = actorTypeNames,
                optionText = { it },
                onSelected = onOtherActorTypeChange,
            )
            else -> Unit
        }
    }
    HorizontalDivider()
    if (event.blocks.isEmpty()) {
        Text(
            modifier = Modifier.padding(16.dp),
            text = stringResource(Res.string.event_blocks_empty),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    Column(
        modifier = Modifier.verticalScroll(rememberScrollState()).padding(start = 12.dp, end = 4.dp, top = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        val nesting = remember(event.blocks) { BlockNesting.of(event.blocks).first }
        event.blocks.forEachIndexed { index, block ->
            BlockRow(
                block = block,
                nesting = nesting[index],
                properties = properties,
                isFirst = index == 0,
                isLast = index == event.blocks.lastIndex,
                onBlockChange = { onBlockChange(index, it) },
                onMoveUp = { onBlockMoved(index, index - 1) },
                onMoveDown = { onBlockMoved(index, index + 1) },
                onRemove = { onBlockRemoved(index) },
                flashKey = highlight?.key?.takeIf { highlight.blockIndex == index },
            )
        }
    }
}
