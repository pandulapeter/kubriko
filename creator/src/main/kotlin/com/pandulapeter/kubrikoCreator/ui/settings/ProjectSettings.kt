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
import androidx.compose.runtime.produceState
import com.pandulapeter.kubrikoCreator.actors.BlockTypeDefinition
import com.pandulapeter.kubrikoCreator.commands.CommandHistory
import com.pandulapeter.kubrikoCreator.commands.Invocation
import com.pandulapeter.kubrikoCreator.commands.project.SetProjectSettings
import com.pandulapeter.kubrikoCreator.project.LibraryInUse
import com.pandulapeter.kubrikoCreator.project.ProjectSettingsSnapshot
import com.pandulapeter.kubrikoCreator.scenes.SceneStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
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
 */
@Composable
internal fun ProjectSettings(
    rootDirectory: File,
    blockTypes: StateFlow<List<BlockTypeDefinition>>?,
    scenes: SceneStore,
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
    ProjectSettingsDialog(
        settings = settings?.getOrNull()?.copy(libraries = libraries),
        failure = settings?.exceptionOrNull(),
        onChange = { onCommand(Invocation(SetProjectSettings, it)) },
        onReviewUpgrade = onReviewUpgrade,
        onDismiss = onDismiss,
    )
}
