/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.actors.codegen

/**
 * The code of the actorAdded and actorRemoved Events ([ActorWatch]): in the Manager type, a counter per watched Actor
 * type that its Actors raise and its next frame runs the Events for, and in the Actor types, the calls that raise them.
 * The counters are atomic, since Kubriko calls `onAdded` and `onRemoved` on a thread of its own while the game ticks on
 * another. Their classes are named in full rather than imported, so an Actor type may be called `AtomicIntArray`, which is
 * why `kotlin` is a reserved Property name of Manager types.
 */
internal object ActorWatchCode {

    const val ADDED_FIELD = "actorsAdded"
    const val REMOVED_FIELD = "actorsRemoved"
    private const val ATOMICS = "kotlin.concurrent.atomics"
    const val OPT_IN = "@OptIn($ATOMICS.ExperimentalAtomicApi::class)"

    /**
     * The Manager's counters, one per watched Actor type, of the Actors added and removed since its last frame.
     */
    fun StringBuilder.appendCounterFields(watches: List<ActorWatch>) {
        val added = watches.filter { it.isAdded }
        val removed = watches.filterNot { it.isAdded }
        appendLine()
        appendLine("    /**")
        appendLine("     * How many Actors were ${listOfNotNull("added".takeIf { added.isNotEmpty() }, "removed by a Block".takeIf { removed.isNotEmpty() }).joinToString(", and ")} since its last frame, for each Actor type")
        appendLine("     * its Events react to, counted on the thread Kubriko adds and removes them on.")
        appendLine("     */")
        if (added.isNotEmpty()) appendLine("    private val $ADDED_FIELD = $ATOMICS.AtomicIntArray(${added.size})")
        if (removed.isNotEmpty()) appendLine("    private val $REMOVED_FIELD = $ATOMICS.AtomicIntArray(${removed.size})")
    }

    /**
     * The lines of the Manager's `onUpdate` running the Events once per Actor counted since its last frame, through
     * [callOf], which names the function of an Event.
     */
    fun StringBuilder.appendEventCalls(watches: List<ActorWatch>, callOf: (ActorWatch) -> String?) {
        watches.forEach { watch ->
            val call = callOf(watch) ?: return@forEach
            appendLine("        repeat(${fieldOf(watch)}.exchangeAt(${indexOf(watch, watches)}, 0)) { $call }")
        }
    }

    /**
     * The functions the Actors call from `onAdded` and `onRemoved`, which do nothing in the Scene editor, where the
     * Manager's Events don't run.
     */
    fun StringBuilder.appendNotifyFunctions(watches: List<ActorWatch>) {
        watches.forEach { watch ->
            appendLine()
            appendLine("    /**")
            appendLine("     * Called by ${watch.actorType} Actors ${if (watch.isAdded) "once they are added to the Scene" else "once a Block removed them"}, so that the Event runs on the next frame.")
            appendLine("     */")
            appendLine("    fun ${watch.notifyFunction}() {")
            appendLine("        if (!isInSceneEditor) ${fieldOf(watch)}.fetchAndAddAt(${indexOf(watch, watches)}, 1)")
            appendLine("    }")
        }
    }

    private fun fieldOf(watch: ActorWatch) = if (watch.isAdded) ADDED_FIELD else REMOVED_FIELD

    private fun indexOf(watch: ActorWatch, watches: List<ActorWatch>) = watches.filter { it.isAdded == watch.isAdded }.indexOf(watch)
}
