<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# Build the app's controllers in an `AppGraph` before `application {}`, and quit through one `shutdown()` that runs off the UI thread under one timeout

**Challenged:** amended — wrote down one shutdown order across A01/A02a/H03b: the assistant closes on IO through A02a's thread-safe `AssistantHost.close()` (which refuses new agents after quit), the pending restart sequence is joined on Main, the project closes game-first.

**Kind:** architecture  ·  **Severity:** medium  ·  **Platforms:** all
**Findings:** ARCH-6, AI-12, TST-11 (app wiring part)  ·  **Depends on:** none; lane A's A01/A02 (phase 1) have landed by then, see below
**Files:** new `creator/src/main/kotlin/com/pandulapeter/kubrikoCreator/AppGraph.kt`, `.../KubrikoCreatorApp.kt`, `.../AppController.kt`, `.../settings/CreatorSettingsController.kt`, `.../ai/assistant/AssistantController.kt` (or, after A02, `AssistantSignIn.kt`), `.../ai/assistant/SignInSession.kt`, `.../mcp/McpController.kt` (only if `close` changes), `creator/src/test/kotlin/com/pandulapeter/kubrikoCreator/AppGraphTest.kt` (new), `.../settings/CreatorSettingsControllerTest.kt`, `.../ai/assistant/SignInSessionTest.kt` (or the existing test of the sign-in), `creator/CLAUDE.md`

## Problem

