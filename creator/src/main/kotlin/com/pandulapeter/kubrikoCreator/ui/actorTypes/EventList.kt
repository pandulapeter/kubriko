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
import com.pandulapeter.kubrikoCreator.actors.BlockTypeKind
import com.pandulapeter.kubrikoCreator.actors.EventDefinition
import com.pandulapeter.kubrikoCreator.actors.EventType
import com.pandulapeter.kubrikoCreator.ui.components.IconActionButton
import com.pandulapeter.kubrikoCreator.ui.components.PanelHeader
import com.pandulapeter.kubrikoCreator.ui.components.rememberFlashColor
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.action_add_event
import kubriko.creator.generated.resources.action_move_down
import kubriko.creator.generated.resources.action_move_up
import kubriko.creator.generated.resources.action_override_event
import kubriko.creator.generated.resources.action_remove_event
import kubriko.creator.generated.resources.actor_type_events
import kubriko.creator.generated.resources.actor_type_events_empty
import kubriko.creator.generated.resources.actor_type_inherited_events
import kubriko.creator.generated.resources.event_collision_choose
import kubriko.creator.generated.resources.ic_add
import kubriko.creator.generated.resources.ic_close
import kubriko.creator.generated.resources.ic_loop
import kubriko.creator.generated.resources.ic_move_down
import kubriko.creator.generated.resources.ic_move_up
import kubriko.creator.generated.resources.timer_name
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * The Events of an Actor type or Manager type, as in GameMaker's object window: selecting one shows its Blocks next to it.
 */
@Composable
internal fun EventList(
    typeKind: BlockTypeKind,
    events: List<EventDefinition>,
    selectedIndex: Int?,
    onSelected: (Int) -> Unit,
    onAdded: (EventType) -> Unit,
    onMoved: (index: Int, targetIndex: Int) -> Unit,
    onRemoved: (Int) -> Unit,
    modifier: Modifier = Modifier,
    highlight: ActorTypeHighlight? = null,
    inheritedEvents: List<EventDefinition> = emptyList(),
    onOverride: (EventDefinition) -> Unit = {},
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
            options = EventType.entries.filter { it.isAvailableIn(typeKind) },
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
                isFirst = index == 0,
                isLast = index == events.lastIndex,
                onClick = { onSelected(index) },
                onMoveUp = { onMoved(index, index - 1) },
                onMoveDown = { onMoved(index, index + 1) },
                onRemove = { onRemoved(index) },
                flashKey = highlight?.key?.takeIf { highlight.eventIndex == index && highlight.blockIndex == null },
            )
        }
        if (inheritedEvents.isNotEmpty()) {
            Text(
                modifier = Modifier.padding(start = 12.dp, top = 8.dp, bottom = 2.dp),
                text = stringResource(Res.string.actor_type_inherited_events),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            inheritedEvents.forEach { event -> InheritedEventItem(event = event, onOverride = { onOverride(event) }) }
        }
    }
}

/**
 * An Event the Actor type gets from its parent, which it can replace with one of its own.
 */
@Composable
private fun InheritedEventItem(
    event: EventDefinition,
    onOverride: () -> Unit,
) = Row(
    modifier = Modifier
        .fillMaxWidth()
        .heightIn(min = 36.dp)
        .padding(start = 12.dp, end = 4.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalAlignment = Alignment.CenterVertically,
) {
    val color = MaterialTheme.colorScheme.onSurfaceVariant
    Icon(
        modifier = Modifier.size(16.dp),
        painter = painterResource(event.type.icon),
        contentDescription = null,
        tint = color,
    )
    Column(
        modifier = Modifier.weight(1f).padding(vertical = 4.dp),
    ) {
        Text(
            text = stringResource(event.type.title),
            style = MaterialTheme.typography.bodyMedium,
            color = color,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        event.detail()?.let { detail ->
            Text(
                text = detail,
                style = MaterialTheme.typography.bodySmall,
                color = color,
                maxLines = 1,
            )
        }
    }
    IconActionButton(
        icon = Res.drawable.ic_add,
        label = Res.string.action_override_event,
        onClick = onOverride,
    )
}

@Composable
private fun EventListItem(
    event: EventDefinition,
    isSelected: Boolean,
    isFirst: Boolean,
    isLast: Boolean,
    onClick: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
    flashKey: Any?,
) = Row(
    modifier = Modifier
        .fillMaxWidth()
        .heightIn(min = 36.dp)
        .then(if (isSelected) Modifier.background(MaterialTheme.colorScheme.secondaryContainer) else Modifier)
        .background(rememberFlashColor(flashKey))
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
    if (isSelected) {
        IconActionButton(icon = Res.drawable.ic_move_up, label = Res.string.action_move_up, onClick = onMoveUp, isEnabled = !isFirst)
        IconActionButton(icon = Res.drawable.ic_move_down, label = Res.string.action_move_down, onClick = onMoveDown, isEnabled = !isLast)
    }
    IconActionButton(
        icon = Res.drawable.ic_close,
        label = Res.string.action_remove_event,
        onClick = onRemove,
    )
}

/**
 * What tells apart Events of the same type: the key, the other Actor type, or the Timer.
 */
@Composable
internal fun EventDefinition.detail(): String? = when (type) {
    EventType.KEY_PRESSED -> key?.let { stringResource(it.title) }
    EventType.COLLISION -> otherActorType ?: stringResource(Res.string.event_collision_choose)
    EventType.TIMER_FINISHED -> timer?.let { stringResource(Res.string.timer_name, it) }
    else -> null
}
