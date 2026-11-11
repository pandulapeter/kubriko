/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.commands.actorTypes

import com.pandulapeter.kubrikoCreator.commands.Applied
import com.pandulapeter.kubrikoCreator.commands.CommandContext
import com.pandulapeter.kubrikoCreator.commands.CommandTarget
import com.pandulapeter.kubrikoCreator.commands.DeletingCommand
import com.pandulapeter.kubrikoCreator.commands.Descriptions
import com.pandulapeter.kubrikoCreator.commands.LookupProblem
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.commands.SimilarNames
import com.pandulapeter.kubrikoCreator.commands.deleteWithUsages
import com.pandulapeter.kubrikoCreator.context.RelationSubject
import com.pandulapeter.kubrikoCreator.foundation.EditorMutation
import com.pandulapeter.kubrikoCreator.schema.Description
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@OptIn(EditorMutation::class)
public object DeleteTimer : DeletingCommand<DeleteTimer.Input> {
    override val name: String = "delete_timer"
    override val description: String = "Deletes a Timer of a type with its timerFinished Events and Timer Blocks (an Actor type shares its Timers with its family)."
    override val inputSerializer: KSerializer<Input> = Input.serializer()

    override fun subject(input: Input): RelationSubject = RelationSubject.Timer(input.actorType, input.timer)

    override fun target(context: CommandContext, input: Input): CommandTarget = CommandTarget.BlockType(input.actorType)

    override fun validate(context: CommandContext, input: Input): Rejection? = context.validateType(input.actorType) ?: run {
        val timerNames = context.timerNamesOf(input.actorType)
        if (input.timer in timerNames) {
            null
        } else {
            SimilarNames.rejection(LookupProblem.TIMER_UNKNOWN, LookupProblem.TIMER_UNKNOWN_SIMILAR, input.timer, timerNames, input.actorType, input.timer)
        }
    }

    override suspend fun apply(context: CommandContext, input: Input): Applied = context.deleteWithUsages(subject(input), delete = null)

    @Serializable
    public data class Input(
        @Description(Descriptions.TYPE_NAME)
        @SerialName("actorType") val actorType: String,
        @Description("The Timer's name.")
        @SerialName("timer") val timer: String,
    )
}
