<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# Group `WorkspaceController`'s 23 parameters by the part each feeds, move the production defaults into `GradleWorkspaceOpener`, and give tests a `workspaceController` fixture

**Kind:** architecture  ·  **Severity:** low  ·  **Platforms:** all (desktop app)
**Written against:** `9bb8df2a6`, to be carried out after F02–F05, which change only the bodies of the `BlockTypesController`,
`CommandContext`, and `SceneEditingController` calls inside `WorkspaceController` (F04 makes the last one take a
`SceneRuntimeSetup(creatorKubrikoVersion, creatorLibraries, entryPointLoader)`). Re-locate code by the parameter names below,
which those plans don't change.
**Files:**
- `:creator:workspace`, under `creator/workspace/src/main/kotlin/com/pandulapeter/kubrikoCreator/workspace/`:
  `WorkspaceController.kt`, `GradleWorkspaceOpener.kt`, and new `ProjectProcesses.kt`, `WorkspaceFiles.kt`,
  `BlockTypesSetup.kt`, `GitRepositories.kt`; new test fixture
  `creator/workspace/src/testFixtures/kotlin/com/pandulapeter/kubrikoCreator/workspace/WorkspaceTesting.kt`;
  `creator/workspace/build.gradle.kts`; tests under `creator/workspace/src/test/kotlin/com/pandulapeter/kubrikoCreator/workspace/`:
  `WorkspaceControllerTest.kt`, `SelectionControllerTest.kt`, `WorkspaceOpeningTest.kt`
- `:creator` (the app): `creator/build.gradle.kts`, `creator/src/test/kotlin/com/pandulapeter/kubrikoCreator/AppControllerTest.kt`,
  `creator/src/test/kotlin/com/pandulapeter/kubrikoCreator/benchmark/CreatorBenchmarkTest.kt`
- `creator/CLAUDE.md`, `creator/plan/README.md`

## Problem

`WorkspaceController`'s primary constructor takes 23 parameters (limit 12, `check_sizes.py`):

```kotlin
public class WorkspaceController(
    public val project: CreatorProject,
    private val build: ProjectBuild,
    private val sessionChannel: SessionChannel,
    private val parentScope: CoroutineScope,
    workspaceScope: CoroutineScope = workspaceScopeOf(parentScope),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    findDesktopModulePath: suspend () -> String = { ":" },
    private val currentTimeMillis: () -> Long = System::currentTimeMillis,
    writeDelayInMilliseconds: Long = 0,
    private val ownWrites: OwnWrites = OwnWrites(),
    private val scanResources: (File, ProjectSnapshot?) -> Map<ResourceGroup, List<ProjectResource>> = { root, snapshot -> ResourceScanner.scan(root, snapshot = snapshot) },
    private val blockTypeRepository: BlockTypeRepository? = null,
    syncBuildFiles: () -> KubrikoBuildFileChanges? = { null },
    creatorKubrikoVersion: String = "",
    creatorLibraries: Map<String, String> = emptyMap(),
    initialFormatStatus: ProjectFormatStatus = ProjectFormatStatus.Writable,
    private val projectFormat: ProjectFormat = ProjectFormat(project.rootDirectory, creatorKubrikoVersion),
    entryPointLoader: EntryPointLoader = ClassLoaderEntryPointLoader(project.rootDirectory),
    startWatching: (onChange: (Set<ProjectChange>, ProjectSnapshot) -> Unit) -> AutoCloseable = { AutoCloseable {} },
    generatedPath: String? = null,
    openGitRepository: () -> GitRepository? = { JGitRepository.open(project.rootDirectory) },
    createGitRepository: (message: String) -> GitRepository = { JGitRepository.create(project.rootDirectory, it) },
    runGame: suspend (GameLaunch, Map<String, String>) -> GameProcess.Exit = GameProcess(project.rootDirectory, SessionInitScript.playDirectoryOf(project.rootDirectory))::run,
)
```

Six callers build it: `GradleWorkspaceOpener` (production) and five tests (`WorkspaceControllerTest`, `SelectionControllerTest`,
`WorkspaceOpeningTest` in `:creator:workspace`; `AppControllerTest`, `CreatorBenchmarkTest` in `:creator`), each relying on a
different subset of the defaults.

