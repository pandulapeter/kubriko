/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.context

import com.pandulapeter.kubrikoCreator.actors.ActorTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.ActorTypeFamily
import com.pandulapeter.kubrikoCreator.actors.BlockDefinition
import com.pandulapeter.kubrikoCreator.actors.BlockTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.EventType
import com.pandulapeter.kubrikoCreator.actors.KotlinIdentifiers

/**
 * Finds the Kotlin Blocks that worked differently before the code of each kind of Event got a function of its own
 * (`EventFunctions`), in the Actor types without a parent or children and the Manager types, whose Events used to be
 * written straight into `update`, `onKeyPressed`, and the others (a family's always had functions):
 * - a `return` in an every-frame, edge-touched, collision, or Timer Event used to skip the rest of the frame or the
 *   remaining collisions, and now ends only the Event's function;
 * - the locals of those functions (`key`, `index`, `collidables`, and the edge handling's) are no longer in scope.
 *
 * It knows only what [KotlinIdentifiers] does: a `return` inside a function the Block declares, or with a label, and a
 * name the Block declares itself with `val` or `var`, are left alone.
 */
public object KotlinBlockScopeChecks {

    public fun of(types: List<BlockTypeDefinition>, family: ActorTypeFamily = ActorTypeFamily(types.filterIsInstance<ActorTypeDefinition>())): List<HealthFinding> = buildList {
        types.filterNot { it is ActorTypeDefinition && (family.parentOf(it.name) != null || family.hasChildren(it.name)) }.forEach { type ->
            type.events.forEachIndexed { eventIndex, event ->
                event.blocks.forEachIndexed { blockIndex, block ->
                    if (block is BlockDefinition.Kotlin) {
                        val place = RelationPlace.Type(type.name, eventIndex, blockIndex).toString()
                        usagesOf(block.code, event.type).forEach { usage ->
                            add(HealthFinding(HealthFinding.Kind.KOTLIN_BLOCK_SCOPE_CHANGED, RelationSubject.Type(type.name), place, usage))
                        }
                    }
                }
            }
        }
    }

    /**
     * `return` and the removed locals [code] uses in an Event of [eventType], each once, in that order.
     */
    private fun usagesOf(code: String, eventType: EventType): List<String> {
        val occurrences = KotlinIdentifiers.find(code)
        val declared = occurrences.zipWithNext().filter { (keyword, name) -> keyword.name in DECLARATIONS && keyword.receiver == null && name.receiver == null }.mapTo(HashSet()) { it.second.name }
        val removed = removedLocalsOf(eventType) - declared
        val usesRemoved = occurrences.filter { it.receiver == null && it.name in removed }.map { it.name }.distinct()
        val returns = eventType in RETURNING_EVENTS && returnsFromTheEvent(code, occurrences)
        return listOfNotNull(RETURN.takeIf { returns }) + usesRemoved
    }

    /**
     * Whether a `return` without a label ends the Event: one that isn't inside a function the code declares, whose
     * body is the part after `fun` that is deeper than it, until the code is back at its depth.
     */
    private fun returnsFromTheEvent(code: String, occurrences: List<KotlinIdentifiers.Occurrence>): Boolean {
        val functionDepths = ArrayDeque<Pair<Int, Boolean>>()
        occurrences.forEach { occurrence ->
            while (functionDepths.isNotEmpty()) {
                val (depth, isInBody) = functionDepths.last()
                when {
                    occurrence.depth > depth -> {
                        if (!isInBody) functionDepths[functionDepths.lastIndex] = depth to true
                        break
                    }
                    isInBody || occurrence.depth < depth -> functionDepths.removeLast()
                    else -> break
                }
            }
            when {
                occurrence.receiver != null -> Unit
                occurrence.name == "fun" -> functionDepths.addLast(occurrence.depth to false)
                occurrence.name == RETURN && functionDepths.isEmpty() && code.getOrNull(occurrence.range.last + 1) != '@' -> return true
            }
        }
        return false
    }

    private fun removedLocalsOf(eventType: EventType) = when (eventType) {
        EventType.KEY_PRESSED -> setOf("key")
        EventType.COLLISION -> setOf("index", "collidables")
        EventType.EDGE_TOUCHED -> setOf("box", "sceneSettings", "topLeft", "bottomRight")
        EventType.GAME_STARTED, EventType.ADDED_TO_SCENE, EventType.EVERY_FRAME, EventType.TIMER_FINISHED -> emptySet()
    }

    public const val RETURN: String = "return"
    private val DECLARATIONS = setOf("val", "var")

    /**
     * The Events whose `return` used to skip more than the rest of the Events of their kind: the rest of the frame, or
     * the remaining collisions.
     */
    private val RETURNING_EVENTS = setOf(EventType.EVERY_FRAME, EventType.EDGE_TOUCHED, EventType.COLLISION, EventType.TIMER_FINISHED)
}
