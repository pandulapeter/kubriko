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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.pandulapeter.kubrikoCreator.AppScreen
import com.pandulapeter.kubrikoCreator.ai.assistant.AssistantController
import com.pandulapeter.kubrikoCreator.mcp.McpController
import com.pandulapeter.kubrikoCreator.mcp.clients.AiClientsController
import com.pandulapeter.kubrikoCreator.recent.RecentProject
import com.pandulapeter.kubrikoCreator.template.NewProjectRequest
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.app_name
import kubriko.creator.generated.resources.creating_project
import kubriko.creator.generated.resources.opening_project
import kubriko.creator.generated.resources.window_title_project
import org.jetbrains.compose.resources.stringResource
import java.io.File

@Composable
internal fun AppContent(
    screen: AppScreen,
    recentProjects: List<RecentProject>,
    defaultParentDirectory: File,
    onNewProject: () -> Unit,
    onForgetRecentProject: (RecentProject) -> Unit,
    onCreateProject: (NewProjectRequest) -> Unit,
    onOpenProject: (File) -> Unit,
    onCloseProject: () -> Unit,
    mcp: McpController,
    aiClients: AiClientsController,
    assistant: AssistantController,
) = Surface(
    modifier = Modifier.fillMaxSize(),
    color = MaterialTheme.colorScheme.surface,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
    ) {
        TitleStrip(
            title = screen.windowTitle(),
        )
        Box(
            modifier = Modifier.weight(1f),
        ) {
            ScreenContent(
                screen = screen,
                recentProjects = recentProjects,
                defaultParentDirectory = defaultParentDirectory,
                onNewProject = onNewProject,
                onForgetRecentProject = onForgetRecentProject,
                onCreateProject = onCreateProject,
                onOpenProject = onOpenProject,
                onCloseProject = onCloseProject,
                mcp = mcp,
                aiClients = aiClients,
                assistant = assistant,
            )
        }
    }
}

@Composable
private fun ScreenContent(
    screen: AppScreen,
    recentProjects: List<RecentProject>,
    defaultParentDirectory: File,
    onNewProject: () -> Unit,
    onForgetRecentProject: (RecentProject) -> Unit,
    onCreateProject: (NewProjectRequest) -> Unit,
    onOpenProject: (File) -> Unit,
    onCloseProject: () -> Unit,
    mcp: McpController,
    aiClients: AiClientsController,
    assistant: AssistantController,
) {
    when (screen) {
        is AppScreen.Welcome -> WelcomeScreen(
            error = screen.error,
            recentProjects = recentProjects,
            onNewProject = onNewProject,
            onOpenProject = onOpenProject,
            onForgetRecentProject = onForgetRecentProject,
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
            mcp = mcp,
            aiClients = aiClients,
            assistant = assistant,
            onCloseProject = onCloseProject,
        )
    }
}

/**
 * The window's title: the open project's name, or the app's.
 */
@Composable
internal fun AppScreen.windowTitle() = (this as? AppScreen.Workspace)?.controller?.project?.name
    ?.let { stringResource(Res.string.window_title_project, it) }
    ?: stringResource(Res.string.app_name)
