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
import com.pandulapeter.kubrikoCreator.commands.CommandProblem
import com.pandulapeter.kubrikoCreator.commands.CommandTarget
import com.pandulapeter.kubrikoCreator.commands.NoInput
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.commands.WriteCommand
import com.pandulapeter.kubrikoCreator.foundation.EditorMutation
import com.pandulapeter.kubrikoCreator.template.ProjectUpgrader
import com.pandulapeter.kubrikoCreator.template.TemplateUpgrade
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer

/**
 * Changes files that belong to the user, so it can't be undone from the history; committing first (or git) brings
 * them back.
 */
@OptIn(EditorMutation::class)
public object UpgradeProject : WriteCommand<NoInput> {
    override val name: String = "upgrade_project"
    override val description: String = "Makes the changes get_project_upgrade lists to the project's template files. They belong to the user: show them the " +
        "changes and ask first, and offer to commit before. This can't be undone, except through version control."
    override val inputSerializer: KSerializer<NoInput> = NoInput.serializer()
    override val isDestructive: Boolean = true

    override fun target(context: CommandContext, input: NoInput): CommandTarget? = CommandTarget.Project

    override fun validate(context: CommandContext, input: NoInput): Rejection? = rejectionOf(context.projectUpgrade())

    override suspend fun apply(context: CommandContext, input: NoInput): Applied {
        val rejection = withContext(context.ioDispatcher) {
            val upgrader = ProjectUpgrader(context.project.rootDirectory)
            rejectionOf(upgrader.check()) ?: run {
                upgrader.apply()
                null
            }
        }
        if (rejection != null) return Applied.failed(rejection)
        context.onProjectUpgraded()
        return Applied(inverse = null, isRewritingFiles = true)
    }

    private fun rejectionOf(upgrade: TemplateUpgrade) = when (upgrade) {
        TemplateUpgrade.UpToDate -> Rejection(CommandProblem.UPGRADE_NOT_NEEDED)
        is TemplateUpgrade.Needed -> if (upgrade.changes.isEmpty()) Rejection(CommandProblem.UPGRADE_BY_HAND, upgrade.byHand.joinToString()) else null
    }
}
