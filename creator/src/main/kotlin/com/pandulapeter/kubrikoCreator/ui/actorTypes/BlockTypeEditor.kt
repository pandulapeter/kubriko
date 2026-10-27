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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import com.pandulapeter.kubrikoCreator.actors.ActorTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.BlockScope
import com.pandulapeter.kubrikoCreator.actors.BlockTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.ManagerTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.ScreenTextDefinition
import com.pandulapeter.kubrikoCreator.commands.Invocation
import com.pandulapeter.kubrikoCreator.commands.actorTypes.AddBlock
import com.pandulapeter.kubrikoCreator.commands.actorTypes.AddEvent
import com.pandulapeter.kubrikoCreator.commands.actorTypes.AddProperty
import com.pandulapeter.kubrikoCreator.commands.actorTypes.DeleteActorType
import com.pandulapeter.kubrikoCreator.commands.actorTypes.DiscardChangesMadeByHand
import com.pandulapeter.kubrikoCreator.commands.actorTypes.EditBlock
import com.pandulapeter.kubrikoCreator.commands.actorTypes.EditProperty
import com.pandulapeter.kubrikoCreator.commands.actorTypes.MoveBlock
import com.pandulapeter.kubrikoCreator.commands.actorTypes.MoveEvent
import com.pandulapeter.kubrikoCreator.commands.actorTypes.RemoveBlock
import com.pandulapeter.kubrikoCreator.commands.actorTypes.RemoveEvent
import com.pandulapeter.kubrikoCreator.commands.actorTypes.RemoveProperty
import com.pandulapeter.kubrikoCreator.commands.actorTypes.SetAppearance
import com.pandulapeter.kubrikoCreator.commands.actorTypes.SetEventKey
import com.pandulapeter.kubrikoCreator.commands.actorTypes.SetEventOtherActorType
import com.pandulapeter.kubrikoCreator.commands.actorTypes.SetEventTimer
import com.pandulapeter.kubrikoCreator.commands.managerTypes.AddText
import com.pandulapeter.kubrikoCreator.commands.managerTypes.DeleteManagerType
import com.pandulapeter.kubrikoCreator.commands.managerTypes.EditText
import com.pandulapeter.kubrikoCreator.commands.managerTypes.RemoveText
import com.pandulapeter.kubrikoCreator.ui.components.IconActionButton
import com.pandulapeter.kubrikoCreator.ui.components.rememberFlashColor
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.action_delete_actor_type
import kubriko.creator.generated.resources.action_delete_manager_type
import kubriko.creator.generated.resources.actor_type_properties_empty
import kubriko.creator.generated.resources.actor_type_regenerated
import kubriko.creator.generated.resources.ic_delete
import kubriko.creator.generated.resources.manager_type_properties_empty
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * Edits a block-built Actor type or Manager type in GameMaker's arrangement: its appearance (or, for a Manager type,
 * the texts it shows) and Properties, the column of its Events, the Blocks of the selected Event in the order they run,
 * and the palette of Blocks to add.
 *
 * @param scope What its Blocks can refer to.
 */
