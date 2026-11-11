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
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.awt.SwingWindow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.LocalWindowExceptionHandlerFactory
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.pandulapeter.kubrikoCreator.ui.AppContent
import com.pandulapeter.kubrikoCreator.ui.components.LocalIsAiEnabled
import com.pandulapeter.kubrikoCreator.ui.theme.CreatorTheme
import com.pandulapeter.kubrikoCreator.ui.theme.ProvideInterfaceScale
import com.pandulapeter.kubrikoCreator.ui.windowTitle
import com.pandulapeter.kubrikoCreator.window.CreatorWindowExceptionHandler
import com.pandulapeter.kubrikoCreator.window.ExtendedTitleBar
import com.pandulapeter.kubrikoCreator.window.ProvideTrackpadPinches
import com.pandulapeter.kubrikoCreator.window.TitleBarAppearance
import com.pandulapeter.kubrikoCreator.window.TitleBarInsets
import com.pandulapeter.kubrikoCreator.window.extendContentIntoTitleBar
import com.pandulapeter.kubrikoCreator.window.rememberIsFullscreen
import kotlinx.coroutines.launch
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.ic_icon
import org.jetbrains.compose.resources.painterResource
import java.awt.Dimension
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

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
        var recoveries by remember { mutableIntStateOf(0) }
        val exceptionHandler = remember { CreatorWindowExceptionHandler(report = controller::reportUnexpectedProblem, onRecover = { recoveries++ }) }
        CompositionLocalProvider(LocalWindowExceptionHandlerFactory provides exceptionHandler.factory) {
            SwingWindow(
                title = screen.windowTitle(),
                icon = painterResource(Res.drawable.ic_icon),
                state = windowState,
                onCloseRequest = {
                    (screen as? AppScreen.Workspace).leaving {
                        if (isQuitting.compareAndSet(false, true)) {
                            graph.scope.launch {
                                graph.shutdown()
                                exitApplication()
                            }
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
                    key(recoveries) {
                        ProvideInterfaceScale(settings.interfaceScale) {
                            CreatorTheme(settings.theme) {
                                titleBar?.let { TitleBarAppearance(window = window, titleBar = it) }
                                CompositionLocalProvider(LocalIsAiEnabled provides settings.isAiEnabled) {
                                    ProvideTrackpadPinches(window.rootPane) {
                                    AppContent(
                                        screen = screen,
                                        recentProjects = recentProjects,
                                        defaultParentDirectory = defaultParentDirectory,
                                        onNewProject = { (screen as? AppScreen.Workspace).leaving { controller.showNewProject() } },
                                        onForgetRecentProject = controller::forgetRecentProject,
                                        onCreateProject = controller::createProject,
                                        onCancelCreatingProject = controller::cancelCreatingProject,
                                        onOpenProject = { file -> (screen as? AppScreen.Workspace).leaving { controller.openProject(file) } },
                                        onCloseProject = { (screen as? AppScreen.Workspace).leaving { controller.closeProject() } },
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
        }
    }
}

/**
 * Runs [proceed] once the workspace may be left (asking about the sprite editor's unsaved changes first), or at once when
 * no project is open.
 */
private fun AppScreen.Workspace?.leaving(proceed: () -> Unit) = if (this == null) proceed() else controller.selection.leaving(proceed)
