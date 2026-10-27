/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.commands.actorTypes

import com.pandulapeter.kubrikoCreator.actors.withPropertyRemoved
import com.pandulapeter.kubrikoCreator.commands.Applied
import com.pandulapeter.kubrikoCreator.commands.CommandContext
import com.pandulapeter.kubrikoCreator.commands.CommandTarget
import com.pandulapeter.kubrikoCreator.commands.Description
import com.pandulapeter.kubrikoCreator.commands.EditorMutation
import com.pandulapeter.kubrikoCreator.commands.Invocation
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.commands.WriteCommand
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.command_remove_property

/**
 * Removes a Property but keeps its values in the Scenes, which loading ignores, so that undoing this brings them back.
 */
@OptIn(EditorMutation::class)
internal object RemoveProperty : WriteCommand<RemoveProperty.Input> {
    override val name = "remove_property"
    override val description = "Removes a Property from a block-built Actor type. Blocks that change it stay, but do nothing until it is added again."
    override val title = Res.string.command_remove_property
    override val inputSerializer = Input.serializer()

    override fun target(context: CommandContext, input: Input) = CommandTarget.ActorType(input.actorType)

    override fun validate(context: CommandContext, input: Input): Rejection? = context.validateProperty(input.actorType, input.property)

    override suspend fun apply(context: CommandContext, input: Input): Applied {
        val definition = context.requireActorType(input.actorType)
        val index = definition.properties.indexOfFirst { it.name == input.property }
        context.requireActorTypes.put(definition.withPropertyRemoved(input.property))
        return Applied(inverse = Invocation(AddProperty, AddProperty.Input(input.actorType, definition.properties[index], index)))
    }

    @Serializable
    data class Input(
        @Description("The name of the Actor type.")
        @SerialName("actorType") val actorType: String,
        @Description("The name of the Property.")
        @SerialName("property") val property: String,
    )
}
