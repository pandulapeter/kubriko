/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.workspace
import com.pandulapeter.kubrikoCreator.gradle.ProjectBuild
import com.pandulapeter.kubrikoCreator.session.GameLaunch
import com.pandulapeter.kubrikoCreator.session.GameProcess
import com.pandulapeter.kubrikoCreator.session.SessionChannel
import java.net.URI

/**
 * The outside processes the workspace builds and runs the project through, each closed or stopped with it.
 *
 * @param build The project's Gradle connection, which runs every build and task.
 * @param sessionChannel The channel the game's session process reports to.
 * @param runGame Starts the game process Creator runs itself ([GameProcess]).
 * @param findDesktopModulePath Asks [build] for the Gradle path of the module that runs the game on desktop.
 * @param openBrowser Opens the system's browser on an address, where the game plays on the web, returning whether it could.
 * @param tools Runs the command-line tools the game is played on a device through.
 */
public class ProjectProcesses(
    internal val build: ProjectBuild,
    internal val sessionChannel: SessionChannel,
    internal val runGame: suspend (GameLaunch, Map<String, String>) -> GameProcess.Exit,
    internal val findDesktopModulePath: suspend () -> String = { ":" },
    internal val openBrowser: (URI) -> Boolean = SystemBrowser::open,
    internal val tools: ToolProcesses = SystemToolProcesses,
)
