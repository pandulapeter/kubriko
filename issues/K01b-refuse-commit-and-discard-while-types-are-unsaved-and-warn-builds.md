<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# Make awaitWrites say whether everything is on disk, refuse Commit, Discard, and Start version control while it isn't, and let builds, Play, and Scenes go ahead with a console warning naming the unsaved types.

**Challenged:** amended — names T03's `CreatorTaskRunnerTest` in Files; awaitWrites' result comes from K01a's written generation, not from `pendingWrites.isEmpty()` at resumption (which said false for a write queued after the call, and true mid-batch); awaitWritten waits on K01a's in-flight-inclusive count.

**Kind:** bug  ·  **Severity:** high  ·  **Platforms:** all
**Findings:** INF-2 = CMD-6 (the `awaitWrites` part)  ·  **Depends on:** K01a
**Files:** `creator/src/main/kotlin/com/pandulapeter/kubrikoCreator/workspace/BlockTypesController.kt`, `commands/CommandContext.kt`, `commands/git/Commit.kt`, `commands/git/DiscardChanges.kt`, `commands/git/StartVersionControl.kt`, `commands/Applied.kt`, `workspace/CreatorTaskRunner.kt`, `workspace/WorkspaceController.kt` (the `awaitWrites` lambdas and `close()`), `creator/src/main/composeResources/values/strings.xml`, tests `workspace/BlockTypesControllerTest.kt`, `commands/git/*` command tests (find them with `grep -rl "Commit\b" creator/src/test`), `workspace/WorkspaceControllerTest.kt` (or the split files lane T's T03 made, `CreatorTaskRunnerTest.kt` among them: it builds the runner with `awaitWrites: suspend () -> Unit`, which this plan changes), `creator/CLAUDE.md` (the `awaitWrites` sentence in the `workspace` row and the version control section)

## Problem

After K01a, `awaitWrites()` returns either when everything is written or when the pipeline is blocked by a failure, and its callers can't tell which:

```kotlin
// commands/git/Commit.kt
    override suspend fun apply(context: CommandContext, input: Input): Applied {
        context.awaitWrites()
        return if (context.git.commit(input.message)) Applied(inverse = null) else Applied.failed(Res.string.status_git_failed)
    }
// commands/git/DiscardChanges.kt
        context.awaitWrites()
        if (!context.git.discard(input.paths)) return Applied.failed(Res.string.status_git_failed)
        context.reloadTypes()
// commands/git/StartVersionControl.kt
        context.awaitWrites()
        return if (context.git.startVersionControl()) ...
// workspace/CreatorTaskRunner.kt run()
        console.append(ConsoleEntry.Status(Res.string.status_building))
        awaitWrites()
// workspace/WorkspaceController.kt close()
        return parentScope.launch {
            blockTypes?.awaitWrites()
            scope.cancel()
```

So a commit records the old files while the user believes their edit is in it; a discard restores files and then reloads, while the failed write (still queued) later writes the discarded version back; a build runs the old code without saying so; closing cancels the retries of edits that were never written.

## Fix

The user decided (decisions.md, "Failed type saves"): Build/Play proceed with a console warning naming unsaved types; Commit/Discard are refused until on disk. Start version control makes the first commit, so it is refused like Commit.

1. `BlockTypesController.awaitWrites(): Boolean`: `true` when every change queued before the call is on disk, `false` when the attempt covering it failed: `val wanted = queuedGeneration; attempted.first { it >= wanted }; return writtenGeneration >= wanted` (K01a's generations). Don't decide from `failure.value == null && pendingWrites.isEmpty()` at resumption: a write queued after the call (a Scene command's `regenerate()`, a reload's `Regenerate`) would make it `false` though everything asked about is on disk, and an in-flight batch leaves `pendingWrites` empty. KDoc: "Returns once every change made so far is written, or writing them failed; whether they all are on disk."
2. `CommandContext.awaitWrites(): Boolean = blockTypes?.awaitWrites() ?: true`, and a `fun unsavedWrite(): Rejection?` helper next to it building `Rejection(Res.string.error_command_changes_not_written, names, reason)` from `blockTypes.unsavedNames()` (joined with `", "`, or the folder/generated-code subject when no type is named) and `blockTypes.failure.value?.reason`.
3. `Applied.failed(rejection: Rejection)` overload (a `failure` with arguments).
4. Commit, DiscardChanges, StartVersionControl: `if (!context.awaitWrites()) return Applied.failed(checkNotNull(context.unsavedWrite()))` before any git call. New string `error_command_changes_not_written`: "Kubriko Creator couldn't write %1$s yet (%2$s), so version control was left alone. It keeps trying; try again once the console says it could." The AI sees the same text through the rejection.
5. Builds: `CreatorTaskRunner(awaitWrites: suspend () -> Boolean, …)`; in `run()`, `if (!awaitWrites()) console.append(ConsoleEntry.Status(Res.string.status_building_with_unsaved_types, isError = true, arguments = listOf(names, reason)))` and carry on. New string `status_building_with_unsaved_types`: "Building with %1$s as it is on disk, since Kubriko Creator couldn't save it: %2$s". `WorkspaceController` passes a function returning that Boolean plus the names; simplest: `awaitWrites = { blockTypes?.awaitWrites() ?: true }` and a second param `unsavedDescription: () -> Pair<String, String>?`, or let `CreatorTaskRunner` take `awaitWrites: suspend () -> UnsavedWrites?` (null when all written) — pick one and keep the runner free of `BlockTypesController`. Play goes through `GameBuilds.prepareLaunch` → `tasks::run`, so it gets the warning too. `SceneEditingController.buildAndLoad` calls `awaitWrites()` and then `runTask` (the same runner), so the warning appears once; leave its lambda as `{ blockTypes?.awaitWrites() }` (Unit-typed parameter, result ignored).
6. `close()`: wait for the writes to really succeed, at most `CLOSE_WRITE_TIMEOUT_IN_MILLISECONDS = 10_000` (`withTimeoutOrNull`), so the retries K01a schedules get their chance, then cancel as now. Waiting for success: loop `while (blockTypes.awaitWrites() == false) delay(...)` is wrong (it returns at once while blocked); instead add `suspend fun awaitWritten()` that suspends until K01a's `unwrittenCount` reaches 0 (queued, failed, and in-flight writes all done; `pendingWrites.isEmpty()` alone is true while the last batch is still being written). Lane H's H03 also changes `close()` (joins the session first): keep this change to the `blockTypes` line so the two merge cleanly.
7. `creator/CLAUDE.md`: in the `workspace` row, "`awaitWrites`, which builds, git commands (`CommandContext.awaitWrites`), and closing wait for" → say that git commands are refused while a write fails, builds warn, and closing waits up to 10 s for retries. Keep it to one sentence.

## Tests

- `BlockTypesControllerTest.awaitWritesSaysWhetherTheChangesAreWritten`: failing save → `false`; after recovery and a new `awaitWrites()` → `true`; nothing queued → `true` immediately. A `regenerate()` queued while an `awaitWrites()` is suspended on a successful batch → that call still returns `true`.
- Command tests (with `commandContext(...)` and a `FakeBlockTypeRepository` with `failingNames = {"Ball"}` and a `FakeGitRepository` with changes): `put(Ball)` then `execute(Commit("x"))` → `Rejected` with `error_command_changes_not_written` and arguments `["Ball", "Locked."]`, `FakeGitRepository.commits` empty; same for `DiscardChanges` (`discarded` empty) and `StartVersionControl` (no repository created). After clearing the failure and advancing past the retry, `Commit` succeeds.
- `CreatorTaskRunner` (via `WorkspaceControllerTest` with `FakeProjectBuild`): a build while a save fails appends `status_building_with_unsaved_types` naming the type and still runs the task.
- `closeWaitsForARetryThatSucceeds`: failing save, `close()`, clear the failure, advance 2 s → the fake has the save before the scope is cancelled; with the failure kept, `close().join()` returns after 10 s of virtual time.
