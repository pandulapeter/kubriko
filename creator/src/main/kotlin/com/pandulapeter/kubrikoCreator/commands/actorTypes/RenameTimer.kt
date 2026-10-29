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

import com.pandulapeter.kubrikoCreator.actors.ActorTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.ActorTypeFamily
import com.pandulapeter.kubrikoCreator.actors.TimerNaming
import com.pandulapeter.kubrikoCreator.actors.timerNames
import com.pandulapeter.kubrikoCreator.actors.withTimerRenamed
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
import kubriko.creator.generated.resources.command_rename_timer
import kubriko.creator.generated.resources.error_command_timer_invalid
import kubriko.creator.generated.resources.error_command_timer_taken
import kubriko.creator.generated.resources.error_command_timer_unknown

@OptIn(EditorMutation::class)
internal object RenameTimer : WriteCommand<RenameTimer.Input> {
    override val name = "rename_timer"
    override val description = "Renames a Timer of a block-built Actor type or Manager type in every timerFinished Event and Timer Block that names it. " +
        "An Actor type shares its Timers with its whole family (its ancestors and their descendants), so they are renamed in all of them."
    override val title = Res.string.command_rename_timer
    override val inputSerializer = Input.serializer()

    override fun target(context: CommandContext, input: Input) = CommandTarget.BlockType(input.actorType)

    override fun validate(context: CommandContext, input: Input): Rejection? = context.validateType(input.actorType) ?: run {
        val timerNames = context.familyOf(input.actorType).flatMap { context.requireType(it).timerNames }
        when {
            input.oldName !in timerNames -> Rejection(Res.string.error_command_timer_unknown, input.actorType, input.oldName)
            !TimerNaming.isValid(input.newName) -> Rejection(Res.string.error_command_timer_invalid)
            input.newName in timerNames -> Rejection(Res.string.error_command_timer_taken)
            else -> null
        }
    }

    override suspend fun apply(context: CommandContext, input: Input): Applied {
        context.familyOf(input.actorType).forEach { name ->
            context.requireType(name).let { definition ->
                if (input.oldName in definition.timerNames) context.requireBlockTypes.put(definition.withTimerRenamed(input.oldName, input.newName))
            }
        }
        return Applied(inverse = Invocation(RenameTimer, input.copy(oldName = input.newName, newName = input.oldName)))
    }

    /**
     * The types whose generated classes share the Timers of [typeName]: every Actor type in its family, or the Manager
     * type alone.
     */
    private fun CommandContext.familyOf(typeName: String) = if (requireType(typeName) is ActorTypeDefinition) {
        ActorTypeFamily(requireBlockTypes.actorTypes.value).let { family -> family.familyOf(family.rootOf(typeName)) }
    } else {
        listOf(typeName)
    }

    @Serializable
    data class Input(
        @Description("The name of the Actor type or Manager type.")
        @SerialName("actorType") val actorType: String,
        @Description("The Timer's current name.")
        @SerialName("oldName") val oldName: String,
        @Description("The Timer's new name, like spawnEnemy: a letter, then letters and digits, starting lowercase. No other Timer of the type (or its family) may have it.")
        @SerialName("newName") val newName: String,
    )
}
