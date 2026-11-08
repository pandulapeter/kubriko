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
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

public object AskUser : ActionCommand<AskUser.Input> {
    override val name: String = "ask_user"
    override val description: String = "Asks the user a question with two to four short answers, which Kubriko Creator shows as buttons beside a field for " +
        "any other answer, and returns the answer once given. Ask in the game's terms, one question at a time."
    override val inputSerializer: KSerializer<Input> = Input.serializer()

    override suspend fun run(context: CommandContext, input: Input): CommandOutcome = run(context, input, origin = null)

    override suspend fun run(context: CommandContext, input: Input, origin: AiStepOrigin?): CommandOutcome {
        val answers = input.answers.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        if (input.question.isBlank() || answers.size !in MINIMUM_ANSWERS..MAXIMUM_ANSWERS || answers.any { it.length > MAXIMUM_ANSWER_LENGTH }) {
            return CommandOutcome.Rejected(Rejection(CommandProblem.QUESTION_INVALID, MAXIMUM_ANSWER_LENGTH.toString()))
        }
        val answer = context.questions.ask(input.question.trim(), answers, origin)
        return CommandOutcome.Success(buildJsonObject { put("answer", answer) })
    }

    private const val MINIMUM_ANSWERS = 2
    private const val MAXIMUM_ANSWERS = 4
    private const val MAXIMUM_ANSWER_LENGTH = 80

    @Serializable
    public data class Input(
        @Description("The question, in the game's terms.")
        @SerialName("question") val question: String,
        @Description("Two to four short answers to choose from, the likeliest first; the user can also type another.")
        @SerialName("answers") val answers: List<String>,
    )
}
