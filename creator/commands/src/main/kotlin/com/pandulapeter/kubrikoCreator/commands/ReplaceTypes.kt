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

import com.pandulapeter.kubrikoCreator.actors.BlockTypeDefinition
import com.pandulapeter.kubrikoCreator.commands.actorTypes.requireBlockTypes
import com.pandulapeter.kubrikoCreator.commands.actorTypes.requireType
import com.pandulapeter.kubrikoCreator.commands.actorTypes.validateType
import com.pandulapeter.kubrikoCreator.foundation.EditorMutation
import kotlinx.serialization.KSerializer

/**
 * Puts several existing types back as [Input.definitions] has them, as one step: what deleting something does to the
 * types using it, and what undoes that. Kubriko Creator's own building block; not a tool.
 */
@OptIn(EditorMutation::class)
internal object ReplaceTypes : WriteCommand<ReplaceTypes.Input> {
    override val name = "replace_types"
    override val description = ""
    override val inputSerializer: KSerializer<Input> = NotATool("ReplaceTypes")

    override fun target(context: CommandContext, input: Input) = CommandTarget.BlockType(input.definitions.first().name)

    override fun targets(context: CommandContext, input: Input) = input.definitions.mapTo(LinkedHashSet()) { CommandTarget.BlockType(it.name) }

    override fun validate(context: CommandContext, input: Input) = input.definitions.firstNotNullOfOrNull { context.validateType(it.name) }

    override suspend fun apply(context: CommandContext, input: Input): Applied {
        val previous = input.definitions.map { context.requireType(it.name) }
        input.definitions.forEach { context.requireBlockTypes.put(it) }
        return Applied(inverse = Invocation(ReplaceTypes, Input(previous)))
    }

    data class Input(
        val definitions: List<BlockTypeDefinition>,
    ) {
        init {
            require(definitions.isNotEmpty()) { "Replacing types needs at least one." }
        }
    }
}
