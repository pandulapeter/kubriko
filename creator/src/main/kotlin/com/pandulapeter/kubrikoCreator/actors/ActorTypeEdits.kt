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

internal fun ActorTypeDefinition.withEventInserted(eventIndex: Int, event: EventDefinition) = copy(
    events = events.toMutableList().apply { add(eventIndex.coerceIn(0, size), event) },
)

internal fun ActorTypeDefinition.withEventRemoved(eventIndex: Int) = if (eventIndex in events.indices) {
    copy(events = events.filterIndexed { index, _ -> index != eventIndex })
} else {
    this
}

internal fun ActorTypeDefinition.withEventMoved(eventIndex: Int, targetIndex: Int) = if (eventIndex in events.indices) {
    copy(events = events.moved(eventIndex, targetIndex))
} else {
    this
}

internal fun ActorTypeDefinition.withEventKey(eventIndex: Int, key: KeyChoice) = withEvent(eventIndex) { it.copy(key = key) }

internal fun ActorTypeDefinition.withEventTimer(eventIndex: Int, timer: Int) = withEvent(eventIndex) { it.copy(timer = timer) }

internal fun ActorTypeDefinition.withEventOtherActorType(eventIndex: Int, actorTypeName: String?) = withEvent(eventIndex) { it.copy(otherActorType = actorTypeName) }

internal fun ActorTypeDefinition.withBlockInserted(eventIndex: Int, blockIndex: Int, block: BlockDefinition) = withEvent(eventIndex) { event ->
    event.copy(blocks = event.blocks.toMutableList().apply { add(blockIndex.coerceIn(0, size), block) })
}

internal fun ActorTypeDefinition.withBlockReplaced(eventIndex: Int, blockIndex: Int, block: BlockDefinition) = withEvent(eventIndex) { event ->
    if (blockIndex in event.blocks.indices) event.copy(blocks = event.blocks.mapIndexed { index, it -> if (index == blockIndex) block else it }) else event
}

internal fun ActorTypeDefinition.withBlockRemoved(eventIndex: Int, blockIndex: Int) = withEvent(eventIndex) { event ->
    event.copy(blocks = event.blocks.filterIndexed { index, _ -> index != blockIndex })
}

internal fun ActorTypeDefinition.withBlockMoved(eventIndex: Int, blockIndex: Int, targetIndex: Int) = withEvent(eventIndex) { event ->
    if (blockIndex in event.blocks.indices) event.copy(blocks = event.blocks.moved(blockIndex, targetIndex)) else event
}

/**
 * Every collision Event and Block that names [oldName] names [newName] instead.
 */
internal fun ActorTypeDefinition.withOtherActorTypeRenamed(oldName: String, newName: String) = copy(
    events = events.map { event ->
        event.copy(
            otherActorType = if (event.otherActorType == oldName) newName else event.otherActorType,
            blocks = event.blocks.map { it.withActorTypeRenamed(oldName, newName) },
        )
    },
)

internal fun ActorTypeDefinition.withPropertyInserted(index: Int, property: PropertyDefinition) = copy(
    properties = properties.toMutableList().apply { add(index.coerceIn(0, size), property) },
)

internal fun ActorTypeDefinition.withPropertyRemoved(name: String) = copy(properties = properties.filter { it.name != name })

/**
 * The Property called [name] replaced by [property]. If that renames it, the Blocks that change it (and Log Blocks that
 * show it) follow.
 */
internal fun ActorTypeDefinition.withPropertyReplaced(name: String, property: PropertyDefinition) = copy(
    properties = properties.map { if (it.name == name) property else it },
    events = if (property.name == name) events else events.map { event -> event.copy(blocks = event.blocks.map { it.withPropertyRenamed(name, property.name) }) },
)

private fun ActorTypeDefinition.withEvent(eventIndex: Int, change: (EventDefinition) -> EventDefinition) = if (eventIndex in events.indices) {
    copy(events = events.mapIndexed { index, event -> if (index == eventIndex) change(event) else event })
} else {
    this
}

private fun <T> List<T>.moved(index: Int, targetIndex: Int) = toMutableList().apply { add(targetIndex.coerceIn(indices), removeAt(index)) }
