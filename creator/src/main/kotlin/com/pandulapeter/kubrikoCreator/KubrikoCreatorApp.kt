/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.awt.SwingWindow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.pandulapeter.kubrikoCreator.ai.assistant.AcpAgent
import com.pandulapeter.kubrikoCreator.ai.assistant.AgentErrorLog
import com.pandulapeter.kubrikoCreator.ai.assistant.AgentRegistryStore
import com.pandulapeter.kubrikoCreator.ai.assistant.Assistant
import com.pandulapeter.kubrikoCreator.ai.assistant.AssistantController
import com.pandulapeter.kubrikoCreator.ai.assistant.AssistantSettingsStore
import com.pandulapeter.kubrikoCreator.ai.assistant.AssistantSystemPrompt
import com.pandulapeter.kubrikoCreator.ai.assistant.BinaryAgentInstaller
import com.pandulapeter.kubrikoCreator.ai.assistant.NodeRuntime
import com.pandulapeter.kubrikoCreator.ai.assistant.ProcessAgentLauncher
import com.pandulapeter.kubrikoCreator.ai.assistant.SavedConversationStore
import com.pandulapeter.kubrikoCreator.ai.assistant.SystemSecretStore
import com.pandulapeter.kubrikoCreator.commands.CommandExecutor
import com.pandulapeter.kubrikoCreator.context.ConversationStart
import com.pandulapeter.kubrikoCreator.context.projectDigest
import com.pandulapeter.kubrikoCreator.foundation.CreatorConfiguration
import com.pandulapeter.kubrikoCreator.mcp.McpController
import com.pandulapeter.kubrikoCreator.mcp.McpSettingsStore
import com.pandulapeter.kubrikoCreator.mcp.McpToolServer
import com.pandulapeter.kubrikoCreator.mcp.clients.AiClientsController
import com.pandulapeter.kubrikoCreator.mcp.clients.ShellProcessRunner
import com.pandulapeter.kubrikoCreator.settings.CreatorSettingsController
import com.pandulapeter.kubrikoCreator.settings.CreatorSettingsStore
import com.pandulapeter.kubrikoCreator.ui.AppContent
import com.pandulapeter.kubrikoCreator.ui.LocalIsAiEnabled
import com.pandulapeter.kubrikoCreator.ui.theme.CreatorTheme
import com.pandulapeter.kubrikoCreator.ui.windowTitle
import com.pandulapeter.kubrikoCreator.window.ExtendedTitleBar
import com.pandulapeter.kubrikoCreator.window.ProvideInterfaceScale
import com.pandulapeter.kubrikoCreator.window.TitleBarAppearance
import com.pandulapeter.kubrikoCreator.window.TitleBarInsets
import com.pandulapeter.kubrikoCreator.window.extendContentIntoTitleBar
import com.pandulapeter.kubrikoCreator.window.rememberIsFullscreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.ic_icon
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import java.awt.Dimension
import java.io.File

/**
 * How long quitting waits for the open project to finish writing, so that a write that never ends can't keep the window open.
 */
private const val CLOSING_TIMEOUT_IN_MILLISECONDS = 10_000L

/**
 * Starts Kubriko Creator. A project folder passed as the first argument is opened right away.
 */
