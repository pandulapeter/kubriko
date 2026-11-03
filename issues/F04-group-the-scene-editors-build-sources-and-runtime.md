<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# Give `SceneEditingController` its build, its sources, and Creator's runtime as three named types instead of eleven parameters

**Kind:** architecture  ·  **Severity:** low  ·  **Platforms:** all (desktop app)
**Written against:** `9bb8df2a6`, after F02 and F03. Re-locate code by the quoted snippets, not by line numbers.
**Files:**
- `:creator:scene-runtime` (package `sceneRuntime`, under `creator/scene-runtime/src/main/kotlin/com/pandulapeter/kubrikoCreator/sceneRuntime/`):
  `SceneEditingController.kt`, `SceneClassesBuild.kt` (new), `SceneSources.kt` (new), `SceneRuntimeSetup.kt` (new),
  `creator/scene-runtime/src/test/kotlin/com/pandulapeter/kubrikoCreator/sceneRuntime/SceneEditingControllerTest.kt`
- `:creator:workspace`: `creator/workspace/src/main/kotlin/com/pandulapeter/kubrikoCreator/workspace/WorkspaceController.kt`,
  `…/workspace/WorkspaceSceneEditing.kt` (new)
- `creator/plan/README.md`

## Problem

`SceneEditingController`'s primary constructor takes 17 parameters (limit 12, `check_sizes.py`):

```kotlin
public class SceneEditingController(
    private val creatorKubrikoVersion: String,
    private val creatorLibraries: Map<String, String> = emptyMap(),
    private val runTask: suspend (taskName: String, properties: Map<String, String>) -> Boolean,
    private val entryPointLoader: EntryPointLoader,
    private val scope: CoroutineScope,
    private val releaseScope: CoroutineScope = scope,
    private val ioDispatcher: CoroutineDispatcher,
    private val report: (ConsoleEntry) -> Unit,
    private val readScene: (ProjectResource) -> List<SceneActorEntry>,
    private val readSceneProblem: (ProjectResource) -> SceneFileProblem? = { null },
    private val classpathFile: File,
    private val spriteFiles: () -> Map<String, File> = { emptyMap() },
    private val requestCleanBuild: () -> Unit = {},
    private val warmUpCompiler: suspend () -> Unit = {},
    private val actorTypeLooks: () -> Map<String, ActorTypeLook> = { emptyMap() },
    private val awaitWrites: suspend () -> Unit = {},
    private val currentTimeMillis: () -> Long = System::currentTimeMillis,
)
```

Its one production caller is `WorkspaceController`:

```kotlin
public val sceneEditing: SceneEditingController = SceneEditingController(
    creatorKubrikoVersion = creatorKubrikoVersion,
    creatorLibraries = creatorLibraries,
    runTask = tasks::run,
    entryPointLoader = entryPointLoader,
    classpathFile = SessionInitScript.classpathFileOf(project.rootDirectory),
    scope = scope,
    releaseScope = parentScope,
    ioDispatcher = ioDispatcher,
    report = ::appendToConsole,
    readScene = { scenes.actors(SceneStore.nameOf(it.file)).orEmpty() },
    readSceneProblem = { scenes.problemOf(SceneStore.nameOf(it.file)) },
    spriteFiles = { SpriteFiles.of(_resources.value).filesByAccessor },
    requestCleanBuild = incrementalCompilation::requestCleanBuild,
    warmUpCompiler = tasks::warmUpCompiler,
    actorTypeLooks = { ActorTypeLook.of(blockTypes?.actorTypes?.value.orEmpty(), SpriteFiles.of(_resources.value).files) },
    awaitWrites = blockTypes?.let { types -> { types.awaitWrites() } } ?: {},
)
```

## Fix

Three groupings, each defined in `sceneRuntime` next to its consumer. Result: 8 parameters (`runtime`, `classes`, `sources`,
`scope`, `releaseScope`, `ioDispatcher`, `report`, `currentTimeMillis`).

- **`SceneClassesBuild`** (`runTask`, `classpathFile`, `requestCleanBuild`, `warmUpCompiler`, `awaitWrites`): how the
  project's classes get built for the editor. They belong together because they are the one build `buildAndLoad` drives
  (wait for pending type writes, run the classpath task into `classpathFile`, ask for a clean build when loading fails, warm the
  compiler once loaded), all backed by the workspace's `CreatorTaskRunner` and incremental-compilation policy.
- **`SceneSources`** (`readScene`, `readSceneProblem`, `spriteFiles`, `actorTypeLooks`): what the open Scene is made of, read
  from the project as it is now. They belong together because each is a read of the workspace's current in-memory project (its
  `SceneStore`, resource scan, and block types) and nothing else; `readSceneProblem` is always called right after `readScene`.
