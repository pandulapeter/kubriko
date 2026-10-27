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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoCreator.actors.EventDefinition
import com.pandulapeter.kubrikoCreator.actors.EventType
import com.pandulapeter.kubrikoCreator.ui.components.IconActionButton
import com.pandulapeter.kubrikoCreator.ui.components.PanelHeader
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.action_add_event
import kubriko.creator.generated.resources.action_remove_event
import kubriko.creator.generated.resources.actor_type_events
import kubriko.creator.generated.resources.actor_type_events_empty
import kubriko.creator.generated.resources.event_collision_choose
import kubriko.creator.generated.resources.ic_add
import kubriko.creator.generated.resources.ic_close
import kubriko.creator.generated.resources.ic_loop
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * The Events of an Actor type, as in GameMaker's object window: selecting one shows its Blocks next to it.
 */
@Composable
internal fun EventList(
    events: List<EventDefinition>,
    selectedIndex: Int?,
    onSelected: (Int) -> Unit,
    onAdded: (EventType) -> Unit,
    onRemoved: (Int) -> Unit,
    modifier: Modifier = Modifier,
) = Column(
    modifier = modifier,
) {
    PanelHeader(
        icon = Res.drawable.ic_loop,
        title = stringResource(Res.string.actor_type_events),
    ) {
        MenuIconButton(
            icon = Res.drawable.ic_add,
            label = Res.string.action_add_event,
            options = EventType.entries,
            optionText = { stringResource(it.title) },
            optionIcon = { it.icon },
            onSelected = onAdded,
        )
    }
    if (events.isEmpty()) {
        Text(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            text = stringResource(Res.string.actor_type_events_empty),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
    ) {
        events.forEachIndexed { index, event ->
            EventListItem(
                event = event,
                isSelected = index == selectedIndex,
                onClick = { onSelected(index) },
                onRemove = { onRemoved(index) },
            )
        }
    }
}

@Composable
private fun EventListItem(
    event: EventDefinition,
    isSelected: Boolean,
    onClick: () -> Unit,
    onRemove: () -> Unit,
) = Row(
    modifier = Modifier
        .fillMaxWidth()
        .heightIn(min = 36.dp)
        .then(if (isSelected) Modifier.background(MaterialTheme.colorScheme.secondaryContainer) else Modifier)
        .clickable(onClick = onClick)
        .padding(start = 12.dp, end = 4.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalAlignment = Alignment.CenterVertically,
) {
    Icon(
        modifier = Modifier.size(16.dp),
        painter = painterResource(event.type.icon),
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
    )
    Column(
        modifier = Modifier.weight(1f).padding(vertical = 4.dp),
    ) {
        Text(
            text = stringResource(event.type.title),
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        event.detail()?.let { detail ->
            Text(
                text = detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
    IconActionButton(
        icon = Res.drawable.ic_close,
        label = Res.string.action_remove_event,
        onClick = onRemove,
    )
}

/**
 * What tells apart Events of the same type: the key, or the other Actor type.
 */
@Composable
internal fun EventDefinition.detail(): String? = when (type) {
    EventType.KEY_PRESSED -> key?.let { stringResource(it.title) }
    EventType.COLLISION -> otherActorType ?: stringResource(Res.string.event_collision_choose)
    else -> null
}
