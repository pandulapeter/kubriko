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

public object SetActorsValues : WriteCommand<SetActorsValues.Input> {
    override val name: String = "set_actors_values"
    override val description: String = "Changes values of many Actors of a Scene at once, as one step to undo, each as set_actor_values does: where it is, " +
        "its size, rotation, and scale, and for block-built Actor types, its color and the values of its Properties. Values left out stay as they are."
    override val inputSerializer: KSerializer<Input> = Input.serializer()

    override fun target(context: CommandContext, input: Input): CommandTarget? = CommandTarget.Scene(input.scene, input.actors.map { it.actorId })

    override fun validate(context: CommandContext, input: Input): Rejection? = context.validateActorChanges(input.scene, input.actors)

    override suspend fun apply(context: CommandContext, input: Input): Applied {
        val previous = context.changeActorValues(input.scene, input.actors)
        return Applied(inverse = Invocation(SetActorsState, SetActorsState.Input(input.scene, previous)))
    }

    @Serializable
    public data class Input(
        @Description(Descriptions.SCENE_NAME)
        @SerialName("scene") val scene: String,
        @Description("The changes, one per Actor.")
        @SerialName("actors") val actors: List<ActorValueChanges>,
    )
}
