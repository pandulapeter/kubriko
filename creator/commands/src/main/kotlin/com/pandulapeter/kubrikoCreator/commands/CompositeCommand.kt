/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.commands

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject

/**
 * Changes made as one step: its parts in order, undone in reverse. Kubriko Creator's own building block for changes no
 * single command makes; not a tool.
 */
internal object CompositeCommand : WriteCommand<CompositeCommand.Input> {
    override val name = "composite"
    override val description = ""
    override val inputSerializer: KSerializer<Input> = NotATool("CompositeCommand")

    override fun commandName(input: Input) = input.parts.first().commandName

    override fun target(context: CommandContext, input: Input) = input.parts.first().target(context)

    override fun targets(context: CommandContext, input: Input) = input.parts.flatMapTo(LinkedHashSet()) { it.targets(context) }

    /**
     * Checks only the first part: the later ones are checked against what the earlier ones leave, while applying.
     */
    override fun validate(context: CommandContext, input: Input) = input.parts.first().validate(context)

    override fun validateRestoring(context: CommandContext, input: Input) = input.parts.first().copy(isRestoring = true).validate(context)

    override suspend fun apply(context: CommandContext, input: Input): Applied {
        val inverses = ArrayList<Invocation<*>?>(input.parts.size)
        var isRewritingFiles = false
        var result: JsonElement = JsonNull
        val details = LinkedHashMap<String, JsonElement>()
        val broughtBack = ArrayList<ActorsBroughtBack>()
        input.parts.forEach { part ->
            part.validate(context)?.let { rejection ->
                rollBack(context, inverses)
                return Applied.failed(rejection)
            }
            val applied = try {
                part.apply(context)
            } catch (exception: Exception) {
                rollBack(context, inverses, exception)
                throw exception
            }
            applied.failure?.let { failure ->
                rollBack(context, inverses)
                return Applied.failed(failure)
            }
            inverses += applied.inverse?.copy(isRestoring = true)
            isRewritingFiles = isRewritingFiles || applied.isRewritingFiles
            result = applied.result
            applied.details?.let(details::putAll)
            applied.broughtBack?.let(broughtBack::add)
        }
        val inverse = if (null in inverses) null else inverses.filterNotNull().asReversed().let { reverted -> reverted.singleOrNull() ?: Invocation(CompositeCommand, Input(reverted)) }
        return Applied(
            inverse = inverse,
            result = result,
            isRewritingFiles = isRewritingFiles,
            details = details.takeIf { it.isNotEmpty() }?.let(::JsonObject),
            broughtBack = ActorsBroughtBack.merged(broughtBack),
        )
    }

    /**
     * Reverts the parts already made, newest first. One that can't be reverted leaves the change partial; what made it
     * throw is added to [cause], if there is one.
     */
    private suspend fun rollBack(context: CommandContext, inverses: List<Invocation<*>?>, cause: Exception? = null) {
        inverses.asReversed().forEach { inverse ->
            if (inverse == null) return@forEach
            try {
                if (inverse.validate(context) == null) inverse.apply(context)
            } catch (exception: Exception) {
                cause?.addSuppressed(exception)
            }
        }
    }

    data class Input(
        val parts: List<Invocation<*>>,
    ) {
        init {
            require(parts.isNotEmpty()) { "A composite change has at least one part." }
        }
    }
}
