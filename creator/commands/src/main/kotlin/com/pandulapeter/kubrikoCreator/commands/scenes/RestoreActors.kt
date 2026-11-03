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
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.putJsonArray

public object RestoreActors : WriteCommand<RestoreActors.Input> {
    override val name: String = "restore_actors"
    override val description: String = "Puts many Actors into a Scene at once with their whole saved states, as read_scene returns them, as one step to undo: " +
        "how removed Actors are brought back, and how Actors of hand-written Actor types are copied in bulk. " +
        "They are put in one after another, in the order given, so each index counts the ones put in before it."
    override val inputSerializer: KSerializer<Input> = Input.serializer()

    override fun target(context: CommandContext, input: Input): CommandTarget? = CommandTarget.Scene(input.scene)

    override fun validate(context: CommandContext, input: Input): Rejection? = context.validateRestorations(input.scene, input.actors)

    override suspend fun apply(context: CommandContext, input: Input): Applied {
        val restoredIds = context.restoreActors(input.scene, input.actors)
        return Applied(
            inverse = Invocation(RemoveActors, RemoveActors.Input(input.scene, restoredIds)),
            result = buildJsonObject { putJsonArray("actorIds") { restoredIds.forEach { add(it) } } },
        )
    }

    @Serializable
    public data class Input(
        @Description(Descriptions.SCENE_NAME)
        @SerialName("scene") val scene: String,
        @Description("The Actors to put in, in order.")
        @SerialName("actors") val actors: List<Restored>,
    )

    @Serializable
    @Description("One Actor to put in.")
    public data class Restored(
        @Description("Where the Actor goes in the Scene's Actor list, starting from 0, once the Actors before it in this list are in.")
        @SerialName("actorIndex") val actorIndex: Int,
        @Description("The type ID of the Actor: the name of its Actor type.")
        @SerialName("typeId") val typeId: String,
        @Description("The saved state of the Actor, as a JSON object in a string.")
        @SerialName("state") val state: String,
        @Description("The ID the Actor gets, or none for a new one: how a removed Actor comes back as itself.")
        @SerialName("actorId") val actorId: String? = null,
    )
}
