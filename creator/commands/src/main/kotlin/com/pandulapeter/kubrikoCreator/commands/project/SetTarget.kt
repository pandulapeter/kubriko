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

import com.pandulapeter.kubrikoCreator.commands.Applied
import com.pandulapeter.kubrikoCreator.commands.CommandContext
import com.pandulapeter.kubrikoCreator.commands.CommandTarget
import com.pandulapeter.kubrikoCreator.commands.Invocation
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.commands.WriteCommand
import com.pandulapeter.kubrikoCreator.foundation.EditorMutation
import com.pandulapeter.kubrikoCreator.schema.Description
import com.pandulapeter.kubrikoCreator.template.GameTarget
import com.pandulapeter.kubrikoCreator.template.TargetChange
import com.pandulapeter.kubrikoCreator.template.TargetProblem
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Turns one of the game's targets on or off in its build files. When turning it back brings every file it changes back
 * as it was (`TargetChange.Proposed.isReversible`; the files it creates stay), that is its undo step; otherwise it can't be
 * undone, and clears the history, as the change it showed said.
 */
@OptIn(EditorMutation::class)
public object SetTarget : WriteCommand<SetTarget.Input> {
    override val name: String = "set_target"
    override val description: String = "Turns one of the game's targets on or off: makes the change get_target_change showed, if the files are still as " +
        "it found them. It is in the user's build files: show it and ask first."
    override val inputSerializer: KSerializer<Input> = Input.serializer()
    override val isDestructive: Boolean = true

    override fun target(context: CommandContext, input: Input): CommandTarget? = CommandTarget.Project

    override fun validate(context: CommandContext, input: Input): Rejection? = context.targetsOrRejection(input.target).second

    override suspend fun apply(context: CommandContext, input: Input): Applied {
        val targets = context.targetsOrRejection(input.target).first ?: return Applied.failed(checkNotNull(validate(context, input)))
        val outcome = withContext(context.ioDispatcher) {
            when (val change = targets.change(input.target, input.isOn)) {
                is TargetChange.Refused -> Rejection(change.problem, change.arguments)
                is TargetChange.Proposed -> when {
                    input.changeId != null && input.changeId != change.id -> Rejection(TargetProblem.CHANGED_SINCE_SHOWN, input.target.key)
                    !targets.apply(change) -> Rejection(TargetProblem.CHANGED_WHILE_APPLYING, input.target.key)
                    else -> change
                }
            }
        }
        if (outcome is Rejection) return Applied.failed(outcome)
        context.buildFiles.onUpgraded()
        return if ((outcome as TargetChange.Proposed).isReversible) {
            Applied(inverse = Invocation(SetTarget, Input(input.target, !input.isOn)))
        } else {
            Applied(inverse = null, isRewritingFiles = true)
        }
    }

    @Serializable
    public data class Input(
        @Description("The target: web, android, or ios (the desktop is always on).")
        @SerialName("target") val target: GameTarget,
        @Description("True to build the game for it, false to stop.")
        @SerialName("isOn") val isOn: Boolean,
        @Description("The changeId get_target_change gave, so that only the change the user saw is made.")
        @SerialName("changeId") val changeId: String? = null,
    )
}
