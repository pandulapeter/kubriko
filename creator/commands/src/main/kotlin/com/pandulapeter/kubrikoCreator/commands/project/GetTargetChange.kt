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
import com.pandulapeter.kubrikoCreator.commands.CommandTarget
import com.pandulapeter.kubrikoCreator.commands.ReadCommand
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.schema.Description
import com.pandulapeter.kubrikoCreator.template.GameTarget
import com.pandulapeter.kubrikoCreator.template.TargetChange
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

public object GetTargetChange : ReadCommand<GetTargetChange.Input> {
    override val name: String = "get_target_change"
    override val description: String = "Shows what turning one of the game's targets on or off changes in its build files (which belong to the user), and " +
        "which files it creates, without changing anything. Show it to the user, then pass its changeId to set_target."
    override val inputSerializer: KSerializer<Input> = Input.serializer()

    override fun target(context: CommandContext, input: Input): CommandTarget? = CommandTarget.Project

    override suspend fun read(context: CommandContext, input: Input): CommandOutcome {
        val (targets, rejection) = context.targetsOrRejection(input.target)
        if (targets == null) return CommandOutcome.Rejected(checkNotNull(rejection))
        return when (val change = withContext(context.ioDispatcher) { targets.change(input.target, input.isOn) }) {
            is TargetChange.Refused -> CommandOutcome.Rejected(Rejection(change.problem, change.arguments))
            is TargetChange.Proposed -> CommandOutcome.Success(changeJson(change))
        }
    }

    @Serializable
    public data class Input(
        @Description("The target: web, android, or ios (the desktop is always on).")
        @SerialName("target") val target: GameTarget,
        @Description("True to build the game for it, false to stop.")
        @SerialName("isOn") val isOn: Boolean,
    )
}
