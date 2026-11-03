<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# Give `BlockTypesController` one `BlockTypesListener` for what it reports, and move the workspace's console reporting of it into `BlockTypesReport`

**Challenged:** amended — `BlockTypesReport`'s constructor callback is `onBatchSaved`, so the class no longer has a property
and a function both named `onSaved`; the KDoc step says the controller has seven `@param on…` lines, not nine; the
`BlockTypesControllerTest` step names the eight overrides and calls the two same-named `createController` parameters through
`.invoke`, so no override reads as calling itself. Checked otherwise: the three constructor callers, every callback use in
`BlockTypesController`, `ConsoleLog.append` returning `Unit` (so the expression-bodied overrides compile), and the
`KubrikoBuildFileChanges` fields the new test sets.

**Kind:** architecture  ·  **Severity:** low  ·  **Platforms:** all (desktop app)
**Written against:** `9bb8df2a6`. Re-locate code by the quoted snippets, not by line numbers.
**Files:**
- `:creator:core`: `creator/core/src/main/kotlin/com/pandulapeter/kubrikoCreator/actors/store/BlockTypesController.kt`,
  `…/actors/store/BlockTypesListener.kt` (new), `creator/core/src/test/kotlin/com/pandulapeter/kubrikoCreator/actors/store/BlockTypesControllerTest.kt`
- `:creator:commands`: `creator/commands/src/testFixtures/kotlin/com/pandulapeter/kubrikoCreator/commands/CommandTesting.kt`
- `:creator:workspace`: `creator/workspace/src/main/kotlin/com/pandulapeter/kubrikoCreator/workspace/WorkspaceController.kt`,
  `…/workspace/BlockTypesReport.kt` (new), `creator/workspace/src/test/kotlin/com/pandulapeter/kubrikoCreator/workspace/BlockTypesReportTest.kt` (new)
- `creator/plan/README.md`

## Problem

`BlockTypesController`'s primary constructor takes 15 parameters (`check_sizes.py` counts every non-data class's primary
constructor parameters; the limit is 12, and a constructor past it may only shrink):

```kotlin
public class BlockTypesController(
    private val repository: BlockTypeRepository,
    private val scope: CoroutineScope,
    private val ioDispatcher: CoroutineDispatcher,
    private val onSaved: (haveTypesChanged: Boolean) -> Unit,
    private val onSaveFailed: (FailedWrite) -> Unit,
    private val onSavedAgain: () -> Unit = {},
    private val onFileUnreadable: (UnreadableBlockTypeFile) -> Unit = {},
    private val onFirstLoaded: (List<BlockTypeDefinition>) -> Unit = {},
    private val onFilesReadableAgain: () -> Unit = {},
    private val onLeftoversRemoved: (List<String>) -> Unit = {},
    private val onLeftoverChangedByHand: (String) -> Unit = {},
    syncBuildFiles: () -> KubrikoBuildFileChanges? = { null },
    private val onBuildFilesSynced: (KubrikoBuildFileChanges) -> Unit = {},
    private val writeDelayInMilliseconds: Long = 0,
    private val isReadOnly: () -> Boolean = { false },
) : BlockTypes {
```

Nine of them are callbacks through which the controller reports what happened while writing, reading, and regenerating the
types. Its one production caller, `WorkspaceController`, turns eight of them into console entries inline, and keeps two private
helpers and a flag only for them (`reportKotlinBlockScopeChanges`, `reportBuildFileChanges`, and
`private val hasReportedNewerKubrikoVersion = AtomicBoolean(false)`), in a file at 495 of its 500 lines.

## Fix

**Grouping:** the nine `on…` callbacks become one listener interface. They belong together because they are one channel: the
events of a single controller, all called on its scope's thread, all implemented by the same caller for the same purpose
(telling the user and the workspace what the type store did). The remaining parameters are the controller's collaborators
(`repository`, `scope`, `ioDispatcher`, `syncBuildFiles`) and its policies (`writeDelayInMilliseconds`, `isReadOnly`).
Result: 7 parameters.

### 1. `actors/store/BlockTypesListener.kt` (new, `:creator:core`)

