/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.actors

internal fun BlockTypeDefinition.withEventInserted(eventIndex: Int, event: EventDefinition) = withEvents(
    events.toMutableList().apply { add(eventIndex.coerceIn(0, size), event) },
)

internal fun BlockTypeDefinition.withEventRemoved(eventIndex: Int) = if (eventIndex in events.indices) {
    withEvents(events.filterIndexed { index, _ -> index != eventIndex })
} else {
    this
}

internal fun BlockTypeDefinition.withEventMoved(eventIndex: Int, targetIndex: Int) = if (eventIndex in events.indices) {
    withEvents(events.moved(eventIndex, targetIndex))
} else {
    this
}

internal fun BlockTypeDefinition.withEventKey(eventIndex: Int, key: KeyChoice) = withEvent(eventIndex) { it.copy(key = key) }

internal fun BlockTypeDefinition.withEventTimer(eventIndex: Int, timer: Int) = withEvent(eventIndex) { it.copy(timer = timer) }

internal fun BlockTypeDefinition.withEventOtherActorType(eventIndex: Int, actorTypeName: String?) = withEvent(eventIndex) { it.copy(otherActorType = actorTypeName) }

internal fun BlockTypeDefinition.withBlockInserted(eventIndex: Int, blockIndex: Int, block: BlockDefinition) = withEvent(eventIndex) { event ->
    event.copy(blocks = event.blocks.toMutableList().apply { add(blockIndex.coerceIn(0, size), block) })
}

internal fun BlockTypeDefinition.withBlockReplaced(eventIndex: Int, blockIndex: Int, block: BlockDefinition) = withEvent(eventIndex) { event ->
    if (blockIndex in event.blocks.indices) event.copy(blocks = event.blocks.mapIndexed { index, it -> if (index == blockIndex) block else it }) else event
}

internal fun BlockTypeDefinition.withBlockRemoved(eventIndex: Int, blockIndex: Int) = withEvent(eventIndex) { event ->
    event.copy(blocks = event.blocks.filterIndexed { index, _ -> index != blockIndex })
}

internal fun BlockTypeDefinition.withBlockMoved(eventIndex: Int, blockIndex: Int, targetIndex: Int) = withEvent(eventIndex) { event ->
    if (blockIndex in event.blocks.indices) event.copy(blocks = event.blocks.moved(blockIndex, targetIndex)) else event
}

/**
 * Every collision Event and Block that names [oldName] names [newName] instead.
 */
internal fun BlockTypeDefinition.withOtherActorTypeRenamed(oldName: String, newName: String) = withEvents(
    events.map { event ->
        event.copy(
            otherActorType = if (event.otherActorType == oldName) newName else event.otherActorType,
            blocks = event.blocks.map { it.withActorTypeRenamed(oldName, newName) },
        )
    },
)

/**
 * Every Block and text that uses a Property of the Manager type [oldName] uses that of [newName] instead.
 */
internal fun BlockTypeDefinition.withManagerRenamed(oldName: String, newName: String) = withBlocks { it.withManagerRenamed(oldName, newName) }
    .withTexts { PropertyPlaceholders.withManagerRenamed(it, oldName, newName) }

/**
 * Every Block and text that uses the Property [oldName] of the Manager type [manager] uses [newName] instead.
 */
internal fun BlockTypeDefinition.withManagerPropertyRenamed(manager: String, oldName: String, newName: String) = withBlocks { it.withPropertyRenamed(oldName, newName, manager) }
    .withTexts { PropertyPlaceholders.withPropertyRenamed(it, manager, oldName, newName) }

internal fun BlockTypeDefinition.withPropertyInserted(index: Int, property: PropertyDefinition) = withProperties(
    properties.toMutableList().apply { add(index.coerceIn(0, size), property) },
)

internal fun BlockTypeDefinition.withPropertyRemoved(name: String) = withProperties(properties.filter { it.name != name })

/**
 * The Property called [name] replaced by [property]. If that renames it, the Blocks that change it (and Log Blocks and
 * texts that show it) follow.
 */
internal fun BlockTypeDefinition.withPropertyReplaced(name: String, property: PropertyDefinition) = withProperties(properties.map { if (it.name == name) property else it }).let { replaced ->
    if (property.name == name) {
        replaced
    } else {
        replaced.withBlocks { it.withPropertyRenamed(name, property.name) }.withTexts { PropertyPlaceholders.withPropertyRenamed(it, null, name, property.name) }
    }
}

internal fun ManagerTypeDefinition.withTextInserted(index: Int, text: ScreenTextDefinition) = copy(
    texts = texts.toMutableList().apply { add(index.coerceIn(0, size), text) },
)

internal fun ManagerTypeDefinition.withTextReplaced(index: Int, text: ScreenTextDefinition) = copy(
    texts = texts.mapIndexed { textIndex, it -> if (textIndex == index) text else it },
)

internal fun ManagerTypeDefinition.withTextRemoved(index: Int) = copy(texts = texts.filterIndexed { textIndex, _ -> textIndex != index })

private fun BlockTypeDefinition.withEvent(eventIndex: Int, change: (EventDefinition) -> EventDefinition) = if (eventIndex in events.indices) {
    withEvents(events.mapIndexed { index, event -> if (index == eventIndex) change(event) else event })
} else {
    this
}

private fun BlockTypeDefinition.withBlocks(change: (BlockDefinition) -> BlockDefinition) = withEvents(
    events.map { event -> event.copy(blocks = event.blocks.map(change)) },
)

/**
 * A Manager type's texts changed by [change]; other types have none.
 */
private fun BlockTypeDefinition.withTexts(change: (String) -> String) = when (this) {
    is ManagerTypeDefinition -> copy(texts = texts.map { it.copy(text = change(it.text)) })
    is ActorTypeDefinition -> this
}

private fun <T> List<T>.moved(index: Int, targetIndex: Int) = toMutableList().apply { add(targetIndex.coerceIn(indices), removeAt(index)) }
