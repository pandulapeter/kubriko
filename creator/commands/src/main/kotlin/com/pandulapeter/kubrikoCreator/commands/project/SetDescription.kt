/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.commands.project

import com.pandulapeter.kubrikoCreator.commands.Applied
import com.pandulapeter.kubrikoCreator.commands.CommandContext
import com.pandulapeter.kubrikoCreator.commands.CommandProblem
import com.pandulapeter.kubrikoCreator.commands.CommandTarget
import com.pandulapeter.kubrikoCreator.commands.Descriptions
import com.pandulapeter.kubrikoCreator.commands.Invocation
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.commands.WriteCommand
import com.pandulapeter.kubrikoCreator.commands.actorTypes.requireBlockTypes
import com.pandulapeter.kubrikoCreator.commands.actorTypes.validateType
import com.pandulapeter.kubrikoCreator.foundation.EditorMutation
import com.pandulapeter.kubrikoCreator.schema.Description
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@OptIn(EditorMutation::class)
public object SetDescription : WriteCommand<SetDescription.Input> {
    override val name: String = "set_description"
    override val description: String = "Says in one short line what an Actor type, a Manager type, or a Scene is for in the game (\"the ball the player " +
        "bounces off the paddle\", \"the first level\"), or takes the description away. The project digest shows it, so describe what " +
        "you create, and what you come to understand, for the next conversation."
    override val inputSerializer: KSerializer<Input> = Input.serializer()

    override fun target(context: CommandContext, input: Input): CommandTarget? = input.type?.let { CommandTarget.BlockType(it) } ?: input.scene?.let { CommandTarget.Scene(it) }

    override fun validate(context: CommandContext, input: Input): Rejection? = when {
        (input.type == null) == (input.scene == null) -> Rejection(CommandProblem.RELATIONS_SUBJECT)
        input.description.length > MAXIMUM_LENGTH || '\n' in input.description -> Rejection(CommandProblem.DESCRIPTION_INVALID, MAXIMUM_LENGTH.toString())
        input.type != null -> context.validateType(input.type)
        !context.scenes.exists(input.scene!!) -> Rejection(CommandProblem.SCENE_MISSING, input.scene)
        else -> null
    }

    override suspend fun apply(context: CommandContext, input: Input): Applied {
        val description = input.description.ifBlank { null }
        val previous = if (input.type != null) {
            val definition = checkNotNull(context.requireBlockTypes[input.type])
            context.requireBlockTypes.put(definition.withDescription(description))
            definition.description
        } else {
            withContext(context.ioDispatcher) {
                context.scenes.descriptions.of(input.scene!!).also { context.scenes.descriptions.set(input.scene, description) }
            }
        }
        return Applied(inverse = Invocation(SetDescription, input.copy(description = previous.orEmpty())))
    }

    @Serializable
    public data class Input(
        @Description(Descriptions.TYPE_NAME)
        @SerialName("type") val type: String? = null,
        @Description("The name of the Scene, instead of a type.")
        @SerialName("scene") val scene: String? = null,
        @Description("The description: one line, empty to take it away.")
        @SerialName("description") val description: String,
    )

    /**
     * How long a description may be.
     */
    public const val MAXIMUM_LENGTH: Int = 200
}
