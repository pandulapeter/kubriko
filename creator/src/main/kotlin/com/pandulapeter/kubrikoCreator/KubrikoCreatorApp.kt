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
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.awt.SwingWindow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.pandulapeter.kubrikoCreator.ui.AppContent
import com.pandulapeter.kubrikoCreator.ui.components.LocalIsAiEnabled
import com.pandulapeter.kubrikoCreator.ui.theme.CreatorTheme
import com.pandulapeter.kubrikoCreator.ui.windowTitle
import com.pandulapeter.kubrikoCreator.window.ExtendedTitleBar
import com.pandulapeter.kubrikoCreator.window.ProvideInterfaceScale
import com.pandulapeter.kubrikoCreator.window.TitleBarAppearance
import com.pandulapeter.kubrikoCreator.window.TitleBarInsets
import com.pandulapeter.kubrikoCreator.window.extendContentIntoTitleBar
import com.pandulapeter.kubrikoCreator.window.rememberIsFullscreen
import com.pandulapeter.kubrikoCreator.workspace.CLOSE_WRITE_TIMEOUT_IN_MILLISECONDS
import kotlinx.coroutines.launch
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.ic_icon
import org.jetbrains.compose.resources.painterResource
import java.awt.Dimension
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

/**
 * How long quitting gives the open project to close after its wait for a failed write ([CLOSE_WRITE_TIMEOUT_IN_MILLISECONDS])
 * ran out: stopping its work and closing its Gradle connection.
 */
private const val CLOSE_WORK_MARGIN_IN_MILLISECONDS = 5_000L

/**
 * How long quitting waits for the open project to close, so that a write that never ends can't keep the window open. It
 * outlasts the project's own wait for its writes, so the project is always closed before the app exits.
 */
internal const val CLOSING_TIMEOUT_IN_MILLISECONDS = CLOSE_WRITE_TIMEOUT_IN_MILLISECONDS + CLOSE_WORK_MARGIN_IN_MILLISECONDS

/**
 * Starts Kubriko Creator. A project folder passed as the first argument is opened right away.
 */
@OptIn(ExperimentalComposeUiApi::class)
fun main(args: Array<String>) {
    // A Gradle or IDE run has no app bundle, so macOS would show the Java launcher's name in the menu bar.
    System.setProperty("apple.awt.application.name", "Kubriko Creator")
    val graph = AppGraph.create()
    val isQuitting = AtomicBoolean(false)
    application {
        val controller = graph.controller
        val settings by graph.creatorSettings.settings.collectAsState()
        val screen by controller.screen.collectAsState()
        val openProjectDirectory = (screen as? AppScreen.Workspace)?.controller?.project?.rootDirectory
        LaunchedEffect(openProjectDirectory) { graph.assistant.setProject(openProjectDirectory) }
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
                if (isQuitting.compareAndSet(false, true)) {
                    graph.scope.launch {
                        graph.shutdown()
                        exitApplication()
                    }
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
                                mcp = graph.mcp,
                                aiClients = graph.aiClients,
                                assistant = graph.assistant,
                                creatorSettings = graph.creatorSettings,
                                homeFolder = homeFolder,
                            )
                        }
                    }
                }
            }
        }
    }
}
