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
 * Every collision Event, Block, and Kotlin code that names [oldName] names [newName] instead.
 */
internal fun BlockTypeDefinition.withOtherActorTypeRenamed(oldName: String, newName: String) = withEvents(
    events.map { event ->
        event.copy(
            otherActorType = if (event.otherActorType == oldName) newName else event.otherActorType,
            blocks = event.blocks.map { it.withActorTypeRenamed(oldName, newName) },
        )
    },
).withKotlinCode { KotlinIdentifiers.withRenamed(it, oldName, newName) }.let { renamed ->
    if (renamed is ActorTypeDefinition && renamed.parent == oldName) renamed.copy(parent = newName) else renamed
}

/**
 * Every goToScene Block that goes to the Scene [oldName] goes to [newName] instead.
 */
internal fun BlockTypeDefinition.withSceneRenamed(oldName: String, newName: String) = withBlocks { block ->
    if (block is BlockDefinition.GoToScene && block.scene == oldName) block.copy(scene = newName) else block
}

/**
 * Every Block, text, and Kotlin code that uses the Manager type [oldName] (by its class, or, in another type, by the
 * field the generated code looks it up in) uses [newName] instead.
 */
internal fun BlockTypeDefinition.withManagerRenamed(oldName: String, newName: String) = withBlocks { it.withManagerRenamed(oldName, newName) }
    .withTexts { PropertyPlaceholders.withManagerRenamed(it, oldName, newName) }
    .withKotlinCode { code ->
        KotlinIdentifiers.withRenamed(code, oldName, newName).let { renamed ->
            if (name == oldName) renamed else KotlinIdentifiers.withRenamed(renamed, scope.managerField(oldName), scope.managerField(newName))
        }
    }

/**
 * Every Block, text, and Kotlin code of another type that uses the Property [oldName] of the Manager type [manager]
 * uses [newName] instead. The Manager type's own code follows through [withPropertyReplaced].
 */
internal fun BlockTypeDefinition.withManagerPropertyRenamed(manager: String, oldName: String, newName: String) = withBlocks { it.withPropertyRenamed(oldName, newName, manager) }
    .withTexts { PropertyPlaceholders.withPropertyRenamed(it, manager, oldName, newName) }
    .withKotlinCode { if (name == manager) it else KotlinIdentifiers.withRenamed(it, oldName, newName, receiver = scope.managerField(manager)) }

internal fun BlockTypeDefinition.withPropertyInserted(index: Int, property: PropertyDefinition) = withProperties(
    properties.toMutableList().apply { add(index.coerceIn(0, size), property) },
)

internal fun BlockTypeDefinition.withPropertyRemoved(name: String) = withProperties(properties.filter { it.name != name })

/**
 * The Property called [name] replaced by [property]. If that renames it, the Blocks that change it (and Log Blocks,
 * texts, and Kotlin code that use it) follow.
 */
internal fun BlockTypeDefinition.withPropertyReplaced(name: String, property: PropertyDefinition) = withProperties(properties.map { if (it.name == name) property else it }).let { replaced ->
    if (property.name == name) {
        replaced
    } else {
        replaced.withBlocks { it.withPropertyRenamed(name, property.name) }
            .withTexts { PropertyPlaceholders.withPropertyRenamed(it, null, name, property.name) }
            .withKotlinCode { KotlinIdentifiers.withRenamed(it, name, property.name) }
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

/**
 * The code of its Kotlin Blocks and, for a Manager type, its Kotlin UI changed by [change].
 */
private fun BlockTypeDefinition.withKotlinCode(change: (String) -> String) = withBlocks { if (it is BlockDefinition.Kotlin) it.copy(code = change(it.code)) else it }.let { changed ->
    when (changed) {
        is ManagerTypeDefinition -> changed.copy(kotlinUi = changed.kotlinUi?.let(change))
        is ActorTypeDefinition -> changed
    }
}

private val BlockTypeDefinition.scope get() = BlockScope(name, kind, properties)

private fun <T> List<T>.moved(index: Int, targetIndex: Int) = toMutableList().apply { add(targetIndex.coerceIn(indices), removeAt(index)) }
