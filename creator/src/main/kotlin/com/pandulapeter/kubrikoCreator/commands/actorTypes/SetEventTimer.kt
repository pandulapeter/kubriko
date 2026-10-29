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

import com.pandulapeter.kubrikoCreator.actors.EventType
import com.pandulapeter.kubrikoCreator.actors.TimerNaming
import com.pandulapeter.kubrikoCreator.actors.withEventTimer
import com.pandulapeter.kubrikoCreator.commands.Applied
import com.pandulapeter.kubrikoCreator.commands.CommandContext
import com.pandulapeter.kubrikoCreator.commands.CommandTarget
import com.pandulapeter.kubrikoCreator.commands.Description
import com.pandulapeter.kubrikoCreator.commands.EditorMutation
import com.pandulapeter.kubrikoCreator.commands.Invocation
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.commands.WriteCommand
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.command_set_event_timer
import kubriko.creator.generated.resources.error_command_timer_invalid
import kubriko.creator.generated.resources.error_command_timer_not_allowed

@OptIn(EditorMutation::class)
internal object SetEventTimer : WriteCommand<SetEventTimer.Input> {
    override val name = "set_event_timer"
    override val description = "Sets the Timer a timerFinished Event of a block-built Actor type or Manager type reacts to."
    override val title = Res.string.command_set_event_timer
    override val inputSerializer = Input.serializer()

    override fun target(context: CommandContext, input: Input) = CommandTarget.BlockType(input.actorType, eventIndex = input.eventIndex)

    override fun validate(context: CommandContext, input: Input): Rejection? = context.validateEvent(input.actorType, input.eventIndex)
        ?: when {
            context.requireType(input.actorType).events[input.eventIndex].type != EventType.TIMER_FINISHED -> Rejection(Res.string.error_command_timer_not_allowed)
            !TimerNaming.isValid(input.timer) -> Rejection(Res.string.error_command_timer_invalid)
            else -> null
        }

    override suspend fun apply(context: CommandContext, input: Input): Applied {
        val definition = context.requireType(input.actorType)
        val previous = definition.events[input.eventIndex].timer ?: TimerNaming.DEFAULT_NAME
        context.requireBlockTypes.put(definition.withEventTimer(input.eventIndex, input.timer))
        return Applied(inverse = Invocation(SetEventTimer, input.copy(timer = previous)))
    }

    @Serializable
    data class Input(
        @Description("The name of the Actor type or Manager type.")
        @SerialName("actorType") val actorType: String,
        @Description("The index of the Event in the Actor type's Event list, starting from 0.")
        @SerialName("eventIndex") val eventIndex: Int,
        @Description("The name of the Timer, like spawnEnemy: a letter, then letters and digits, starting lowercase. A Timer exists once a Block or an Event names it.")
        @SerialName("timer") val timer: String,
    )
}
