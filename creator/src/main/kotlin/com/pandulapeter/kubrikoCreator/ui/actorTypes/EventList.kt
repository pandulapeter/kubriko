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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import com.pandulapeter.kubrikoCreator.actors.BlockTypeKind
import com.pandulapeter.kubrikoCreator.actors.EventDefinition
import com.pandulapeter.kubrikoCreator.actors.EventType
import com.pandulapeter.kubrikoCreator.ui.components.Accent
import com.pandulapeter.kubrikoCreator.ui.components.IconActionButton
import com.pandulapeter.kubrikoCreator.ui.components.PanelHeader
import com.pandulapeter.kubrikoCreator.ui.components.rememberFlashColor
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.action_add_event
import kubriko.creator.generated.resources.action_override_event
import kubriko.creator.generated.resources.actor_type_events
import kubriko.creator.generated.resources.actor_type_events_empty
import kubriko.creator.generated.resources.actor_type_inherited_events
import kubriko.creator.generated.resources.event_collision_choose
import kubriko.creator.generated.resources.ic_add
import kubriko.creator.generated.resources.ic_close
import kubriko.creator.generated.resources.ic_event
import kubriko.creator.generated.resources.ic_loop
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
    listIdentity: Any? = null,
    inheritedEvents: List<EventDefinition> = emptyList(),
    onOverride: (EventDefinition) -> Unit = {},
) = Column(
    modifier = modifier,
) {
    PanelHeader(
        icon = Res.drawable.ic_event,
        title = stringResource(Res.string.actor_type_events),
        accent = Accent.EVENT,
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
    key(listIdentity) {
        val currentOnMoved by rememberUpdatedState(onMoved)
        val dragState = remember { ListDragState<Int>(onDrop = { index, gap -> moveTarget(index, gap)?.let { currentOnMoved(index, it) } }) }
        val listState = rememberLazyListState()
        val itemKeys = remember { ListItemKeys<EventDefinition>() }
        val keys = remember(itemKeys, events) { itemKeys.update(events) }
        val gap = dragState.gap
        Box {
            LazyColumn(
                modifier = Modifier.lazyListDropTarget(dragState, listState, events.size, EVENT_CONTENT_TYPE),
                state = listState,
            ) {
                items(count = events.size, key = { keys[it] }, contentType = { EVENT_CONTENT_TYPE }) { index ->
                    EventListItem(
                        modifier = Modifier.animateItem(),
                        event = events[index],
                        index = index,
                        isSelected = index == selectedIndex,
                        dragState = dragState,
                        isDropAbove = gap == index,
                        isDropBelow = gap == events.size && index == events.lastIndex,
                        onClick = { onSelected(index) },
                        onRemove = { onRemoved(index) },
                        flashKey = highlight?.key?.takeIf { highlight.eventIndex == index && highlight.blockIndex == null } ?: keys[index].takeIf(itemKeys::isRecentlyAdded),
                    )
                }
                if (inheritedEvents.isNotEmpty()) {
                    item {
                        Text(
                            modifier = Modifier.padding(start = 12.dp, top = 8.dp, bottom = 2.dp),
                            text = stringResource(Res.string.actor_type_inherited_events),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    items(inheritedEvents) { event -> InheritedEventItem(event = event, onOverride = { onOverride(event) }) }
                }
            }
            EventDragPreview(dragState, events)
        }
    }
}

/**
 * The Event being dragged, following the pointer.
 */
@Composable
private fun BoxScope.EventDragPreview(dragState: ListDragState<Int>, events: List<EventDefinition>) {
    val event = dragState.dragged?.let(events::getOrNull) ?: return
    var origin by remember { mutableStateOf(Offset.Zero) }
    Box(
        modifier = Modifier.matchParentSize().onGloballyPositioned { origin = it.positionInRoot() },
    ) {
        Row(
            modifier = Modifier
                .offset { (dragState.pointer - origin + Offset(12f, 12f)).round() }
                .shadow(6.dp, MaterialTheme.shapes.small)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest, MaterialTheme.shapes.small)
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                modifier = Modifier.size(16.dp),
                painter = painterResource(event.type.icon),
                contentDescription = null,
                tint = Accent.EVENT.color,
            )
            Text(
                text = stringResource(event.type.title),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

private const val EVENT_CONTENT_TYPE = "event"

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
