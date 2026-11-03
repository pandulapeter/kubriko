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
import com.pandulapeter.kubrikoCreator.template.TargetChange
import com.pandulapeter.kubrikoCreator.template.TargetProblem
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer

/**
 * Turns one of the game's targets on or off in its build files. Its inverse turns it back, which brings the build files back
 * as they were and keeps the files turning it on created.
 */
@OptIn(EditorMutation::class)
public object SetTarget : WriteCommand<GetTargetChange.Input> {
    override val name: String = "set_target"
    override val description: String = "Turns one of the game's targets on or off: makes the changes get_target_change shows. They are in the user's build " +
        "files: show them and ask first."
    override val inputSerializer: KSerializer<GetTargetChange.Input> = GetTargetChange.Input.serializer()

    override fun target(context: CommandContext, input: GetTargetChange.Input): CommandTarget? = CommandTarget.Project

    override fun validate(context: CommandContext, input: GetTargetChange.Input): Rejection? = context.targetsOrRejection(input.target).second

    override suspend fun apply(context: CommandContext, input: GetTargetChange.Input): Applied {
        val targets = context.targetsOrRejection(input.target).first ?: return Applied.failed(checkNotNull(validate(context, input)))
        val rejection = withContext(context.ioDispatcher) {
            when (val change = targets.change(input.target, input.isOn)) {
                is TargetChange.Refused -> Rejection(change.problem, change.arguments)
                is TargetChange.Proposed -> if (targets.apply(change)) null else Rejection(TargetProblem.CHANGED_WHILE_APPLYING, input.target.key)
            }
        }
        if (rejection != null) return Applied.failed(rejection)
        context.buildFiles.onUpgraded()
        return Applied(inverse = Invocation(SetTarget, input.copy(isOn = !input.isOn)))
    }
}
