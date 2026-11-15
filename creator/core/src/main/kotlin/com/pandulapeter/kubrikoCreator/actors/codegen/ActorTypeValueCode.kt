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

import com.pandulapeter.kubrikoCreator.actors.ActorTypeValue
import com.pandulapeter.kubrikoCreator.actors.BlockScope
import com.pandulapeter.kubrikoCreator.actors.BlockTypeNaming
import com.pandulapeter.kubrikoCreator.actors.codegen.BlockCodeGenerator.REMOVED_FIELD
import com.pandulapeter.kubrikoCreator.actors.codegen.BlockCodeGenerator.stringLiteral
import com.pandulapeter.kubrikoCreator.actors.propertyReference

/**
 * What the Blocks taking an [ActorTypeValue] need when it reads a Property, whose type is known only while the game
 * runs: the registry of Actor types looks the type up by its name, with a `when` over every name, which allocates
 * nothing.
 */
internal object ActorTypeValueCode {
    private const val METADATA_FUNCTION = "metadataOf"
    private const val NEAREST_FUNCTION = "nearestActiveOf"

    /**
     * The Kotlin expression of the name of the type [value] reads from its Property, or null if that Property doesn't
     * exist (renamed, deleted, or not chosen yet).
     */
    fun nameExpression(value: ActorTypeValue.Property, scope: BlockScope) =
        value.propertyReference?.takeIf { scope.hasProperty(it) }?.let { BlockCodeGenerator.propertyExpression(it, scope) }

    /**
     * The metadata of the Actor type named by [name] (a Kotlin expression), or null for a name no type has.
     */
    fun metadataCall(name: String) = "${ActorTypeCodeGenerator.REGISTRY_NAME}.$METADATA_FUNCTION($name)"

    /**
     * The nearest Actor of [actors] (a Kotlin expression) to [from] (one) other than it, of the type named by [name] (one)
     * or a type inheriting from it, that no Block has removed, or null if there is none.
     */
    fun nearestCall(actors: String, name: String, from: String) = "${ActorTypeCodeGenerator.REGISTRY_NAME}.$NEAREST_FUNCTION($actors, $name, $from)"

    /**
     * The registry's two lookups over [actorTypeNames], whose types in [baseClassNames] have children, so their Actors
     * are instances of a base class.
     */
    fun StringBuilder.appendLookups(actorTypeNames: List<String>, baseClassNames: Set<String>) {
        val names = actorTypeNames.sorted()
        appendLine()
        appendLine("    /**")
        appendLine("     * The Scene metadata of the Actor type called [name], or null if there is none, for the Blocks adding an Actor of a")
        appendLine("     * type an Actor type reference Property names.")
        appendLine("     */")
        appendLine("    fun $METADATA_FUNCTION(name: String): EditableMetadata<*>? = when (name) {")
        names.forEach { appendLine("        ${stringLiteral(it)} -> $it.metadata") }
        appendLine("        else -> null")
        appendLine("    }")
        appendLine()
        appendLine("    /**")
        appendLine("     * The nearest of [actors] to [from], other than it, of the Actor type called [name] or one inheriting from it, that no")
        appendLine("     * Block has removed, or null if there is none, for the Blocks moving towards a type an Actor type reference")
        appendLine("     * Property names.")
        appendLine("     */")
        appendLine("    fun $NEAREST_FUNCTION(actors: List<Actor>, name: String, from: $POSITIONABLE): $POSITIONABLE? {")
        // The name is matched once, rather than for every candidate.
        appendLine("        val type = typeIndexOf(name)")
        appendLine("        if (type < 0) return null")
        appendLine("        var nearest: $POSITIONABLE? = null")
        appendLine("        var nearestDistanceSquared = Float.MAX_VALUE")
        appendLine("        for (index in actors.indices) {")
        appendLine("            val candidate = actors[index]")
        appendLine("            if (candidate === from || candidate !is $POSITIONABLE || !isActiveOf(candidate, type)) continue")
        appendLine("            val offset = candidate.body.position - from.body.position")
        appendLine("            val distanceSquared = offset.x.raw * offset.x.raw + offset.y.raw * offset.y.raw")
        appendLine("            if (distanceSquared < nearestDistanceSquared) {")
        appendLine("                nearest = candidate")
        appendLine("                nearestDistanceSquared = distanceSquared")
        appendLine("            }")
        appendLine("        }")
        appendLine("        return nearest")
        appendLine("    }")
        appendLine()
        appendLine("    private fun typeIndexOf(name: String): Int = when (name) {")
        names.forEachIndexed { index, name -> appendLine("        ${stringLiteral(name)} -> $index") }
        appendLine("        else -> -1")
        appendLine("    }")
        appendLine()
        appendLine("    private fun isActiveOf(actor: Actor, type: Int): Boolean = when (type) {")
        names.forEachIndexed { index, name ->
            val actorClass = if (name in baseClassNames) name + BlockTypeNaming.BASE_SUFFIX else name
            appendLine("        $index -> actor is $actorClass && !actor.$REMOVED_FIELD")
        }
        appendLine("        else -> false")
        appendLine("    }")
    }

    /**
     * Named in full, since an Actor type may be called like it and its name isn't reserved.
     */
    private const val POSITIONABLE = "com.pandulapeter.kubriko.actor.traits.Positionable"
}
