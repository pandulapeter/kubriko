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
import com.pandulapeter.kubrikoCreator.actors.AudioFiles
import com.pandulapeter.kubrikoCreator.actors.SpriteFiles
import com.pandulapeter.kubrikoCreator.actors.store.BlockTypesController
import com.pandulapeter.kubrikoCreator.commands.CommandContext
import com.pandulapeter.kubrikoCreator.commands.CommandExecutor
import com.pandulapeter.kubrikoCreator.commands.PlayTarget
import com.pandulapeter.kubrikoCreator.commands.ProjectBuildFiles
import com.pandulapeter.kubrikoCreator.commands.ScannedFiles
import com.pandulapeter.kubrikoCreator.console.ConsoleEntry
import com.pandulapeter.kubrikoCreator.console.ConsoleLines
import com.pandulapeter.kubrikoCreator.foundation.unexpectedProblemHandler
import com.pandulapeter.kubrikoCreator.git.GitController
import com.pandulapeter.kubrikoCreator.gradle.IncrementalCompilationPolicy
import com.pandulapeter.kubrikoCreator.project.CreatorProject
import com.pandulapeter.kubrikoCreator.project.ExpectDeclarations
import com.pandulapeter.kubrikoCreator.project.ProjectChange
import com.pandulapeter.kubrikoCreator.project.ProjectFormat
import com.pandulapeter.kubrikoCreator.project.ProjectFormatStatus
import com.pandulapeter.kubrikoCreator.project.ProjectProblem
import com.pandulapeter.kubrikoCreator.project.ProjectResource
import com.pandulapeter.kubrikoCreator.project.ProjectSnapshot
import com.pandulapeter.kubrikoCreator.project.ResourceGroup
import com.pandulapeter.kubrikoCreator.project.UnreadableProjectFileException
import com.pandulapeter.kubrikoCreator.sceneRuntime.ClassLoaderEntryPointLoader
import com.pandulapeter.kubrikoCreator.sceneRuntime.SceneEditingController
import com.pandulapeter.kubrikoCreator.sceneRuntime.SceneRuntimeSetup
import com.pandulapeter.kubrikoCreator.scenes.SceneStore
import com.pandulapeter.kubrikoCreator.session.SessionInitScript
import com.pandulapeter.kubrikoCreator.template.GameTargets
import com.pandulapeter.kubrikoCreator.template.ProjectUpgrader
import com.pandulapeter.kubrikoCreator.template.TargetHost
import com.pandulapeter.kubrikoCreator.template.TemplateUpgrade
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.io.IOException

/**
 * Holds the state of an open project: its resources, its console, and the session process that runs or edits it.
 *
 * Its parts are built in dependency order, each reading only those built before it.
 *
 * @param processes The Gradle connection, the game process, and the session channel the project is built and run through.
 * @param workspaceScope The scope everything the workspace starts runs in, which closing it cancels: a child of
 *   [parentScope], made by whoever also starts [processes]' session channel and the watcher in it.
 * @param files How the workspace notices what changed in the project's files.
 * @param blockTypeSetup What the project's block-built types need, null if the project hasn't opted in to block-built types
 *   (it has no `.kubriko/project.json`).
 * @param sceneRuntime What the project's classes are loaded next to in the Scene editor.
 * @param initialFormatStatus Whether the project may be changed, as [projectFormat] found it while opening it, which it
 *   checks again whenever `.kubriko/project.json` changes.
 * @param gitRepositories Opens the project's repository, or makes the project one.
 */
