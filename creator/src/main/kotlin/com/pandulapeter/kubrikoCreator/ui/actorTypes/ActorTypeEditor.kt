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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import com.pandulapeter.kubrikoCreator.actors.withEventOtherActorType
import com.pandulapeter.kubrikoCreator.actors.withEventRemoved
import com.pandulapeter.kubrikoCreator.ui.components.IconActionButton
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.action_add_event
import kubriko.creator.generated.resources.action_delete_actor_type
import kubriko.creator.generated.resources.actor_type_appearance
import kubriko.creator.generated.resources.actor_type_events
import kubriko.creator.generated.resources.actor_type_events_empty
import kubriko.creator.generated.resources.actor_type_regenerated
import kubriko.creator.generated.resources.ic_actor_type
import kubriko.creator.generated.resources.ic_add
import kubriko.creator.generated.resources.ic_delete
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * Edits a block-built Actor type: how it looks, and which Blocks run on which Events.
 */
@Composable
internal fun ActorTypeEditor(
    definition: ActorTypeDefinition,
    actorTypeNames: List<String>,
    isChangedByHand: Boolean,
    onChange: (ActorTypeDefinition) -> Unit,
    onDelete: () -> Unit,
    onDiscardChangesMadeByHand: () -> Unit,
    modifier: Modifier = Modifier,
) = Column(
    modifier = modifier.verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            modifier = Modifier.size(22.dp),
            painter = painterResource(Res.drawable.ic_actor_type),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(
            modifier = Modifier.weight(1f),
            text = definition.name,
            style = MaterialTheme.typography.titleLarge,
        )
        IconActionButton(
            icon = Res.drawable.ic_delete,
            label = Res.string.action_delete_actor_type,
            onClick = onDelete,
        )
    }
    if (isChangedByHand) {
        ChangedByHandNotice(
            actorTypeName = definition.name,
            onDiscard = onDiscardChangesMadeByHand,
        )
    }
    Text(
        modifier = Modifier.widthIn(max = 640.dp),
        text = stringResource(Res.string.actor_type_regenerated),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    SectionTitle(stringResource(Res.string.actor_type_appearance))
    AppearanceSection(
        definition = definition,
        onChange = onChange,
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SectionTitle(
            modifier = Modifier.weight(1f),
            text = stringResource(Res.string.actor_type_events),
        )
        MenuButton(
            label = stringResource(Res.string.action_add_event),
            icon = Res.drawable.ic_add,
            options = EventType.entries,
            optionText = { stringResource(it.title) },
            optionIcon = { it.icon },
            onSelected = { onChange(definition.withEventAdded(it)) },
        )
    }
    if (definition.events.isEmpty()) {
        Text(
            text = stringResource(Res.string.actor_type_events_empty),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    definition.events.forEachIndexed { eventIndex, event ->
        EventCard(
            event = event,
            actorTypeNames = actorTypeNames,
            onKeyChange = { onChange(definition.withEventKey(eventIndex, it)) },
            onOtherActorTypeChange = { onChange(definition.withEventOtherActorType(eventIndex, it)) },
            onRemove = { onChange(definition.withEventRemoved(eventIndex)) },
            onBlockAdded = { onChange(definition.withBlockAdded(eventIndex, it)) },
            onBlockChange = { blockIndex, block -> onChange(definition.withBlockReplaced(eventIndex, blockIndex, block)) },
            onBlockMoved = { blockIndex, offset -> onChange(definition.withBlockMoved(eventIndex, blockIndex, offset)) },
            onBlockRemoved = { onChange(definition.withBlockRemoved(eventIndex, it)) },
        )
    }
}

@Composable
private fun SectionTitle(
    text: String,
    modifier: Modifier = Modifier,
) = Text(
    modifier = modifier.padding(top = 8.dp),
    text = text,
    style = MaterialTheme.typography.titleMedium,
)
