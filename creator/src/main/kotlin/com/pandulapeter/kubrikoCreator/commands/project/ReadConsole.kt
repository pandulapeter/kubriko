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
import com.pandulapeter.kubrikoCreator.commands.Description
import com.pandulapeter.kubrikoCreator.commands.ReadCommand
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray

internal object ReadConsole : ReadCommand<ReadConsole.Input> {
    override val name = "read_console"
    override val description = "Reads the newest lines of Kubriko Creator's console, oldest first: what Kubriko Creator reports, compiler problems with their locations, " +
        "and the logs of the running game."
    override val inputSerializer = Input.serializer()

    override suspend fun read(context: CommandContext, input: Input) = CommandOutcome.Success(JsonArray(context.console.value.takeLast(input.limit.coerceAtLeast(0)).map { it.toJson() }))

    @Serializable
    data class Input(
        @Description("How many of the newest lines to read.")
        @SerialName("limit") val limit: Int = 100,
    )
}
