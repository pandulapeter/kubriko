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

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.pandulapeter.kubrikoCreator.AppScreen
import java.io.File

@Composable
internal fun AppContent(
    screen: AppScreen,
    onOpenProject: (File) -> Unit,
    onCloseProject: () -> Unit,
) = Surface(
    modifier = Modifier.fillMaxSize(),
    color = MaterialTheme.colorScheme.background,
) {
    when (screen) {
        is AppScreen.Welcome -> WelcomeScreen(
            error = screen.error,
            onOpenProject = onOpenProject,
        )
        is AppScreen.Opening -> OpeningScreen(
            directory = screen.directory,
        )
        is AppScreen.Workspace -> WorkspaceScreen(
            controller = screen.controller,
            onCloseProject = onCloseProject,
        )
    }
}
