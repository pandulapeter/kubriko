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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.pandulapeter.kubrikoCreator.actors.BlockDefinition
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
import com.pandulapeter.kubrikoCreator.commands.actorTypes.SetParent
import com.pandulapeter.kubrikoCreator.commands.managerTypes.AddText
import com.pandulapeter.kubrikoCreator.commands.managerTypes.DeleteManagerType
import com.pandulapeter.kubrikoCreator.commands.managerTypes.EditText
import com.pandulapeter.kubrikoCreator.commands.managerTypes.RemoveText
import com.pandulapeter.kubrikoCreator.commands.managerTypes.SetKotlinUi
import com.pandulapeter.kubrikoCreator.ui.components.AccentIcon
import com.pandulapeter.kubrikoCreator.ui.components.IconActionButton
import com.pandulapeter.kubrikoCreator.ui.components.rememberFlashColor
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.action_delete_actor_type
import kubriko.creator.generated.resources.action_delete_manager_type
import kubriko.creator.generated.resources.actor_type_properties_empty
import kubriko.creator.generated.resources.ic_delete
import kubriko.creator.generated.resources.manager_type_properties_empty
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * Edits a block-built Actor type or Manager type in GameMaker's arrangement: its appearance (or, for a Manager type,
 * the texts and Kotlin UI it shows) and Properties, the column of its Events, the Blocks of the selected Event in the order they run,
 * and the palette of Blocks to add.
 *
 * @param scope What its Blocks can refer to.
 * @param sprites The images an Actor type can be drawn as, by their path from `composeResources`.
 * @param inheritance What an Actor type gets from its parent, and the parents it can have.
 */
@Composable
internal fun BlockTypeEditor(
    definition: BlockTypeDefinition,
    scope: BlockScope,
    sprites: List<String>,
    inheritance: Inheritance,
    isChangedByHand: Boolean,
    onCommand: (Invocation<*>) -> Unit,
    onRename: (String) -> StringResource?,
    onTypeSelected: (String) -> Unit,
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
    var blockListScrollToEndRequest by remember(name) { mutableIntStateOf(0) }
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
        BlockTypeHeader(
            definition = definition,
            inheritance = inheritance,
            flashKey = highlight?.key?.takeIf { highlight.eventIndex == null },
            onRename = onRename,
            onParentChange = { onCommand(Invocation(SetParent, SetParent.Input(name, it))) },
            onTypeSelected = onTypeSelected,
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
            LazyColumn(
                modifier = Modifier.width(250.dp).fillMaxHeight(),
            ) {
                if (definition is ActorTypeDefinition) {
                    item {
                        Column {
                            AppearanceColumn(
                                definition = definition,
                                sprites = sprites,
                                onChange = { onCommand(Invocation(SetAppearance, it)) },
                            )
                            HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
                        }
                    }
                }
                propertyListItems(
                    properties = definition.properties,
                    inheritedProperties = inheritance.properties,
                    emptyHint = if (definition is ManagerTypeDefinition) Res.string.manager_type_properties_empty else Res.string.actor_type_properties_empty,
                    onAdded = { onCommand(Invocation(AddProperty, AddProperty.Input(name, it))) },
                    onChanged = { propertyName, property -> onCommand(Invocation(EditProperty, EditProperty.Input(name, propertyName, property))) },
                    onRemoved = { onCommand(Invocation(RemoveProperty, RemoveProperty.Input(name, it))) },
                )
                if (definition is ManagerTypeDefinition) {
                    item {
                        Column {
                            HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
                            ScreenTextList(
                                texts = definition.texts,
                                onAdded = { onCommand(Invocation(AddText, AddText.Input(name, ScreenTextDefinition(text = "{${definition.properties.firstOrNull()?.name ?: "score"}}")))) },
                                onChanged = { index, text -> onCommand(Invocation(EditText, EditText.Input(name, index, text))) },
                                onRemoved = { index -> onCommand(Invocation(RemoveText, RemoveText.Input(name, index))) },
                            )
                            HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
                            KotlinUiSection(
                                managerTypeName = name,
                                code = definition.kotlinUi,
                                onCodeChange = { onCommand(Invocation(SetKotlinUi, SetKotlinUi.Input(name, it))) },
                            )
                        }
                    }
                }
            }
            VerticalDivider()
            EventList(
                modifier = Modifier.width(280.dp).fillMaxHeight(),
                typeKind = definition.kind,
                events = definition.events,
                listIdentity = name,
                inheritedEvents = inheritance.events,
                onOverride = { event ->
                    onCommand(Invocation(AddEvent, AddEvent.Input(name, event.type, event.key, event.otherActorType, event.timer, listOf(BlockDefinition.RunParentEvent))))
                    requestedEventIndex = definition.events.size
                },
                selectedIndex = eventIndex,
                onSelected = { requestedEventIndex = it },
                onAdded = { type ->
                    onCommand(Invocation(AddEvent, AddEvent.Input(name, type)))
                    requestedEventIndex = definition.events.size
                },
                onMoved = { index, targetIndex ->
                    onCommand(Invocation(MoveEvent, MoveEvent.Input(name, index, targetIndex)))
                    requestedEventIndex = selectedEventIndexAfterMove(eventIndex, index, targetIndex)
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
                listIdentity = name to eventIndex,
                scrollToEndRequest = blockListScrollToEndRequest,
            )
            VerticalDivider()
            BlockPalette(
                modifier = Modifier.width(220.dp).fillMaxHeight().background(MaterialTheme.colorScheme.surfaceContainerLow),
                eventType = event?.type,
                typeKind = definition.kind,
                dragState = dragState,
                onBlockKindSelected = { kind ->
                    eventIndex?.let {
                        onCommand(Invocation(AddBlock, AddBlock.Input(name, it, kind.createDefault())))
                        blockListScrollToEndRequest++
                    }
                },
            )
        }
    }
    DragPreview(dragState)
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
                tint = dragged.kind.category.accent.color,
            )
            Text(
                text = stringResource(dragged.kind.title),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
