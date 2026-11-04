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
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

public object GetProjectUpgrade : ReadCommand<NoInput> {
    override val name: String = "get_project_upgrade"
    override val description: String = "Tells whether the project's template files (Game.kt, GameEntryPoint.kt, and others the user owns) need a change Kubriko " +
        "Creator relies on: each change it can make (the file before and after; after left out to delete it; added instead for a file that isn't text, such as a font) and the files changed too much, left to the user. " +
        "Show the changes to the user before upgrade_project."
    override val inputSerializer: KSerializer<NoInput> = NoInput.serializer()

    override suspend fun read(context: CommandContext, input: NoInput): CommandOutcome = CommandOutcome.Success(
        when (val upgrade = withContext(context.ioDispatcher) { ProjectUpgrader(context.project.rootDirectory).check() }) {
            TemplateUpgrade.UpToDate -> buildJsonObject { put("isNeeded", false) }
            is TemplateUpgrade.Needed -> buildJsonObject {
                put("isNeeded", true)
                putJsonArray("changes") {
                    upgrade.changes.forEach { change ->
                        add(buildJsonObject {
                            put("path", change.path)
                            if (change.resource != null) {
                                put("added", "a file that isn't text, shown by its path only")
                            } else {
                                put("before", change.before)
                                change.after?.let { put("after", it) }
                            }
                        })
                    }
                }
                putJsonArray("byHand") { upgrade.byHand.forEach { add(it) } }
            }
        },
    )
}
