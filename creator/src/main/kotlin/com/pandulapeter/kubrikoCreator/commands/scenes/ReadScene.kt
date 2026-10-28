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

import com.pandulapeter.kubrikoCreator.commands.CommandContext
import com.pandulapeter.kubrikoCreator.commands.CommandOutcome
import com.pandulapeter.kubrikoCreator.commands.CommandTarget
import com.pandulapeter.kubrikoCreator.commands.Description
import com.pandulapeter.kubrikoCreator.commands.ReadCommand
import com.pandulapeter.kubrikoCreator.scenes.SceneDocument
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.command_read_scene

internal object ReadScene : ReadCommand<ReadScene.Input> {
    override val name = "read_scene"
    override val description = "Reads the Actors of a Scene, in order: the type ID of each one (the name of its Actor type), its position when it has one, " +
        "and its whole saved state. Actors are addressed by their actorId. content is the raw Scene file."
    override val inputSerializer = Input.serializer()
    override val title = Res.string.command_read_scene

    override fun target(context: CommandContext, input: Input) = CommandTarget.Scene(input.scene)

    override suspend fun read(context: CommandContext, input: Input): CommandOutcome = withContext(context.ioDispatcher) {
        context.validateScene(input.scene)?.let { return@withContext CommandOutcome.Rejected(it) }
        val actors = context.readScene(input.scene)
        CommandOutcome.Success(
            buildJsonObject {
                putJsonArray("actors") {
                    actors.forEach { actor ->
                        addJsonObject {
                            put("actorId", actor.id)
                            put("typeId", actor.typeId)
                            SceneDocument.positionOf(actor.state)?.let { (x, y) ->
                                put("x", x)
                                put("y", y)
                            }
                            put("state", actor.state)
                        }
                    }
                }
                put("content", SceneDocument.format(actors))
            },
        )
    }

    @Serializable
    data class Input(
        @Description("The name of the Scene: its path in the Scenes folder without .json, like level-2 or levels/1-1.")
        @SerialName("scene") val scene: String,
    )
}