```kotlin
/**
 * What a [BlockTypesController] reports while it writes, reads, and regenerates the types, called on its scope's thread.
 * Saving and failing to save have to be handled; the rest do nothing unless overridden.
 */
public interface BlockTypesListener {

    /** Called after each batch, with whether the types changed, rather than only the Scenes they were regenerated for. */
    public fun onSaved(haveTypesChanged: Boolean)

    /** Called with what a batch couldn't write, once until the failure changes. */
    public fun onSaveFailed(failure: FailedWrite)

    /** Called when a batch succeeds after one failed. */
    public fun onSavedAgain() {}

    /** Called for each definition file found unreadable, once until it changes. */
    public fun onFileUnreadable(file: UnreadableBlockTypeFile) {}

    /** Called once the types are first loaded. */
    public fun onFirstLoaded(types: List<BlockTypeDefinition>) {}

    /** Called when no definition file is unreadable any more. */
    public fun onFilesReadableAgain() {}

    /** Called with the generated files (by path) a regeneration deleted, since their types no longer exist. */
    public fun onLeftoversRemoved(paths: List<String>) {}

    /**
     * Called for each generated file (by path) whose type no longer exists but which was changed by hand, so it was kept,
     * once until it is gone.
     */
    public fun onLeftoverChangedByHand(path: String) {}

    /** Called with what syncing the build files after a batch changed. */
    public fun onBuildFilesSynced(changes: KubrikoBuildFileChanges) {}
}
```

`onSaved` and `onSaveFailed` stay without a default, as their lambdas had none, so no caller can forget a save failure.

### 2. `actors/store/BlockTypesController.kt` (`:creator:core`)

New constructor:

```kotlin
public class BlockTypesController(
    private val repository: BlockTypeRepository,
    private val scope: CoroutineScope,
    private val ioDispatcher: CoroutineDispatcher,
    private val listener: BlockTypesListener,
    syncBuildFiles: () -> KubrikoBuildFileChanges? = { null },
    private val writeDelayInMilliseconds: Long = 0,
    private val isReadOnly: () -> Boolean = { false },
) : BlockTypes {
```

Replace each use, keeping the calls where and when they are:
- `onFirstLoaded(_types.value)` → `listener.onFirstLoaded(_types.value)`
- `files.filter { it !in previous }.forEach(onFileUnreadable)` → `….forEach(listener::onFileUnreadable)`
- `if (files.isEmpty() && previous.isNotEmpty()) onFilesReadableAgain()` → `listener.onFilesReadableAgain()`
- `onSavedAgain()` → `listener.onSavedAgain()`
- `if (regenerated.removedLeftovers.isNotEmpty()) onLeftoversRemoved(regenerated.removedLeftovers)` → `listener.onLeftoversRemoved(…)`
- `….sorted().forEach(onLeftoverChangedByHand)` → `….forEach(listener::onLeftoverChangedByHand)`
- `completed.buildFileChanges?.let(onBuildFilesSynced)` → `….let(listener::onBuildFilesSynced)`
- `onSaved(haveTypesChanged)` → `listener.onSaved(haveTypesChanged)`
- `if (failure != previous) onSaveFailed(failure)` → `listener.onSaveFailed(failure)`

In the class KDoc, replace the seven `@param on…` lines it has today (`onSaved` to `onLeftoverChangedByHand`; `onFirstLoaded` and
`onBuildFilesSynced` have none) with one: "`@param listener` Told what the controller did; see [BlockTypesListener]." Keep the
rest of the KDoc, including the sentence about [syncBuildFiles].

### 3. `workspace/BlockTypesReport.kt` (new, `:creator:workspace`)

The workspace's listener, which says in the console what the type store did and hands saved batches on. It takes over
`WorkspaceController`'s `reportKotlinBlockScopeChanges`, `reportBuildFileChanges`, and `hasReportedNewerKubrikoVersion`,
moved verbatim:

```kotlin
/**
 * Says in the [console] what the project's type store did, and hands each saved batch to [onBatchSaved].
 */
internal class BlockTypesReport(
    private val console: ConsoleLog,
    private val onBatchSaved: (haveTypesChanged: Boolean) -> Unit,
) : BlockTypesListener {
    private val hasReportedNewerKubrikoVersion = AtomicBoolean(false)

    override fun onSaved(haveTypesChanged: Boolean) = onBatchSaved(haveTypesChanged)
    override fun onSaveFailed(failure: FailedWrite) = console.append(failure.toConsoleEntry())
    override fun onSavedAgain() = console.append(ConsoleEntry.Status(WorkspaceStatus.BLOCK_TYPES_SAVED_AGAIN))
    override fun onFileUnreadable(file: UnreadableBlockTypeFile) = console.append(file.toConsoleEntry())
    override fun onFilesReadableAgain() = console.append(ConsoleEntry.Status(WorkspaceStatus.TYPE_FILES_READABLE_AGAIN))
    override fun onLeftoversRemoved(paths: List<String>) =
        console.append(ConsoleEntry.Status(WorkspaceStatus.GENERATED_LEFTOVERS_REMOVED, arguments = listOf(paths.joinToString())))
    override fun onLeftoverChangedByHand(path: String) =
        console.append(ConsoleEntry.Status(WorkspaceStatus.GENERATED_LEFTOVER_CHANGED_BY_HAND, isError = true, arguments = listOf(path)))

    /**
     * Names, once per type, the types whose Kotlin Blocks rely on what changed when each kind of Event got a function of
     * its own, which the health checks list Block by Block.
     */
    override fun onFirstLoaded(types: List<BlockTypeDefinition>) { /* the body of WorkspaceController.reportKotlinBlockScopeChanges */ }

    override fun onBuildFilesSynced(changes: KubrikoBuildFileChanges) { /* the body of WorkspaceController.reportBuildFileChanges */ }
}
```

