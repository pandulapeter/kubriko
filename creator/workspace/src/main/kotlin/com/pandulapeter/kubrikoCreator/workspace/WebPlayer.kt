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

import com.pandulapeter.kubrikoCreator.console.ConsoleEntry
import com.pandulapeter.kubrikoCreator.session.WebGameServer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URI

/**
 * Plays the game in the browser: builds its web distribution, serves it from this computer, and opens the system's
 * browser on it. A change while it plays builds it again and reloads the page, which is the web's stand-in for hot
 * reload; a change during that build builds once more after it.
 *
 * @param runTask Runs a task of the game's module, returning whether it succeeded.
 * @param moduleDirectory The folder of the game's module, once a build of it worked.
 * @param openBrowser Opens the system's browser on an address, returning whether it could.
 * @param startServer Serves a folder, as [WebGameServer.start] does.
 */
internal class WebPlayer(
    private val runTask: suspend (taskName: String, properties: Map<String, String>) -> Boolean,
    private val moduleDirectory: suspend () -> File,
    private val openBrowser: (URI) -> Boolean,
    private val console: ConsoleLog,
    scope: CoroutineScope,
    private val ioDispatcher: CoroutineDispatcher,
    private val startServer: suspend (File) -> WebGameServer = WebGameServer::start,
) {
    @Volatile
    private var server: WebGameServer? = null
    private val rebuilds = Channel<Unit>(Channel.CONFLATED)

    init {
        scope.launch {
            for (rebuild in rebuilds) {
                if (server == null) continue
                try {
                    if (runTask(DISTRIBUTION_TASK_NAME, emptyMap())) {
                        server?.let { served ->
                            served.reload()
                            console.append(ConsoleEntry.Status(WorkspaceStatus.WEB_GAME_RELOADED))
                        }
                    }
                } catch (exception: CancellationException) {
                    throw exception
                } catch (exception: Exception) {
                    console.append(ConsoleEntry.Status(WorkspaceStatus.UNEXPECTED_PROBLEM, isError = true, arguments = listOf(exception.toString())))
                }
            }
        }
    }

    /**
     * Builds and serves the game, starting in [firstScene] (its first Scene when null), calls [onUp] once the console
     * says where and the browser is on it, and serves it until cancelled.
     *
     * @return False if it couldn't be built; it never returns otherwise.
     * @throws java.io.IOException If it can't be served.
     */
    suspend fun play(firstScene: String?, onUp: () -> Unit): Boolean {
        if (!runTask(DISTRIBUTION_TASK_NAME, emptyMap())) return false
        val directory = File(moduleDirectory(), DISTRIBUTION_PATH)
        if (!withContext(ioDispatcher) { File(directory, PAGE).isFile }) {
            console.append(ConsoleEntry.Status(WorkspaceStatus.WEB_GAME_MISSING, isError = true, arguments = listOf(directory.path)))
            return false
        }
        val started = startServer(directory)
        try {
            server = started
            val address = started.addressOf(firstScene)
            console.append(ConsoleEntry.Status(WorkspaceStatus.WEB_GAME_SERVED, arguments = listOf(address.toString())))
            if (!withContext(ioDispatcher) { openBrowser(address) }) {
                console.append(ConsoleEntry.Status(WorkspaceStatus.BROWSER_NOT_OPENED, arguments = listOf(address.toString())))
            }
            onUp()
            awaitCancellation()
        } finally {
            server = null
            withContext(NonCancellable + ioDispatcher) { started.close() }
        }
    }

    /**
     * Builds the game again and reloads the page, if it plays in the browser.
     */
    fun onCodeChanged() {
        if (server != null) rebuilds.trySend(Unit)
    }

    internal companion object {
        const val DISTRIBUTION_TASK_NAME = "wasmJsBrowserDevelopmentExecutableDistribution"

        /**
         * Where [DISTRIBUTION_TASK_NAME] puts the game, under its module.
         */
        val DISTRIBUTION_PATH = listOf("build", "dist", "wasmJs", "developmentExecutable").joinToString(File.separator)
        private const val PAGE = "index.html"
    }
}
