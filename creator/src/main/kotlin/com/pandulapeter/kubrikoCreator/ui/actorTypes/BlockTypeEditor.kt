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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoCreator.actors.ActorTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.BlockScope
import com.pandulapeter.kubrikoCreator.actors.BlockTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.ManagerTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.PropertyDefinition
import com.pandulapeter.kubrikoCreator.commands.Invocation
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.ui.components.DescriptionField
import com.pandulapeter.kubrikoCreator.ui.components.DragPreviewChip
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.actor_type_properties_empty
import kubriko.creator.generated.resources.manager_type_properties_empty
import org.jetbrains.compose.resources.stringResource

/**
 * Edits a block-built Actor type or Manager type in GameMaker's arrangement: its appearance (or, for a Manager type,
 * the texts and Kotlin UI it shows) and Properties, the column of its Events, the Blocks of the selected Event in the order they run,
 * and the palette of Blocks to add.
 *
 * @param scope What its Blocks can refer to.
 * @param sprites The images an Actor type can be drawn as, by their path from `composeResources`.
 * @param inheritance What an Actor type gets from its parent, and the parents it can have.
 * @param currentDefinition The type as it is now, which an add that succeeded reads to find the Event it made, since
 *   [definition] may not show it yet, or may show the AI's changes from before it.
 */
@Composable
internal fun BlockTypeEditor(
    definition: BlockTypeDefinition,
    scope: BlockScope,
    sprites: List<String>,
    inheritance: Inheritance,
    isChangedByHand: Boolean,
    onCommand: (Invocation<*>, onSuccess: () -> Unit) -> Unit,
    currentDefinition: () -> BlockTypeDefinition?,
    onRename: (String) -> Rejection?,
    onTypeSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    highlight: ActorTypeHighlight? = null,
) = Box(
    modifier = modifier,
) {
    val name = definition.name
    val eventSelection = remember(name) { EventSelectionState() }
    val eventKeys = remember(eventSelection, definition.events) { eventSelection.keysOf(definition.events) }
    val eventIndex = eventSelection.shownIndex(eventKeys, highlight)
    val event = eventIndex?.let { definition.events[it] }
    val actions = remember(definition, onCommand) { BlockTypeEditorActions(definition, onCommand) }
    val selectAddedEvent: () -> Unit = { currentDefinition()?.let { eventSelection.selectAdded(it.events) } }
    var blockListScrollToEndRequest by remember(name) { mutableIntStateOf(0) }
    val propertyItemKeys = remember(name) { ListItemKeys<PropertyDefinition>() }
    val propertyKeys = remember(propertyItemKeys, definition.properties) { propertyItemKeys.update(definition.properties) }
    val dragState = rememberBlockDragState(name, eventIndex, actions)
    Column {
        TypeDetails(
            definition = definition,
            inheritance = inheritance,
            isChangedByHand = isChangedByHand,
            flashKey = highlight?.key?.takeIf { highlight.eventIndex == null },
            onRename = onRename,
            onTypeSelected = onTypeSelected,
            actions = actions,
        )
        HorizontalDivider()
        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth().weight(1f),
        ) {
            val columns = remember(maxWidth) { BlockTypeEditorColumns.of(maxWidth) }
            Row(
                modifier = Modifier.fillMaxSize(),
            ) {
                PropertiesColumn(
                    modifier = Modifier.width(columns.properties).fillMaxHeight(),
                    definition = definition,
                    sprites = sprites,
                    propertyKeys = propertyKeys,
                    inheritedProperties = inheritance.properties,
                    actions = actions,
                )
                VerticalDivider()
                EventList(
                    modifier = Modifier.width(columns.events).fillMaxHeight(),
                    typeKind = definition.kind,
                    events = definition.events,
                    keys = eventKeys,
                    isRecentlyAdded = eventSelection::isRecentlyAdded,
                    listIdentity = name,
                    inheritedEvents = inheritance.events,
                    onOverride = { event -> actions.overrideEvent(event, selectAddedEvent) },
                    selectedIndex = eventIndex,
                    onSelected = { eventSelection.select(eventKeys[it]) },
                    onAdded = { type -> actions.addEvent(type, selectAddedEvent) },
                    onMoved = actions::moveEvent,
                    onRemoved = actions::removeEvent,
                    highlight = highlight,
                )
                VerticalDivider()
                EventBlocks(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    event = event,
                    scope = scope,
                    onKeyChange = { key -> eventIndex?.let { actions.setEventKey(it, key) } },
                    onTimerChange = { timer -> eventIndex?.let { actions.setEventTimer(it, timer) } },
                    onTimerRename = actions::renameTimer,
                    onOtherActorTypeChange = { other -> eventIndex?.let { actions.setEventOtherActorType(it, other) } },
                    onBlockChange = { blockIndex, block -> eventIndex?.let { actions.editBlock(it, blockIndex, block) } },
                    dragState = dragState,
                    onBlockRemoved = { blockIndex -> eventIndex?.let { actions.removeBlock(it, blockIndex) } },
                    highlight = highlight?.takeIf { it.eventIndex == eventIndex },
                    listIdentity = name to eventIndex?.let(eventKeys::get),
                    scrollToEndRequest = blockListScrollToEndRequest,
                )
                VerticalDivider()
                BlockPalette(
                    modifier = Modifier.width(columns.palette).fillMaxHeight().background(MaterialTheme.colorScheme.surfaceContainerLow),
                    eventType = event?.type,
                    typeKind = definition.kind,
                    dragState = dragState,
                    onBlockKindSelected = { kind ->
                        eventIndex?.let {
                            actions.addBlock(it, kind)
                            blockListScrollToEndRequest++
                        }
                    },
                )
            }
        }
    }
    DragPreview(dragState)
}