`creator/plan/README.md` suggested "a factory that builds its collaborators". A factory function would have to take the same 23
values (a function's parameters aren't counted, so it would only hide them), and the collaborators need the workspace's own
scope and console, which exist only inside the controller. The parameters do, however, fall into groups by the collaborator
they feed, and grouping them that way is the refactor.

## Fix

Four new types in the `workspace` package, plus F04's `SceneRuntimeSetup` passed through. Result: 12 parameters.

| New parameter | Replaces | Why they belong together |
|---|---|---|
| `processes: ProjectProcesses` | `build`, `sessionChannel`, `findDesktopModulePath`, `runGame` | The outside processes the workspace builds and runs the project through: the Gradle connection and the desktop module asked of it, the game process, and the channel the game's session reports to. All come from `:creator:gradle-session`, all feed `CreatorTaskRunner`/`SessionController`, and all are closed or stopped with the workspace. |
| `files: WorkspaceFiles` | `ownWrites`, `scanResources`, `startWatching` | How the workspace notices what changed in the project's files: Creator's own writes (which don't count), the scan that builds the resource tree, and the watcher that reports outside changes. In production they share one `OwnWrites` and the hidden generated folder. |
| `blockTypeSetup: BlockTypesSetup?` | `blockTypeRepository`, `generatedPath`, `syncBuildFiles`, `writeDelayInMilliseconds` | Everything an opted-in project's block types need from outside: where they are kept, where their code is generated, how the build files follow them, and how long writes wait to be batched. Null together for a project that hasn't opted in. |
| `sceneRuntime: SceneRuntimeSetup` (F04, `:creator:scene-runtime`) | `creatorKubrikoVersion`, `creatorLibraries`, `entryPointLoader` | What the project's classes are loaded next to; handed to `SceneEditingController` unchanged. |
| `gitRepositories: GitRepositories` | `openGitRepository`, `createGitRepository` | Opening and creating the same project's repository, the two things `GitController` asks of the outside. |

Kept as they are: `project`, `parentScope`, `workspaceScope`, `ioDispatcher`, `currentTimeMillis` (environment seams that
several collaborators share), `initialFormatStatus` and `projectFormat` (the format and what preparing it found at opening).

`generatedPath` moving into `BlockTypesSetup` is behaviour-preserving: every caller that passes it today passes it together with
the repository it comes from (`blockTypeStore?.generatedPath` with `blockTypeRepository = blockTypeStore` in the opener,
`store.generatedPath` with `store` in the benchmark), and without block types there is no generated code for git to group.

### 1. New types (`:creator:workspace`)

Plain classes (not data classes) with `internal val` members, since only the workspace reads them, and KDoc on each class and
parameter (the parameter texts are today's `@param` texts where there are any):

```kotlin
/**
 * The outside processes the workspace builds and runs the project through, each closed or stopped with it.
 *
 * @param findDesktopModulePath Asks [build] for the Gradle path of the module that runs the game on desktop.
 * @param runGame Starts the game process Creator runs itself ([GameProcess]).
 */
public class ProjectProcesses(
    internal val build: ProjectBuild,
    internal val sessionChannel: SessionChannel,
    internal val runGame: suspend (GameLaunch, Map<String, String>) -> GameProcess.Exit,
    internal val findDesktopModulePath: suspend () -> String = { ":" },
)
```

```kotlin
/**
 * How the workspace notices what changed in the project's files.
 *
 * @param ownWrites The files as Creator itself last wrote them, which don't count as changed.
 * @param scanResources Builds the resource tree, from the watcher's snapshot when one is given.
 * @param startWatching Starts watching the project for outside changes, until the returned handle is closed.
 */
public class WorkspaceFiles(
    internal val ownWrites: OwnWrites = OwnWrites(),
    internal val scanResources: (File, ProjectSnapshot?) -> Map<ResourceGroup, List<ProjectResource>> = { root, snapshot -> ResourceScanner.scan(root, snapshot = snapshot) },
    internal val startWatching: (onChange: (Set<ProjectChange>, ProjectSnapshot) -> Unit) -> AutoCloseable = { AutoCloseable {} },
)
```

```kotlin
/**
 * What the block-built types of a project that opted in to them need from outside the workspace.
 *
 * @param generatedPath Where their code is generated, which version control groups with its types.
 * @param syncBuildFiles Brings the project's Kubriko dependencies in line with the code after each batch of writes.
 * @param writeDelayInMilliseconds How long changes wait to be written together with the next ones.
 */
public class BlockTypesSetup(
    internal val repository: BlockTypeRepository,
    internal val generatedPath: String? = null,
    internal val syncBuildFiles: () -> KubrikoBuildFileChanges? = { null },
    internal val writeDelayInMilliseconds: Long = 0,
)
```

```kotlin
/**
 * Opens the project's repository, or makes the project one.
 *
 * @param open The project's repository, or null if it has none.
 * @param create Makes the project a repository, committing everything with the given message.
 */
public class GitRepositories(
    internal val open: () -> GitRepository?,
    internal val create: (message: String) -> GitRepository,
) {
    public companion object {
        /**
         * The repository at [rootDirectory], through JGit.
         */
        public fun of(rootDirectory: File): GitRepositories = GitRepositories(
            open = { JGitRepository.open(rootDirectory) },
            create = { JGitRepository.create(rootDirectory, it) },
        )
    }
}
```

### 2. `WorkspaceController.kt`

New constructor:

```kotlin
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
)
```

(`gitRepositories`, not `git`, since `public val git: GitController` is a member; `blockTypeSetup`, not `blockTypes`, for the
same reason.) Replace every use in the body:
- `DesktopModuleLookup(scope, ioDispatcher, findDesktopModulePath)` → `processes.findDesktopModulePath`;
- `SceneStore(project.rootDirectory, ownWrites)` and `ownWrites.generation` in `scanResourcesNow` → `files.ownWrites`;
- `scanResources(project.rootDirectory, currentSnapshot)` in `scanResourcesNow` → `files.scanResources(…)`;
- `private val watching = startWatching(::onProjectChanged)` → `files.startWatching(::onProjectChanged)`;
- `GitController(openRepository = openGitRepository, createRepository = createGitRepository, generatedPath = generatedPath, …)` →
  `openRepository = gitRepositories.open, createRepository = gitRepositories.create, generatedPath = blockTypeSetup?.generatedPath`;
- `blockTypeRepository?.let { repository -> BlockTypesController(repository = repository, …, syncBuildFiles = syncBuildFiles, …,
  writeDelayInMilliseconds = writeDelayInMilliseconds, …) }` → `blockTypeSetup?.let { setup -> BlockTypesController(repository =
  setup.repository, …, syncBuildFiles = setup.syncBuildFiles, …, writeDelayInMilliseconds = setup.writeDelayInMilliseconds, …) }`,
  the other arguments (F02's `listener`, `isReadOnly`) unchanged;
- `CreatorTaskRunner(…, build = build, …)` and `withContext(ioDispatcher) { build.close() }` in `close()` → `processes.build`;
- `SessionController(channel = sessionChannel, …, runGame = runGame)` → `processes.sessionChannel`, `processes.runGame`;
- F04's `runtime = SceneRuntimeSetup(creatorKubrikoVersion, creatorLibraries, entryPointLoader)` → `runtime = sceneRuntime`.

KDoc: in "`@param workspaceScope` … made by whoever also starts the [sessionChannel] and the watcher in it", say
"[processes]' session channel" instead of "[sessionChannel]"; add `@param` lines for `processes`, `files`, `blockTypeSetup`
("null if the project hasn't opted in to block-built types (it has no `.kubriko/project.json`)"), `sceneRuntime`, and
`gitRepositories`. Drop the imports nothing uses any more (`BlockTypeRepository`, `GitRepository`, `JGitRepository`,
`ProjectBuild`, `ResourceScanner`, `SessionChannel`, `GameLaunch`, `GameProcess`, `EntryPointLoader`, `KubrikoBuildFileChanges`
if unused). The file must stay at most 500 lines.

### 3. `GradleWorkspaceOpener.kt` (the production wiring)

The `WorkspaceController(…)` call inside `openWorkspace(scope) { opening -> … }` becomes:

```kotlin
WorkspaceController(
    project = project,
    processes = ProjectProcesses(
        build = connection,
        sessionChannel = opening.track(SessionServer(opening.scope)),
        runGame = GameProcess(directory, SessionInitScript.playDirectoryOf(directory))::run,
        findDesktopModulePath = { DesktopModules.pathOf(connection) },
    ),
    parentScope = scope,
    workspaceScope = opening.scope,
    files = WorkspaceFiles(
        ownWrites = ownWrites,
        scanResources = { root, snapshot -> ResourceScanner.scan(root, hiddenDirectory = blockTypeStore?.generatedPath, snapshot = snapshot) },
        startWatching = { onChange ->
            opening.track(ProjectWatcher(directory, blockTypeStore?.generatedPath, opening.scope, Dispatchers.IO, ownWrites = ownWrites).start(onChange))
        },
    ),
    blockTypeSetup = blockTypeStore?.let { store ->
        BlockTypesSetup(
            repository = store,
            generatedPath = store.generatedPath,
            syncBuildFiles = KubrikoBuildFiles(directory, configuration.kubrikoVersion, configuration.useMavenLocal)::sync,
            writeDelayInMilliseconds = WRITE_DELAY_IN_MILLISECONDS,
        )
    },
    sceneRuntime = SceneRuntimeSetup(
        creatorKubrikoVersion = configuration.kubrikoVersion,
        creatorLibraries = creatorLibraries,
        entryPointLoader = ClassLoaderEntryPointLoader(directory),
    ),
    initialFormatStatus = formatStatus,
    projectFormat = projectFormat,
).also { it.sceneEditing.prepare() }
```

`directory` is `project.rootDirectory` (`CreatorProject(rootDirectory = directory, …)` above), so the game process and the
entry point loader are the ones the controller's defaults built before, created at the same moment. `gitRepositories` keeps
its default, as the opener passes none today. Add the imports (`GameProcess`, `SessionInitScript` from `session`,
`ClassLoaderEntryPointLoader` and `SceneRuntimeSetup` from `sceneRuntime`).

### 4. Test fixture `WorkspaceTesting.kt` (new, `:creator:workspace` test fixtures)

So that tests keep the flat, defaulted parameters they use today, add (with the MPL header and a KDoc saying it builds a
workspace with the controller's production defaults unless told otherwise):

```kotlin
public fun workspaceController(
    project: CreatorProject,
    parentScope: CoroutineScope,
    ioDispatcher: CoroutineDispatcher,
    build: ProjectBuild = FakeProjectBuild(),
    sessionChannel: SessionChannel = FakeSessionChannel(),
    findDesktopModulePath: suspend () -> String = { ":" },
    runGame: suspend (GameLaunch, Map<String, String>) -> GameProcess.Exit = GameProcess(project.rootDirectory, SessionInitScript.playDirectoryOf(project.rootDirectory))::run,
    ownWrites: OwnWrites = OwnWrites(),
    scanResources: (File, ProjectSnapshot?) -> Map<ResourceGroup, List<ProjectResource>> = { root, snapshot -> ResourceScanner.scan(root, snapshot = snapshot) },
    startWatching: (onChange: (Set<ProjectChange>, ProjectSnapshot) -> Unit) -> AutoCloseable = { AutoCloseable {} },
    blockTypeRepository: BlockTypeRepository? = null,
    generatedPath: String? = null,
    syncBuildFiles: () -> KubrikoBuildFileChanges? = { null },
    writeDelayInMilliseconds: Long = 0,
    creatorKubrikoVersion: String = "",
    creatorLibraries: Map<String, String> = emptyMap(),
    entryPointLoader: EntryPointLoader = ClassLoaderEntryPointLoader(project.rootDirectory),
    initialFormatStatus: ProjectFormatStatus = ProjectFormatStatus.Writable,
    openGitRepository: () -> GitRepository? = { JGitRepository.open(project.rootDirectory) },
    createGitRepository: (message: String) -> GitRepository = { JGitRepository.create(project.rootDirectory, it) },
): WorkspaceController = WorkspaceController(
    project = project,
    processes = ProjectProcesses(build, sessionChannel, runGame, findDesktopModulePath),
    parentScope = parentScope,
    ioDispatcher = ioDispatcher,
    files = WorkspaceFiles(ownWrites, scanResources, startWatching),
    blockTypeSetup = blockTypeRepository?.let { BlockTypesSetup(it, generatedPath, syncBuildFiles, writeDelayInMilliseconds) },
    sceneRuntime = SceneRuntimeSetup(creatorKubrikoVersion, creatorLibraries, entryPointLoader),
    initialFormatStatus = initialFormatStatus,
    gitRepositories = GitRepositories(openGitRepository, createGitRepository),
)
```

`workspaceScope` is left to the controller's default (`workspaceScopeOf` is `internal`); the one test that passes its own,
`WorkspaceOpeningTest`, builds the controller directly (step 5). `creator/workspace/build.gradle.kts` gains
`testFixturesApi(testFixtures(projects.creator.gradleSession))` (for `FakeProjectBuild` and `FakeSessionChannel`); the module's
own tests see its fixtures already. `creator/build.gradle.kts` gains `testImplementation(testFixtures(projects.creator.workspace))`
next to the other `testFixtures` lines.

### 5. Callers in tests

- `WorkspaceControllerTest.createController(…)` and `SelectionControllerTest.createController()`: call `workspaceController(…)`
  with the same named arguments they pass today (`openGitRepository = { gitRepository }` and the rest keep their names).
- `AppControllerTest.workspace(…)` and `CreatorBenchmarkTest` (`:creator`): the same, importing
  `com.pandulapeter.kubrikoCreator.workspace.workspaceController`.
- `WorkspaceOpeningTest.open(…)`: builds `WorkspaceController` directly with the new types:
  `processes = ProjectProcesses(build = opening.track(build), sessionChannel = opening.track(channel), runGame = { _, _ ->
  GameProcess.Exit(0, emptyList()) })` (it never plays), `workspaceScope = opening.scope`,
  `files = WorkspaceFiles(scanResources = { _, _ -> emptyMap() }, startWatching = { opening.track(AutoCloseable { isWatcherClosed = true }) })`,
  `blockTypeSetup = BlockTypesSetup(FakeBlockTypeRepository())`, and `gitRepositories = GitRepositories(open = { null }, create =
  { error("No repository is created here.") })`, the rest as today.

### 6. Docs

- `creator/CLAUDE.md` → Modules, the test fixtures sentence: after "commands' `CommandTesting.kt`, `FakeProjectRunner`, and
  `BreakoutFixture`;" add "workspace's `WorkspaceTesting.kt`;" before "ai's `FakeAgent` and `FakeAgentLauncher`".
- `creator/plan/README.md` → Structure: delete the bullet "**`WorkspaceController`'s constructor takes 23 parameters** …" (F05
  wrote it) and the lead-in "Left of the structure itself, before the classes involved grow further:" that only it was left
  under, so the paragraph ends at "…rather than in a Composable."

## Tests

- No behaviour changes, so the existing suites are the test: `WorkspaceControllerTest` (play, close, watcher, git, format, and
  Scene cases), `SelectionControllerTest`, `WorkspaceOpeningTest` (a failed or cancelled opening still closes the channel, the
  watcher, and the build, now reached through `ProjectProcesses` and `WorkspaceFiles`), `GradleWorkspaceOpenerTest`,
  `AppControllerTest`, and the benchmark (opt-in; compile it).
- Run `./gradlew :creator:allTests` and `./gradlew spotlessCheck`. After staging, `python3 .github/scripts/check_sizes.py` must
  print nothing (`WorkspaceController` now has 12 parameters, and every class this sweep touched is at most 12), and
  `python3 .github/scripts/check_doc_names.py --staged` must pass for the `CLAUDE.md` line.
- Grep the repository for each removed parameter name used as a named argument (`openGitRepository =`, `blockTypeRepository =`,
  `startWatching =`, `scanResources =`, `runGame =`, …) outside the fixture and the opener to confirm no caller was missed.
