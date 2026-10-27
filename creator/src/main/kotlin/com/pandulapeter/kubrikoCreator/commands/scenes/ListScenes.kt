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
import com.pandulapeter.kubrikoCreator.commands.NoInput
import com.pandulapeter.kubrikoCreator.commands.ReadCommand
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.command_list_scenes

internal object ListScenes : ReadCommand<NoInput> {
    override val name = "list_scenes"
    override val description = "Lists the Scenes of the project with how many Actors each one has (or that its file isn't a valid Scene)."
    override val inputSerializer = NoInput.serializer()
    override val title = Res.string.command_list_scenes

    override suspend fun read(context: CommandContext, input: NoInput) = CommandOutcome.Success(
        withContext(context.ioDispatcher) {
            JsonArray(
                context.scenes.names().map { name ->
                    buildJsonObject {
                        put("name", name)
                        if (context.validateScene(name) == null) put("actorCount", context.readScene(name).size) else put("isValid", false)
                    }
                },
            )
        },
    )
}