Copy the two bodies from `WorkspaceController` unchanged except `appendToConsole(…)` → `console.append(…)`. Today they read:

```kotlin
KotlinBlockScopeChecks.of(types).map { it.subject.toString() }.distinct().forEach { name ->
    appendToConsole(ConsoleEntry.Status(WorkspaceStatus.KOTLIN_BLOCK_SCOPE_CHANGED, isError = true, arguments = listOf(name)))
}
```

and

```kotlin
changes.previousVersion?.let { appendToConsole(ConsoleEntry.Status(WorkspaceStatus.KUBRIKO_VERSION_CHANGED, arguments = listOf(changes.version, it))) }
if (changes.isNewerThanCreator && !hasReportedNewerKubrikoVersion.getAndSet(true)) {
    appendToConsole(ConsoleEntry.Status(WorkspaceStatus.KUBRIKO_VERSION_NEWER, arguments = listOf(changes.version)))
}
if (changes.addedArtifacts.isNotEmpty()) {
    appendToConsole(ConsoleEntry.Status(WorkspaceStatus.KUBRIKO_LIBRARIES_ADDED, arguments = listOf(changes.addedArtifacts.joinToString())))
}
if (changes.removedArtifacts.isNotEmpty()) {
    appendToConsole(ConsoleEntry.Status(WorkspaceStatus.KUBRIKO_LIBRARIES_REMOVED, arguments = listOf(changes.removedArtifacts.joinToString())))
}
```

`ConsoleLog` (`append`), `FailedWrite.toConsoleEntry()` (`workspace/FailedWriteEntry.kt`), and
`UnreadableBlockTypeFile.toConsoleEntry()` (`workspace/UnreadableBlockTypeFileEntry.kt`) are `internal` to
`:creator:workspace`, which is why the class lives there. Format the expression bodies as ktlint and the file's neighbours do
(`./gradlew spotlessApply` settles it).

### 4. `workspace/WorkspaceController.kt` (`:creator:workspace`)

Replace the callback arguments of the `BlockTypesController(…)` call:

```kotlin
onSaved = { haveTypesChanged -> savedTypeBatches.trySend(haveTypesChanged) },
onSaveFailed = { appendToConsole(it.toConsoleEntry()) },
onSavedAgain = { appendToConsole(ConsoleEntry.Status(WorkspaceStatus.BLOCK_TYPES_SAVED_AGAIN)) },
onFileUnreadable = { appendToConsole(it.toConsoleEntry()) },
onFirstLoaded = ::reportKotlinBlockScopeChanges,
onFilesReadableAgain = { appendToConsole(ConsoleEntry.Status(WorkspaceStatus.TYPE_FILES_READABLE_AGAIN)) },
onLeftoversRemoved = { appendToConsole(ConsoleEntry.Status(WorkspaceStatus.GENERATED_LEFTOVERS_REMOVED, arguments = listOf(it.joinToString()))) },
onLeftoverChangedByHand = { appendToConsole(ConsoleEntry.Status(WorkspaceStatus.GENERATED_LEFTOVER_CHANGED_BY_HAND, isError = true, arguments = listOf(it))) },
syncBuildFiles = syncBuildFiles,
onBuildFilesSynced = ::reportBuildFileChanges,
```

with

```kotlin
listener = BlockTypesReport(consoleLog) { haveTypesChanged -> savedTypeBatches.trySend(haveTypesChanged) },
syncBuildFiles = syncBuildFiles,
```

