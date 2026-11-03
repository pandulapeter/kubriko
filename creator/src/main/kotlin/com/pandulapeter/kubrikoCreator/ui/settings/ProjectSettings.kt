/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.pandulapeter.kubrikoCreator.actors.BlockTypeDefinition
import com.pandulapeter.kubrikoCreator.commands.CommandHistory
import com.pandulapeter.kubrikoCreator.commands.Invocation
import com.pandulapeter.kubrikoCreator.commands.project.SetProjectSettings
import com.pandulapeter.kubrikoCreator.commands.project.SetTarget
import com.pandulapeter.kubrikoCreator.project.LibraryInUse
import com.pandulapeter.kubrikoCreator.project.ProjectSettingsSnapshot
import com.pandulapeter.kubrikoCreator.scenes.SceneStore
import com.pandulapeter.kubrikoCreator.template.GameTarget
import com.pandulapeter.kubrikoCreator.template.GameTargets
import com.pandulapeter.kubrikoCreator.template.TargetChange
import com.pandulapeter.kubrikoCreator.template.TargetState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

/**
 * [ProjectSettingsDialog] for the project in [rootDirectory], reading its settings again after every change to the
 * project (undo included), and each time it opens, so that a value changed by hand in the file shows. The libraries in use
 * are read when it opens and when the types change.
 *
 * @param blockTypes The project's Block-built types, which say why it uses its libraries.
 * @param scenes The project's Scenes, which the game can start with.
 * @param targets The platforms the game is built for, or null if they can't be changed here.
 */
@Composable
internal fun ProjectSettings(
    rootDirectory: File,
    blockTypes: StateFlow<List<BlockTypeDefinition>>?,
    scenes: SceneStore,
    targets: GameTargets?,
    history: StateFlow<CommandHistory>,
    onCommand: (Invocation<*>) -> Unit,
    onReviewUpgrade: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    val currentHistory by history.collectAsState()
    val settings by produceState<Result<ProjectSettingsSnapshot>?>(null, rootDirectory, currentHistory) {
        value = try {
            Result.success(withContext(Dispatchers.IO) { ProjectSettingsSnapshot.readSettings(rootDirectory, scenes) })
        } catch (exception: IOException) {
            Result.failure(exception)
        }
    }
    val definitions = blockTypes?.collectAsState()?.value.orEmpty()
    // Reading the libraries walks every source, so it follows the types rather than each keystroke in a field.
    val libraries by produceState(emptyList(), rootDirectory, definitions) {
        value = withContext(Dispatchers.IO) { LibraryInUse.read(rootDirectory, definitions, scenes) }
    }
    val targetStates by produceState<List<TargetState>?>(null, targets, currentHistory) {
        value = targets?.let { readTargets(it) }
    }
    var pendingTarget by remember { mutableStateOf<PendingTargetChange?>(null) }
    val scope = rememberCoroutineScope()
    ProjectSettingsDialog(
        settings = settings?.getOrNull()?.copy(libraries = libraries),
        failure = settings?.exceptionOrNull(),
        targets = targetStates,
        onChange = { onCommand(Invocation(SetProjectSettings, it)) },
        onToggleTarget = { target, isOn ->
            targets?.let { scope.launch { pendingTarget = changeOf(it, target, isOn) } }
        },
        onReviewUpgrade = onReviewUpgrade,
        onDismiss = onDismiss,
    )
    pendingTarget?.let { change ->
        TargetChangeDialog(
            change = change,
            onApply = {
                onCommand(Invocation(SetTarget, SetTarget.Input(change.target, change.isOn, changeId = (change.result as? TargetChange.Proposed)?.id)))
                pendingTarget = null
            },
            onDismiss = { pendingTarget = null },
        )
    }
}

private suspend fun readTargets(targets: GameTargets) = try {
    withContext(Dispatchers.IO) { targets.read() }
} catch (_: IOException) {
    null
}

private suspend fun changeOf(targets: GameTargets, target: GameTarget, isOn: Boolean) = try {
    PendingTargetChange(target, isOn, withContext(Dispatchers.IO) { targets.change(target, isOn) })
} catch (_: IOException) {
    null
}
