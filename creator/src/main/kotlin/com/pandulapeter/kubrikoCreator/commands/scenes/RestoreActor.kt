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
import com.pandulapeter.kubrikoCreator.scenes.SceneActorEntry
import com.pandulapeter.kubrikoCreator.scenes.SceneDocument
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.command_restore_actor
import kubriko.creator.generated.resources.error_command_index_invalid
import kubriko.creator.generated.resources.error_command_scene_content_invalid

internal object RestoreActor : WriteCommand<RestoreActor.Input> {
    override val name = "restore_actor"
    override val description = "Puts an Actor into a Scene with its whole saved state, as read_scene returns it: how a removed Actor is brought back, " +
        "and how Actors of hand-written Actor types are copied."
    override val title = Res.string.command_restore_actor
    override val inputSerializer = Input.serializer()

    override fun validate(context: CommandContext, input: Input): Rejection? = context.validateScene(input.scene) ?: when {
        input.actorIndex !in 0..context.readScene(input.scene).size -> Rejection(Res.string.error_command_index_invalid, input.actorIndex.toString())
        !SceneDocument.isValid(SceneDocument.format(listOf(SceneActorEntry(input.typeId, input.state)))) -> Rejection(Res.string.error_command_scene_content_invalid)
        else -> null
    }

    override suspend fun apply(context: CommandContext, input: Input): Applied {
        val actors = context.readScene(input.scene)
        context.writeScene(input.scene, actors.toMutableList().apply { add(input.actorIndex, SceneActorEntry(input.typeId, input.state)) })
        return Applied(inverse = Invocation(RemoveActor, RemoveActor.Input(input.scene, input.actorIndex)))
    }

    @Serializable
    data class Input(
        @Description("The name of the Scene: its file name without .json.")
        @SerialName("scene") val scene: String,
        @Description("Where the Actor goes in the Scene's Actor list, starting from 0. The Actors from there on move one index up.")
        @SerialName("actorIndex") val actorIndex: Int,
        @Description("The type ID of the Actor: the name of its Actor type.")
        @SerialName("typeId") val typeId: String,
        @Description("The saved state of the Actor, as a JSON object in a string.")
        @SerialName("state") val state: String,
    )
}