(`consoleLog` is declared above `blockTypes`, so it exists by then). Delete `hasReportedNewerKubrikoVersion`,
`reportKotlinBlockScopeChanges`, and `reportBuildFileChanges`, and the imports nothing uses any more
(`KotlinBlockScopeChecks`, `AtomicBoolean`, `BlockTypeDefinition` if unused; `KubrikoBuildFileChanges` stays for the
`syncBuildFiles` parameter). The file shrinks by about 30 lines, which the later plans F03, F04, and F06 rely on.

### 5. `commands/CommandTesting.kt` (`:creator:commands` test fixtures)

`blockTypesController(…)` passes `onSaved = {}` and `onSaveFailed = {}`; replace both with

```kotlin
listener = object : BlockTypesListener {
    override fun onSaved(haveTypesChanged: Boolean) = Unit
    override fun onSaveFailed(failure: FailedWrite) = Unit
},
```

### 6. `creator/plan/README.md`

In the Structure section's constructors bullet, replace
"**Five constructors take more than 12 parameters** (`WorkspaceController` 23, `AssistantController` 18,
`SceneEditingController` 17, `BlockTypesController` 15, and `CommandContext` 13)" with
"**Four constructors take more than 12 parameters** (`WorkspaceController` 23, `AssistantController` 18,
`SceneEditingController` 17, and `CommandContext` 13)", and its last sentence "Direction: take a responsibility out of each
(for `WorkspaceController`, a factory that builds its collaborators)." with "Direction: group the parameters that belong
together into a named collaborator, or take a responsibility out (`issues/`)."

## Tests

- `BlockTypesControllerTest.createController` builds the listener from its counters instead of the lambdas, in an
  `object : BlockTypesListener { … }` that overrides the eight callbacks it records today (`onSaved`, `onSaveFailed`,
  `onSavedAgain`, `onFileUnreadable`, `onFilesReadableAgain`, `onLeftoversRemoved`, `onLeftoverChangedByHand`,
  `onBuildFilesSynced`). Two of `createController`'s parameters share a name with the override that calls them, so call them
  as `onSaved.invoke()` and `onBuildFilesSynced.invoke(changes)`: a bare `onBuildFilesSynced(changes)` inside
  `override fun onBuildFilesSynced` reads as the override calling itself, and `.invoke` on the name can only mean the
  parameter. No test case changes otherwise.
- New `BlockTypesReportTest` (`:creator:workspace`), since a collaborator of `WorkspaceController` is tested on its own
  (`creator/CLAUDE.md` → Running and testing). Plain JUnit, no coroutines: build `BlockTypesReport(ConsoleLog()) { saved += it }`
  and assert on `console.entries.value`:
  - `onBuildFilesSynced(KubrikoBuildFileChanges(version = "2.0.0", previousVersion = "1.0.0", isNewerThanCreator = true,
    addedArtifacts = …, removedArtifacts = …))` called twice reports `KUBRIKO_VERSION_CHANGED` twice, `KUBRIKO_VERSION_NEWER`
    once, and the added and removed libraries each time (`KubrikoBuildFileChanges` is a data class in `:creator:core`'s
    `project/KubrikoBuildFiles.kt`: `addedArtifacts`, `removedArtifacts`, `previousVersion`, `version`, `isNewerThanCreator`);
  - `onFirstLoaded` with a type whose Kotlin Block trips `KotlinBlockScopeChecks` reports `KOTLIN_BLOCK_SCOPE_CHANGED` once per
    type (reuse the definition `WorkspaceControllerTest.openingAProjectNamesEachTypeWhoseKotlinBlocksRelyOnWhatTheEventFunctionsChanged`
    builds);
  - `onLeftoversRemoved(listOf("a.kt", "b.kt"))` reports one `GENERATED_LEFTOVERS_REMOVED` with `"a.kt, b.kt"`, and
    `onLeftoverChangedByHand("c.kt")` an error `GENERATED_LEFTOVER_CHANGED_BY_HAND` with `"c.kt"`;
  - `onSaved(true)` reaches the lambda and writes nothing to the console.

  `WorkspaceControllerTest`'s `aFailedActorTypeSaveIsReportedInTheConsole` and
  `openingAProjectNamesEachTypeWhoseKotlinBlocksRelyOnWhatTheEventFunctionsChanged` stay as the wiring cases.
- Run `./gradlew :creator:allTests` and `./gradlew spotlessCheck`. After staging, `python3 .github/scripts/check_sizes.py` must
  print nothing; `BlockTypesController` now has 7 parameters, and `WorkspaceController.kt` is shorter than before.
