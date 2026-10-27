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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoCreator.actors.ActorTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.EventType
import com.pandulapeter.kubrikoCreator.actors.withBlockAdded
import com.pandulapeter.kubrikoCreator.actors.withBlockMoved
import com.pandulapeter.kubrikoCreator.actors.withBlockRemoved
import com.pandulapeter.kubrikoCreator.actors.withBlockReplaced
import com.pandulapeter.kubrikoCreator.actors.withEventAdded
import com.pandulapeter.kubrikoCreator.actors.withEventKey
import com.pandulapeter.kubrikoCreator.actors.withEventRemoved
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.action_add_event
import kubriko.creator.generated.resources.action_delete_actor_type
import kubriko.creator.generated.resources.actor_type_events
import kubriko.creator.generated.resources.actor_type_events_empty
import kubriko.creator.generated.resources.actor_type_regenerated
import org.jetbrains.compose.resources.stringResource

/**
 * Edits a block-built Actor type: how it looks, and which Blocks run on which Events.
 */
@Composable
internal fun ActorTypeEditor(
    definition: ActorTypeDefinition,
    onChange: (ActorTypeDefinition) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) = Column(
    modifier = modifier.verticalScroll(rememberScrollState()).padding(24.dp),
    verticalArrangement = Arrangement.spacedBy(24.dp),
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = definition.name,
            style = MaterialTheme.typography.headlineSmall,
        )
        Spacer(Modifier.weight(1f))
        TextButton(onClick = onDelete) {
            Text(stringResource(Res.string.action_delete_actor_type))
        }
    }
    Text(
        text = stringResource(Res.string.actor_type_regenerated),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    AppearanceSection(
        definition = definition,
        onChange = onChange,
    )
    Text(
        text = stringResource(Res.string.actor_type_events),
        style = MaterialTheme.typography.titleLarge,
    )
    if (definition.events.isEmpty()) {
        Text(
            text = stringResource(Res.string.actor_type_events_empty),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
    definition.events.forEachIndexed { eventIndex, event ->
        EventCard(
            event = event,
            onKeyChange = { onChange(definition.withEventKey(eventIndex, it)) },
            onRemove = { onChange(definition.withEventRemoved(eventIndex)) },
            onBlockAdded = { onChange(definition.withBlockAdded(eventIndex, it)) },
            onBlockChange = { blockIndex, block -> onChange(definition.withBlockReplaced(eventIndex, blockIndex, block)) },
            onBlockMoved = { blockIndex, offset -> onChange(definition.withBlockMoved(eventIndex, blockIndex, offset)) },
            onBlockRemoved = { onChange(definition.withBlockRemoved(eventIndex, it)) },
        )
    }
    MenuButton(
        label = stringResource(Res.string.action_add_event),
        options = EventType.entries,
        optionTitle = { it.title },
        onSelected = { onChange(definition.withEventAdded(it)) },
    )
}