public class WorkspaceController(
    public val project: CreatorProject,
    private val processes: ProjectProcesses,
    private val parentScope: CoroutineScope,
    workspaceScope: CoroutineScope = workspaceScopeOf(parentScope),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val currentTimeMillis: () -> Long = System::currentTimeMillis,
    private val files: WorkspaceFiles = WorkspaceFiles(),
    blockTypeSetup: BlockTypesSetup? = null,
    sceneRuntime: SceneRuntimeSetup = SceneRuntimeSetup(creatorKubrikoVersion = "", entryPointLoader = ClassLoaderEntryPointLoader(project.rootDirectory)),
    initialFormatStatus: ProjectFormatStatus = ProjectFormatStatus.Writable,
    private val projectFormat: ProjectFormat = ProjectFormat(project.rootDirectory, sceneRuntime.creatorKubrikoVersion),
    gitRepositories: GitRepositories = GitRepositories.of(project.rootDirectory),
) {
    /**
     * Everything the workspace starts runs here, and stops when it is closed.
     */
    private val scope = CoroutineScope(workspaceScope.coroutineContext + unexpectedProblemHandler(::reportUnexpectedProblem))

    /** The module that is the game, which Creator builds, plays, and edits the Scenes of. */
    public val gameModule: GameModuleChoice = GameModuleChoice(project.rootDirectory, scope, ioDispatcher, processes.findDesktopModulePaths) { sceneEditing.markOutdated() }
    private val _resources = MutableStateFlow(emptyMap<ResourceGroup, List<ProjectResource>>())

    /** The project's resources, by group, as the last scan found them. */
    public val resources: StateFlow<Map<ResourceGroup, List<ProjectResource>>> = _resources.asStateFlow()
    private val _layout = MutableStateFlow(ProjectLayout())

    /** Where the project keeps what Kubriko Creator makes, as the last scan found it. */
    public val layout: StateFlow<ProjectLayout> = _layout.asStateFlow()
    private val generatedPath = blockTypeSetup?.generatedPath
    private val sceneHome = blockTypeSetup?.scenePath?.let { File(project.rootDirectory, it) }
    private val incrementalCompilation = IncrementalCompilationPolicy(
        hasExpectDeclarations = ExpectDeclarations(project.rootDirectory).let { declarations -> { withContext(ioDispatcher) { declarations.any() } } },
    )
    private val consoleLog = ConsoleLog()

    /** The lines of the workspace console, oldest first. */
    public val console: StateFlow<ConsoleLines> = consoleLog.lines
    private val _formatStatus = MutableStateFlow(initialFormatStatus)

    /**
     * Whether this Kubriko Creator may change the project: one saved by a newer Kubriko Creator is only browsed, built,
     * and played, rejecting every change, and generating no code.
     */
    public val formatStatus: StateFlow<ProjectFormatStatus> = _formatStatus.asStateFlow()

    /**
     * The platforms the game is built for, turned on and off through `set_target`, or null if they can't be changed here.
     */
    public val targets: GameTargets? get() = files.targets

    private val upgrader = ProjectUpgrader(project.rootDirectory)
    private val _projectUpgrade = MutableStateFlow<TemplateUpgrade>(TemplateUpgrade.UpToDate)

    /**
     * What the project's template files (`Game.kt` and its neighbors, which belong to the user) need for this version of
     * Kubriko Creator.
     */
    public val projectUpgrade: StateFlow<TemplateUpgrade> = _projectUpgrade.asStateFlow()

    /**
     * The project's Scene files, by name.
     */
    public val scenes: SceneStore = SceneStore(project.rootDirectory, files.ownWrites, homeDirectory = sceneHome) { _resources.value[ResourceGroup.SCENES].orEmpty() }

    /** The project's version control. */
    public val git: GitController = GitController(
        openRepository = gitRepositories.open,
        createRepository = gitRepositories.create,
        generatedPath = generatedPath,
        scope = scope,
        ioDispatcher = ioDispatcher,
        report = ::appendToConsole,
    )

    /**
     * Whether each batch of block type writes changed the types, handed to [changes] once it exists.
     */
    private val savedTypeBatches = Channel<Boolean>(Channel.UNLIMITED)

    /**
     * The project's block-built Actor types, or null if the project hasn't opted in to them (it has no `.kubriko/project.json`).
     */
    public val blockTypes: BlockTypesController? = blockTypeSetup?.let { setup ->
        BlockTypesController(
            repository = setup.repository,
            scope = scope,
            ioDispatcher = ioDispatcher,
            listener = BlockTypesReport(consoleLog) { haveTypesChanged -> savedTypeBatches.trySend(haveTypesChanged) },
            syncBuildFiles = setup.syncBuildFiles,
            writeDelayInMilliseconds = setup.writeDelayInMilliseconds,
            isReadOnly = { _formatStatus.value is ProjectFormatStatus.ReadOnly },
        )
    }
    private val tasks = CreatorTaskRunner(
        rootDirectory = project.rootDirectory,
        build = processes.build,
        gameModule = gameModule,
        incrementalCompilation = incrementalCompilation,
        console = consoleLog,
        awaitWrites = blockTypes?.let { types -> { if (types.awaitWrites()) null else types.unsavedWrites() } } ?: { null },
        currentTimeMillis = currentTimeMillis,
    )
    private val sessions = SessionController(
        channel = processes.sessionChannel,
        runTask = tasks::run,
        console = consoleLog,
        scope = scope,
        runGame = processes.runGame,
    )

    /** Whether the session process is idle, starting, or running. */
    public val sessionState: StateFlow<SessionState> = sessions.state

    /**
     * How many of the project's builds the console reports are running, which Play waits for.
     */
    public val runningBuilds: StateFlow<Int> = tasks.runningBuilds

    private val players = TargetPlayers(
        targets = { files.targets },
        androidDevices = ::androidDevices,
        web = WebPlayer(
            runTask = tasks::run,
            moduleDirectory = { File(project.rootDirectory, gameModule.await().removePrefix(":").replace(':', File.separatorChar)) },
            openBrowser = processes.openBrowser,
            console = consoleLog,
            ioDispatcher = ioDispatcher,
        ),
        android = AndroidPlayer(
            rootDirectory = project.rootDirectory,
            devices = ::androidDevices,
            runTask = tasks::runAt,
            tools = processes.tools,
            console = consoleLog,
            ioDispatcher = ioDispatcher,
        ),
        iosSimulators = ::iosSimulators,
        ios = IosSimulatorPlayer(project.rootDirectory, ::iosSimulators, tasks::runOutside, processes.tools, consoleLog, ioDispatcher),
    )

    /** Where the game can be played, and where Play plays it. */
    public val playTargets: PlayTargetChoice = PlayTargetChoice(
        rootDirectory = project.rootDirectory,
        sources = players.sources,
        scope = scope,
        ioDispatcher = ioDispatcher,
    )

    /** Makes new iOS simulators, each chosen in Play's menu once it is made. */
    public val iosSimulatorSetup: IosSimulatorSetup = IosSimulatorSetup(processes.tools, scope, ioDispatcher) { target ->
        playTargets.choose(target)
        playTargets.startRefresh()
    }
    private val gameBuilds = GameBuilds(
        rootDirectory = project.rootDirectory,
        sessions = sessions,
        runTask = tasks::run,
        console = consoleLog,
        scope = scope,
        ioDispatcher = ioDispatcher,
        currentTimeMillis = currentTimeMillis,
    )

    /**
     * The Scene shown in Kubriko Creator's own window.
     */
    public val sceneEditing: SceneEditingController = SceneEditingController(
        runtime = sceneRuntime,
        classes = WorkspaceSceneClasses(tasks, incrementalCompilation, SessionInitScript.classpathFileOf(project.rootDirectory), blockTypes),
        sources = WorkspaceSceneSources(scenes, resources, blockTypes),
        scope = scope,
        releaseScope = parentScope,
        ioDispatcher = ioDispatcher,
        report = ::appendToConsole,
    )

    /**
     * What the center shows, which every selection of a type or a Scene goes through.
     */
    public val selection: SelectionController = SelectionController(
        blockTypes = blockTypes,
        sceneEditing = sceneEditing,
        sceneOf = { name -> _resources.value[ResourceGroup.SCENES].orEmpty().firstOrNull { SceneStore.nameOf(it.file) == name } },
        scope = scope,
    )

    /**
     * Where Creator's own writes and the watcher's changes are routed to the parts that follow them.
     */
    private val changes = ProjectChanges(
        scope = scope,
        scanResources = ::scanResourcesNow,
        onScenesChanged = sceneEditing::onScenesChanged,
        onSceneRenamed = { oldName, newName ->
            val file = scenes.fileOf(newName)
            sceneEditing.onSceneRenamed(oldName, ProjectResource(ResourceGroup.SCENES, file, file.relativeTo(project.rootDirectory).invariantSeparatorsPath))
        },
        onScenesChangedOnDisk = sceneEditing::onScenesChangedOnDisk,
        markSceneOutdated = sceneEditing::markOutdated,
        regenerate = { blockTypes?.regenerate() },
        onCodeChanged = {
            gameBuilds.onCodeChanged()
            players.onCodeChanged()
        },
        refreshGit = git::refresh,
        refreshFormatStatus = ::refreshFormatStatus,
        reloadTypes = { blockTypes?.reloadFromDisk() },
        refreshProjectUpgrade = ::refreshProjectUpgrade,
    )

    /**
     * Every change to the project goes through here, so that it can be undone, and the AI can make it too.
     */
    public val commands: CommandExecutor = CommandExecutor(
        context = CommandContext(
            project = project,
            console = console,
            blockTypes = blockTypes,
            git = git,
            scenes = scenes,
            runner = WorkspaceRunner(
                sessions = sessions,
                startSession = ::play,
                playTargets = playTargets::refresh,
                runTask = tasks::run,
                console = consoleLog,
                classpathFile = SessionInitScript.buildClasspathFileOf(project.rootDirectory),
            ),
            ioDispatcher = ioDispatcher,
            scannedFiles = ScannedFiles(
                audioFiles = { AudioFiles.ofScanned(_resources.value) },
                spriteFiles = { SpriteFiles.of(_resources.value) },
            ),
            report = ::appendToConsole,
            buildFiles = object : ProjectBuildFiles {
                override fun current() = _projectUpgrade.value

                override fun onUpgraded() = refreshProjectUpgrade()

                override val targets = files.targets
            },
            rejectChanges = { (_formatStatus.value as? ProjectFormatStatus.ReadOnly)?.toRejection() },
        ),
        scope = scope,
        onRejected = { appendToConsole(ConsoleEntry.Status(it.reason, isError = true, arguments = it.arguments)) },
        onSceneChanges = changes::onSceneChanges,
    )

    /** Creates what the sidebar names in place. */
    public val creation: WorkspaceCreation = WorkspaceCreation(commands, selection)

    /** The AI panel's Game map, computed only while it is shown. */
    public val gameMap: GameMapState = GameMapState.of(blockTypes, resources, scenes, commands, sceneEditing, scope, ::reportUnexpectedProblem)

    /**
     * What about the project is worth a look, for the sidebar and the editors.
     */
    public val health: ProjectHealth = ProjectHealth.of(blockTypes, resources, commands, sceneEditing, scope, ::reportUnexpectedProblem)

    /**
     * The assistant's one offer to write the screenplay of a game that has none.
     */
    public val screenplayOffer: ScreenplayOffer = ScreenplayOffer(project.rootDirectory, commands.context.screenplay.current, commands.context.screenplay.revision, scope, ioDispatcher)
    private val missingReferences = MissingReferencesReport.of(blockTypes, resources, commands, scope, ::appendToConsole, ::reportUnexpectedProblem)

    private val watching = files.startWatching(::onProjectChanged)

    init {
        scope.launch {
            for (haveTypesChanged in savedTypeBatches) changes.onTypesSaved(haveTypesChanged)
        }
        changes.refreshResources()
        scope.launch { commands.context.documents.structureRevision.drop(1).collect { changes.refreshResources() } }
        refreshScreenplay()
        refreshProjectUpgrade()
        blockTypes?.let { types ->
            scope.launch {
                combine(types.actorTypes, _resources) { _, _ -> }.collect { sceneEditing.onActorTypesChanged() }
            }
        }
    }

    /**
     * Brings Kubriko Creator up to date with files that changed on disk, whoever changed them; the resource tree is
     * built from [snapshot], the files as the watcher found them, if it is given.
     */
    internal fun onProjectChanged(changes: Set<ProjectChange>, snapshot: ProjectSnapshot? = null) {
        if (ProjectChange.MODULES in changes) gameModule.onBuildFilesChanged()
        if (ProjectChange.SCREENPLAY in changes) refreshScreenplay()
        if (ProjectChange.DOCUMENTS in changes) commands.context.documents.onChangedOutside()
        this.changes.onProjectChanged(changes, snapshot)
        missingReferences.onChangedOutside()
    }

    private fun refreshScreenplay() {
        scope.launch(ioDispatcher) { commands.context.screenplay.refresh() }
    }

    /**
     * Checks the template files again, recording the new template version if they were upgraded by hand, and where the
     * game can be played, which they decide. A project that may not be changed is offered no upgrade.
     */
    internal fun refreshProjectUpgrade() {
        playTargets.startRefresh()
        scope.launch {
            _projectUpgrade.value = withContext(ioDispatcher) {
                if (_formatStatus.value is ProjectFormatStatus.ReadOnly) {
                    TemplateUpgrade.UpToDate
                } else {
                    upgrader.recordVersionIfUpToDate()
                    upgrader.check()
                }
            }
        }
    }

    /**
     * Checks the project's format version again after `.kubriko/project.json` changed (a git checkout of a branch a newer
     * Kubriko Creator saved, for example), migrating an older one; the files a migration rewrites are then reloaded as
     * any other change on disk.
     */
    private fun refreshFormatStatus() {
        scope.launch {
            val status = try {
                withContext(ioDispatcher) { projectFormat.prepare() }
            } catch (exception: UnreadableProjectFileException) {
                appendToConsole(ConsoleEntry.Status(exception.reason, isError = true, arguments = exception.arguments))
                return@launch
            } catch (exception: IOException) {
                appendToConsole(ConsoleEntry.Status(ProjectProblem.FORMAT_NOT_UPGRADED, isError = true, arguments = listOf(exception.message ?: exception.toString())))
                return@launch
            }
            val previous = _formatStatus.value
            _formatStatus.value = status
            if (status != previous) {
                refreshProjectUpgrade()
                if (status == ProjectFormatStatus.Writable) blockTypes?.regenerate()
            }
        }
    }

    /**
     * Scans the resource tree from [snapshot], unless one of Creator's own writes came after it was taken, whose change
     * the watcher would then never report, or by walking the project.
     */
    private suspend fun scanResourcesNow(snapshot: ProjectSnapshot?) {
        val scannedAt = files.ownWrites.generation
        val currentSnapshot = snapshot?.takeIf { it.ownWriteGeneration == scannedAt }
        val (resources, layout) = withContext(ioDispatcher) {
            files.scanResources(project.rootDirectory, currentSnapshot).let { it to ProjectLayout.of(project.rootDirectory, it, generatedPath, sceneHome, currentSnapshot) }
        }
        commands.context.documents.refresh(resources[ResourceGroup.DOCUMENTS].orEmpty().map { it.relativePath }, scannedAt)
        _resources.value = resources
        _layout.value = layout
        // After the new resources are in, so that Scenes outside the Scenes folder are found too.
        withContext(ioDispatcher) { scenes.refresh() }
    }

    /**
     * Plays the game on [target], from its first Scene or [firstScene], which a project whose template files are older than
     * that ([projectUpgrade]) may not honor, as the console says then.
     */
    public fun play(firstScene: String? = null, target: PlayTarget = playTargets.targets.value.chosen) {
        if (firstScene != null && _projectUpgrade.value is TemplateUpgrade.Needed) {
            appendToConsole(ConsoleEntry.Status(WorkspaceStatus.PLAY_FROM_SCENE_NEEDS_UPGRADE, arguments = listOf(firstScene)))
        }
        when (target.kind) {
            PlayTarget.Kind.DESKTOP -> sessions.play(firstScene, gameBuilds::prepareLaunch)
            PlayTarget.Kind.WEB, PlayTarget.Kind.ANDROID, PlayTarget.Kind.IOS -> sessions.playElsewhere(target) { onUp -> players.play(target, firstScene, onUp) }
        }
    }

    private fun androidDevices() = files.targets?.androidSdk()?.let { AndroidDevices(it, processes.tools) }

    private fun iosSimulators() = IosSimulators(processes.tools).takeIf { files.targets?.canBuildIos ?: TargetHost.System.canBuildIos }

    /**
     * Opens Android's guide to creating emulators in the browser, off the main thread.
     */
    public fun openAndroidEmulatorGuide() {
        scope.launch(ioDispatcher) { AndroidEmulatorGuide(processes.openBrowser, consoleLog).open() }
    }

    /** Stops the session process, if one runs. */
    public fun stopSession(): Unit = sessions.stop()

    /**
     * Says in the console that something failed that nothing expected to, naming the exception.
     */
    public fun reportUnexpectedProblem(exception: Throwable) {
        appendToConsole(ConsoleEntry.Status(WorkspaceStatus.UNEXPECTED_PROBLEM, isError = true, arguments = listOf(exception.toString())))
    }

    /** Empties the workspace console. */
    public fun clearConsole(): Unit = consoleLog.clear()

    private fun appendToConsole(entry: ConsoleEntry) = consoleLog.append(entry)

    /**
     * Stops everything the workspace runs, once the game is gone and the changes made so far are written, giving the game
     * [CLOSE_GAME_TIMEOUT_IN_MILLISECONDS] to stop (a game outliving it is killed as Creator exits) and then a failed
     * write [CLOSE_WRITE_TIMEOUT_IN_MILLISECONDS] to succeed on a retry.
     *
     * @return The job that finishes closing it, for an app that is quitting to wait for.
     */
    public fun close(): Job {
        watching.close()
        sceneEditing.dispose()
        return parentScope.launch(start = CoroutineStart.UNDISPATCHED) {
            withTimeoutOrNull(CLOSE_GAME_TIMEOUT_IN_MILLISECONDS) { sessions.closeAndJoin() }
            blockTypes?.let { withTimeoutOrNull(CLOSE_WRITE_TIMEOUT_IN_MILLISECONDS) { it.awaitWritten() } }
            scope.cancel()
            withContext(ioDispatcher) { processes.build.close() }
        }
    }
}
