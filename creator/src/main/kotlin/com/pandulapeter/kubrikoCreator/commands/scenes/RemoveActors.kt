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
import kubriko.creator.generated.resources.command_remove_actors

internal object RemoveActors : WriteCommand<RemoveActors.Input> {
    override val name = "remove_actors"
    override val description = "Removes many Actors from a Scene at once, as one step to undo."
    override val title = Res.string.command_remove_actors
    override val inputSerializer = Input.serializer()

    override fun target(context: CommandContext, input: Input) = CommandTarget.Scene(input.scene)

    override fun validate(context: CommandContext, input: Input): Rejection? = context.validateSceneActors(input.scene, input.actorIds)

    override suspend fun apply(context: CommandContext, input: Input): Applied {
        val actors = context.readScene(input.scene)
        val ids = input.actorIds.toHashSet()
        val removed = actors.withIndex().filter { it.value.id in ids }
        context.writeScene(input.scene, actors.filter { it.id !in ids })
        return Applied(
            inverse = Invocation(
                RestoreActors,
                RestoreActors.Input(input.scene, removed.map { (index, actor) -> RestoreActors.Restored(index, actor.typeId, actor.state, actor.id) }),
            ),
        )
    }

    @Serializable
    data class Input(
        @Description("The name of the Scene: its path in the Scenes folder without .json, like level-2 or levels/1-1.")
        @SerialName("scene") val scene: String,
        @Description("The IDs of the Actors in the Scene, as read_scene returns them.")
        @SerialName("actorIds") val actorIds: List<String>,
    )
}