@Composable
internal fun BlockTypeEditor(
    definition: BlockTypeDefinition,
    scope: BlockScope,
    isChangedByHand: Boolean,
    onCommand: (Invocation<*>) -> Unit,
    modifier: Modifier = Modifier,
    highlight: ActorTypeHighlight? = null,
) = Box(
    modifier = modifier,
) {
    val name = definition.name
    var requestedEventIndex by remember(definition.name) { mutableStateOf<Int?>(null) }
    LaunchedEffect(highlight?.key) { highlight?.eventIndex?.let { requestedEventIndex = it } }
    val eventIndex = selectedEventIndex(requestedEventIndex, definition.events.size)
    val event = eventIndex?.let { definition.events[it] }
    val currentEventIndex by rememberUpdatedState(eventIndex)
    val currentOnCommand by rememberUpdatedState(onCommand)
    val dragState = remember(name) {
        BlockDragState(
            onDrop = { block, gap ->
                currentEventIndex?.let { index ->
                    when (block) {
                        is DraggedBlock.New -> currentOnCommand(Invocation(AddBlock, AddBlock.Input(name, index, block.kind.createDefault(), index = gap)))
                        is DraggedBlock.Existing -> moveTarget(block.index, gap)?.let { currentOnCommand(Invocation(MoveBlock, MoveBlock.Input(name, index, block.index, it))) }
                    }
                }
            },
        )
    }
    Column {
        Header(
            flashKey = highlight?.key?.takeIf { highlight.eventIndex == null },
            definition = definition,
            onDelete = {
                when (definition) {
                    is ActorTypeDefinition -> onCommand(Invocation(DeleteActorType, DeleteActorType.Input(name)))
                    is ManagerTypeDefinition -> onCommand(Invocation(DeleteManagerType, DeleteManagerType.Input(name)))
                }
            },
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
            Column(
                modifier = Modifier.width(230.dp).fillMaxHeight().verticalScroll(rememberScrollState()),
            ) {
                if (definition is ActorTypeDefinition) {
                    AppearanceColumn(
                        definition = definition,
                        onChange = { onCommand(Invocation(SetAppearance, it)) },
                    )
                    HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
                }
                PropertyList(
                    properties = definition.properties,
                    emptyHint = if (definition is ManagerTypeDefinition) Res.string.manager_type_properties_empty else Res.string.actor_type_properties_empty,
                    onAdded = { onCommand(Invocation(AddProperty, AddProperty.Input(name, it))) },
                    onChanged = { propertyName, property -> onCommand(Invocation(EditProperty, EditProperty.Input(name, propertyName, property))) },
                    onRemoved = { onCommand(Invocation(RemoveProperty, RemoveProperty.Input(name, it))) },
                )
                if (definition is ManagerTypeDefinition) {
                    HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
                    ScreenTextList(
                        texts = definition.texts,
                        onAdded = { onCommand(Invocation(AddText, AddText.Input(name, ScreenTextDefinition(text = "{${definition.properties.firstOrNull()?.name ?: "score"}}")))) },
                        onChanged = { index, text -> onCommand(Invocation(EditText, EditText.Input(name, index, text))) },
                        onRemoved = { index -> onCommand(Invocation(RemoveText, RemoveText.Input(name, index))) },
                    )
                }
            }
            VerticalDivider()
            EventList(
                modifier = Modifier.width(260.dp).fillMaxHeight(),
                typeKind = definition.kind,
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
                scope = scope,
                onKeyChange = { key -> eventIndex?.let { onCommand(Invocation(SetEventKey, SetEventKey.Input(name, it, key))) } },
                onTimerChange = { timer -> eventIndex?.let { onCommand(Invocation(SetEventTimer, SetEventTimer.Input(name, it, timer))) } },
                onOtherActorTypeChange = { other -> eventIndex?.let { onCommand(Invocation(SetEventOtherActorType, SetEventOtherActorType.Input(name, it, other))) } },
                onBlockChange = { blockIndex, block -> eventIndex?.let { onCommand(Invocation(EditBlock, EditBlock.Input(name, it, blockIndex, block))) } },
                dragState = dragState,
                onBlockRemoved = { blockIndex -> eventIndex?.let { onCommand(Invocation(RemoveBlock, RemoveBlock.Input(name, it, blockIndex))) } },
                highlight = highlight?.takeIf { it.eventIndex == eventIndex },
            )
            VerticalDivider()
            BlockPalette(
                modifier = Modifier.width(200.dp).fillMaxHeight().background(MaterialTheme.colorScheme.surfaceContainerLow),
                eventType = event?.type,
                typeKind = definition.kind,
                dragState = dragState,
                onBlockKindSelected = { kind -> eventIndex?.let { onCommand(Invocation(AddBlock, AddBlock.Input(name, it, kind.createDefault()))) } },
            )
        }
    }
    DragPreview(dragState)
}

@Composable
private fun Header(
    flashKey: Any?,
    definition: BlockTypeDefinition,
    onDelete: () -> Unit,
) = Row(
    modifier = Modifier.fillMaxWidth().background(rememberFlashColor(flashKey)).padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
    horizontalArrangement = Arrangement.spacedBy(10.dp),
    verticalAlignment = Alignment.CenterVertically,
) {
    Icon(
        modifier = Modifier.size(20.dp),
        painter = painterResource(definition.kind.icon),
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
        label = if (definition is ManagerTypeDefinition) Res.string.action_delete_manager_type else Res.string.action_delete_actor_type,
        onClick = onDelete,
    )
}

/**
 * The Block being dragged, following the pointer.
 */
@Composable
private fun DragPreview(dragState: BlockDragState) {
    val dragged = dragState.dragged ?: return
    var origin by remember { mutableStateOf(Offset.Zero) }
    Box(
        modifier = Modifier.fillMaxSize().onGloballyPositioned { origin = it.positionInRoot() },
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
                painter = painterResource(dragged.kind.icon),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
            )
            Text(
                text = stringResource(dragged.kind.title),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
