/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import com.pandulapeter.kubrikoCreator.commands.Invocation
import com.pandulapeter.kubrikoCreator.commands.NoInput
import com.pandulapeter.kubrikoCreator.commands.git.Commit
import com.pandulapeter.kubrikoCreator.commands.project.UpgradeProject
import com.pandulapeter.kubrikoCreator.git.GitState
import com.pandulapeter.kubrikoCreator.template.TemplateUpgrade
import com.pandulapeter.kubrikoCreator.ui.settings.ProjectSettings
import com.pandulapeter.kubrikoCreator.workspace.WorkspaceController
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.project_upgrade_commit_message
import org.jetbrains.compose.resources.stringResource

/**
 * The workspace's dialogs: Project settings and the template upgrade, while [uiState] shows them.
 *
 * @param projectUpgrade The template upgrade the project needs, if any.
 */
@Composable
internal fun WorkspaceDialogs(
    controller: WorkspaceController,
    uiState: WorkspaceUiState,
    projectUpgrade: TemplateUpgrade.Needed?,
) {
    if (uiState.isShowingProjectSettings) {
        ProjectSettings(
            rootDirectory = controller.project.rootDirectory,
            history = controller.commands.history,
            onCommand = controller.commands::dispatch,
            onReviewUpgrade = projectUpgrade?.let { uiState::reviewUpgrade },
            onDismiss = uiState::hideProjectSettings,
        )
    }
    val upgradeCommitMessage = stringResource(Res.string.project_upgrade_commit_message)
    if (projectUpgrade != null && uiState.isReviewingUpgrade) {
        ProjectUpgradeDialog(
            upgrade = projectUpgrade,
            onApply = {
                controller.commands.dispatch(Invocation(UpgradeProject, NoInput))
                uiState.finishReviewingUpgrade()
            },
            onCommitFirst = (controller.git.state.collectAsState().value as? GitState.Ready)?.takeIf { it.changes.isNotEmpty() }?.let {
                { controller.commands.dispatch(Invocation(Commit, Commit.Input(upgradeCommitMessage))) }
            },
            onDismiss = uiState::finishReviewingUpgrade,
        )
    }
}
