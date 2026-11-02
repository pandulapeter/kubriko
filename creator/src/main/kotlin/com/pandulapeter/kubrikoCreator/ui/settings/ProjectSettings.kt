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
import com.pandulapeter.kubrikoCreator.commands.CommandHistory
import com.pandulapeter.kubrikoCreator.commands.Invocation
import com.pandulapeter.kubrikoCreator.commands.project.SetProjectSettings
import com.pandulapeter.kubrikoCreator.project.GameIdentity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

/**
 * [ProjectSettingsDialog] for the project in [rootDirectory], reading its settings again after every change to the
 * project (undo included), and each time it opens, so that a value changed by hand in the file shows.
 */
@Composable
internal fun ProjectSettings(
    rootDirectory: File,
    history: StateFlow<CommandHistory>,
    onCommand: (Invocation<*>) -> Unit,
    onReviewUpgrade: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    val currentHistory by history.collectAsState()
    val identity by produceState<Result<GameIdentity?>?>(null, rootDirectory, currentHistory) {
        value = try {
            Result.success(withContext(Dispatchers.IO) { GameIdentity.read(rootDirectory) })
        } catch (exception: IOException) {
            Result.failure(exception)
        }
    }
    ProjectSettingsDialog(
        identity = identity?.getOrNull(),
        isLoaded = identity != null,
        failure = identity?.exceptionOrNull(),
        onChange = { onCommand(Invocation(SetProjectSettings, it)) },
        onReviewUpgrade = onReviewUpgrade,
        onDismiss = onDismiss,
    )
}
