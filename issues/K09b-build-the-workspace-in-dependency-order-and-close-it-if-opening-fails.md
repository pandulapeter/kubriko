<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# Build the workspace's parts in dependency order without forward references, let commands see the block-built types through an interface, and run the session server and watcher in the workspace scope, closing everything when opening fails.

**Kind:** architecture  ·  **Severity:** medium  ·  **Platforms:** all
**Findings:** INF-16 (construction and opener parts; the `AppController` catch-all landed in 728c3a06b), ARCH-2  ·  **Depends on:** K09a, and every other lane K plan that adds `CommandContext` members (K01b, K03, K06), so the interface is extracted once
**Files:** `creator/src/main/kotlin/com/pandulapeter/kubrikoCreator/workspace/WorkspaceController.kt`, `workspace/GradleWorkspaceOpener.kt`, `workspace/BlockTypesController.kt` (implements the interface), new `commands/BlockTypes.kt` (the interface), `commands/CommandContext.kt`, `commands/actorTypes/ActorTypeValidation.kt` (`requireBlockTypes`' type), `workspace/SessionController.kt` and `workspace/GameBuilds.kt` (only to break their cycle), `project/ProjectWatcher.kt` (only if its `start` signature changes), tests `workspace/WorkspaceControllerTest.kt` (or T03's split), new `workspace/GradleWorkspaceOpenerTest.kt` (or a test of the factory function), `commands/CommandTesting.kt`, `creator/CLAUDE.md` (the `workspace` row)

Lane H's H03 (orderly shutdown) also changes `WorkspaceController.close()` and `AppController.openProject`; H01/H02 change other parts of `WorkspaceController`. Re-read the file after lane H merged, if it has, and keep their changes.

## Problem

`WorkspaceController`'s constructor has 21 parameters and builds the graph with forward references through lambdas that only work because they run later:

```kotlin
    private val tasks = CreatorTaskRunner(
        ...
        awaitWrites = { blockTypes?.awaitWrites() },          // blockTypes is declared further down
    ...
    private val sessions: SessionController = SessionController(
        ...
        prepareLaunch = { gameBuilds.prepareLaunch() },        // gameBuilds is declared right after
    ...
    val sceneEditing = SceneEditingController(
        ...
        actorTypeLooks = { ActorTypeLook.of(blockTypes?.actorTypes?.value.orEmpty(), ...) },
        awaitWrites = { blockTypes?.awaitWrites() },
```

A call during construction would hit a null. `commands/CommandContext.kt` takes the concrete `BlockTypesController?` (from `workspace`), so `commands` depends on `workspace`, which blocks lane M's module split.

`workspace/GradleWorkspaceOpener.kt` creates the session server and the watcher in the app scope it was given, and doesn't clean up when building the workspace fails:

```kotlin
        val connection = GradleProjectConnection(directory)
        return WorkspaceController(
            ...
            sessionChannel = SessionServer(scope),
            ...
            startWatching = { onChange ->
                ProjectWatcher(directory, blockTypeStore?.generatedPath, scope, Dispatchers.IO, ownWrites = ownWrites).start(onChange)
            },
        ).also { it.sceneEditing.prepare() }
```

If the constructor or `sceneEditing.prepare()` throws, the `ServerSocket`, the Gradle connection, the watcher, and the workspace scope's already-launched coroutines (the type loader, the write loop) leak for the rest of the session.

## Fix

1. **`commands/BlockTypes.kt`**: `internal interface BlockTypes` with exactly the members commands and `context/` use of `BlockTypesController` (`grep -rhn "requireBlockTypes\.\|blockTypes\.\|blockTypes?\." creator/src/main/kotlin/com/pandulapeter/kubrikoCreator/commands creator/src/main/kotlin/com/pandulapeter/kubrikoCreator/context` lists them: `types`, `actorTypes`, `managerTypes`, `folders`, `get`, `actorType`, `managerType`, `takenNames`, `put`, `delete`, `rename`, `createFolder`, `deleteFolder`, `discardChangesMadeByHand`, `awaitWrites`, `reload`, `unsavedNames`, `failure`, `select` if used, …), the mutating ones `@EditorMutation`. `BlockTypesController : BlockTypes`. `CommandContext.blockTypes: BlockTypes?`. The UI keeps using `BlockTypesController`. `GitController` and `SceneStore` stay concrete (their packages sit below `commands`; lane M's layering test decides whether they need interfaces).
2. **Construction order.** Declare the parts so that every lambda reads only parts declared before it: `blockTypes` (its callbacks go to the K09a router, which is constructed first and given its targets as functions over parts that already exist, or which receives `BlockTypesController`'s results through a `SharedFlow` it collects once everything exists; choose the latter if the former still needs a later part), then `tasks` and `sceneEditing` (`awaitWrites = blockTypes::awaitWrites` or a no-op when null), then sessions and game builds.
   - The one real cycle: `SessionController(prepareLaunch = { gameBuilds.prepareLaunch() })` and `GameBuilds(sessions = sessions, …)` (it reads `sessions.state` for hot reload). Break it without a forward reference: preferably `GameBuilds` takes `sessionState: StateFlow<SessionState>` from a `MutableStateFlow` the factory creates first and `SessionController` publishes into, or `SessionController.play(firstScene, prepareLaunch)` takes the preparation as an argument. Note which in the commit; keep the public behaviour.
   - Move the construction into a factory (`WorkspaceController.create(...)` companion function or a `WorkspaceParts` class) if the constructor stays over ~10 parameters after this; keep `WorkspaceController` under 500 lines.
3. **Scopes.** The opener creates the workspace scope (`CoroutineScope(parent.coroutineContext + SupervisorJob(parent.coroutineContext[Job]) + unexpectedProblemHandler(...))`, as `WorkspaceController` does today) and passes it in, or `WorkspaceController` takes factories `createSessionChannel: (CoroutineScope) -> SessionChannel` and `startWatching: (CoroutineScope, (Set<ProjectChange>) -> Unit) -> AutoCloseable` and calls them with its own scope. Either way `SessionServer` and `ProjectWatcher` run in the workspace scope and stop with it.
4. **Closing on failure.** In the opener: `try { create(...).also { it.sceneEditing.prepare() } } catch (exception: Exception) { workspaceScope.cancel(); sessionChannel?.close(); withContext(NonCancellable + Dispatchers.IO) { connection.close() }; throw exception }` (whatever was created; cancellation rethrown after the same cleanup). `AppController.openProject` already reports the reason (728c3a06b).
5. `creator/CLAUDE.md` `workspace` row: mention that the session server and watcher belong to the workspace scope and that a failed opening closes what it started; one sentence.

## Tests

- `WorkspaceControllerTest.nothingReadsAPartBeforeItExists`: construct with a `FakeBlockTypeRepository` whose `load()` and a `findDesktopModulePath` that call back immediately (UnconfinedTestDispatcher) → no exception (it failed with a forward null before, when a lambda ran during construction).
- Opener/factory test `aFailedOpeningClosesWhatItStarted`: a fake session channel and watcher factory recording `close`, a `sceneEditing.prepare` (or an injected step) that throws → the exception propagates, the channel and watcher are closed, the scope is cancelled (a child launched in it is cancelled), and the fake connection's `close` ran.
- `CommandTesting.commandContext(...)` takes a `BlockTypes?`; existing command tests compile unchanged (they pass a `BlockTypesController`).
- A small `FakeBlockTypes` is not needed; don't add one (the real controller over the fake repository is what the tests use).
