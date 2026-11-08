/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.commands.screenplay

import com.pandulapeter.kubrikoCreator.commands.Applied
import com.pandulapeter.kubrikoCreator.commands.CommandContext
import com.pandulapeter.kubrikoCreator.commands.CommandProblem
import com.pandulapeter.kubrikoCreator.commands.CommandTarget
import com.pandulapeter.kubrikoCreator.commands.Invocation
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.commands.WriteCommand
import com.pandulapeter.kubrikoCreator.foundation.EditorMutation
import com.pandulapeter.kubrikoCreator.schema.Description
import com.pandulapeter.kubrikoCreator.screenplay.ScreenplaySettings
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.io.File

@OptIn(EditorMutation::class)
public object SetScreenplayReminders : WriteCommand<SetScreenplayReminders.Input> {
    override val name: String = "set_screenplay_reminders"
    override val description: String = "Turns on or off the reminders about pages that don't keep up with the game: in the health checks and when committing."
    override val inputSerializer: KSerializer<Input> = Input.serializer()

    override fun target(context: CommandContext, input: Input): CommandTarget = CommandTarget.Project

    override fun validate(context: CommandContext, input: Input): Rejection? = null

    override suspend fun apply(context: CommandContext, input: Input): Applied = withContext(context.ioDispatcher) {
        val root = context.project.rootDirectory
        val file = File(root, ScreenplaySettings.FILE)
        var previous = ScreenplaySettings()
        val isWritten = ScreenplaySettings.store(root).update { current ->
            current?.let { previous = it }
            (current ?: ScreenplaySettings()).copy(areRemindersShown = input.isOn)
        }
        if (!isWritten) return@withContext Applied.failed(Rejection(CommandProblem.SCREENPLAY_SETTINGS_UNREADABLE, ScreenplaySettings.FILE))
        // A project keeping the defaults has no file, so that undoing the first change leaves none behind.
        if (ScreenplaySettings.read(root) == ScreenplaySettings()) file.delete()
        context.scenes.ownWrites.record(file)
        context.screenplay.onFilesChanged()
        Applied(inverse = Invocation(SetScreenplayReminders, Input(previous.areRemindersShown)))
    }

    @Serializable
    public data class Input(
        @Description("Whether the reminders are on.")
        @SerialName("isOn") val isOn: Boolean,
    )
}
