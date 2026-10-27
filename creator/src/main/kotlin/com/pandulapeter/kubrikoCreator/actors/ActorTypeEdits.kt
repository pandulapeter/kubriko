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

/**
 * The changes the Actor type editor can make, as pure functions returning the changed definition. Out-of-range
 * indices leave the definition unchanged.
 */
internal fun ActorTypeDefinition.withEventAdded(type: EventType) = copy(
    events = events + EventDefinition(type = type, key = if (type == EventType.KEY_PRESSED) KeyChoice.SPACE else null),
)

internal fun ActorTypeDefinition.withEventRemoved(eventIndex: Int) = if (eventIndex in events.indices) {
    copy(events = events.filterIndexed { index, _ -> index != eventIndex })
} else {
    this
}

internal fun ActorTypeDefinition.withEventKey(eventIndex: Int, key: KeyChoice) = withEvent(eventIndex) { it.copy(key = key) }

internal fun ActorTypeDefinition.withBlockAdded(eventIndex: Int, block: BlockDefinition) = withEvent(eventIndex) { it.copy(blocks = it.blocks + block) }

internal fun ActorTypeDefinition.withBlockReplaced(eventIndex: Int, blockIndex: Int, block: BlockDefinition) = withEvent(eventIndex) { event ->
    if (blockIndex in event.blocks.indices) event.copy(blocks = event.blocks.mapIndexed { index, it -> if (index == blockIndex) block else it }) else event
}

internal fun ActorTypeDefinition.withBlockRemoved(eventIndex: Int, blockIndex: Int) = withEvent(eventIndex) { event ->
    event.copy(blocks = event.blocks.filterIndexed { index, _ -> index != blockIndex })
}

/**
 * Moves a Block by [offset] places within its Event; moves past either end stop at the end.
 */
internal fun ActorTypeDefinition.withBlockMoved(eventIndex: Int, blockIndex: Int, offset: Int) = withEvent(eventIndex) { event ->
    if (blockIndex !in event.blocks.indices) return@withEvent event
    val targetIndex = (blockIndex + offset).coerceIn(event.blocks.indices)
    event.copy(blocks = event.blocks.toMutableList().apply { add(targetIndex, removeAt(blockIndex)) })
}

private fun ActorTypeDefinition.withEvent(eventIndex: Int, change: (EventDefinition) -> EventDefinition) = if (eventIndex in events.indices) {
    copy(events = events.mapIndexed { index, event -> if (index == eventIndex) change(event) else event })
} else {
    this
}
