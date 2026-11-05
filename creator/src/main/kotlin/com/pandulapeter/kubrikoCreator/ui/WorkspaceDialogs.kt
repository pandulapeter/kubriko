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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.pandulapeter.kubrikoCreator.actors.BlockTypeDefinition
import com.pandulapeter.kubrikoCreator.commands.CommandHistory
import com.pandulapeter.kubrikoCreator.commands.Invocation
import com.pandulapeter.kubrikoCreator.commands.NoInput
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.commands.git.Commit
import com.pandulapeter.kubrikoCreator.commands.project.UpgradeProject
import com.pandulapeter.kubrikoCreator.context.Usage
import com.pandulapeter.kubrikoCreator.git.GitState
import com.pandulapeter.kubrikoCreator.scenes.SceneStore
import com.pandulapeter.kubrikoCreator.template.GameTargets
import com.pandulapeter.kubrikoCreator.template.TemplateUpgrade
import com.pandulapeter.kubrikoCreator.ui.settings.ProjectSettings
import kotlinx.coroutines.CancellationException
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
 * @param targets The platforms the game is built for, which Project settings turns on and off, or null if they can't be.
 * @param history The command history, after every change of which Project settings reads the settings again.
 * @param gitState The project's version control, collected only while the upgrade offers to commit first.
 * @param onCommand Dispatches a command.
 * @param usagesOf Reads what a delete would do to each usage of what it deletes.
 * @param onConfirmedDelete Dispatches a delete (or a change to a document) the user confirmed, with what runs once it was
 *   applied.
 * @param validate Why a change can't be made, or null, for what a confirmation lets the user still change.
 * @param onUsageSelected Goes to the place of a usage the delete's confirmation lists.
 */
@Composable
internal fun WorkspaceDialogs(
    uiState: WorkspaceUiState,
    projectUpgrade: TemplateUpgrade.Needed?,
    rootDirectory: File,
    blockTypes: StateFlow<List<BlockTypeDefinition>>?,
    scenes: SceneStore,
    targets: GameTargets?,
    history: StateFlow<CommandHistory>,
    gitState: StateFlow<GitState>,
    onCommand: (Invocation<*>) -> Unit,
    usagesOf: suspend (Invocation<*>) -> List<Usage>?,
    onConfirmedDelete: (Invocation<*>, () -> Unit) -> Unit,
    validate: (Invocation<*>) -> Rejection?,
    onUsageSelected: (Usage) -> Unit,
) {
    uiState.pendingDocumentChange?.let { pending ->
        DocumentChangeConfirmation(
            pending = pending,
            validate = validate,
            onConfirm = { confirmed ->
                uiState.finishDocumentChange()
                onConfirmedDelete(confirmed) { pending.onSuccess(confirmed) }
            },
            onDismiss = uiState::finishDocumentChange,
        )
    }
    uiState.pendingDelete?.let { pending ->
        PendingDeleteConfirmation(
            pending = pending,
            usagesOf = usagesOf,
            onConfirm = {
                uiState.finishDeleting()
                onConfirmedDelete(pending.invocation, pending.onSuccess)
            },
            onUsageSelected = { usage ->
                uiState.finishDeleting()
                onUsageSelected(usage)
            },
            onDismiss = uiState::finishDeleting,
        )
    }
    if (uiState.isShowingProjectSettings) {
        ProjectSettings(
            rootDirectory = rootDirectory,
            blockTypes = blockTypes,
            scenes = scenes,
            targets = targets,
            history = history,
            onCommand = onCommand,
            onReviewUpgrade = projectUpgrade?.let { uiState::reviewUpgrade },
            onDismiss = uiState::hideProjectSettings,
            initialPage = uiState.projectSettingsPage,
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

/**
 * The confirmation of [pending], with its usages once [usagesOf] has read them.
 */
@Composable
private fun PendingDeleteConfirmation(
    pending: PendingDelete,
    usagesOf: suspend (Invocation<*>) -> List<Usage>?,
    onConfirm: () -> Unit,
    onUsageSelected: (Usage) -> Unit,
    onDismiss: () -> Unit,
) {
    val subject = pending.invocation.deletedSubject ?: return
    var readProblem by remember(pending) { mutableStateOf<String?>(null) }
    val usages by produceState<List<Usage>?>(null, pending) {
        value = try {
            usagesOf(pending.invocation).orEmpty()
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            readProblem = exception.message ?: exception.toString()
            emptyList()
        }
    }
    DeleteConfirmation(
        subject = subject,
        usages = usages,
        readProblem = readProblem,
        onConfirm = onConfirm,
        onUsageSelected = onUsageSelected,
        onDismiss = onDismiss,
    )
}