@OptIn(ExperimentalComposeUiApi::class)
fun main(args: Array<String>) {
    // A Gradle or IDE run has no app bundle, so macOS would show the Java launcher's name in the menu bar.
    System.setProperty("apple.awt.application.name", "Kubriko Creator")
    application {
        val compositionScope = rememberCoroutineScope()
        val controller = remember { AppController(compositionScope) }
        val scope = controller.scope
        val creatorSettings = remember { CreatorSettingsController(CreatorSettingsStore.default(), scope, Dispatchers.IO) }
        val settings by creatorSettings.settings.collectAsState()
        val mcp = remember {
            McpController(
                server = McpToolServer(
                    executor = { controller.openProjectCommands },
                    callDispatcher = Dispatchers.Main,
                    version = CreatorConfiguration.load().kubrikoVersion,
                ),
                store = McpSettingsStore.default(),
                scope = scope,
                ioDispatcher = Dispatchers.IO,
                isAiEnabled = creatorSettings.isAiEnabled,
            )
        }
        val aiClients = remember { AiClientsController(mcp = mcp, runner = ShellProcessRunner(), scope = scope, ioDispatcher = Dispatchers.IO) }
        val assistant = remember { assistantController(scope, creatorSettings.isAiEnabled) { controller.openProjectCommands } }
        val screen by controller.screen.collectAsState()
        val openProjectDirectory = (screen as? AppScreen.Workspace)?.controller?.project?.rootDirectory
        LaunchedEffect(openProjectDirectory) { assistant.setProject(openProjectDirectory) }
        val recentProjects by controller.recentProjects.collectAsState()
        val homeFolder = remember { File(System.getProperty("user.home")) }
        val defaultParentDirectory = settings.defaultProjectFolder?.let(::File) ?: homeFolder
        val windowState = rememberWindowState(width = 1280.dp, height = 820.dp)
        var titleBar by remember { mutableStateOf<ExtendedTitleBar?>(null) }
        LaunchedEffect(Unit) {
            args.firstOrNull()?.let { controller.openProject(File(it)) }
        }
        SwingWindow(
            title = screen.windowTitle(),
            icon = painterResource(Res.drawable.ic_icon),
            state = windowState,
            onCloseRequest = {
                mcp.close()
                assistant.close()
                val closing = controller.closeProject()
                scope.launch {
                    withTimeoutOrNull(CLOSING_TIMEOUT_IN_MILLISECONDS) { closing?.join() }
                    exitApplication()
                }
            },
            init = { window ->
                titleBar = window.extendContentIntoTitleBar()
                window.minimumSize = Dimension(960, 600)
            },
        ) {
            TitleBarInsets(
                titleBar = titleBar,
                isFullscreen = rememberIsFullscreen(window = window, windowState = windowState),
            ) {
                ProvideInterfaceScale(settings.interfaceScale) {
                    CreatorTheme(settings.theme) {
                        titleBar?.let { TitleBarAppearance(window = window, titleBar = it) }
                        CompositionLocalProvider(LocalIsAiEnabled provides settings.isAiEnabled) {
                            AppContent(
                            screen = screen,
                            recentProjects = recentProjects,
                            defaultParentDirectory = defaultParentDirectory,
                            onNewProject = controller::showNewProject,
                            onForgetRecentProject = controller::forgetRecentProject,
                            onCreateProject = controller::createProject,
                            onOpenProject = controller::openProject,
                            onCloseProject = controller::closeProject,
                            mcp = mcp,
                            aiClients = aiClients,
                            assistant = assistant,
                            creatorSettings = creatorSettings,
                            homeFolder = homeFolder,
                        )
                        }
                    }
                }
            }
        }
    }
}

/**
 * The assistant, whose agent runs Claude Agent through the user's `npx`, with tools of its own working on the project
 * [executor] gives, while [isAiEnabled].
 */
private fun assistantController(scope: CoroutineScope, isAiEnabled: StateFlow<Boolean>, executor: () -> CommandExecutor?): AssistantController {
    val version = CreatorConfiguration.load().kubrikoVersion
    val errorLog = AgentErrorLog.default()
    val installer = BinaryAgentInstaller.default()
    return AssistantController(
        scope = scope,
        runner = ShellProcessRunner(),
        store = AssistantSettingsStore.default(),
        secrets = SystemSecretStore(),
        signInLauncher = ProcessAgentLauncher(File(System.getProperty("user.home"))),
        ioDispatcher = Dispatchers.IO,
        createAssistant = { command, npx, profile ->
            Assistant(
                agent = AcpAgent(
                    command = command,
                    launcher = ProcessAgentLauncher(File(System.getProperty("user.home"))),
                    clientInfo = AcpAgent.clientInfo(version),
                    scope = scope,
                    ioDispatcher = Dispatchers.IO,
                    errorLog = errorLog,
                ),
                toolServer = McpToolServer(executor = executor, callDispatcher = Dispatchers.Main, version = version),
                ioDispatcher = Dispatchers.IO,
                profile = profile,
                systemPrompt = AssistantSystemPrompt::withProject,
                npx = npx,
            )
        },
        projectDigest = { executor()?.context?.let { ConversationStart.of(it) } },
        changeLog = { executor()?.changeLog },
        undoTurn = { toolsId, sinceRevision -> executor()?.undoTurn(toolsId, sinceRevision) ?: 0 },
        registry = AgentRegistryStore.default(),
        installer = installer,
        node = NodeRuntime(installer, File(BinaryAgentInstaller.defaultDirectory(), NodeRuntime.NODE_ID)),
        errorLogFile = errorLog.file,
        conversations = SavedConversationStore.default(),
        isAiEnabled = isAiEnabled,
    )
}
