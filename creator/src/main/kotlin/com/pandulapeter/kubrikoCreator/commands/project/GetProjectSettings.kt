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
import com.pandulapeter.kubrikoCreator.commands.CommandProblem
import com.pandulapeter.kubrikoCreator.commands.CommandTarget
import com.pandulapeter.kubrikoCreator.commands.NoInput
import com.pandulapeter.kubrikoCreator.commands.ReadCommand
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.project.GameIdentity
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

internal object GetProjectSettings : ReadCommand<NoInput> {
    override val name = "get_project_settings"
    override val description = "Reads the game's name, version name and number, application ID, vendor, and description. A project whose " +
        "template is too old to have them says so (see get_project_upgrade)."
    override val inputSerializer = NoInput.serializer()

    override fun target(context: CommandContext, input: NoInput) = CommandTarget.Project

    override suspend fun read(context: CommandContext, input: NoInput): CommandOutcome {
        val identity = withContext(context.ioDispatcher) { GameIdentity.read(context.project.rootDirectory) }
            ?: return CommandOutcome.Rejected(Rejection(CommandProblem.PROJECT_SETTINGS_UNAVAILABLE))
        return CommandOutcome.Success(
            buildJsonObject {
                put("name", identity.name)
                put("versionName", identity.versionName)
                put("versionNumber", identity.versionNumber)
                put("applicationId", identity.applicationId)
                put("vendor", identity.vendor)
                put("description", identity.description)
            },
        )
    }
}
