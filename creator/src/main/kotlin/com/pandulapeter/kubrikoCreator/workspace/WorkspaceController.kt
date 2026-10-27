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

import com.pandulapeter.kubriko.creatorSession.protocol.SessionMessage
import com.pandulapeter.kubriko.creatorSession.protocol.SessionMode
import com.pandulapeter.kubriko.creatorSession.protocol.SessionProtocol
import com.pandulapeter.kubrikoCreator.gradle.GradleBuildEvent
import com.pandulapeter.kubrikoCreator.gradle.ProjectBuild
import com.pandulapeter.kubrikoCreator.project.CreatorProject
import com.pandulapeter.kubrikoCreator.project.ProjectResource
import com.pandulapeter.kubrikoCreator.project.ResourceGroup
import com.pandulapeter.kubrikoCreator.project.ResourceScanner
import com.pandulapeter.kubrikoCreator.session.SessionChannel
import com.pandulapeter.kubrikoCreator.session.SessionInitScript
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.status_build_failed
import kubriko.creator.generated.resources.status_building
import kubriko.creator.generated.resources.status_session_ended
import kubriko.creator.generated.resources.status_session_started
import kubriko.creator.generated.resources.status_session_stopped
import kubriko.creator.generated.resources.status_unsupported_protocol
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Holds the state of an open project: its resources, its console, and the session process that runs or edits it.
 */
internal class WorkspaceController(
    val project: CreatorProject,
    private val build: ProjectBuild,
    private val sessionChannel: SessionChannel,
    private val scope: CoroutineScope,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val scanResources: (File) -> Map<ResourceGroup, List<ProjectResource>> = ResourceScanner::scan,
) : AutoCloseable {
    private val _resources = MutableStateFlow(emptyMap<ResourceGroup, List<ProjectResource>>())
    val resources = _resources.asStateFlow()
    private val _console = MutableStateFlow(emptyList<ConsoleEntry>())
    val console = _console.asStateFlow()
    private val _sessionState = MutableStateFlow<SessionState>(SessionState.Idle)
    val sessionState = _sessionState.asStateFlow()
    private var sessionJob: Job? = null

    init {
        scope.launch {
            sessionChannel.messages.collect(::onSessionMessage)
        }
        refreshResources()
    }

    fun refreshResources() {
        scope.launch {
            _resources.value = withContext(ioDispatcher) { scanResources(project.rootDirectory) }
        }
    }

    fun play() = startSession(SessionMode.PLAY, scene = null)

    fun editScene(scene: ProjectResource) = startSession(SessionMode.EDIT, scene)

    fun stopSession() {
        if (sessionJob?.isActive == true) {
            sessionJob?.cancel()
            appendToConsole(ConsoleEntry.Status(Res.string.status_session_stopped))
        }
    }

    fun clearConsole() {
        _console.value = emptyList()
    }

    private fun startSession(mode: SessionMode, scene: ProjectResource?) {
        sessionJob?.cancel()
        sessionJob = scope.launch {
            _sessionState.value = SessionState.Starting(mode)
            appendToConsole(ConsoleEntry.Status(Res.string.status_building))
            val outputTail = ArrayDeque<String>()
            val hasReportedProblems = AtomicBoolean(false)
            try {
                val isSuccessful = build.run(
                    taskPath = sessionTaskPath(),
                    arguments = sessionArguments(mode, scene),
                    onEvent = { event ->
                        when (event) {
                            is GradleBuildEvent.Output -> synchronized(outputTail) {
                                outputTail.addLast(event.line)
                                if (outputTail.size > OUTPUT_TAIL_SIZE) outputTail.removeFirst()
                            }
                            is GradleBuildEvent.Problem -> {
                                hasReportedProblems.set(true)
                                appendToConsole(event.toConsoleEntry())
                            }
                        }
                    },
                )
                if (isSuccessful) {
                    appendToConsole(ConsoleEntry.Status(Res.string.status_session_ended))
                } else {
                    appendToConsole(ConsoleEntry.Status(Res.string.status_build_failed, isError = true))
                    if (!hasReportedProblems.get()) synchronized(outputTail) { outputTail.toList() }.forEach { appendToConsole(ConsoleEntry.BuildOutput(it)) }
                }
            } finally {
                _sessionState.value = SessionState.Idle
            }
        }
    }

    private fun sessionTaskPath() = if (project.desktopModulePath == ":") {
        ":${SessionInitScript.TASK_NAME}"
    } else {
        "${project.desktopModulePath}:${SessionInitScript.TASK_NAME}"
    }

    private fun sessionArguments(mode: SessionMode, scene: ProjectResource?) = buildList {
        add("--init-script")
        add(SessionInitScript.file.absolutePath)
        add("--no-configuration-cache")
        add("-PkubrikoCreatorPort=${sessionChannel.port}")
        when (mode) {
            SessionMode.PLAY -> add("-PkubrikoCreatorMode=play")
            SessionMode.EDIT -> {
                add("-PkubrikoCreatorMode=edit")
                scene?.let {
                    add("-PkubrikoCreatorSceneFolder=${it.file.parentFile.absolutePath}")
                    add("-PkubrikoCreatorSceneFile=${it.name}")
                }
            }
        }
    }

    private fun onSessionMessage(message: SessionMessage) = when (message) {
        is SessionMessage.Hello -> if (message.protocolVersion == SessionProtocol.VERSION) {
            _sessionState.value = SessionState.Running(message.mode)
            appendToConsole(ConsoleEntry.Status(Res.string.status_session_started))
        } else {
            appendToConsole(ConsoleEntry.Status(Res.string.status_unsupported_protocol, isError = true))
            stopSession()
        }
        is SessionMessage.Log -> appendToConsole(
            ConsoleEntry.Log(
                message = message.message,
                source = message.source,
                isImportant = message.importance == "HIGH",
            ),
        )
        is SessionMessage.Failure -> appendToConsole(ConsoleEntry.SessionFailure(message.message))
    }

    private fun GradleBuildEvent.Problem.toConsoleEntry() = ConsoleEntry.Problem(
        message = message,
        location = file?.let { file ->
            listOfNotNull(file.relativeToOrSelf(project.rootDirectory).invariantSeparatorsPath, line, column).joinToString(":")
        },
    )

    private fun appendToConsole(entry: ConsoleEntry) = _console.update { (it + entry).takeLast(CONSOLE_SIZE) }

    override fun close() {
        sessionJob?.cancel()
        sessionChannel.close()
        build.close()
    }

    private companion object {
        const val OUTPUT_TAIL_SIZE = 30
        const val CONSOLE_SIZE = 2000
    }
}
