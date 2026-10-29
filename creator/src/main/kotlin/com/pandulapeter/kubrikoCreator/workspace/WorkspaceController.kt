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

import com.pandulapeter.kubrikoCreator.actors.ActorTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.AudioFiles
import com.pandulapeter.kubrikoCreator.actors.BlockTypeRepository
import com.pandulapeter.kubrikoCreator.actors.ManagerTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.SpriteFiles
import com.pandulapeter.kubrikoCreator.commands.CommandContext
import com.pandulapeter.kubrikoCreator.commands.CommandExecutor
import com.pandulapeter.kubrikoCreator.commands.Invocation
import com.pandulapeter.kubrikoCreator.commands.actorTypes.CreateActorType
import com.pandulapeter.kubrikoCreator.commands.managerTypes.CreateManagerType
import com.pandulapeter.kubrikoCreator.commands.scenes.CreateScene
import com.pandulapeter.kubrikoCreator.git.GitController
import com.pandulapeter.kubrikoCreator.git.GitRepository
import com.pandulapeter.kubrikoCreator.git.JGitRepository
import com.pandulapeter.kubrikoCreator.gradle.GradleBuildEvent
import com.pandulapeter.kubrikoCreator.gradle.IncrementalCompilationPolicy
import com.pandulapeter.kubrikoCreator.gradle.ProjectBuild
import com.pandulapeter.kubrikoCreator.project.CreatorProject
import com.pandulapeter.kubrikoCreator.project.ExpectDeclarations
import com.pandulapeter.kubrikoCreator.project.KubrikoBuildFileChanges
import com.pandulapeter.kubrikoCreator.project.OwnWrites
import com.pandulapeter.kubrikoCreator.project.ProjectChange
import com.pandulapeter.kubrikoCreator.project.ProjectInspectionException
import com.pandulapeter.kubrikoCreator.project.ProjectResource
import com.pandulapeter.kubrikoCreator.project.ResourceGroup
import com.pandulapeter.kubrikoCreator.project.ResourceScanner
import com.pandulapeter.kubrikoCreator.scenes.ActorTypeLook
import com.pandulapeter.kubrikoCreator.scenes.ClassLoaderEntryPointLoader
import com.pandulapeter.kubrikoCreator.scenes.EntryPointLoader
import com.pandulapeter.kubrikoCreator.scenes.SceneEditingController
import com.pandulapeter.kubrikoCreator.scenes.SceneStore
import com.pandulapeter.kubrikoCreator.session.GameLaunch
import com.pandulapeter.kubrikoCreator.session.GameProcess
import com.pandulapeter.kubrikoCreator.session.SessionChannel
import com.pandulapeter.kubrikoCreator.session.SessionInitScript
import com.pandulapeter.kubrikoCreator.template.ProjectUpgrader
import com.pandulapeter.kubrikoCreator.template.TemplateUpgrade
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.error_not_a_gradle_project
import kubriko.creator.generated.resources.status_actor_type_not_saved
import kubriko.creator.generated.resources.status_build_failed
import kubriko.creator.generated.resources.status_building
import kubriko.creator.generated.resources.status_built
import kubriko.creator.generated.resources.status_kubriko_libraries_added
import kubriko.creator.generated.resources.status_kubriko_libraries_removed
import kubriko.creator.generated.resources.status_kubriko_version_changed
import kubriko.creator.generated.resources.status_kubriko_version_newer
import kubriko.creator.generated.resources.status_play_from_scene_needs_upgrade
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
    private val parentScope: CoroutineScope,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    findDesktopModulePath: () -> String = { ":" },
    private val currentTimeMillis: () -> Long = System::currentTimeMillis,
    writeDelayInMilliseconds: Long = 0,
    ownWrites: OwnWrites = OwnWrites(),
    private val scanResources: (File) -> Map<ResourceGroup, List<ProjectResource>> = ResourceScanner::scan,
    private val blockTypeRepository: BlockTypeRepository? = null,
    syncBuildFiles: () -> KubrikoBuildFileChanges? = { null },
    val creatorKubrikoVersion: String = "",
    entryPointLoader: EntryPointLoader = ClassLoaderEntryPointLoader,
    startWatching: (onChange: (Set<ProjectChange>) -> Unit) -> AutoCloseable = { AutoCloseable {} },
    generatedPath: String? = null,
    openGitRepository: () -> GitRepository? = { JGitRepository.open(project.rootDirectory) },
    createGitRepository: (message: String) -> GitRepository = { JGitRepository.create(project.rootDirectory, it) },
    runGame: suspend (GameLaunch, Map<String, String>) -> GameProcess.Exit = GameProcess(project.rootDirectory, SessionInitScript.playDirectoryOf(project.rootDirectory))::run,
) {
    /**
     * Everything the workspace starts runs here, and stops when it is closed.
     */
    private val scope = CoroutineScope(parentScope.coroutineContext + SupervisorJob(parentScope.coroutineContext[Job]))

    /**
     * The Gradle path of the module that runs the game on desktop, asked from Gradle in the background as soon as the
     * workspace opens, since only builds need it.
     */
    private val desktopModulePath = DesktopModuleLookup(scope, ioDispatcher, findDesktopModulePath)
    private val _resources = MutableStateFlow(emptyMap<ResourceGroup, List<ProjectResource>>())
    val resources = _resources.asStateFlow()
    private val _sceneFolders = MutableStateFlow(emptySet<String>())

    /**
     * The folders of the Scenes folder, empty ones too.
     */
    val sceneFolders = _sceneFolders.asStateFlow()
    private val incrementalCompilation = IncrementalCompilationPolicy(
        hasExpectDeclarations = ExpectDeclarations(project.rootDirectory).let { declarations -> { withContext(ioDispatcher) { declarations.any() } } },
    )
    private val consoleLog = ConsoleLog()

    /**
     * Keeps builds of the classpath from overlapping each other or the start of a session, since a build that starts
     * while another one runs needs a Gradle daemon of its own.
     */
    private val classpathBuildLock = Mutex()
    private val hasReportedNewerKubrikoVersion = AtomicBoolean(false)
    val console = consoleLog.entries
    private val sessions: SessionController = SessionController(
        channel = sessionChannel,
        runTask = ::runCreatorTask,
        console = consoleLog,
        scope = scope,
        prepareLaunch = { gameBuilds.prepareLaunch() },
        runGame = runGame,
    )
    val sessionState = sessions.state
    private val gameBuilds: GameBuilds = GameBuilds(
        rootDirectory = project.rootDirectory,
        sessions = sessions,
        runTask = ::runCreatorTask,
        console = consoleLog,
        scope = scope,
        ioDispatcher = ioDispatcher,
        currentTimeMillis = currentTimeMillis,
    )

    /**
     * The project's Scene files, by name.
     */
    val scenes = SceneStore(project.rootDirectory, ownWrites) { _resources.value[ResourceGroup.SCENES].orEmpty() }

    /**
     * The Scene shown in Kubriko Creator's own window. Declared before [blockTypes], whose changes it hears about.
     */
    val sceneEditing = SceneEditingController(
        creatorKubrikoVersion = creatorKubrikoVersion,
        runTask = ::runCreatorTask,
        entryPointLoader = entryPointLoader,
        classpathFile = SessionInitScript.classpathFileOf(project.rootDirectory),
        scope = scope,
        ioDispatcher = ioDispatcher,
        report = ::appendToConsole,
        readScene = { scenes.actors(SceneStore.nameOf(it.file)).orEmpty() },
        spriteFiles = { SpriteFiles.of(_resources.value).filesByAccessor },
        requestCleanBuild = incrementalCompilation::requestCleanBuild,
        warmUpCompiler = ::warmUpCompiler,
        actorTypeLooks = { ActorTypeLook.of(blockTypes?.actorTypes?.value.orEmpty(), SpriteFiles.of(_resources.value).files) },
        awaitWrites = { blockTypes?.awaitWrites() },
    )

    /**
     * The project's block-built Actor types, or null if the project hasn't opted in to them (it has no `.kubriko/project.json`).
     */
    val blockTypes: BlockTypesController? = blockTypeRepository?.let { repository ->
        BlockTypesController(
            repository = repository,
            scope = scope,
            ioDispatcher = ioDispatcher,
            onSaved = { haveTypesChanged ->
                refreshResources()
                if (haveTypesChanged) {
                    sceneEditing.markOutdated()
                    gameBuilds.onCodeChanged()
                }
            },
            onSaveFailed = { appendToConsole(ConsoleEntry.Status(Res.string.status_actor_type_not_saved, isError = true)) },
            syncBuildFiles = syncBuildFiles,
            onBuildFilesSynced = ::reportBuildFileChanges,
            writeDelayInMilliseconds = writeDelayInMilliseconds,
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
     * Creates an empty Manager type and selects it.
     *
     * @return Why [name] can't be used, or null if the Manager type is being created.
     */
    fun createManagerType(name: String): StringResource? {
        val invocation = Invocation(CreateManagerType, ManagerTypeDefinition(name = name))
        commands.validate(invocation)?.let { return it.reason }
        commands.dispatch(invocation) { blockTypes?.select(name) }
        return null
    }

    /**
     * Creates an empty Scene.
     *
     * @return Why [name] can't be used, or null if the Scene is being created.
     */
    fun createScene(name: String) = dispatchIfValid(Invocation(CreateScene, CreateScene.Input(name)))

    /**
     * Applies [invocation] unless it is rejected.
     *
     * @return Why it was rejected, or null if it is being applied.
     */
    fun dispatchIfValid(invocation: Invocation<*>): StringResource? {
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
            scenes = scenes,
            runner = WorkspaceRunner(
                sessions = sessions,
                startSession = ::play,
                runTask = ::runCreatorTask,
                console = consoleLog,
                classpathFile = SessionInitScript.buildClasspathFileOf(project.rootDirectory),
            ),
            ioDispatcher = ioDispatcher,
            audioFiles = { AudioFiles.of(_resources.value) },
            spriteFiles = { SpriteFiles.of(_resources.value) },
            onScenesChanged = { names ->
                refreshResources()
                sceneEditing.onScenesChanged(names)
                blockTypes?.onScenesChanged()
                gameBuilds.onCodeChanged()
            },
            onSceneRenamed = { oldName, newName ->
                val file = scenes.fileOf(newName)
                sceneEditing.onSceneRenamed(oldName, ProjectResource(ResourceGroup.SCENES, file, file.relativeTo(project.rootDirectory).invariantSeparatorsPath))
            },
            report = ::appendToConsole,
            onProjectUpgraded = ::refreshProjectUpgrade,
        ),
        scope = scope,
        onRejected = { appendToConsole(ConsoleEntry.Status(it.reason, isError = true, arguments = it.arguments)) },
    )

    private val upgrader = ProjectUpgrader(project.rootDirectory)
    private val _projectUpgrade = MutableStateFlow<TemplateUpgrade>(TemplateUpgrade.UpToDate)

    /**
     * What the project's template files (`Game.kt` and its neighbors, which belong to the user) need for this version of
     * Kubriko Creator.
     */
    val projectUpgrade = _projectUpgrade.asStateFlow()

    private val watching = startWatching(::onProjectChanged)

    init {
        refreshResources()
        refreshProjectUpgrade()
        blockTypes?.let { types ->
            scope.launch {
                combine(types.actorTypes, _resources) { _, _ -> }.collect { sceneEditing.onActorTypesChanged() }
            }
        }
    }

    /**
     * Brings Kubriko Creator up to date with files that changed on disk, whoever changed them.
     */
    fun onProjectChanged(changes: Set<ProjectChange>) {
        if (ProjectChange.BLOCK_TYPE_DEFINITIONS in changes) blockTypes?.reloadFromDisk()
        if (ProjectChange.SOURCES in changes) {
            sceneEditing.markOutdated()
            refreshProjectUpgrade()
        }
        if (ProjectChange.SCENES in changes) {
            blockTypes?.onScenesChanged()
            sceneEditing.onScenesChangedOnDisk()
        }
        if (ProjectChange.SCENES in changes || ProjectChange.SOURCES in changes) {
            refreshResources()
            gameBuilds.onCodeChanged()
        }
        git.refresh()
    }

    /**
     * Checks the template files again, recording the new template version if they were upgraded by hand.
     */
    fun refreshProjectUpgrade() {
        scope.launch {
            _projectUpgrade.value = withContext(ioDispatcher) {
                upgrader.recordVersionIfUpToDate()
                upgrader.check()
            }
        }
    }

    fun refreshResources() {
        scope.launch {
            _resources.value = withContext(ioDispatcher) { scanResources(project.rootDirectory) }
            _sceneFolders.value = withContext(ioDispatcher) { scenes.folders() }
        }
    }

    /**
     * Plays the game, from its first Scene or [firstScene], which a project whose template files are older than that
     * ([projectUpgrade]) may not honor, as the console says then.
     */
    fun play(firstScene: String? = null) {
        if (firstScene != null && _projectUpgrade.value is TemplateUpgrade.Needed) {
            appendToConsole(ConsoleEntry.Status(Res.string.status_play_from_scene_needs_upgrade, arguments = listOf(firstScene)))
        }
        sessions.play(firstScene)
    }

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
        blockTypes?.awaitWrites()
        val modulePath = try {
            desktopModulePath.await()
        } catch (exception: ProjectInspectionException) {
            appendToConsole(ConsoleEntry.Status(exception.reason, isError = true))
            return false
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Exception) {
            appendToConsole(ConsoleEntry.Status(Res.string.error_not_a_gradle_project, isError = true))
            return false
        }
        var startTime = 0L
        val outputTail = ArrayDeque<String>()
        val hasReportedProblems = AtomicBoolean(false)
        suspend fun runBuild() = incrementalCompilation.build { mode ->
            startTime = currentTimeMillis()
            build.run(
                taskPath = taskPathOf(modulePath, taskName),
                arguments = SessionInitScript.gradleArguments(properties, mode),
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
        }
        val isSuccessful = if (taskName != SessionInitScript.SESSION_TASK_NAME) {
            classpathBuildLock.withLock { runBuild() }
        } else {
            // A session's build lasts as long as the game runs, so it only waits for the classpath build before it.
            classpathBuildLock.withLock {}
            runBuild()
        }
        if (!isSuccessful) {
            appendToConsole(ConsoleEntry.Status(Res.string.status_build_failed, isError = true))
            if (!hasReportedProblems.get()) synchronized(outputTail) { outputTail.toList() }.forEach { appendToConsole(ConsoleEntry.BuildOutput(it)) }
        }
        if (isSuccessful && taskName == SessionInitScript.CLASSPATH_TASK_NAME) {
            appendToConsole(ConsoleEntry.Status(Res.string.status_built, arguments = listOf(secondsSince(startTime))))
        }
        return isSuccessful
    }

    /**
     * Compiles one generated file next to the project's code, without a word in the console, so that Kotlin's compile
     * daemon runs and is warm before the first change: the build at opening starts it only when something needed
     * compiling, and a cold one makes the first change take seconds longer.
     */
    private suspend fun warmUpCompiler() {
        val modulePath = try {
            desktopModulePath.await()
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Exception) {
            return
        }
        classpathBuildLock.withLock {
            build.run(
                taskPath = taskPathOf(modulePath, SessionInitScript.WARM_UP_TASK_NAME),
                arguments = SessionInitScript.gradleArguments(mapOf("kubrikoCreatorMode" to "warmUp"), incrementalCompilation.incrementalMode()),
                onEvent = {},
            )
        }
    }

    private fun taskPathOf(modulePath: String, taskName: String) = if (modulePath == ":") ":$taskName" else "$modulePath:$taskName"

    private fun GradleBuildEvent.Problem.toConsoleEntry() = ConsoleEntry.Problem(
        message = message,
        location = file?.let { file ->
            listOfNotNull(file.relativeToOrSelf(project.rootDirectory).invariantSeparatorsPath, line, column).joinToString(":")
        },
    )

    private fun appendToConsole(entry: ConsoleEntry) = consoleLog.append(entry)

    private fun reportBuildFileChanges(changes: KubrikoBuildFileChanges) {
        changes.previousVersion?.let { appendToConsole(ConsoleEntry.Status(Res.string.status_kubriko_version_changed, arguments = listOf(changes.version, it))) }
        if (changes.isNewerThanCreator && !hasReportedNewerKubrikoVersion.getAndSet(true)) {
            appendToConsole(ConsoleEntry.Status(Res.string.status_kubriko_version_newer, arguments = listOf(changes.version)))
        }
        if (changes.addedArtifacts.isNotEmpty()) {
            appendToConsole(ConsoleEntry.Status(Res.string.status_kubriko_libraries_added, arguments = listOf(changes.addedArtifacts.joinToString())))
        }
        if (changes.removedArtifacts.isNotEmpty()) {
            appendToConsole(ConsoleEntry.Status(Res.string.status_kubriko_libraries_removed, arguments = listOf(changes.removedArtifacts.joinToString())))
        }
    }

    /**
     * Stops everything the workspace runs, once the changes made so far are written.
     *
     * @return The job that finishes closing it, for an app that is quitting to wait for.
     */
    fun close(): Job {
        watching.close()
        sceneEditing.dispose()
        sessions.close()
        return parentScope.launch {
            blockTypes?.awaitWrites()
            scope.cancel()
            withContext(ioDispatcher) { build.close() }
        }
    }

    private fun secondsSince(startTime: Long) = ((currentTimeMillis() - startTime) / 100 / 10f).toString()

    private companion object {
        const val OUTPUT_TAIL_SIZE = 30
    }
}
