/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.commands.actorTypes

import com.pandulapeter.kubrikoCreator.commands.CommandContext
import com.pandulapeter.kubrikoCreator.commands.CommandOutcome
import com.pandulapeter.kubrikoCreator.commands.NoInput
import com.pandulapeter.kubrikoCreator.commands.ReadCommand
import com.pandulapeter.kubrikoCreator.commands.Rejection
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.error_command_no_actor_types
import kubriko.creator.generated.resources.command_list_actor_types

internal object ListActorTypes : ReadCommand<NoInput> {
    override val name = "list_actor_types"
    override val description = "Lists the block-built Actor types of the project with the Events each one reacts to."
    override val inputSerializer = NoInput.serializer()
    override val title = Res.string.command_list_actor_types

    override suspend fun read(context: CommandContext, input: NoInput): CommandOutcome {
        val actorTypes = context.blockTypes ?: return CommandOutcome.Rejected(Rejection(Res.string.error_command_no_actor_types))
        return CommandOutcome.Success(
            JsonArray(
                actorTypes.actorTypes.value.map { definition ->
                    buildJsonObject {
                        put("name", definition.name)
                        putJsonArray("events") { definition.events.forEach { add(it.type.serialName) } }
                        put("isChangedByHand", definition.name in actorTypes.changedByHand.value)
                    }
                },
            ),
        )
    }
}
