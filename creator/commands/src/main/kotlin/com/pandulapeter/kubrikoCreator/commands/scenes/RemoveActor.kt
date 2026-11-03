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
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

public object RemoveActor : WriteCommand<RemoveActor.Input> {
    override val name: String = "remove_actor"
    override val description: String = "Removes an Actor from a Scene."
    override val inputSerializer: KSerializer<Input> = Input.serializer()

    override fun target(context: CommandContext, input: Input): CommandTarget? = CommandTarget.Scene(input.scene)

    override fun validate(context: CommandContext, input: Input): Rejection? = context.validateRemovals(input.scene, listOf(input.actorId))

    override suspend fun apply(context: CommandContext, input: Input): Applied {
        val removed = context.removeActors(input.scene, listOf(input.actorId)).single()
        return Applied(inverse = Invocation(RestoreActor, RestoreActor.Input(input.scene, removed.actorIndex, removed.typeId, removed.state, removed.actorId)))
    }

    @Serializable
    public data class Input(
        @Description(Descriptions.SCENE_NAME)
        @SerialName("scene") val scene: String,
        @Description(Descriptions.SCENE_ACTOR_ID)
        @SerialName("actorId") val actorId: String,
    )
}
