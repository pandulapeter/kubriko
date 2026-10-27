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
import com.pandulapeter.kubrikoCreator.template.NewProjectRequest
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.creating_project
import kubriko.creator.generated.resources.opening_project
import org.jetbrains.compose.resources.stringResource
import java.io.File

@Composable
internal fun AppContent(
    screen: AppScreen,
    defaultParentDirectory: File,
    onNewProject: () -> Unit,
    onCreateProject: (NewProjectRequest) -> Unit,
    onOpenProject: (File) -> Unit,
    onCloseProject: () -> Unit,
) = Surface(
    modifier = Modifier.fillMaxSize(),
    color = MaterialTheme.colorScheme.background,
) {
    when (screen) {
        is AppScreen.Welcome -> WelcomeScreen(
            error = screen.error,
            onNewProject = onNewProject,
            onOpenProject = onOpenProject,
        )
        is AppScreen.NewProject -> NewProjectScreen(
            error = screen.error,
            defaultParentDirectory = defaultParentDirectory,
            onCreate = onCreateProject,
            onCancel = onCloseProject,
        )
        is AppScreen.CreatingProject -> ProgressScreen(
            text = stringResource(Res.string.creating_project, screen.directory.name),
        )
        is AppScreen.Opening -> ProgressScreen(
            text = stringResource(Res.string.opening_project, screen.directory.name),
        )
        is AppScreen.Workspace -> WorkspaceScreen(
            controller = screen.controller,
            onCloseProject = onCloseProject,
        )
    }
}
