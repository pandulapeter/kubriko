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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.pandulapeter.kubrikoCreator.AppScreen
import com.pandulapeter.kubrikoCreator.ai.assistant.AssistantController
import com.pandulapeter.kubrikoCreator.mcp.McpController
import com.pandulapeter.kubrikoCreator.mcp.clients.AiClientsController
import com.pandulapeter.kubrikoCreator.recent.RecentProject
import com.pandulapeter.kubrikoCreator.settings.CreatorSettingsController
import com.pandulapeter.kubrikoCreator.template.NewProjectForm
import com.pandulapeter.kubrikoCreator.ui.assistant.agentLogOpening
import com.pandulapeter.kubrikoCreator.ui.components.DialogPage
import com.pandulapeter.kubrikoCreator.ui.components.PanelLayout
import com.pandulapeter.kubrikoCreator.ui.components.ProgressScreen
import com.pandulapeter.kubrikoCreator.ui.mcp.AiConnectionSettings
import com.pandulapeter.kubrikoCreator.ui.settings.AssistantSettingsPage
import com.pandulapeter.kubrikoCreator.ui.settings.CreatorSettingsDialog
import com.pandulapeter.kubrikoCreator.ui.settings.CreatorSettingsPage
import com.pandulapeter.kubrikoCreator.ui.settings.LocalOpenCreatorSettings
import com.pandulapeter.kubrikoCreator.ui.settings.OpenCreatorSettings
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.app_name
import kubriko.creator.generated.resources.ic_send
import kubriko.creator.generated.resources.ic_web
import kubriko.creator.generated.resources.opening_project
import kubriko.creator.generated.resources.settings_section_ai_connection
import kubriko.creator.generated.resources.settings_section_ai_connection_description
import kubriko.creator.generated.resources.settings_section_assistant
import kubriko.creator.generated.resources.settings_section_assistant_description
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
    onCreateProject: (NewProjectForm) -> Unit,
    onCancelCreatingProject: () -> Unit,
    onOpenProject: (File) -> Unit,
    onCloseProject: () -> Unit,
    mcp: McpController,
    aiClients: AiClientsController,
    assistant: AssistantController,
    creatorSettings: CreatorSettingsController,
    homeFolder: File,
) = Surface(
    modifier = Modifier.fillMaxSize(),
    color = MaterialTheme.colorScheme.surface,
) {
    var isShowingSettings by remember { mutableStateOf(false) }
    var settingsPage by remember { mutableStateOf<CreatorSettingsPage?>(null) }
    val openSettings = remember {
        OpenCreatorSettings { page ->
            settingsPage = page
            isShowingSettings = true
        }
    }
    val settings by creatorSettings.settings.collectAsState()
    val panelLayout = remember(settings.panelWidths, creatorSettings) {
        PanelLayout(settings.panelWidths, creatorSettings::setPanelSize, creatorSettings::resetPanelSize)
    }
    if (isShowingSettings) {
        CreatorSettingsDialog(
            settings = settings,
            homeFolder = homeFolder,
            onAiEnabledChange = creatorSettings::setAiEnabled,
            onThemeChange = creatorSettings::setTheme,
            onInterfaceScaleChange = creatorSettings::setInterfaceScale,
            onDefaultProjectFolderChange = creatorSettings::setDefaultProjectFolder,
            onFollowingAiChange = creatorSettings::setFollowingAi,
            onAiFollowDelayChange = creatorSettings::setAiFollowDelay,
            onTurnReviewShownChange = creatorSettings::setTurnReviewShown,
            onDismiss = { isShowingSettings = false },
            initialPage = settingsPage,
            aiPages = aiSettingsPages(mcp = mcp, aiClients = aiClients, assistant = assistant),
        )
    }
    Column(
        modifier = Modifier.fillMaxSize(),
    ) {
        TitleStrip(
            title = screen.windowTitle(),
        )
        Box(
            modifier = Modifier.weight(1f),
        ) {
            val aiActivitySettings = remember(settings, creatorSettings) { AiActivitySettings.of(settings, creatorSettings::setFollowingAi) }
            CompositionLocalProvider(
                LocalAiActivitySettings provides aiActivitySettings,
                LocalOpenCreatorSettings provides openSettings,
            ) {
                ScreenContent(
                screen = screen,
                recentProjects = recentProjects,
                defaultParentDirectory = defaultParentDirectory,
                onNewProject = onNewProject,
                onForgetRecentProject = onForgetRecentProject,
                onCreateProject = onCreateProject,
                onCancelCreatingProject = onCancelCreatingProject,
                onOpenProject = onOpenProject,
                onCloseProject = onCloseProject,
                mcp = mcp,
                aiClients = aiClients,
                assistant = assistant,
                    panelLayout = panelLayout,
                    onOpenSettings = {
                        settingsPage = null
                        isShowingSettings = true
                    },
                )
            }
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
    onCreateProject: (NewProjectForm) -> Unit,
    onCancelCreatingProject: () -> Unit,
    onOpenProject: (File) -> Unit,
    onCloseProject: () -> Unit,
    mcp: McpController,
    aiClients: AiClientsController,
    assistant: AssistantController,
    panelLayout: PanelLayout,
    onOpenSettings: () -> Unit,
) {
    when (screen) {
        is AppScreen.Welcome -> WelcomeScreen(
            error = screen.error,
            errorArguments = screen.errorArguments,
            recentProjects = recentProjects,
            onNewProject = onNewProject,
            onOpenProject = onOpenProject,
            onForgetRecentProject = onForgetRecentProject,
            onOpenSettings = onOpenSettings,
        )
        is AppScreen.NewProject -> NewProjectScreen(
            initialForm = screen.form,
            error = screen.error,
            errorArguments = screen.errorArguments,
            defaultParentDirectory = defaultParentDirectory,
            onCreate = onCreateProject,
            onCancel = onCloseProject,
        )
        is AppScreen.CreatingProject -> CreatingProjectScreen(
            directoryName = screen.directory.name,
            onCancel = onCancelCreatingProject,
        )
        is AppScreen.Opening -> ProgressScreen(
            text = stringResource(Res.string.opening_project, screen.directory.name),
            onCancel = onCloseProject,
        )
        is AppScreen.Workspace -> WorkspaceScreen(
            controller = screen.controller,
            mcp = mcp,
            aiClients = aiClients,
            assistant = assistant,
            panelLayout = panelLayout,
            onCloseProject = onCloseProject,
            onOpenSettings = onOpenSettings,
            writersRoom = screen.writersRoom,
        )
    }
}

/**
 * The pages of Creator settings about the assistant and the AI connection, from their controllers.
 */
@Composable
private fun aiSettingsPages(
    mcp: McpController,
    aiClients: AiClientsController,
    assistant: AssistantController,
) = listOf(
    DialogPage(
        key = CreatorSettingsPage.ASSISTANT.key,
        icon = Res.drawable.ic_send,
        title = stringResource(Res.string.settings_section_assistant),
        description = stringResource(Res.string.settings_section_assistant_description),
    ) {
        LaunchedEffect(Unit) { assistant.findConfigurations() }
        AssistantSettingsPage(
            agent = assistant.agent.collectAsState().value,
            agents = assistant.agents.collectAsState().value,
            customAgentCommand = assistant.customAgentCommand,
            changingFilesUnasked = assistant.agentsChangingFilesUnasked.collectAsState().value,
            configuration = assistant.configuration.collectAsState().value,
            configurations = assistant.configurations.collectAsState().value,
            onAgentSelected = { assistant.selectAgent(it, isPanelShown = false) },
            onConfigurationSelected = { assistant.selectConfiguration(it, isPanelShown = false) },
            onOpenLog = assistant.agentLogOpening(),
        )
    },
    DialogPage(
        key = CreatorSettingsPage.AI_CONNECTION.key,
        icon = Res.drawable.ic_web,
        title = stringResource(Res.string.settings_section_ai_connection),
        description = stringResource(Res.string.settings_section_ai_connection_description),
    ) {
        AiConnectionSettings(
            settings = mcp.settings.collectAsState().value,
            onPortChange = mcp::setPort,
            onRegenerateToken = mcp::regenerateToken,
            onCommandChange = aiClients::setCommand,
        )
    },
)

/**
 * The window's title: the open project's name, or the app's.
 */
@Composable
internal fun AppScreen.windowTitle() = (this as? AppScreen.Workspace)?.controller?.project?.name
    ?.let { stringResource(Res.string.window_title_project, it) }
    ?: stringResource(Res.string.app_name)