`KubrikoCreatorApp.kt` `main` builds the whole app inside composition:
```kotlin
application {
    val compositionScope = rememberCoroutineScope()
    val controller = remember { AppController(compositionScope) }
    val scope = controller.scope
    val creatorSettings = remember { CreatorSettingsController(CreatorSettingsStore.default(), scope, Dispatchers.IO) }
    …
    val mcp = remember { McpController(server = McpToolServer(…, version = CreatorConfiguration.load().kubrikoVersion), store = McpSettingsStore.default(), …) }
    val aiClients = remember { AiClientsController(…) }
    val assistant = remember { assistantController(scope, creatorSettings.isAiEnabled) { controller.openProjectCommands } }
```
- `CreatorSettingsController`'s constructor runs `MutableStateFlow(store.load())` and `McpController` loads its store: disk reads on the UI thread during the first composition.
- `CreatorConfiguration.load()` (which re-reads the bundled `creator.properties`) runs five times: `KubrikoCreatorApp.kt` twice (`McpToolServer` version, `assistantController`), `AppController`'s default `ProjectGenerator(CreatorConfiguration.load())`, `workspace/GradleWorkspaceOpener.kt`, and `commands/project/GetKubrikoApi.kt`.
- Quitting blocks the UI thread and can lose work:
  ```kotlin
  onCloseRequest = {
      mcp.close()          // McpToolServer.stop(timeoutMillis = 1_000)
      assistant.close()    // AcpAgent.close → ProcessAgentLauncher.stop: destroy, waitFor up to 3 s, then force
      val closing = controller.closeProject()
      scope.launch {
          withTimeoutOrNull(CLOSING_TIMEOUT_IN_MILLISECONDS) { closing?.join() }
          exitApplication()
      }
  },
  ```
  Up to ~5 s with a frozen window after the click; a second click runs all of it again. `AssistantController.close()` does
  `_signIn.value?.let { session -> scope.launch { session.stop() } }`, which races `exitApplication()` and can lose, leaving the
  sign-in program (`claude /login`, another agent's) running. `CreatorSettingsController.update` saves with
  `scope.launch(ioDispatcher) { saves.withLock { store.save(_settings.value) } }`, which the exit can cut off: a setting changed
  just before quitting may not be saved.
- None of this wiring is tested.

## Fix

1. `AppGraph.kt` (root package):
   ```kotlin
   /** Kubriko Creator's app-wide parts, made once before the window and closed in order when it quits ([shutdown]). */
   internal class AppGraph(
       val configuration: CreatorConfiguration,
       val scope: CoroutineScope,                  // SupervisorJob + Dispatchers.Main + unexpectedProblemHandler(controller::reportUnexpectedProblem)
       val controller: AppController,
       val creatorSettings: CreatorSettingsController,
       val mcp: McpController,
       val aiClients: AiClientsController,
       val assistant: AssistantController,
       private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
   ) {
       /**
        * Closes everything within [timeoutInMilliseconds]: the AI server and agent (on [ioDispatcher], since they wait for
        * processes), the open project's writes, and the settings' last save, side by side.
        */
       suspend fun shutdown(timeoutInMilliseconds: Long = CLOSING_TIMEOUT_IN_MILLISECONDS)

       companion object { fun create(): AppGraph }   // the production wiring now in main and assistantController()
   }
   ```
   - `create()` runs in `main` before `application {}` (on the main thread, not the AWT event thread, before any window exists), so the stores' constructor reads no longer run in composition. `AppController` gets `parentScope = scope` and `projectCreator = ProjectGenerator(configuration)`; `McpToolServer` and `assistantController` get `configuration.kubrikoVersion`. Pass `configuration` on to `GradleWorkspaceOpener` (it is an `object` with `CreatorConfiguration.load()` inside `open`; give `open` the configuration through the `WorkspaceOpener` it is passed as, or make it a class constructed with it) and leave `GetKubrikoApi`'s lazy load alone (it is lazy and runs once per command instance; out of scope). **K09:** if K09 turned `GradleWorkspaceOpener` into a factory, pass the configuration to that factory instead.
   - `AppController`'s `scope` today is built from `parentScope` inside it; keep that (the graph's `scope` is the parent) so `AppControllerTest` is untouched.
   - `shutdown()`:
     ```kotlin
     withTimeoutOrNull(timeoutInMilliseconds) {
         coroutineScope {
             launch(ioDispatcher) { mcp.close() }
             launch(ioDispatcher) { assistant.close() }
             launch { assistant.awaitPending() }
             launch { controller.closeProject()?.join() }
         }
         creatorSettings.awaitSaves()
     }
     ```
     Each `launch` catches and prints its own `Exception` (not `CancellationException`), so one failing close doesn't skip the others.
     **The one shutdown order** (lane A's A01/A02a and H03b fit into it; keep it as written):
     1. On the click (Main): the `isQuitting` guard, then `graph.scope.launch { shutdown(); exitApplication() }`.
     2. Side by side, inside the timeout:
        - `mcp.close()` on IO (A01 made `McpToolServer.stop()` safe from any thread);
        - `assistant.close()` on IO: A02a's `AssistantHost.close()` marks the host closed (so a chat or sign-in waiting in `get()`, or a creation finishing later, can't leave a new agent behind: an agent process outlives Creator's JVM), takes the current agent atomically and closes it, blocking until its process is gone; the sign-in session is taken atomically too and stopped with `stopNow()` (step 3). Nothing in it reads Main-confined state, which is why it may run on IO; no conversation is closed one by one at quit (no `session/close`), as today;
        - on Main, `assistant.awaitPending()` (A02a's host: joins A01's restart sequence, if one is running, so an agent being replaced at that moment is closed before the JVM exits; A01 bounds each `session/close` the sequence waits for);
        - on Main, `controller.closeProject()?.join()`: after H03b, the game is stopped and gone first, then the type writes finish, then the workspace scope is cancelled and Gradle closed.
     3. Then `creatorSettings.awaitSaves()`, so the last setting change is on disk.
     4. `exitApplication()`. Whatever is still running when the timeout ends is cut off; H03b's shutdown hook kills a game left alive.
     If A02a didn't land (its plan was skipped), make the same three changes in `AssistantController` itself: a `@Volatile isClosed` checked after the creation in `assistant()`, `assistant.getAndUpdate { null }?.close()`, and a Main-side join of A01's `lifecycle`.
   - `KubrikoCreatorApp.kt`: `val graph = AppGraph.create()` before `application {}`; composition only reads `graph.*`. `onCloseRequest`: guarded by an `AtomicBoolean isQuitting` (a second click does nothing), `graph.scope.launch { graph.shutdown(); exitApplication() }`. Hide nothing: the window stays until `exitApplication`, but no longer freezes. `exitApplication` is the `ApplicationScope` one, captured in the composable.

2. `CreatorSettingsController.awaitSaves()`: `suspend fun awaitSaves() = saves.withLock { store.save(_settings.value) }` would double-write; instead track the last save `Job` (`private var lastSave: Job?`, replaced in `update`) and `awaitSaves() = lastSave?.join()`. Since saves write the newest settings under the mutex, joining the last one is enough.

3. AI-12, the sign-in stop: `SignInSession` gets a blocking `fun stopNow()` (`channel.stop(); scope.cancel()`), KDoc: for quitting, called off the main thread. `AssistantController.close()` (after A02a: `AssistantSignIn.stop`'s quit form) takes the session with `_signIn.getAndUpdate { null }` and calls `stopNow()` on it instead of launching `stop()`, then closes the assistant through `host.close()`. `AssistantController.awaitPending()` delegates to the host's. `close()`'s KDoc says it blocks until the agent's processes are gone, so it is called on IO. **Lane A:** A01 (host lifecycle, close swallowing protocol-closed) and A02 (split into `AgentSelection`/`AssistantHost`/`AssistantSignIn`) land in phase 1, before this plan. Apply the sign-in change to whichever class owns the sign-in session after A02 (`AssistantSignIn`), and keep `AssistantController.close()` (or the A02 facade's) as the one call `shutdown` makes. Drop step 3 if A01/A02 already made the sign-in stop synchronous on close.

`creator/CLAUDE.md`: the Processes paragraph's "…and quitting waits for that" becomes: quitting (`AppGraph.shutdown`) closes the AI server, the agent, and the open project side by side off the UI thread, then the settings' last save, all within 10 s. Mention `AppGraph` in the package table's root row (or wherever `KubrikoCreatorApp` is described).

## Tests

`AppGraphTest` (new):
- (An agent created after quit is closed and refused by A02a's `AssistantHostTest.aCreationFinishingAfterCloseIsClosedAndRefused`; don't repeat it here.)
- `shutdownClosesTheAiPartsOnIoAndWaitsForTheProjectsWrites`: an open workspace whose `FakeBlockTypeRepository` blocks a write on a `CompletableDeferred`; `shutdown()` doesn't return until it is completed; record the thread names of the AI closes → not the test's main dispatcher thread.
- `aCloseThatThrowsDoesNotSkipTheOthers`.
- `shutdownGivesUpAfterTheTimeout`: a write that never ends, `shutdown(timeoutInMilliseconds = 1_000)` on virtual time returns after 1 s.
`CreatorSettingsControllerTest.aSettingChangedJustBeforeQuittingIsSaved`: `setTheme(DARK)`; `awaitSaves()`; a new `CreatorSettingsStore` on the same file loads `DARK` (with `StandardTestDispatcher` as `ioDispatcher`, which fails without `awaitSaves`).
`SignInSessionTest.stopNowEndsTheCommandBeforeReturning`: a real `sleep 30` (or a `FakeAgentChannel` recording `stop()`) → after `stopNow()` returns, the channel is stopped (no coroutine needed).

## Manual check

`:creator:run`, open a project, start Play and the assistant, and quit: the window doesn't freeze, the app exits within a few seconds, and `ps` shows no game or agent process left; change the theme and quit right away: it is kept on the next start.
