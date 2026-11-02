<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# Close a workspace only once its game process (and that process's children) is gone, kill leftovers at JVM exit, and open a project only after the previous one closed

**Challenged:** amended — `closeProject()` keeps the closing job only when it closed a workspace, since `createProject`'s inner `openProject` call would otherwise reset it to null and skip the wait; one test added. Fits H03a's shutdown order (game first, then writes). Then amended by the orchestrator: agent processes register with the shutdown hook too, so an agent still being replaced when quitting times out can't outlive Creator.

**Kind:** bug  ·  **Severity:** medium  ·  **Platforms:** all (descendants matter most for Hot Reload and Gradle-started sessions)
**Findings:** INF-8  ·  **Depends on:** H03a (quitting already waits off the UI thread through `AppGraph.shutdown`)
**Files:** `creator/src/main/kotlin/com/pandulapeter/kubrikoCreator/workspace/SessionController.kt`, `.../workspace/WorkspaceController.kt` (or K09's controller), `.../session/GameProcess.kt`, new `.../session/LiveGameProcesses.kt`, `.../ai/assistant/ProcessAgentLauncher.kt` (wherever lane A left the agent `Process` start), `.../AppController.kt`, `creator/src/test/kotlin/com/pandulapeter/kubrikoCreator/workspace/WorkspaceControllerTest.kt` (or K09/T03's split `SessionControllerTest`), `.../session/GameProcessTest.kt`, `.../AppControllerTest.kt`, `creator/CLAUDE.md`

## Problem

- `workspace/SessionController.kt`:
  ```kotlin
  override fun close() {
      job?.cancel()
      channel.close()
  }
  ```
  cancels the play job without joining it. `WorkspaceController.close()` then only waits for writes:
  ```kotlin
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
  ```
  The game is stopped in `session/GameProcess.kt` `run`'s `catch (exception: CancellationException) { withContext(NonCancellable) { stop(process) } … }`, where `stop` is `process.destroy(); if (!process.waitFor(3 s)) { destroyForcibly(); waitFor() }`. Quitting can call `exitApplication()` while that is still running on an IO (daemon) thread, so the JVM exits with the game alive.
- `Process.destroy()` kills only the direct child. A Hot Reload launch or a Gradle-run session (`runTask(SESSION_TASK_NAME)` when the build gave no `launch.` lines) leaves descendants running.
- No shutdown hook: a JVM exit by any other route (Cmd+Q path not through `onCloseRequest`, a crash) leaves the game running.
- `AppController.openProject` calls `closeProject()` and ignores the returned job, so reopening the same project starts a new `BlockTypesController` that loads definitions while the old one still flushes its writes, and its regeneration races the old batch.

## Fix

