/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.commands.managerTypes

import com.pandulapeter.kubrikoCreator.actors.PropertyKind
import com.pandulapeter.kubrikoCreator.commands.CommandContext
import com.pandulapeter.kubrikoCreator.commands.CommandOutcome
import com.pandulapeter.kubrikoCreator.commands.CommandProblem
import com.pandulapeter.kubrikoCreator.commands.NoInput
import com.pandulapeter.kubrikoCreator.commands.ReadCommand
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.commands.actorTypes.serialName
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

public object ListManagerTypes : ReadCommand<NoInput> {
    override val name: String = "list_manager_types"
    override val description: String = "Lists the block-built Manager types of the project with their Properties (name and kind), which Blocks of every type can use, and the Events each one reacts to."
    override val inputSerializer: KSerializer<NoInput> = NoInput.serializer()

    override suspend fun read(context: CommandContext, input: NoInput): CommandOutcome {
        val types = context.blockTypes ?: return CommandOutcome.Rejected(Rejection(CommandProblem.NO_ACTOR_TYPES))
        return CommandOutcome.Success(
            JsonArray(
                types.managerTypes.value.map { definition ->
                    buildJsonObject {
                        put("name", definition.name)
                        putJsonArray("properties") {
                            definition.properties.forEach { property ->
                                add(buildJsonObject {
                                    put("name", property.name)
                                    put("kind", PropertyKind.of(property).serialName)
                                })
                            }
                        }
                        putJsonArray("events") { definition.events.forEach { add(it.type.serialName) } }
                        put("isChangedByHand", definition.name in types.changedByHand.value)
                    }
                },
            ),
        )
    }
}
