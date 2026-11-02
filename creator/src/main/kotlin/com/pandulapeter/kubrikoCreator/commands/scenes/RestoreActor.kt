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
import com.pandulapeter.kubrikoCreator.commands.Descriptions
import com.pandulapeter.kubrikoCreator.commands.Invocation
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.commands.WriteCommand
import com.pandulapeter.kubrikoCreator.schema.Description
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kubriko.creator.generated.resources.Res

internal object RestoreActor : WriteCommand<RestoreActor.Input> {
    override val name = "restore_actor"
    override val description = "Puts an Actor into a Scene with its whole saved state, as read_scene returns it: how a removed Actor is brought back, " +
        "and how Actors of hand-written Actor types are copied."
    override val inputSerializer = Input.serializer()

    override fun target(context: CommandContext, input: Input) = CommandTarget.Scene(input.scene)

    override fun validate(context: CommandContext, input: Input): Rejection? = context.validateRestorations(input.scene, listOf(input.restored))

    override suspend fun apply(context: CommandContext, input: Input): Applied {
        val id = context.restoreActors(input.scene, listOf(input.restored)).single()
        return Applied(
            inverse = Invocation(RemoveActor, RemoveActor.Input(input.scene, id)),
            result = buildJsonObject { put("actorId", id) },
        )
    }

    @Serializable
    data class Input(
        @Description(Descriptions.SCENE_NAME)
        @SerialName("scene") val scene: String,
        @Description("Where the Actor goes in the Scene's Actor list, starting from 0, which decides the order Actors are added in.")
        @SerialName("actorIndex") val actorIndex: Int,
        @Description("The type ID of the Actor: the name of its Actor type.")
        @SerialName("typeId") val typeId: String,
        @Description("The saved state of the Actor, as a JSON object in a string.")
        @SerialName("state") val state: String,
        @Description("The ID the Actor gets, or none for a new one: how a removed Actor comes back as itself.")
        @SerialName("actorId") val actorId: String? = null,
    ) {
        val restored get() = RestoreActors.Restored(actorIndex, typeId, state, actorId)
    }
}
