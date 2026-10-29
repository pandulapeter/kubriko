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
import com.pandulapeter.kubrikoCreator.scenes.SceneActorEntry
import com.pandulapeter.kubrikoCreator.scenes.SceneDocument
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.putJsonArray
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.command_restore_actors
import kubriko.creator.generated.resources.error_command_index_invalid
import kubriko.creator.generated.resources.error_command_scene_content_invalid

internal object RestoreActors : WriteCommand<RestoreActors.Input> {
    override val name = "restore_actors"
    override val description = "Puts many Actors into a Scene at once with their whole saved states, as read_scene returns them, as one step to undo: " +
        "how removed Actors are brought back, and how Actors of hand-written Actor types are copied in bulk. " +
        "They are put in one after another, in the order given, so each index counts the ones put in before it."
    override val title = Res.string.command_restore_actors
    override val inputSerializer = Input.serializer()

    override fun target(context: CommandContext, input: Input) = CommandTarget.Scene(input.scene)

    override fun validate(context: CommandContext, input: Input): Rejection? = context.validateScene(input.scene)
        ?: validateActorList(input.actors.map { it.actorId })
        ?: run {
            val actors = context.readScene(input.scene)
            val ids = actors.mapTo(HashSet()) { it.id }
            input.actors.withIndex().firstNotNullOfOrNull { (position, restored) ->
                when {
                    restored.actorIndex !in 0..actors.size + position -> Rejection(Res.string.error_command_index_invalid, restored.actorIndex.toString())
                    SceneDocument.stateObject(restored.state) == null -> Rejection(Res.string.error_command_scene_content_invalid)
                    else -> restored.actorId?.let { id -> validateNewActorId(id) { it in ids } }
                }
            }
        }

    override suspend fun apply(context: CommandContext, input: Input): Applied {
        val actors = context.readScene(input.scene).toMutableList()
        val ids = actors.mapTo(HashSet()) { it.id }
        input.actors.forEach { restored -> restored.actorId?.let(ids::add) }
        val restoredIds = input.actors.map { restored ->
            val id = restored.actorId ?: SceneDocument.newId(ids).also(ids::add)
            actors.add(restored.actorIndex, SceneActorEntry(restored.typeId, restored.state, id))
            id
        }
        context.writeScene(input.scene, actors)
        return Applied(
            inverse = Invocation(RemoveActors, RemoveActors.Input(input.scene, restoredIds)),
            result = buildJsonObject { putJsonArray("actorIds") { restoredIds.forEach { add(it) } } },
        )
    }

    @Serializable
    data class Input(
        @Description("The name of the Scene: its path in the Scenes folder without .json, like level-2 or levels/1-1.")
        @SerialName("scene") val scene: String,
        @Description("The Actors to put in, in order.")
        @SerialName("actors") val actors: List<Restored>,
    )

    @Serializable
    @Description("One Actor to put in.")
    data class Restored(
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