- **`SceneRuntimeSetup`** (`creatorKubrikoVersion`, `creatorLibraries`, `entryPointLoader`): what the project's classes are
  loaded next to. They belong together because they decide one thing, whether and how a project's classes can share Creator's
  (`buildAndLoad` compares the classpath's Kubriko version with `creatorKubrikoVersion`, checks its libraries against
  `creatorLibraries` through `SharedLibraries.check`, then calls `entryPointLoader.load`). F06 passes this type through
  `WorkspaceController` unchanged.

### 1. New files in `sceneRuntime` (`:creator:scene-runtime`)

```kotlin
/**
 * How the project's classes are built for the Scene editor, one build at a time.
 */
public interface SceneClassesBuild {

    /**
     * Where the classpath task writes the project's classpath, the same file for every build ([SessionInitScript.classpathFileOf]).
     */
    public val classpathFile: File

    /**
     * Runs one of the init script's tasks with the given properties, reporting problems in the console; whether it worked.
     */
    public suspend fun runTask(taskName: String, properties: Map<String, String>): Boolean

    /**
     * Makes the next build compile everything from scratch, for when the classes may not match the sources: on Reload, and
     * when they couldn't be loaded.
     */
    public fun requestCleanBuild() {}

    /**
     * Gets the compiler ready for the first change, once [SceneEditingController.prepare] has loaded the classes.
     */
    public suspend fun warmUpCompiler() {}

    /**
     * Returns once the Actor types are loaded and their changes written, so that a build knows which looks it compiles.
     */
    public suspend fun awaitWrites() {}
}
```

```kotlin
/**
 * What the open Scene is made of, read from the project as it is now.
 */
public interface SceneSources {

    /**
     * The Actors of a Scene file, empty if it can't be read. Called off the main thread, while the project builds, so that the
     * [SceneStore] it reads from has the Scene parsed for the first command on it.
     */
    public fun readScene(scene: ProjectResource): List<SceneActorEntry>

    /**
     * Why a Scene file can't be read, or null if it can, called right after [readScene].
     */
    public fun readSceneProblem(scene: ProjectResource): SceneFileProblem? = null

    /**
     * The project's sprite files by the name of their resource accessor, which the Scene editor reads itself
     * ([SpriteLoadingEntryPoint]).
     */
    public fun spriteFiles(): Map<String, File> = emptyMap()

    /**
     * The looks of the block-built Actor types as they are now, whose changes the open Scene shows before they are compiled
     * ([SceneEditingState.Open.previews], kept current by [SceneEditingController.onActorTypesChanged]).
     */
    public fun actorTypeLooks(): Map<String, ActorTypeLook> = emptyMap()
}
```

```kotlin
/**
 * What a project's classes are loaded next to: Creator's own Kubriko version and library versions, which the project's must
 * match to share Creator's classes ([SharedLibraries]), and the loader that loads them.
 */
public class SceneRuntimeSetup(
    public val creatorKubrikoVersion: String,
    public val creatorLibraries: Map<String, String> = emptyMap(),
    public val entryPointLoader: EntryPointLoader,
)
```

The KDoc texts are the constructor's `@param` texts of today, moved; the members are `public` because `WorkspaceController`
(another module) implements the interfaces and, from F06, reads `creatorKubrikoVersion`. A plain class, not a data class:
nothing compares or copies it.

### 2. `SceneEditingController.kt`

```kotlin
public class SceneEditingController(
    private val runtime: SceneRuntimeSetup,
    private val classes: SceneClassesBuild,
    private val sources: SceneSources,
    private val scope: CoroutineScope,
    private val releaseScope: CoroutineScope = scope,
    private val ioDispatcher: CoroutineDispatcher,
    private val report: (ConsoleEntry) -> Unit,
    private val currentTimeMillis: () -> Long = System::currentTimeMillis,
)
```

Replace every use, keeping order and timing: `readScene(…)` → `sources.readScene(…)` and `readSceneProblem(…)` →
`sources.readSceneProblem(…)` (in `open` and in the background update), `spriteFiles()` → `sources.spriteFiles()` (in `show`),
`actorTypeLooks()` → `sources.actorTypeLooks()` (in `currentPreviews` and `buildAndLoad`), `awaitWrites()`, `runTask(…)`,
`classpathFile`, `requestCleanBuild()`, and `warmUpCompiler()` → the same on `classes.`, and `creatorKubrikoVersion`,
`creatorLibraries`, `entryPointLoader` → the same on `runtime.`. In the class KDoc, replace the `@param` lines that moved into
the new types with "`@param runtime`", "`@param classes`", and "`@param sources`" lines pointing at them; keep `releaseScope`'s.

### 3. `workspace/WorkspaceSceneEditing.kt` (new, `:creator:workspace`)

The workspace's implementations, so `WorkspaceController` stays short:

