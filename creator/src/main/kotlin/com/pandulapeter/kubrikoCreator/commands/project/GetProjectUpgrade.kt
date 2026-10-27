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
import com.pandulapeter.kubrikoCreator.template.ProjectUpgrader
import com.pandulapeter.kubrikoCreator.template.TemplateUpgrade
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.command_get_project_upgrade

internal object GetProjectUpgrade : ReadCommand<NoInput> {
    override val name = "get_project_upgrade"
    override val description = "Tells whether the project's template files (Game.kt and GameEntryPoint.kt, which belong to the user) need a change that this " +
        "Kubriko Creator relies on, such as passing the generated Manager types to Kubriko. Lists each change Kubriko Creator can make (the file before " +
        "and after) and the files that were changed too much, which the user has to change by hand. Show the changes to the user before upgrade_project."
    override val inputSerializer = NoInput.serializer()
    override val title = Res.string.command_get_project_upgrade

    override suspend fun read(context: CommandContext, input: NoInput) = CommandOutcome.Success(
        when (val upgrade = withContext(context.ioDispatcher) { ProjectUpgrader(context.project.rootDirectory).check() }) {
            TemplateUpgrade.UpToDate -> buildJsonObject { put("isNeeded", false) }
            is TemplateUpgrade.Needed -> buildJsonObject {
                put("isNeeded", true)
                putJsonArray("changes") {
                    upgrade.changes.forEach { change ->
                        add(buildJsonObject {
                            put("path", change.path)
                            put("before", change.before)
                            put("after", change.after)
                        })
                    }
                }
                putJsonArray("byHand") { upgrade.byHand.forEach { add(it) } }
            }
        } as JsonObject,
    )
}
