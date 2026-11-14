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

import com.pandulapeter.kubrikoCreator.actors.BlockDefinition
import com.pandulapeter.kubrikoCreator.actors.BlockScope
import com.pandulapeter.kubrikoCreator.actors.BlockTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.ConditionDefinition
import com.pandulapeter.kubrikoCreator.actors.codegen.BlockCodeGenerator.SCENES_FIELD

/**
 * The counts of Actors the actorCount condition reads, so that a count costs the same however many Actors there are:
 * `GeneratedScenes` keeps one per counted Actor type, and the Actors of that type (and of those inheriting from it) add
 * themselves once they are added and take themselves away once a Block marks them removed (`isRemoved`) or Kubriko
 * removes them, whichever comes first. The two run on different threads, so a guard per Actor lets only one of them
 * take it away.
 */
internal object ActorCountCode {

    private const val COUNTS_FIELD = "actorCounts"
    private const val COUNT_FUNCTION = "actorCount"
    private const val CHANGE_FUNCTION = "changeActorCount"
    private const val GUARD_CLASS = "ActorCountGuard"
    const val GUARD_FIELD = "actorCountGuard"
    private const val STOP_FUNCTION = "stopCounting"
    private const val ATOMICS = "kotlin.concurrent.atomics"

    /**
     * The atomics are named in full rather than imported, so an Actor type may be called `AtomicBoolean`.
     */
    const val OPT_IN = "@OptIn($ATOMICS.ExperimentalAtomicApi::class)"

    /**
     * The Actor types the actorCount conditions whose code is generated count, among [definitions], sorted.
     */
    fun countedTypesOf(definitions: List<BlockTypeDefinition>, actorTypeNames: Collection<String>) = definitions.flatMap { definition ->
        definition.events.filter { GeneratedCode.isGenerated(it, definition.kind, actorTypeNames) }.flatMap { event ->
            event.blocks.mapNotNull { block -> ((block as? BlockDefinition.If)?.condition as? ConditionDefinition.ActorCount)?.actorType?.takeIf { it in actorTypeNames } }
        }
    }.distinct().sorted()

    /**
     * The constant of `GeneratedScenes` naming the count of [actorType]: `ACTORS_BIG_BRICK` for `BigBrick`. Names that differ
     * in more than case, which is all the types of a project, never make the same one.
     */
    fun constantOf(actorType: String) = "ACTORS_" + buildString {
        actorType.forEachIndexed { index, character ->
            if (index > 0 && character.isUpperCase() && !actorType[index - 1].isUpperCase()) append('_')
            append(character.uppercaseChar())
        }
    }

    /**
     * How a condition reads the count of [actorType].
     */
    fun countOf(actorType: String) = "$SCENES_FIELD.$COUNT_FUNCTION(${ScenesCodeGenerator.SCENES_NAME}.${constantOf(actorType)})"

    /**
     * What `GeneratedScenes` keeps the counts of [countedTypes] in, and how the Actors change them.
     */
    fun StringBuilder.appendScenesMembers(countedTypes: List<String>) {
        appendLine()
        appendLine("    private val $COUNTS_FIELD = $ATOMICS.AtomicIntArray(${countedTypes.size})")
        appendLine()
        appendLine("    /**")
        appendLine("     * How many Actors of a counted Actor type ([${constantOf(countedTypes.first())}], …) are in the Scene, those of the types inheriting")
        appendLine("     * from it included, leaving out those a Block removed.")
        appendLine("     */")
        appendLine("    fun $COUNT_FUNCTION(type: Int): Int = $COUNTS_FIELD.loadAt(type)")
        appendLine()
        appendLine("    /**")
        appendLine("     * Counts an Actor among those of [type] ([by] 1), or no longer (-1), which its [$GUARD_CLASS] allows once each way.")
        appendLine("     */")
        appendLine("    fun $CHANGE_FUNCTION(type: Int, by: Int) {")
        appendLine("        $COUNTS_FIELD.fetchAndAddAt(type, by)")
        appendLine("    }")
        appendLine()
        appendLine("    /**")
        appendLine("     * Whether an Actor is counted: from when it is added until a Block or Kubriko removes it, whichever is first, so the")
        appendLine("     * threads they run on never both take it away.")
        appendLine("     */")
        appendLine("    class $GUARD_CLASS {")
        appendLine("        private val isCounted = $ATOMICS.AtomicBoolean(false)")
        appendLine()
        appendLine("        fun start() = isCounted.compareAndSet(false, true)")
        appendLine()
        appendLine("        fun stop() = isCounted.compareAndSet(true, false)")
        appendLine("    }")
    }

    /**
     * The constants naming the counts, for `GeneratedScenes`' companion.
     */
    fun constantsOf(countedTypes: List<String>) = countedTypes.mapIndexed { index, type -> "        const val ${constantOf(type)} = $index" }

    /**
     * The guard of an Actor counted among [countedTypes].
     */
    fun StringBuilder.appendGuardField() {
        appendLine()
        appendLine("    private val $GUARD_FIELD = ${ScenesCodeGenerator.SCENES_NAME}.$GUARD_CLASS()")
    }

    /**
     * The field marking the Actor removed, which stops counting it at once rather than once Kubriko removes it.
     */
    fun StringBuilder.appendRemovedFieldSetter() {
        appendLine("        set(value) {")
        appendLine("            field = value")
        appendLine("            if (value) $STOP_FUNCTION()")
        appendLine("        }")
    }

    /**
     * The lines of `onAdded` that start counting the Actor among each of [countedTypes] it belongs to: each only for the
     * Actors of its Actor type (and its descendants) in a family's base class, named [name].
     */
    fun StringBuilder.appendStart(name: String, countedTypes: List<String>, scope: BlockScope) {
        appendLine("        if ($GUARD_FIELD.start()) {")
        appendChanges(name, countedTypes, scope, by = 1)
        appendLine("        }")
    }

    /**
     * The line of `onRemoved` that stops counting the Actor, if a Block didn't already.
     */
    const val STOP_CALL = "$STOP_FUNCTION()"

    fun StringBuilder.appendStopFunction(name: String, countedTypes: List<String>, scope: BlockScope) {
        appendLine()
        appendLine("    private fun $STOP_FUNCTION() {")
        appendLine("        if ($GUARD_FIELD.stop()) {")
        appendChanges(name, countedTypes, scope, by = -1)
        appendLine("        }")
        appendLine("    }")
    }

    private fun StringBuilder.appendChanges(name: String, countedTypes: List<String>, scope: BlockScope, by: Int) {
        countedTypes.forEach { type ->
            val call = "$SCENES_FIELD.$CHANGE_FUNCTION(${ScenesCodeGenerator.SCENES_NAME}.${constantOf(type)}, $by)"
            appendLine(if (type == name) "            $call" else "            if (this is ${scope.classOf(type)}) $call")
        }
    }
}
