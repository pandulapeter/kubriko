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

import com.pandulapeter.kubrikoCreator.commands.ActionCommand
import com.pandulapeter.kubrikoCreator.commands.CommandContext
import com.pandulapeter.kubrikoCreator.commands.CommandOutcome
import com.pandulapeter.kubrikoCreator.commands.NoInput
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

public object Build : ActionCommand<NoInput> {
    override val name: String = "build"
    override val description: String = "Compiles the project, and returns whether it succeeded with the compiler problems it reported. " +
        "Changes to Actor types reach the game only through a build, which play does on its own."
    override val inputSerializer: KSerializer<NoInput> = NoInput.serializer()

    override suspend fun run(context: CommandContext, input: NoInput): CommandOutcome = context.runner.build().let { result ->
        CommandOutcome.Success(
            buildJsonObject {
                put("isSuccessful", result.isSuccessful)
                put("console", result.console.toJsonArray())
            },
        )
    }
}
