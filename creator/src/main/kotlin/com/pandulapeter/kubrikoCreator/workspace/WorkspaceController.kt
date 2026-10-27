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
import com.pandulapeter.kubrikoCreator.actors.ActorTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.BlockTypeRepository
import com.pandulapeter.kubrikoCreator.commands.CommandContext
import com.pandulapeter.kubrikoCreator.commands.CommandExecutor
import com.pandulapeter.kubrikoCreator.commands.Invocation
import com.pandulapeter.kubrikoCreator.commands.actorTypes.CreateActorType
import com.pandulapeter.kubrikoCreator.commands.scenes.CreateScene
import com.pandulapeter.kubrikoCreator.git.GitController
import com.pandulapeter.kubrikoCreator.git.GitRepository
import com.pandulapeter.kubrikoCreator.git.JGitRepository
import com.pandulapeter.kubrikoCreator.gradle.GradleBuildEvent
import com.pandulapeter.kubrikoCreator.gradle.ProjectBuild
import com.pandulapeter.kubrikoCreator.project.CreatorProject
import com.pandulapeter.kubrikoCreator.project.ProjectChange
import com.pandulapeter.kubrikoCreator.project.ProjectResource
import com.pandulapeter.kubrikoCreator.project.ResourceGroup
import com.pandulapeter.kubrikoCreator.project.ResourceScanner
import com.pandulapeter.kubrikoCreator.scenes.ClassLoaderEntryPointLoader
import com.pandulapeter.kubrikoCreator.scenes.EntryPointLoader
import com.pandulapeter.kubrikoCreator.scenes.SceneEditingController
import com.pandulapeter.kubrikoCreator.scenes.SceneStore
import com.pandulapeter.kubrikoCreator.session.SessionChannel
import com.pandulapeter.kubrikoCreator.session.SessionInitScript
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.status_actor_type_not_saved
import kubriko.creator.generated.resources.status_build_failed
import kubriko.creator.generated.resources.status_building
import org.jetbrains.compose.resources.StringResource
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
    private val blockTypeRepository: BlockTypeRepository? = null,
    creatorKubrikoVersion: String = "",
    entryPointLoader: EntryPointLoader = ClassLoaderEntryPointLoader,
    startWatching: (onChange: (Set<ProjectChange>) -> Unit) -> AutoCloseable = { AutoCloseable {} },
    generatedPath: String? = null,
    openGitRepository: () -> GitRepository? = { JGitRepository.open(project.rootDirectory) },
    createGitRepository: (message: String) -> GitRepository = { JGitRepository.create(project.rootDirectory, it) },
) : AutoCloseable {
    private val _resources = MutableStateFlow(emptyMap<ResourceGroup, List<ProjectResource>>())
    val resources = _resources.asStateFlow()
    private val consoleLog = ConsoleLog()
    val console = consoleLog.entries
    private val sessions = SessionController(
        channel = sessionChannel,
        runTask = ::runCreatorTask,
        console = consoleLog,
        scope = scope,
    )
    val sessionState = sessions.state

    /**
     * The Scene shown in Kubriko Creator's own window. Declared before [blockTypes], whose changes it hears about.
     */
    val sceneEditing = SceneEditingController(
        creatorKubrikoVersion = creatorKubrikoVersion,
        runTask = ::runCreatorTask,
        entryPointLoader = entryPointLoader,
        scope = scope,
        ioDispatcher = ioDispatcher,
        openInSessionWindow = ::editSceneInWindow,
        report = ::appendToConsole,
    )

    /**
     * The project's block-built Actor types, or null if the project hasn't opted in to them (it has no `.kubriko/project.json`).
     */
    val blockTypes = blockTypeRepository?.let { repository ->
        BlockTypesController(
            repository = repository,
            scope = scope,
            ioDispatcher = ioDispatcher,
            onSaved = {
                refreshResources()
                sceneEditing.markOutdated()
            },
            onSaveFailed = { appendToConsole(ConsoleEntry.Status(Res.string.status_actor_type_not_saved, isError = true)) },
        )
    }

    /**
     * Creates an empty Actor type and selects it.
     *
     * @return Why [name] can't be used, or null if the Actor type is being created.
     */
    fun createActorType(name: String): StringResource? {
        val invocation = Invocation(CreateActorType, ActorTypeDefinition(name = name))
        commands.validate(invocation)?.let { return it.reason }
        commands.dispatch(invocation) { blockTypes?.select(name) }
        return null
    }

    /**
     * Creates an empty Scene.
     *
     * @return Why [name] can't be used, or null if the Scene is being created.
     */
    fun createScene(name: String): StringResource? {
        val invocation = Invocation(CreateScene, CreateScene.Input(name))
        commands.validate(invocation)?.let { return it.reason }
        commands.dispatch(invocation)
        return null
    }

    val git = GitController(
        openRepository = openGitRepository,
        createRepository = createGitRepository,
        generatedPath = generatedPath,
        scope = scope,
        ioDispatcher = ioDispatcher,
        report = ::appendToConsole,
    )

    /**
     * Every change to the project goes through here, so that it can be undone, and the AI can make it too.
     */
    val commands = CommandExecutor(
        context = CommandContext(
            project = project,
            console = console,
            blockTypes = blockTypes,
            git = git,
            scenes = SceneStore(project.rootDirectory) { _resources.value[ResourceGroup.SCENES].orEmpty() },
            runner = WorkspaceRunner(sessions = sessions, runTask = ::runCreatorTask, console = consoleLog, ioDispatcher = ioDispatcher),
            ioDispatcher = ioDispatcher,
            onScenesChanged = { names ->
                refreshResources()
                sceneEditing.onScenesChanged(names)
            },
        ),
        scope = scope,
        onRejected = { appendToConsole(ConsoleEntry.Status(it.reason, isError = true, arguments = it.arguments)) },
    )

    private val watching = startWatching(::onProjectChanged)

    init {
        refreshResources()
    }

    /**
     * Brings Kubriko Creator up to date with files that changed on disk, whoever changed them.
     */
    fun onProjectChanged(changes: Set<ProjectChange>) {
        if (ProjectChange.ACTOR_TYPE_DEFINITIONS in changes) blockTypes?.reloadFromDisk()
        if (ProjectChange.SOURCES in changes) sceneEditing.markOutdated()
        if (ProjectChange.SCENES in changes || ProjectChange.SOURCES in changes) refreshResources()
        git.refresh()
    }

    fun refreshResources() {
        scope.launch {
            _resources.value = withContext(ioDispatcher) { scanResources(project.rootDirectory) }
        }
    }

    fun play() = sessions.start(SessionMode.PLAY, scene = null)

    /**
     * Edits [scene] in a session window of its own, rather than in Kubriko Creator's ([sceneEditing]).
     */
    fun editSceneInWindow(scene: ProjectResource) = sessions.start(SessionMode.EDIT, scene)

    fun stopSession() = sessions.stop()

    fun clearConsole() = consoleLog.clear()

    /**
     * Runs one of the init script's tasks in the project's desktop module with Kubriko Creator's [properties], reporting
     * compiler errors (or, when the build reports none, the end of its output) in the console.
     *
     * @return Whether the build succeeded.
     */
    private suspend fun runCreatorTask(taskName: String, properties: Map<String, String>): Boolean {
        appendToConsole(ConsoleEntry.Status(Res.string.status_building))
        val outputTail = ArrayDeque<String>()
        val hasReportedProblems = AtomicBoolean(false)
        val isSuccessful = build.run(
            taskPath = if (project.desktopModulePath == ":") ":$taskName" else "${project.desktopModulePath}:$taskName",
            arguments = listOf("--init-script", SessionInitScript.file.absolutePath, "--no-configuration-cache") + properties.map { (key, value) -> "-P$key=$value" },
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
        if (!isSuccessful) {
            appendToConsole(ConsoleEntry.Status(Res.string.status_build_failed, isError = true))
            if (!hasReportedProblems.get()) synchronized(outputTail) { outputTail.toList() }.forEach { appendToConsole(ConsoleEntry.BuildOutput(it)) }
        }
        return isSuccessful
    }

    private fun GradleBuildEvent.Problem.toConsoleEntry() = ConsoleEntry.Problem(
        message = message,
        location = file?.let { file ->
            listOfNotNull(file.relativeToOrSelf(project.rootDirectory).invariantSeparatorsPath, line, column).joinToString(":")
        },
    )

    private fun appendToConsole(entry: ConsoleEntry) = consoleLog.append(entry)

    override fun close() {
        watching.close()
        sceneEditing.close()
        sessions.close()
        build.close()
    }

    private companion object {
        const val OUTPUT_TAIL_SIZE = 30
    }
}
