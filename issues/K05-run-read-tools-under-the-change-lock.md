<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# Run read tools under the executor's change lock, so an AI never reads a project half-way through another change.

**Challenged:** amended — `get_kubriko_api` downloads Kubriko's sources from Maven Central on a cache miss and reads nothing of the project, so it stays outside the lock (under it, every user edit would wait for a download); reads that don't read the project opt out through a `ReadCommand` flag; the Problem's example follows K02a's amended order; the deadlock and starvation checks are spelled out.

**Kind:** bug  ·  **Severity:** medium  ·  **Platforms:** all
**Findings:** CMD-8  ·  **Depends on:** K03 (same file and the same `step`/`execute` blocks; order only)
**Files:** `creator/src/main/kotlin/com/pandulapeter/kubrikoCreator/commands/CommandExecutor.kt`, `commands/ReadCommand.kt`, `commands/project/GetKubrikoApi.kt` (and `commands/actorTypes/GetBlockCatalog.kt`, `commands/project/ReadConsole.kt` if they opt out), test `commands/CommandExecutorTest.kt`

## Problem

`CommandExecutor.call` runs reads and actions without the `changes` mutex:

```kotlin
            val outcome = when (command) {
                is ReadCommand -> command.read(context, input)
                is WriteCommand -> execute(Invocation(command, input), origin, isByAi = true)
                is ActionCommand -> command.run(context, input)
            }
```

Tool calls run on the main thread, but a write suspends mid-apply on `withContext(ioDispatcher)` (e.g. `RenameActorType` between the in-memory rename and the Scene writes, `AddProperty` between `put` and `changeSceneStates`, after K02a), which frees the main thread for another client's or conversation's read: `read_scene`, `get_project_digest`, `get_relations`, or `get_health_checks` then sees the type renamed but the Scenes not yet, or the other way round. Each source alone is a consistent snapshot; the tearing is across one multi-step write.

## Fix

- `is ReadCommand -> if (command.readsProject) changes.withLock { command.read(context, input) } else command.read(context, input)`, with `val readsProject: Boolean get() = true` on `ReadCommand` (KDoc: "Whether it reads the open project, and so waits for a change in progress; false for one that reads only Kubriko Creator's own knowledge."). `GetKubrikoApi` overrides it to `false`: `KubrikoSources.find` blocks on an HTTP download of a sources JAR when it isn't cached yet (seconds, or the 30 s connect timeout offline), and every user change (`dispatch`, typing included) would queue behind it. `GetBlockCatalog` and `ReadConsole` may override it too (in-memory, no project files); leave the rest at `true`.
- The project reads that suspend on IO under the lock (`read_scene`, `list_scenes`, `read_game_notes`, `get_project_settings`, `get_project_overview`, `get_project_upgrade`) read a few small files each; holding the lock for that is fine. While one runs, a user's `dispatch` waits for it instead of showing at once, as it already waits for an AI write; say so in the `dispatch` KDoc ("…shows before this returns, unless an AI tool call holds the change lock").
- `ActionCommand`s (wait, build, play, take a screenshot, press keys) stay outside the lock: they run for seconds to minutes, and some of them (build) wait for writes that would need the lock.
- The final `publish(step.copy(target = runCatching { command.target(context, input) }…))` in `finally` stays outside the lock; `target` reads one in-memory value. Don't move it inside a `withLock` in a `finally` (it would block cancellation).
- `Mutex` isn't reentrant: check that no `ReadCommand.read` calls back into `execute`/`step`/`call` (grep `context.` uses in `commands/**/Read*.kt`, `Get*.kt`, `List*.kt`, `CheckChange.kt`; `check_change` calls `command.validate` directly, not the executor, so it is fine). A read that did would deadlock; add that sentence to the `ReadCommand` KDoc: "Runs while no change is being made, so it must not make one."
- Deadlock and starvation, checked: no code path calls the executor through `runBlocking` (none in `creator/src/main`), tool calls hop to Main with `withContext` (`McpToolServer`), so a read waiting for the lock suspends without blocking the thread that the holder resumes on; the writes that wait inside the lock (`Commit`, `DiscardChanges`, `StartVersionControl` → `awaitWrites`) wait for `BlockTypesController`'s write loop, which never takes the lock, and after K01a they return once the attempt fails instead of waiting for it to succeed. `Mutex` is FIFO, so a read queued behind a write runs right after it. A write that waits long (a slow git commit) holds reads up for that long, which is the point.
- A plain mutex serializes reads with each other too; at tool-call rates that costs nothing measurable (kotlinx has no read/write mutex).

## Tests

`CommandExecutorTest.aReadWaitsForAChangeInProgress` (`StandardTestDispatcher`): a test `WriteCommand` whose `apply` changes type `A`, suspends on a `CompletableDeferred` gate, then changes Scene `level`; start it with `launch { executor.execute(...) }`, `runCurrent()`; start `executor.call("read_scene", {scene: level})` in another `launch`, `runCurrent()` → the read hasn't completed; complete the gate, `advanceUntilIdle()` → the read's result contains the Scene's second change. A second test: two reads back to back both complete (no deadlock), and an `ActionCommand` (`wait`, with `FakeProjectRunner`) completes while a write is held at the gate. A third: a test `ReadCommand` with `readsProject = false` (custom `registry`) completes while a write is held at the gate, and `GetKubrikoApi.readsProject` is `false`.
