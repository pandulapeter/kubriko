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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.pandulapeter.kubriko.uiComponents.theme.KubrikoTheme
import com.pandulapeter.kubrikoCreator.ui.AppContent
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.app_name
import org.jetbrains.compose.resources.stringResource
import java.io.File

/**
 * Starts Kubriko Creator. A project folder passed as the first argument is opened right away.
 */
fun main(args: Array<String>) = application {
    val scope = rememberCoroutineScope()
    val controller = remember { AppController(scope) }
    val screen by controller.screen.collectAsState()
    LaunchedEffect(Unit) {
        args.firstOrNull()?.let { controller.openProject(File(it)) }
    }
    Window(
        title = stringResource(Res.string.app_name),
        state = rememberWindowState(width = 1200.dp, height = 800.dp),
        onCloseRequest = {
            controller.closeProject()
            exitApplication()
        },
    ) {
        KubrikoTheme {
            AppContent(
                screen = screen,
                onOpenProject = controller::openProject,
                onCloseProject = controller::closeProject,
            )
        }
    }
}
