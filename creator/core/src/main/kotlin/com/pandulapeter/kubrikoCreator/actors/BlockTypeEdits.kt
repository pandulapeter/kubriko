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
 * [event] inserted at [eventIndex], or at the nearest end when [eventIndex] is out of range.
 */
public fun BlockTypeDefinition.withEventInserted(eventIndex: Int, event: EventDefinition): BlockTypeDefinition = withEvents(
    events.toMutableList().apply { add(eventIndex.coerceIn(0, size), event) },
)

/**
 * The Event at [eventIndex] removed, or the type as it is when there is none.
 */
public fun BlockTypeDefinition.withEventRemoved(eventIndex: Int): BlockTypeDefinition = if (eventIndex in events.indices) {
    withEvents(events.filterIndexed { index, _ -> index != eventIndex })
} else {
    this
}

/**
 * The Event at [eventIndex] moved to [targetIndex].
 */
public fun BlockTypeDefinition.withEventMoved(eventIndex: Int, targetIndex: Int): BlockTypeDefinition = if (eventIndex in events.indices) {
    withEvents(events.moved(eventIndex, targetIndex))
} else {
    this
}

/**
 * The Event at [eventIndex] reacting to [key].
 */
public fun BlockTypeDefinition.withEventKey(eventIndex: Int, key: KeyChoice): BlockTypeDefinition = withEvent(eventIndex) { it.copy(key = key) }

/**
 * The Event at [eventIndex] reacting to the Timer [timer].
 */
public fun BlockTypeDefinition.withEventTimer(eventIndex: Int, timer: String): BlockTypeDefinition = withEvent(eventIndex) { it.copy(timer = timer) }

/**
 * The Timers its Events react to and its Blocks start or stop.
 */
public val BlockTypeDefinition.timerNames: List<String>
    get() = events.flatMap { event -> listOfNotNull(event.timer) + event.blocks.mapNotNull { it.timerName } }.distinct()

/**
 * Every timerFinished Event and Timer Block that names the Timer [oldName] names [newName] instead.
 */
public fun BlockTypeDefinition.withTimerRenamed(oldName: String, newName: String): BlockTypeDefinition = withEvents(
    events.map { event ->
        event.copy(
            timer = if (event.timer == oldName) newName else event.timer,
            blocks = event.blocks.map { it.withTimerRenamed(oldName, newName) },
        )
    },
)

/**
 * The Event at [eventIndex] reacting to colliding with [actorTypeName].
 */
public fun BlockTypeDefinition.withEventOtherActorType(eventIndex: Int, actorTypeName: String?): BlockTypeDefinition = withEvent(eventIndex) { it.copy(otherActorType = actorTypeName) }

/**
 * [block] inserted at [blockIndex] of the Event at [eventIndex], or at the nearest end when [blockIndex] is out of range.
 */
public fun BlockTypeDefinition.withBlockInserted(eventIndex: Int, blockIndex: Int, block: BlockDefinition): BlockTypeDefinition = withEvent(eventIndex) { event ->
    event.copy(blocks = event.blocks.toMutableList().apply { add(blockIndex.coerceIn(0, size), block) })
}

/**
 * The Block at [blockIndex] of the Event at [eventIndex] replaced by [block].
 */
public fun BlockTypeDefinition.withBlockReplaced(eventIndex: Int, blockIndex: Int, block: BlockDefinition): BlockTypeDefinition = withEvent(eventIndex) { event ->
    if (blockIndex in event.blocks.indices) event.copy(blocks = event.blocks.mapIndexed { index, it -> if (index == blockIndex) block else it }) else event
}

/**
 * The Block at [blockIndex] of the Event at [eventIndex] removed.
 */
public fun BlockTypeDefinition.withBlockRemoved(eventIndex: Int, blockIndex: Int): BlockTypeDefinition = withEvent(eventIndex) { event ->
    event.copy(blocks = event.blocks.filterIndexed { index, _ -> index != blockIndex })
}

/**
 * The [count] Blocks from [blockIndex] of the Event at [eventIndex] replaced by [blocks], or the Event as it was when that
 * range isn't in it.
 */
public fun BlockTypeDefinition.withBlocksReplaced(eventIndex: Int, blockIndex: Int, count: Int, blocks: List<BlockDefinition>): BlockTypeDefinition =
    withEvent(eventIndex) { event ->
        if (blockIndex < 0 || count < 0 || blockIndex + count > event.blocks.size) {
            event
        } else {
            event.copy(blocks = event.blocks.subList(0, blockIndex) + blocks + event.blocks.subList(blockIndex + count, event.blocks.size))
        }
    }

/**
 * The Block at [blockIndex] of the Event at [eventIndex] moved to [targetIndex].
 */