/**
 * The drag state of the type [name]'s Blocks, which outlives the definitions it is composed with, so it drops into the
 * latest [eventIndex] through the latest [actions].
 */
@Composable
private fun rememberBlockDragState(name: String, eventIndex: Int?, actions: BlockTypeEditorActions): BlockDragState {
    val currentEventIndex by rememberUpdatedState(eventIndex)
    val currentActions by rememberUpdatedState(actions)
    return remember(name) {
        BlockDragState(
            onDrop = { block, gap ->
                currentEventIndex?.let { index ->
                    when (block) {
                        is DraggedBlock.New -> currentActions.addBlock(index, block.kind, gap)
                        is DraggedBlock.Existing -> currentActions.moveBlock(index, block.index, gap)
                    }
                }
            },
        )
    }
}

/**
 * The header, description, and (when [isChangedByHand]) the notice about hand-made changes of [definition], one under the
 * other in the caller's column.
 */
@Composable
private fun TypeDetails(
    definition: BlockTypeDefinition,
    inheritance: Inheritance,
    isChangedByHand: Boolean,
    flashKey: Any?,
    onRename: (String) -> Rejection?,
    onTypeSelected: (String) -> Unit,
    actions: BlockTypeEditorActions,
) {
    BlockTypeHeader(
        definition = definition,
        inheritance = inheritance,
        flashKey = flashKey,
        onRename = onRename,
        onParentChange = actions::setParent,
        onTypeSelected = onTypeSelected,
        onDelete = actions::delete,
    )
    DescriptionField(
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
        key = definition.name,
        description = definition.description,
        onDescriptionChange = actions::setDescription,
    )
    if (isChangedByHand) {
        ChangedByHandNotice(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            actorTypeName = definition.name,
            onDiscard = actions::discardChangesMadeByHand,
        )
    }
}

/**
 * The column of [definition]'s appearance (for an Actor type), its Properties, and (for a Manager type) the texts and Kotlin UI
 * it shows.
 */
@Composable
private fun PropertiesColumn(
    definition: BlockTypeDefinition,
    sprites: List<String>,
    propertyKeys: List<Long>,
    inheritedProperties: List<PropertyDefinition>,
    actions: BlockTypeEditorActions,
    modifier: Modifier = Modifier,
) = LazyColumn(
    modifier = modifier,
) {
    if (definition is ActorTypeDefinition) {
        item {
            Column {
                AppearanceColumn(
                    definition = definition,
                    sprites = sprites,
                    onChange = actions::setAppearance,
                )
                HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
            }
        }
    }
    propertyListItems(
        properties = definition.properties,
        propertyKeys = propertyKeys,
        inheritedProperties = inheritedProperties,
        emptyHint = if (definition is ManagerTypeDefinition) Res.string.manager_type_properties_empty else Res.string.actor_type_properties_empty,
        onAdded = actions::addProperty,
        onChanged = actions::editProperty,
        onRemoved = actions::removeProperty,
    )
    if (definition is ManagerTypeDefinition) {
        item {
            Column {
                HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
                ScreenTextList(
                    texts = definition.texts,
                    onAdded = actions::addText,
                    onChanged = actions::editText,
                    onRemoved = actions::removeText,
                )
                HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
                KotlinUiSection(
                    managerTypeName = definition.name,
                    code = definition.kotlinUi,
                    onCodeChange = actions::setKotlinUi,
                )
            }
        }
    }
}

/**
 * The Block being dragged, following the pointer.
 */
@Composable
private fun DragPreview(dragState: BlockDragState) {
    val dragged = dragState.dragged ?: return
    DragPreviewChip(
        pointer = { dragState.pointer },
        icon = dragged.kind.icon,
        tint = dragged.kind.category.accent.color,
        text = stringResource(dragged.kind.title),
    )
}
