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
import com.pandulapeter.kubrikoCreator.commands.AiStepOrigin
import com.pandulapeter.kubrikoCreator.commands.CommandContext
import com.pandulapeter.kubrikoCreator.commands.CommandOutcome
import com.pandulapeter.kubrikoCreator.commands.CommandProblem
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.schema.Description
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

public object RememberForNextTime : ActionCommand<RememberForNextTime.Input> {
    override val name: String = "remember_for_next_time"
    override val description: String = "Notes what this conversation did and what is still open, which the next conversation about the project starts with. " +
        "Each call replaces the last; outside Kubriko Creator's own panel it does nothing."
    override val inputSerializer: KSerializer<Input> = Input.serializer()
    override val leavesEverythingAsItWas: Boolean = true

    override suspend fun run(context: CommandContext, input: Input): CommandOutcome = run(context, input, origin = null)

    override suspend fun run(context: CommandContext, input: Input, origin: AiStepOrigin?): CommandOutcome {
        val summary = input.summary.trim()
        if (summary.isEmpty() || summary.length > MAXIMUM_LENGTH) {
            return CommandOutcome.Rejected(Rejection(CommandProblem.SUMMARY_INVALID, MAXIMUM_LENGTH.toString()))
        }
        origin?.let { context.summaries.remember(it.conversationId, summary) }
        return CommandOutcome.Success(JsonObject(emptyMap()))
    }

    internal const val MAXIMUM_LENGTH = 600

    @Serializable
    public data class Input(
        @Description("One to three sentences: what was done, in the game's terms, and what is still open.")
        @SerialName("summary") val summary: String,
    )
}