public fun BlockTypeDefinition.withBlockMoved(eventIndex: Int, blockIndex: Int, targetIndex: Int): BlockTypeDefinition = withEvent(eventIndex) { event ->
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
 * Every goToScene Block that goes to a Scene of [renamed] (each old name with its new one) goes to its new name instead.
 */
public fun BlockTypeDefinition.withScenesRenamed(renamed: Map<String, String>): BlockTypeDefinition = withBlocks { block ->
    if (block is BlockDefinition.GoToScene && block.scene in renamed) block.copy(scene = renamed.getValue(block.scene)) else block
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
public fun BlockTypeDefinition.withManagerPropertyRenamed(manager: String, oldName: String, newName: String): BlockTypeDefinition = withBlocks { it.withPropertyRenamed(oldName, newName, manager) }
    .withTexts { PropertyPlaceholders.withPropertyRenamed(it, manager, oldName, newName) }
    .withKotlinCode { if (name == manager) it else KotlinIdentifiers.withRenamed(it, oldName, newName, receiver = scope.managerField(manager)) }

/**
 * [property] inserted at [index], or at the nearest end when [index] is out of range.
 */
public fun BlockTypeDefinition.withPropertyInserted(index: Int, property: PropertyDefinition): BlockTypeDefinition = withProperties(
    properties.toMutableList().apply { add(index.coerceIn(0, size), property) },
)

/**
 * The Property called [name] removed, or the type as it is when it has none.
 */
public fun BlockTypeDefinition.withPropertyRemoved(name: String): BlockTypeDefinition = withProperties(properties.filter { it.name != name })

/**
 * The Property called [name] replaced by [property]. If that renames it, the Blocks that change it (and Log Blocks,
 * texts, and Kotlin code that use it) follow.
 */
public fun BlockTypeDefinition.withPropertyReplaced(name: String, property: PropertyDefinition): BlockTypeDefinition = withProperties(properties.map { if (it.name == name) property else it }).let { replaced ->
    if (property.name == name) replaced else replaced.withPropertyUsesRenamed(name, property.name)
}

/**
 * The Blocks that change the type's own or inherited Property [oldName] (and Log Blocks, texts, and Kotlin code that use
 * it) use [newName] instead.
 */
public fun BlockTypeDefinition.withPropertyUsesRenamed(oldName: String, newName: String): BlockTypeDefinition = withBlocks { it.withPropertyRenamed(oldName, newName) }
    .withTexts { PropertyPlaceholders.withPropertyRenamed(it, null, oldName, newName) }
    .withKotlinCode { KotlinIdentifiers.withRenamed(it, oldName, newName) }

/**
 * [text] inserted at [index], or at the nearest end when [index] is out of range.
 */
public fun ManagerTypeDefinition.withTextInserted(index: Int, text: ScreenTextDefinition): ManagerTypeDefinition = copy(
    texts = texts.toMutableList().apply { add(index.coerceIn(0, size), text) },
)

/**
 * The text at [index] replaced by [text].
 */
public fun ManagerTypeDefinition.withTextReplaced(index: Int, text: ScreenTextDefinition): ManagerTypeDefinition = copy(
    texts = texts.mapIndexed { textIndex, it -> if (textIndex == index) text else it },
)

/**
 * The text at [index] removed.
 */
public fun ManagerTypeDefinition.withTextRemoved(index: Int): ManagerTypeDefinition = copy(texts = texts.filterIndexed { textIndex, _ -> textIndex != index })

private fun BlockTypeDefinition.withEvent(eventIndex: Int, change: (EventDefinition) -> EventDefinition) = if (eventIndex in events.indices) {
    withEvents(events.mapIndexed { index, event -> if (index == eventIndex) change(event) else event })
} else {
    this
}

private fun BlockTypeDefinition.withBlocks(change: (BlockDefinition) -> BlockDefinition) = withEvents(
    events.map { event -> event.copy(blocks = event.blocks.map(change)) },
)

/**
 * Kotlin code that named another Manager type by the field generated code looked it up in before format version 1 (`score`
 * for `Score`, unless one of the type's Properties, inherited ones included, had that name) names it by the one it has
 * since (`scoreManager`, [BlockScope.managerField]).
 */
internal fun BlockTypeDefinition.withManagerFieldsSuffixed(types: List<BlockTypeDefinition>): BlockTypeDefinition {
    val propertyNames = BlockScope.of(this, types).properties.mapTo(HashSet()) { it.name }
    return types.filterIsInstance<ManagerTypeDefinition>().filter { it.name != name }.fold(this) { definition, manager ->
        val oldField = manager.name.replaceFirstChar { it.lowercase() }
        if (oldField in propertyNames) definition else definition.withKotlinCode { KotlinIdentifiers.withRenamed(it, oldField, scope.managerField(manager.name)) }
    }
}

/**
 * A Manager type's texts changed by [change]; other types have none.
 */
private fun BlockTypeDefinition.withTexts(change: (String) -> String) = when (this) {
    is ManagerTypeDefinition -> copy(texts = texts.map { it.copy(text = change(it.text)) })
    is ActorTypeDefinition -> this
}

/**
 * The code of its Kotlin Blocks and, for a Manager type, its Kotlin UI changed by [change], except the import lines it
 * starts with.
 */
private fun BlockTypeDefinition.withKotlinCode(change: (String) -> String): BlockTypeDefinition {
    val statementsChange = { code: String -> withStatementsChanged(code, change) }
    return withBlocks { if (it is BlockDefinition.Kotlin) it.copy(code = statementsChange(it.code)) else it }.let { changed ->
        when (changed) {
            is ManagerTypeDefinition -> changed.copy(kotlinUi = changed.kotlinUi?.let(statementsChange))
            is ActorTypeDefinition -> changed
        }
    }
}

/**
 * [code] with [change] applied to what follows the import lines it starts with, which are kept exactly as they are, so a
 * rename never touches a package name.
 */
private fun withStatementsChanged(code: String, change: (String) -> String): String {
    val lines = code.lines()
    val headerSize = KotlinUi.headerLineCount(code)
    if (headerSize == 0) return change(code)
    if (headerSize == lines.size) return code
    return lines.take(headerSize).joinToString("\n") + "\n" + change(lines.drop(headerSize).joinToString("\n"))
}

private val BlockTypeDefinition.scope get() = BlockScope(name, kind, properties)

private fun <T> List<T>.moved(index: Int, targetIndex: Int) = toMutableList().apply { add(targetIndex.coerceIn(indices), removeAt(index)) }
