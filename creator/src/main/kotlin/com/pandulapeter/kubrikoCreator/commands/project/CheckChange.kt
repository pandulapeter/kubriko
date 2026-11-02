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
import com.pandulapeter.kubrikoCreator.commands.CommandRegistry
import com.pandulapeter.kubrikoCreator.commands.InputProblem
import com.pandulapeter.kubrikoCreator.commands.ReadCommand
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.commands.WriteCommand
import com.pandulapeter.kubrikoCreator.schema.Description
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.command_check_change
import kubriko.creator.generated.resources.error_command_input_invalid
import kubriko.creator.generated.resources.error_command_not_a_change
import kubriko.creator.generated.resources.error_command_unknown

internal object CheckChange : ReadCommand<CheckChange.Input> {
    override val name = "check_change"
    override val description = "Tells whether a write tool would accept the given arguments, changing nothing: the rejection it would give, or that it would be " +
        "accepted. For a change built from many parts, checked before making it."
    override val inputSerializer = Input.serializer()
    override val title = Res.string.command_check_change
    private val json = Json { ignoreUnknownKeys = false }

    override suspend fun read(context: CommandContext, input: Input): CommandOutcome {
        val command = CommandRegistry.editor[input.tool] ?: return CommandOutcome.Rejected(Rejection(Res.string.error_command_unknown, input.tool))
        if (command !is WriteCommand<*>) return CommandOutcome.Rejected(Rejection(Res.string.error_command_not_a_change, input.tool))
        return check(context, command, input.arguments)
    }

    private fun <I> check(context: CommandContext, command: WriteCommand<I>, arguments: JsonObject): CommandOutcome {
        val decoded = try {
            json.decodeFromJsonElement(command.inputSerializer, arguments)
        } catch (exception: SerializationException) {
            return CommandOutcome.Rejected(Rejection(Res.string.error_command_input_invalid, InputProblem.describe(exception.message)))
        } catch (exception: IllegalArgumentException) {
            return CommandOutcome.Rejected(Rejection(Res.string.error_command_input_invalid, InputProblem.describe(exception.message)))
        }
        return command.validate(context, decoded)?.let(CommandOutcome::Rejected) ?: CommandOutcome.Success(JsonPrimitive(ACCEPTED))
    }

    private const val ACCEPTED = "It would be accepted."

    @Serializable
    data class Input(
        @Description("The name of the write tool, like add_block.")
        @SerialName("tool") val tool: String,
        @Description("The arguments the write tool would be called with.")
        @SerialName("arguments") val arguments: JsonObject,
    )
}
