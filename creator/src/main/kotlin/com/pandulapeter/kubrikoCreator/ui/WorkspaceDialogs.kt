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
import com.pandulapeter.kubrikoCreator.actors.BlockTypeDefinition
import com.pandulapeter.kubrikoCreator.commands.CommandHistory
import com.pandulapeter.kubrikoCreator.commands.Invocation
import com.pandulapeter.kubrikoCreator.commands.NoInput
import com.pandulapeter.kubrikoCreator.commands.git.Commit
import com.pandulapeter.kubrikoCreator.commands.project.UpgradeProject
import com.pandulapeter.kubrikoCreator.git.GitState
import com.pandulapeter.kubrikoCreator.scenes.SceneStore
import com.pandulapeter.kubrikoCreator.template.TemplateUpgrade
import com.pandulapeter.kubrikoCreator.ui.settings.ProjectSettings
import kotlinx.coroutines.flow.StateFlow
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.project_upgrade_commit_message
import org.jetbrains.compose.resources.stringResource
import java.io.File

/**
 * The workspace's dialogs: Project settings and the template upgrade, while [uiState] shows them.
 *
 * @param projectUpgrade The template upgrade the project needs, if any.
 * @param rootDirectory The project's folder, whose settings Project settings shows.
 * @param blockTypes The project's Block-built types, which Project settings says its libraries are used by.
 * @param scenes The project's Scenes, which Project settings offers as the first one.
 * @param history The command history, after every change of which Project settings reads the settings again.
 * @param gitState The project's version control, collected only while the upgrade offers to commit first.
 * @param onCommand Dispatches a command.
 */
@Composable
internal fun WorkspaceDialogs(
    uiState: WorkspaceUiState,
    projectUpgrade: TemplateUpgrade.Needed?,
    rootDirectory: File,
    blockTypes: StateFlow<List<BlockTypeDefinition>>?,
    scenes: SceneStore,
    history: StateFlow<CommandHistory>,
    gitState: StateFlow<GitState>,
    onCommand: (Invocation<*>) -> Unit,
) {
    if (uiState.isShowingProjectSettings) {
        ProjectSettings(
            rootDirectory = rootDirectory,
            blockTypes = blockTypes,
            scenes = scenes,
            history = history,
            onCommand = onCommand,
            onReviewUpgrade = projectUpgrade?.let { uiState::reviewUpgrade },
            onDismiss = uiState::hideProjectSettings,
        )
    }
    val upgradeCommitMessage = stringResource(Res.string.project_upgrade_commit_message)
    if (projectUpgrade != null && uiState.isReviewingUpgrade) {
        ProjectUpgradeDialog(
            upgrade = projectUpgrade,
            onApply = {
                onCommand(Invocation(UpgradeProject, NoInput))
                uiState.finishReviewingUpgrade()
            },
            onCommitFirst = (gitState.collectAsState().value as? GitState.Ready)?.takeIf { it.changes.isNotEmpty() }?.let {
                { onCommand(Invocation(Commit, Commit.Input(upgradeCommitMessage))) }
            },
            onDismiss = uiState::finishReviewingUpgrade,
        )
    }
}