1. `SessionController.close()` stays non-suspending (it's `AutoCloseable`) but returns nothing new; add `suspend fun closeAndJoin() { val running = job; close(); running?.join() }` (cancelling then joining `job`, whose `GameProcess.run` returns only once the process is gone). Make `job` private if nothing outside reads it (it is `var job: Job?` today; check `WorkspaceControllerTest`).
2. `WorkspaceController.close()`: inside the returned job, `sessions.closeAndJoin()` first (the game stops while writes finish), then the pending writes as K01b left them (`withTimeoutOrNull(10 s) { awaitWritten() }` — keep K01b's line, don't restore `blockTypes?.awaitWrites()`), `scope.cancel()`, `build.close()`. Keep `watching.close()` and `sceneEditing.dispose()` synchronous at the top. **K09:** if K09 moved close into the split controller/factory, apply the same order there; the point is "join sessions before cancelling the scope".
3. `GameProcess.stop(process)`: `val descendants = process.descendants().toList()` before destroying; `process.destroy()`; `descendants.forEach(ProcessHandle::destroy)`; after the 3 s wait, `destroyForcibly()` both the process and every descendant still `isAlive`.
4. `session/LiveGameProcesses.kt`: an `internal object LiveGameProcesses` with a concurrent set of `Process`; `GameProcess.run` adds the process right after `start()` and removes it in its `finally`. The assistant's agent processes register too (`ProcessAgentLauncher`'s channel adds its process after starting it and removes it when it stops), since an agent replacement still running when quitting's timeout ends would otherwise outlive Creator; if lane A's plans moved that code, register wherever the agent `Process` is started now. On first add it installs one `Runtime.getRuntime().addShutdownHook(Thread { … })` that `destroyForcibly()`s each live process and its descendants (no waiting: hooks must be quick). Install lazily, once (`AtomicBoolean`), and never throw from the hook (catch `Exception`).
5. `AppController`: keep the last closing job (`private var closing: Job?`), set in `closeProject()` only when it closed a workspace (`controller?.close()?.also { closing = it }`): `createProject` calls `openProject`, whose own `closeProject()` finds no workspace, and must not replace the job the first call kept with null; `showNewProject` likewise. `openProject` and `createProject`, inside `launchOperation`, start with `withTimeoutOrNull(CLOSING_TIMEOUT_IN_MILLISECONDS) { closing?.join() }` before opening, while the screen already says Opening/Creating. Move `CLOSING_TIMEOUT_IN_MILLISECONDS` from `KubrikoCreatorApp.kt` to `AppController`'s companion (H03a's `AppGraph.shutdown` uses it from there). A cancelled open (the user picked another project meanwhile) just stops waiting.

`creator/CLAUDE.md`, Processes paragraph ("Each workspace runs in a scope of its own …, which `close()` cancels once the pending writes are done … and quitting waits for that"): say that closing first stops the game and waits until it (and any process it started) is gone, that a game still running when Creator's JVM exits is killed by a shutdown hook (`LiveGameProcesses`), and that opening a project waits for the previous one to close.

## Tests

- `WorkspaceControllerTest.closingWaitsUntilTheGameHasStopped`: `runGame = { _, _ -> try { awaitCancellation() } finally { withContext(NonCancellable) { stopped.await() } } }` (a `CompletableDeferred<Unit>` the test completes); play; `val closing = controller.close()`; `runCurrent()` → `closing.isCompleted == false`; `stopped.complete(Unit)`; → completed. Fails at HEAD, where close doesn't join.
- `GameProcessTest.stoppingTheGameAlsoStopsTheProcessesItStarted`: extend the existing test game (`GameProcessTestGame`, which T-lane's TST fix gave a pid file) with a `"spawn"` mode that starts a child `java … sleep` and writes both pids; cancel `run` → both `ProcessHandle.of(pid)` are no longer alive. Gate nothing: it uses the test JVM's own `java`.
- `GameProcessTest.aRunningGameIsTrackedUntilItEnds`: while running, `LiveGameProcesses` contains it; after cancel, it doesn't. (The hook itself isn't unit-tested.)
- `ProcessAgentLauncherTest.aRunningAgentIsTrackedUntilItStops`: the same for an agent process started through the launcher (a test JVM `java` program), removed once its channel stops.
- `AppControllerTest.reopeningAProjectWaitsUntilThePreviousOneClosed`: a `FakeBlockTypeRepository` whose save blocks on a `CompletableDeferred`; make a change, `openProject(directory)` again → the opener isn't called (`openedDirectories.size == 1`) until the deferred completes, then it is.
- `AppControllerTest.reopeningGivesUpWaitingAfterTheTimeout` on virtual time.
- `creatingAProjectRightAfterClosingOneWaitsForTheClose`: same blocked save; `createProject(request)` → the opener isn't called for the new project until the deferred completes (fails if `openProject`'s inner `closeProject()` resets `closing` to null).

## Manual check

macOS: Play a Hot Reload project from `:creator:run`, quit Creator: `ps -ef | grep -i kubriko` shows no game or Hot Reload JVM left. Then `kill -TERM` Creator's JVM while a game plays: the game goes too.
