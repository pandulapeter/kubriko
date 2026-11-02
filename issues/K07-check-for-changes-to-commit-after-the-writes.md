<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# Decide whether there is anything to commit from the repository's status after the writes, inside the git operation, instead of from the panel's state when the commit is asked for.

**Kind:** bug  ·  **Severity:** medium  ·  **Platforms:** all
**Findings:** CMD-12  ·  **Depends on:** K01b (`Commit.apply` refuses unsaved writes first)
**Files:** `creator/src/main/kotlin/com/pandulapeter/kubrikoCreator/git/GitController.kt`, new `git/CommitResult.kt`, `commands/git/Commit.kt`, tests `git/GitControllerTest.kt`, `testing/FakeGitRepository.kt` (where T01 moved it), the Commit command test (`grep -rl "Commit.Input" creator/src/test`)

## Problem

`commands/git/Commit.kt` validates against the panel's snapshot:

```kotlin
    override fun validate(context: CommandContext, input: Input): Rejection? = when (val state = context.git.state.value) {
        GitState.Loading, GitState.NoRepository -> Rejection(Res.string.error_command_no_repository)
        is GitState.Ready -> when {
            input.message.isBlank() -> Rejection(Res.string.error_command_commit_message_empty)
            state.changes.isEmpty() -> Rejection(Res.string.error_command_nothing_to_commit)
            state.changes.any { it.kind == ChangeKind.CONFLICTING } -> Rejection(Res.string.error_command_commit_conflicts)
```

`GitController.state` updates asynchronously (`refresh()` after the watcher notices a change, and type writes are themselves delayed), so right after an edit (an AI typically edits and then commits) validate rejects with "There are no changes to commit." though there are. The other way round, a stale non-empty state lets `apply` call `commitAll` with nothing to commit, and JGit's `CommitCommand` creates an empty commit by default (`JGitRepository.commitAll` doesn't `setAllowEmpty(false)`).

## Fix

1. `git/CommitResult.kt`: `internal sealed interface CommitResult { data object Committed; data object NothingToCommit; data object Conflicts; data object Failed }`.
2. `GitController.commit(message): CommitResult`: inside `perform`'s lock and on IO, read `repository.status()` first; no changes → `NothingToCommit` (the state is still refreshed from that status); a `CONFLICTING` change (group with `ChangeGrouping` as `readState` does, or check `FileChange.kind`) → `Conflicts`; otherwise `commitAll` and `Committed`; an exception → `Failed` (as `perform` maps today). Report `status_git_committed` only for `Committed`. `perform` needs to return the operation's own result besides the state: give it a type parameter (`perform<T>(…, operation: (GitRepository?) -> Pair<GitState, T>): T?`) rather than a side variable.
3. `Commit.validate` keeps the cheap checks that don't depend on timing: no repository, blank message. Drop the `changes.isEmpty()` and conflicts checks from `validate` (they move into the operation).
4. `Commit.apply`: after K01b's unsaved check, `when (context.git.commit(input.message)) { Committed -> Applied(inverse = null); NothingToCommit -> Applied.failed(error_command_nothing_to_commit); Conflicts -> Applied.failed(error_command_commit_conflicts); Failed -> Applied.failed(status_git_failed) }`.
5. The Version control panel keeps using `state` for enabling its commit button; a click on stale state now gets the right answer through the rejection, which the console shows (`GitPanel`'s `onCommitted` runs only on success, so the message isn't cleared on a refusal).
6. `GitRepository` doesn't change. `JGitRepository.commitAll` may additionally `setAllowEmpty(false)` as a second guard; if added, map JGit's `EmptyCommitException` to `NothingToCommit` too.

## Tests

- `FakeGitRepository` gains a `statusCalls` counter; keep its contract test (lane T's T01) passing.
- `GitControllerTest.nothingToCommitDoesntCommit`: status with no changes → `NothingToCommit`, `commits` empty.
- `GitControllerTest.conflictsArentCommitted`: a `CONFLICTING` change → `Conflicts`, `commits` empty.
- Commit command test `aCommitRightAfterAnEditIsMade`: `GitController.state` still `Ready(changes = [])` (no refresh since), the fake's status has a change → `execute(Commit("x"))` succeeds and `commits == ["x"]`.
- Commit command test `aStaleNonEmptyStateMakesNoEmptyCommit`: state lists a change, the fake's status has none → `Rejected(error_command_nothing_to_commit)`, `commits` empty.
