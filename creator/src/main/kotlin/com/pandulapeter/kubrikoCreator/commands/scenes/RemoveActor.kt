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
    override val description = "Removes an Actor from a Scene. The Actors after it move one index down."
    override val title = Res.string.command_remove_actor
    override val inputSerializer = Input.serializer()

    override fun validate(context: CommandContext, input: Input): Rejection? = context.validateSceneActor(input.scene, input.actorIndex)

    override suspend fun apply(context: CommandContext, input: Input): Applied {
        val actors = context.readScene(input.scene)
        val removed = actors[input.actorIndex]
        context.writeScene(input.scene, actors.filterIndexed { index, _ -> index != input.actorIndex })
        return Applied(inverse = Invocation(RestoreActor, RestoreActor.Input(input.scene, input.actorIndex, removed.typeId, removed.state)))
    }

    @Serializable
    data class Input(
        @Description("The name of the Scene: its file name without .json.")
        @SerialName("scene") val scene: String,
        @Description("The index of the Actor in the Scene's Actor list, starting from 0.")
        @SerialName("actorIndex") val actorIndex: Int,
    )
}