```kotlin
/**
 * The Scene editor's builds, run by the workspace's task runner after the type changes are written.
 */
internal class WorkspaceSceneClasses(
    private val tasks: CreatorTaskRunner,
    private val incrementalCompilation: IncrementalCompilationPolicy,
    override val classpathFile: File,
    private val blockTypes: BlockTypesController?,
) : SceneClassesBuild {
    override suspend fun runTask(taskName: String, properties: Map<String, String>) = tasks.run(taskName, properties)
    override fun requestCleanBuild() = incrementalCompilation.requestCleanBuild()
    override suspend fun warmUpCompiler() = tasks.warmUpCompiler()
    override suspend fun awaitWrites() {
        blockTypes?.awaitWrites()
    }
}

/**
 * What the open Scene is made of, read from the workspace's Scene store, resource scan, and block types.
 */
internal class WorkspaceSceneSources(
    private val scenes: SceneStore,
    private val resources: StateFlow<Map<ResourceGroup, List<ProjectResource>>>,
    private val blockTypes: BlockTypesController?,
) : SceneSources {
    override fun readScene(scene: ProjectResource) = scenes.actors(SceneStore.nameOf(scene.file)).orEmpty()
    override fun readSceneProblem(scene: ProjectResource) = scenes.problemOf(SceneStore.nameOf(scene.file))
    override fun spriteFiles() = SpriteFiles.of(resources.value).filesByAccessor
    override fun actorTypeLooks() = ActorTypeLook.of(blockTypes?.actorTypes?.value.orEmpty(), SpriteFiles.of(resources.value).files)
}
```

(`CreatorTaskRunner` is `internal` to `:creator:workspace`, with `suspend fun run(taskName: String, properties: Map<String,
String>): Boolean` and `suspend fun warmUpCompiler()`; `IncrementalCompilationPolicy.requestCleanBuild()` is public in
`:creator:gradle-session`. `resources.value` is what `_resources.value` was.)

### 4. `workspace/WorkspaceController.kt`

Replace the `SceneEditingController(…)` call above with

```kotlin
public val sceneEditing: SceneEditingController = SceneEditingController(
    runtime = SceneRuntimeSetup(creatorKubrikoVersion, creatorLibraries, entryPointLoader),
    classes = WorkspaceSceneClasses(tasks, incrementalCompilation, SessionInitScript.classpathFileOf(project.rootDirectory), blockTypes),
    sources = WorkspaceSceneSources(scenes, resources, blockTypes),
    scope = scope,
    releaseScope = parentScope,
    ioDispatcher = ioDispatcher,
    report = ::appendToConsole,
)
```

Keep its KDoc. Add the `SceneRuntimeSetup` import and drop `ActorTypeLook`'s, which nothing in the file uses any more
(`SpriteFiles` and `SceneStore` still are, by the `CommandContext` call and `selection`). The file shrinks by about 9 lines.

### 5. `creator/plan/README.md`

In the constructors bullet, replace "**Three constructors take more than 12 parameters** (`WorkspaceController` 23,
`AssistantController` 18, and `SceneEditingController` 17)" with "**Two constructors take more than 12 parameters**
(`WorkspaceController` 23 and `AssistantController` 18)".

## Tests

- `SceneEditingControllerTest.createController` builds the three types from the test's fields, with the same values:
  `runtime = SceneRuntimeSetup("1.0.0", creatorLibraries, entryPointLoader = { loader.load(it) })`,
  `classes = object : SceneClassesBuild { override val classpathFile = File(root, "classpath.txt"); override suspend fun runTask(…)`
  (the body of today's `runTask` lambda) `; override fun requestCleanBuild() { cleanBuildRequests++ }; override suspend fun warmUpCompiler() { warmUpCount++ }; override suspend fun awaitWrites() { writesGate?.await() } }`,
  and `sources = object : SceneSources { … }` with today's `readScene`, `readSceneProblem = sceneProblem`, and
  `actorTypeLooks = actorTypeLooks` (it passes no `spriteFiles`, so leave the default). `EntryPointLoader` is a `fun interface`
  (today's `entryPointLoader = { loader.load(it) }` already relies on that). No test case changes.
- `WorkspaceControllerTest`'s Scene cases (`theCompilerIsWarmedUpSilentlyOnceThePreparedClassesAreLoadedAndASessionWaitsForIt`,
  `aGameWaitsForTheSceneEditorsClasspathBuildAndThenBuildsItsOwn`, `aBuildWaitsForTheBlockTypeChangesThatAreStillBeingWritten`)
  and `SelectionControllerTest` cover the workspace's implementations unchanged.
- Run `./gradlew :creator:allTests` and `./gradlew spotlessCheck`; after staging, `python3 .github/scripts/check_sizes.py` must
  print nothing.
