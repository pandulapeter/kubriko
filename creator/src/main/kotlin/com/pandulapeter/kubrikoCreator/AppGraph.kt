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

import com.pandulapeter.kubrikoCreator.ai.assistant.AcpAgent
import com.pandulapeter.kubrikoCreator.ai.assistant.AgentErrorLog
import com.pandulapeter.kubrikoCreator.ai.assistant.AgentSources
import com.pandulapeter.kubrikoCreator.ai.assistant.Assistant
import com.pandulapeter.kubrikoCreator.ai.assistant.AssistantController
import com.pandulapeter.kubrikoCreator.ai.assistant.AssistantStorage
import com.pandulapeter.kubrikoCreator.ai.assistant.AssistantSystemPrompt
import com.pandulapeter.kubrikoCreator.ai.assistant.OpenProjectAccess
import com.pandulapeter.kubrikoCreator.ai.assistant.ProcessAgentLauncher
import com.pandulapeter.kubrikoCreator.commands.CommandExecutor
import com.pandulapeter.kubrikoCreator.foundation.CreatorConfiguration
import com.pandulapeter.kubrikoCreator.mcp.McpController
import com.pandulapeter.kubrikoCreator.mcp.McpSettingsStore
import com.pandulapeter.kubrikoCreator.mcp.McpToolServer
import com.pandulapeter.kubrikoCreator.mcp.clients.AiClientsController
import com.pandulapeter.kubrikoCreator.mcp.clients.ShellProcessRunner
import com.pandulapeter.kubrikoCreator.settings.CreatorSettingsController
import com.pandulapeter.kubrikoCreator.settings.CreatorSettingsStore
import com.pandulapeter.kubrikoCreator.template.KubrikoAvailability
import com.pandulapeter.kubrikoCreator.template.ProjectGenerator
import com.pandulapeter.kubrikoCreator.workspace.GradleWorkspaceOpener
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File

/**
 * Kubriko Creator's app-wide parts, made once before the window and closed in order when it quits ([shutdown]).
 *
 * @param scope Where everything app-wide runs, on the main thread: [AppController.scope], which reports what one of its
 *   coroutines throws unexpectedly.
 */
internal class AppGraph(
    val scope: CoroutineScope,
    val controller: AppController,
    val creatorSettings: CreatorSettingsController,
    val mcp: McpController,
    val aiClients: AiClientsController,
    val assistant: AssistantController,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    /**
     * Closes everything within [timeoutInMilliseconds]: the AI server and agent (on [ioDispatcher], since they wait for
     * processes), the assistant's pending restarts, and the open project's writes side by side, then the settings' last
     * save. Called on the main thread.
     */
    suspend fun shutdown(timeoutInMilliseconds: Long = AppController.CLOSING_TIMEOUT_IN_MILLISECONDS) = shutDownSideBySide(
        timeoutInMilliseconds = timeoutInMilliseconds,
        ioDispatcher = ioDispatcher,
        offMainThread = listOf(mcp::close, assistant::close),
        onMainThread = listOf(assistant::awaitPending, ::closeProject),
        last = creatorSettings::awaitSaves,
    )

    private suspend fun closeProject() {
        controller.closeProject()?.join()
    }

    companion object {
        /**
         * The production wiring, run once in `main` before the window exists, so the settings stores' reads happen there
         * rather than in the first composition.
         */
        fun create(): AppGraph {
            val configuration = CreatorConfiguration.load()
            val isKubrikoAvailable = KubrikoAvailability.of(configuration)
            val controller = AppController(
                parentScope = CoroutineScope(SupervisorJob() + Dispatchers.Main),
                workspaceOpener = GradleWorkspaceOpener(configuration, isKubrikoAvailable = isKubrikoAvailable),
                projectCreator = ProjectGenerator(configuration, isKubrikoAvailable = isKubrikoAvailable),
            )
            val scope = controller.scope
            val creatorSettings = CreatorSettingsController(CreatorSettingsStore.default(), scope, Dispatchers.IO)
            val mcp = McpController(
                server = McpToolServer(
                    executor = { controller.openProjectCommands },
                    callDispatcher = Dispatchers.Main,
                    version = configuration.kubrikoVersion,
                ),
                store = McpSettingsStore.default(),
                scope = scope,
                ioDispatcher = Dispatchers.IO,
                isAiEnabled = creatorSettings.isAiEnabled,
            )
            return AppGraph(
                scope = scope,
                controller = controller,
                creatorSettings = creatorSettings,
                mcp = mcp,
                aiClients = AiClientsController(mcp = mcp, runner = ShellProcessRunner(), scope = scope, ioDispatcher = Dispatchers.IO),
                assistant = assistantController(configuration.kubrikoVersion, scope, creatorSettings.isAiEnabled) { controller.openProjectCommands },
            )
        }

        /**
         * The assistant, whose agent runs Claude Agent through the user's `npx`, with tools of its own working on the
         * project [executor] gives, while [isAiEnabled].
         */
        private fun assistantController(
            version: String,
            scope: CoroutineScope,
            isAiEnabled: StateFlow<Boolean>,
            executor: () -> CommandExecutor?,
        ): AssistantController {
            val errorLog = AgentErrorLog.default()
            return AssistantController(
                scope = scope,
                runner = ShellProcessRunner(),
                storage = AssistantStorage.default(errorLogFile = errorLog.file),
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
                openProject = OpenProjectAccess.of(executor),
                sources = AgentSources.default(),
                isAiEnabled = isAiEnabled,
            )
        }
    }
}

/**
 * Runs the blocking [offMainThread] closes on [ioDispatcher] (interrupted when the time is up) and the [onMainThread] ones
 * on the caller's dispatcher, all side by side, then [last], everything within [timeoutInMilliseconds]. One that throws is
 * printed and doesn't stop the others.
 */
internal suspend fun shutDownSideBySide(
    timeoutInMilliseconds: Long,
    ioDispatcher: CoroutineDispatcher,
    offMainThread: List<() -> Unit>,
    onMainThread: List<suspend () -> Unit>,
    last: suspend () -> Unit,
) {
    withTimeoutOrNull(timeoutInMilliseconds) {
        coroutineScope {
            offMainThread.forEach { close -> launch { printingFailure { runInterruptible(ioDispatcher) { close() } } } }
            onMainThread.forEach { close -> launch { printingFailure { close() } } }
        }
        printingFailure { last() }
    }
}

private suspend fun printingFailure(block: suspend () -> Unit) {
    try {
        block()
    } catch (exception: CancellationException) {
        throw exception
    } catch (exception: Exception) {
        exception.printStackTrace()
    }
}
