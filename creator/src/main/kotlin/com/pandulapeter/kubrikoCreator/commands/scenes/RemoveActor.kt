/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.commands.scenes

import com.pandulapeter.kubrikoCreator.commands.Applied
import com.pandulapeter.kubrikoCreator.commands.CommandContext
import com.pandulapeter.kubrikoCreator.commands.CommandTarget
import com.pandulapeter.kubrikoCreator.commands.Description
import com.pandulapeter.kubrikoCreator.commands.Invocation
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.commands.WriteCommand
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.command_remove_actor

internal object RemoveActor : WriteCommand<RemoveActor.Input> {
    override val name = "remove_actor"
    override val description = "Removes an Actor from a Scene."
    override val title = Res.string.command_remove_actor
    override val inputSerializer = Input.serializer()

    override fun target(context: CommandContext, input: Input) = CommandTarget.Scene(input.scene)

    override fun validate(context: CommandContext, input: Input): Rejection? = context.validateSceneActor(input.scene, input.actorId)

    override suspend fun apply(context: CommandContext, input: Input): Applied {
        val actors = context.readScene(input.scene)
        val index = actors.indexOfFirst { it.id == input.actorId }
        val removed = actors[index]
        context.writeScene(input.scene, actors.filterIndexed { actorIndex, _ -> actorIndex != index })
        return Applied(inverse = Invocation(RestoreActor, RestoreActor.Input(input.scene, index, removed.typeId, removed.state, removed.id)))
    }

    @Serializable
    data class Input(
        @Description("The name of the Scene: its path in the Scenes folder without .json, like level-2 or levels/1-1.")
        @SerialName("scene") val scene: String,
        @Description("The ID of the Actor in the Scene, as read_scene returns it.")
        @SerialName("actorId") val actorId: String,
    )
}
