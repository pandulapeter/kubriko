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
import com.pandulapeter.kubrikoCreator.commands.EditorMutation
import com.pandulapeter.kubrikoCreator.commands.NoInput
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.commands.WriteCommand
import com.pandulapeter.kubrikoCreator.template.ProjectUpgrader
import com.pandulapeter.kubrikoCreator.template.TemplateUpgrade
import kotlinx.coroutines.withContext
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.command_upgrade_project
import kubriko.creator.generated.resources.error_command_upgrade_by_hand
import kubriko.creator.generated.resources.error_command_upgrade_not_needed

/**
 * Changes files that belong to the user, so it can't be undone from the history; committing first (or git) brings
 * them back.
 */
@OptIn(EditorMutation::class)
internal object UpgradeProject : WriteCommand<NoInput> {
    override val name = "upgrade_project"
    override val description = "Makes the changes get_project_upgrade lists to the project's template files. They belong to the user: show them the " +
        "changes and ask first, and offer to commit before. This can't be undone, except through version control."
    override val title = Res.string.command_upgrade_project
    override val inputSerializer = NoInput.serializer()
    override val isDestructive = true

    override fun target(context: CommandContext, input: NoInput) = CommandTarget.Project

    override fun validate(context: CommandContext, input: NoInput): Rejection? = when (val upgrade = ProjectUpgrader(context.project.rootDirectory).check()) {
        TemplateUpgrade.UpToDate -> Rejection(Res.string.error_command_upgrade_not_needed)
        is TemplateUpgrade.Needed -> if (upgrade.changes.isEmpty()) Rejection(Res.string.error_command_upgrade_by_hand, upgrade.byHand.joinToString()) else null
    }

    override suspend fun apply(context: CommandContext, input: NoInput): Applied {
        withContext(context.ioDispatcher) { ProjectUpgrader(context.project.rootDirectory).apply() }
        context.onProjectUpgraded()
        return Applied(inverse = null)
    }
}
