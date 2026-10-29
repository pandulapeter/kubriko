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
import com.pandulapeter.kubrikoCreator.ai.assistant.Assistant
import com.pandulapeter.kubrikoCreator.ai.assistant.AssistantController
import com.pandulapeter.kubrikoCreator.ai.assistant.AssistantSettingsStore
import com.pandulapeter.kubrikoCreator.ai.assistant.AssistantSystemPrompt
import com.pandulapeter.kubrikoCreator.ai.assistant.ClaudeAgentOptions
import com.pandulapeter.kubrikoCreator.ai.assistant.ProcessAgentLauncher
import com.pandulapeter.kubrikoCreator.ai.assistant.SystemSecretStore
import com.pandulapeter.kubrikoCreator.commands.CommandExecutor
import com.pandulapeter.kubrikoCreator.mcp.McpController
import com.pandulapeter.kubrikoCreator.mcp.McpSettingsStore
import com.pandulapeter.kubrikoCreator.mcp.McpToolServer
import com.pandulapeter.kubrikoCreator.mcp.clients.AiClientsController
import com.pandulapeter.kubrikoCreator.mcp.clients.ShellProcessRunner
import com.pandulapeter.kubrikoCreator.ui.AppContent
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
import kotlinx.coroutines.launch
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.ic_icon
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import java.awt.Dimension
import java.io.File

/**
 * Starts Kubriko Creator. A project folder passed as the first argument is opened right away.
 */
@OptIn(ExperimentalComposeUiApi::class)
fun main(args: Array<String>) {
    // A Gradle or IDE run has no app bundle, so macOS would show the Java launcher's name in the menu bar.
    System.setProperty("apple.awt.application.name", "Kubriko Creator")
    application {
        val scope = rememberCoroutineScope()
        val controller = remember { AppController(scope) }
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
            )
        }
        val aiClients = remember { AiClientsController(mcp = mcp, runner = ShellProcessRunner(), scope = scope, ioDispatcher = Dispatchers.IO) }
        val assistant = remember { assistantController(scope) { controller.openProjectCommands } }
        val screen by controller.screen.collectAsState()
        val openProjectDirectory = (screen as? AppScreen.Workspace)?.controller?.project?.rootDirectory
        LaunchedEffect(openProjectDirectory) { assistant.setProject(openProjectDirectory) }
        val recentProjects by controller.recentProjects.collectAsState()
        val defaultParentDirectory = remember { File(System.getProperty("user.home")) }
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
                    closing?.join()
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
                ProvideInterfaceScale {
                    CreatorTheme {
                        titleBar?.let { TitleBarAppearance(window = window, titleBar = it) }
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
                        )
                    }
                }
            }
        }
    }
}

/**
 * The assistant, whose agent runs Claude Agent through the user's `npx`, with tools of its own working on the project
 * [executor] gives.
 */
private fun assistantController(scope: CoroutineScope, executor: () -> CommandExecutor?): AssistantController {
    val version = CreatorConfiguration.load().kubrikoVersion
    return AssistantController(
        scope = scope,
        runner = ShellProcessRunner(),
        store = AssistantSettingsStore.default(),
        secrets = SystemSecretStore(),
        signInLauncher = ProcessAgentLauncher(File(System.getProperty("user.home"))),
        ioDispatcher = Dispatchers.IO,
        createAssistant = { command ->
            Assistant(
                agent = AcpAgent(
                    command = command,
                    launcher = ProcessAgentLauncher(File(System.getProperty("user.home"))),
                    clientInfo = AcpAgent.clientInfo(version),
                    scope = scope,
                    ioDispatcher = Dispatchers.IO,
                ),
                toolServer = McpToolServer(executor = executor, callDispatcher = Dispatchers.Main, version = version),
                ioDispatcher = Dispatchers.IO,
                sessionMeta = ClaudeAgentOptions.sessionMeta(AssistantSystemPrompt.text),
            )
        },
    )
}
