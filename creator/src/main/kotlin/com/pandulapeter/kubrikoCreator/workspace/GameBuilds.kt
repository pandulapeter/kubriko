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

import com.pandulapeter.kubriko.creatorSession.protocol.SessionMode
import com.pandulapeter.kubrikoCreator.scenes.ProjectClasspath
import com.pandulapeter.kubrikoCreator.session.ClassShape
import com.pandulapeter.kubrikoCreator.session.GameLaunchPreparation
import com.pandulapeter.kubrikoCreator.session.SessionInitScript
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File

/**
 * The builds behind playing the game: the one that says how to start it, with Compose Hot Reload when the project can,
 * and those that bring a game started so up to date with the changes made while it plays ([GameReloader]).
 *
 * @param runTask Runs one of the init script's tasks with Kubriko Creator's properties, returning whether it succeeded.
 */
internal class GameBuilds(
    private val rootDirectory: File,
    sessions: SessionController,
    private val runTask: suspend (taskName: String, properties: Map<String, String>) -> Boolean,
    console: ConsoleLog,
    scope: CoroutineScope,
    private val ioDispatcher: CoroutineDispatcher,
    currentTimeMillis: () -> Long,
) {
    private val reloader = GameReloader(
        scope = scope,
        buildClasses = ::buildProjectClasses,
        readShapes = { withContext(ioDispatcher) { ClassShape.read(it) } },
        isGameRunning = { sessions.state.first { it !is SessionState.Starting } == SessionState.Running(SessionMode.PLAY) },
        reload = { runTask(it, mapOf(MODE_PROPERTY to "reload")) },
        restartGame = sessions::restart,
        console = console,
        currentTimeMillis = currentTimeMillis,
    )

    /**
     * Builds the project's classpath for a play session, with how to start the game from it.
     */
    suspend fun prepareLaunch(): GameLaunchPreparation {
        val file = SessionInitScript.playClasspathFileOf(rootDirectory)
        if (!runTask(SessionInitScript.CLASSPATH_TASK_NAME, classpathPropertiesOf(file) + (HOT_RELOAD_PROPERTY to "true"))) return GameLaunchPreparation.Failed
        val (launch, shapes) = withContext(ioDispatcher) {
            val classpath = file.takeIf { it.isFile }?.readText()?.let(ProjectClasspath::parse)
            val launch = classpath?.launch?.let { launch ->
                classpath.hotReloadLaunch?.let { hot ->
                    hot.argumentFile.takeIf { it.isFile }?.readText()?.let { hot.applyTo(launch, it, classpath.files) }
                } ?: launch
            }
            launch to if (launch?.reloadTaskName == null) emptyMap() else ClassShape.read(classpath.ownClassFolders())
        }
        reloader.onGameStarting(launch?.reloadTaskName, shapes)
        return launch?.let(GameLaunchPreparation::Ready) ?: GameLaunchPreparation.Unavailable
    }

    /**
     * Applies the project's changes to the game that plays, if it was started with Compose Hot Reload.
     */
    fun onCodeChanged() = reloader.onCodeChanged()

    private suspend fun buildProjectClasses(): List<File>? {
        val file = SessionInitScript.classpathFileOf(rootDirectory)
        if (!runTask(SessionInitScript.CLASSPATH_TASK_NAME, classpathPropertiesOf(file))) return null
        return withContext(ioDispatcher) { file.takeIf { it.isFile }?.readText()?.let(ProjectClasspath::parse).ownClassFolders() }
    }

    private fun classpathPropertiesOf(file: File) = mapOf(MODE_PROPERTY to "classpath", "kubrikoCreatorClasspathFile" to file.absolutePath)

    private fun ProjectClasspath?.ownClassFolders() = this?.files.orEmpty().filter { it.isDirectory && it.canonicalFile.startsWith(rootDirectory.canonicalFile) }

    private companion object {
        const val MODE_PROPERTY = "kubrikoCreatorMode"
        const val HOT_RELOAD_PROPERTY = "kubrikoCreatorHotReload"
    }
}
