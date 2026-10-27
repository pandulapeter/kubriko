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

import com.pandulapeter.kubrikoCreator.actors.ActorTypeNaming
import com.pandulapeter.kubrikoCreator.commands.Applied
import com.pandulapeter.kubrikoCreator.commands.CommandContext
import com.pandulapeter.kubrikoCreator.commands.CommandTarget
import com.pandulapeter.kubrikoCreator.commands.Description
import com.pandulapeter.kubrikoCreator.commands.EditorMutation
import com.pandulapeter.kubrikoCreator.commands.Invocation
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.commands.WriteCommand
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.command_rename_actor_type

@OptIn(EditorMutation::class)
internal object RenameActorType : WriteCommand<RenameActorType.Input> {
    override val name = "rename_actor_type"
    override val description = "Renames a block-built Actor type, its generated Kotlin class, and every reference to it in collision Events and Scenes."
    override val title = Res.string.command_rename_actor_type
    override val inputSerializer = Input.serializer()

    override fun target(context: CommandContext, input: Input) = CommandTarget.ActorType(if (context.blockTypes?.get(input.newName) != null) input.newName else input.actorType)

    override fun validate(context: CommandContext, input: Input): Rejection? = context.validateActorType(input.actorType)
        ?: ActorTypeNaming.validate(input.newName, context.requireActorTypes.actorTypes.value.map { it.name })?.let(::Rejection)

    override suspend fun apply(context: CommandContext, input: Input): Applied {
        context.requireActorTypes.rename(input.actorType, input.newName)
        val changedScenes = withContext(context.ioDispatcher) { context.scenes.renameTypeId(input.actorType, input.newName) }
        if (changedScenes.isNotEmpty()) context.onScenesChanged(changedScenes)
        return Applied(inverse = Invocation(RenameActorType, Input(actorType = input.newName, newName = input.actorType)))
    }

    @Serializable
    data class Input(
        @Description("The current name of the Actor type.")
        @SerialName("actorType") val actorType: String,
        @Description("The new name: an uppercase letter, then letters and numbers.")
        @SerialName("newName") val newName: String,
    )
}
