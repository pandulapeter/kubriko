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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoCreator.actors.ActorTypeDefinition
import com.pandulapeter.kubrikoCreator.commands.Invocation
import com.pandulapeter.kubrikoCreator.commands.actorTypes.AddBlock
import com.pandulapeter.kubrikoCreator.commands.actorTypes.AddEvent
import com.pandulapeter.kubrikoCreator.commands.actorTypes.DeleteActorType
import com.pandulapeter.kubrikoCreator.commands.actorTypes.DiscardChangesMadeByHand
import com.pandulapeter.kubrikoCreator.commands.actorTypes.EditBlock
import com.pandulapeter.kubrikoCreator.commands.actorTypes.MoveBlock
import com.pandulapeter.kubrikoCreator.commands.actorTypes.MoveEvent
import com.pandulapeter.kubrikoCreator.commands.actorTypes.RemoveBlock
import com.pandulapeter.kubrikoCreator.commands.actorTypes.RemoveEvent
import com.pandulapeter.kubrikoCreator.commands.actorTypes.SetAppearance
import com.pandulapeter.kubrikoCreator.commands.actorTypes.SetEventKey
import com.pandulapeter.kubrikoCreator.commands.actorTypes.SetEventOtherActorType
import com.pandulapeter.kubrikoCreator.ui.components.IconActionButton
import com.pandulapeter.kubrikoCreator.ui.components.rememberFlashColor
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.action_delete_actor_type
import kubriko.creator.generated.resources.actor_type_regenerated
import kubriko.creator.generated.resources.ic_actor_type
import kubriko.creator.generated.resources.ic_delete
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * Edits a block-built Actor type in GameMaker's arrangement: its appearance, the column of its Events, the Blocks of
 * the selected Event in the order they run, and the palette of Blocks to add.
 */
@Composable
internal fun ActorTypeEditor(
    definition: ActorTypeDefinition,
    actorTypeNames: List<String>,
    isChangedByHand: Boolean,
    onCommand: (Invocation<*>) -> Unit,
    modifier: Modifier = Modifier,
    highlight: ActorTypeHighlight? = null,
) = Column(
    modifier = modifier,
) {
    val name = definition.name
    var requestedEventIndex by remember(definition.name) { mutableStateOf<Int?>(null) }
    LaunchedEffect(highlight?.key) { highlight?.eventIndex?.let { requestedEventIndex = it } }
    val eventIndex = selectedEventIndex(requestedEventIndex, definition.events.size)
    val event = eventIndex?.let { definition.events[it] }
    Header(
        flashKey = highlight?.key?.takeIf { highlight.eventIndex == null },
        definition = definition,
        onDelete = { onCommand(Invocation(DeleteActorType, DeleteActorType.Input(name))) },
    )
    if (isChangedByHand) {
        ChangedByHandNotice(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            actorTypeName = definition.name,
            onDiscard = { onCommand(Invocation(DiscardChangesMadeByHand, DiscardChangesMadeByHand.Input(name))) },
        )
    }
    HorizontalDivider()
    Row(
        modifier = Modifier.fillMaxWidth().weight(1f),
    ) {
        AppearanceColumn(
            modifier = Modifier.width(210.dp).fillMaxHeight(),
            definition = definition,
            onChange = { onCommand(Invocation(SetAppearance, it)) },
        )
        VerticalDivider()
        EventList(
            modifier = Modifier.width(260.dp).fillMaxHeight(),
            events = definition.events,
            selectedIndex = eventIndex,
            onSelected = { requestedEventIndex = it },
            onAdded = { type ->
                onCommand(Invocation(AddEvent, AddEvent.Input(name, type)))
                requestedEventIndex = definition.events.size
            },
            onMoved = { index, targetIndex ->
                onCommand(Invocation(MoveEvent, MoveEvent.Input(name, index, targetIndex)))
                requestedEventIndex = targetIndex
            },
            onRemoved = { index ->
                onCommand(Invocation(RemoveEvent, RemoveEvent.Input(name, index)))
                if (eventIndex != null && index < eventIndex) requestedEventIndex = eventIndex - 1
            },
            highlight = highlight,
        )
        VerticalDivider()
        EventBlocks(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            event = event,
            actorTypeNames = actorTypeNames,
            onKeyChange = { key -> eventIndex?.let { onCommand(Invocation(SetEventKey, SetEventKey.Input(name, it, key))) } },
            onOtherActorTypeChange = { other -> eventIndex?.let { onCommand(Invocation(SetEventOtherActorType, SetEventOtherActorType.Input(name, it, other))) } },
            onBlockChange = { blockIndex, block -> eventIndex?.let { onCommand(Invocation(EditBlock, EditBlock.Input(name, it, blockIndex, block))) } },
            onBlockMoved = { blockIndex, targetIndex -> eventIndex?.let { onCommand(Invocation(MoveBlock, MoveBlock.Input(name, it, blockIndex, targetIndex))) } },
            onBlockRemoved = { blockIndex -> eventIndex?.let { onCommand(Invocation(RemoveBlock, RemoveBlock.Input(name, it, blockIndex))) } },
            highlight = highlight?.takeIf { it.eventIndex == eventIndex },
        )
        VerticalDivider()
        BlockPalette(
            modifier = Modifier.width(200.dp).fillMaxHeight().background(MaterialTheme.colorScheme.surfaceContainerLow),
            eventType = event?.type,
            onBlockKindSelected = { kind -> eventIndex?.let { onCommand(Invocation(AddBlock, AddBlock.Input(name, it, kind.createDefault()))) } },
        )
    }
}

@Composable
private fun Header(
    flashKey: Any?,
    definition: ActorTypeDefinition,
    onDelete: () -> Unit,
) = Row(
    modifier = Modifier.fillMaxWidth().background(rememberFlashColor(flashKey)).padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
    horizontalArrangement = Arrangement.spacedBy(10.dp),
    verticalAlignment = Alignment.CenterVertically,
) {
    Icon(
        modifier = Modifier.size(20.dp),
        painter = painterResource(Res.drawable.ic_actor_type),
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
    )
    Column(
        modifier = Modifier.weight(1f),
    ) {
        Text(
            text = definition.name,
            style = MaterialTheme.typography.titleLarge,
        )
        Text(
            text = stringResource(Res.string.actor_type_regenerated),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    IconActionButton(
        icon = Res.drawable.ic_delete,
        label = Res.string.action_delete_actor_type,
        onClick = onDelete,
    )
}
