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
import com.pandulapeter.kubrikoCreator.context.healthFindings
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.JsonPrimitive

public object GetHealthChecks : ReadCommand<NoInput> {
    override val name: String = "get_health_checks"
    override val description: String = "Lists what about the game is worth a look: a first Scene that doesn't exist, Actor types nothing " +
        "places or adds, Scenes nothing goes to, Properties nothing reads, timerFinished or collision Events that can never fire, what names something " +
        "deleted, and pages' images unused or gone. Not all are mistakes."
    override val inputSerializer: KSerializer<NoInput> = NoInput.serializer()

    override suspend fun read(context: CommandContext, input: NoInput): CommandOutcome =
        CommandOutcome.Success(JsonPrimitive(context.healthFindings().joinToString("\n").ifEmpty { "Nothing worth a look." }))
}
