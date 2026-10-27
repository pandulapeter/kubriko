/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.commands.project

import com.pandulapeter.kubrikoCreator.commands.CommandContext
import com.pandulapeter.kubrikoCreator.commands.CommandOutcome
import com.pandulapeter.kubrikoCreator.commands.NoInput
import com.pandulapeter.kubrikoCreator.commands.ReadCommand
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

internal object GetProjectOverview : ReadCommand<NoInput> {
    override val name = "get_project_overview"
    override val description = "Describes the open project: its name, its block-built Actor types (or that it doesn't use them), and its Scenes. " +
        "A good first call, before reading anything else."
    override val inputSerializer = NoInput.serializer()

    override suspend fun read(context: CommandContext, input: NoInput) = CommandOutcome.Success(
        buildJsonObject {
            put("name", context.project.name)
            put("usesBlockBuiltActorTypes", context.actorTypes != null)
            context.actorTypes?.let { actorTypes -> putJsonArray("actorTypes") { actorTypes.actorTypes.value.forEach { add(it.name) } } }
            putJsonArray("scenes") { context.scenes.names().forEach { add(it) } }
        },
    )
}
