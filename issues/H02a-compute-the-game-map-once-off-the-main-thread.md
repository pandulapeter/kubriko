<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# Compute the AI panel's Game map (digest and health findings) once per burst of changes, off the main thread, in a workspace-layer `GameMapState`

**Challenged:** amended — the StateFlow triggers now `drop(1)` (their replays would cancel the first, undelayed map and delay every opening by the debounce), `aiSteps` joins them (the old `latestAiStep` key), the off-Main compute is limited to thread-safe reads, and the K09 note says where it goes; one test added.

**Kind:** performance  ·  **Severity:** medium  ·  **Platforms:** all
**Findings:** UI-5  ·  **Depends on:** none (K09 note below)
**Files:** new `creator/src/main/kotlin/com/pandulapeter/kubrikoCreator/workspace/GameMapState.kt`, `.../workspace/WorkspaceController.kt` (or K09's factory), `.../ui/WorkspaceScreen.kt`, `.../ui/AiPanel.kt` (only if its parameters change), `.../context/ProjectDigestReading.kt`, `.../context/HealthFindings.kt`, `creator/src/test/kotlin/com/pandulapeter/kubrikoCreator/workspace/GameMapStateTest.kt` (new), `creator/CLAUDE.md`

## Problem

`ui/WorkspaceScreen.kt`, inside `if (openPanel == SidePanel.AI && isAiEnabled)`:
```kotlin
val digest by produceState<ProjectDigest?>(null, aiPanelView, blockTypes, resources, sceneDescriptions, latestAiStep?.id, sceneEditing) {
    value = if (aiPanelView == AiPanelView.GAME_MAP) controller.commands.context.projectDigest() else null
}
val findings by produceState(emptyList<HealthFinding>(), aiPanelView, blockTypes, resources, latestAiStep?.id, sceneEditing) {
    value = if (aiPanelView == AiPanelView.GAME_MAP) controller.commands.context.healthFindings() else emptyList()
}
```
Both call `sceneTypeCounts()` (`context/SceneTypeCounts.kt`: `scenes.names()` walks the Scenes folder, then `scenes.actors(name)` per
Scene), so every edit while the Game map is shown walks and stats the Scene folder twice. The I/O is on `ioDispatcher`, but
`ProjectDigest.of` and `HealthChecks.of` (which builds `ProjectRelations` and an `ActorTypeFamily`) run on the composition's
dispatcher, the main thread. Typing in a description field restarts both on every keystroke.

## Fix

`workspace/GameMapState.kt`:
```kotlin
/** What the AI panel's Game map shows. */
internal data class GameMap(val digest: ProjectDigest, val findings: List<HealthFinding>)

/**
 * The Game map of the open project, computed off the main thread [DEBOUNCE_IN_MILLISECONDS] after the last change, and only
 * while something collects [gameMap]. [changes] emits changes only, never a current value (drop a StateFlow's first).
 */
internal class GameMapState(
    changes: Flow<Any?>,                       // emits whenever the map may have changed
    private val compute: suspend () -> GameMap,
    scope: CoroutineScope,
    private val computeDispatcher: CoroutineDispatcher = Dispatchers.Default,
) {
    val gameMap: StateFlow<GameMap?> = flow {
        var isFirst = true
        // A newer change cancels the delayed or running computation (mapLatest), which is the debounce; the first one
        // isn't delayed, so opening the Game map shows it at once.
        emitAll(changes.onStart { emit(Unit) }.mapLatest {
            if (!isFirst) delay(DEBOUNCE_IN_MILLISECONDS)
            isFirst = false
            computeReporting()
        }.filterNotNull())
    }.stateIn(scope, SharingStarted.WhileSubscribed(stopTimeoutMillis = 0), null)

    private suspend fun computeReporting(): GameMap? = try {
        withContext(computeDispatcher) { compute() }
    } catch (exception: CancellationException) {
        throw exception
    } catch (exception: Exception) {
        report(exception)
        null
    }
}
```
Threading: `compute` runs on `Dispatchers.Default`, so it may only read what is safe off the main thread: `StateFlow.value`s (`blockTypes.types`, `resources`) and the Scene store reads that already run on `ioDispatcher` today (`sceneTypeCounts`, `descriptions.all()`); it must not call anything Main-confined (the executor, `SceneEditingController`, `BlockTypesController`'s mutators). A map computed while a command is half-applied is replaced by the next change's, which that command's own emission triggers.

`report: (Throwable) -> Unit` is a constructor parameter (`WorkspaceController::reportUnexpectedProblem`): one failed computation is reported in the console, the value stays the previous map, and the flow lives on, so the next change computes again (a failure must not end the flow for the rest of the session). `const val DEBOUNCE_IN_MILLISECONDS = 250L` in its companion.

The one computation reads the Scene counts once:
- in `context/`, add `internal suspend fun CommandContext.gameMap(): GameMap` (or keep it in `GameMapState.kt` if `context` mustn't know `GameMap`; prefer `context/GameMapReading.kt` returning a `Pair` and keep `GameMap` in workspace), which calls `sceneTypeCounts()` once and builds both `ProjectDigest.of(...)` and `HealthChecks.of(...)` from it. `projectDigest()` and `healthFindings()` keep working for their other callers (`ConversationStart`, the tools) by delegating to overloads that take the counts.

Wiring: `WorkspaceController` builds `val gameMap = GameMapState(changes = merge(blockTypes?.types?.drop(1) ?: emptyFlow(), resources.drop(1), scenes.descriptions.current.drop(1), commands.history.drop(1), commands.aiSteps.drop(1), sceneEditing.state.drop(1)), compute = { commands.context.gameMap() }, scope = scope, report = ::reportUnexpectedProblem)` after `commands`. These triggers are the ones the two `produceState`s keyed on today (`latestAiStep?.id` is `commands.aiSteps`; the history covers the user's Scene edits). **Each source is a `StateFlow`, which replays its current value to every new collector: without `drop(1)` their replays arrive right after `onStart`'s emission, `mapLatest` cancels the first, undelayed computation, and opening the Game map waits out the debounce after all.** The `onStart { emit(Unit) }` stands for the current state. **K09 (it lands before lane H):** K09a's `ProjectChanges` is a main-thread routing table driven through function parameters and, as planned, exposes no flow; re-locate the wiring into K09b's construction order (after `commands`, `scenes`, and `sceneEditing` exist) and keep the merged list. Only if the router as landed does expose one flow or revision for "something in the project changed" (own writes and the watcher), use it plus `blockTypes.types` instead of the merged list, and build `GameMapState` in K09's factory.

UI: replace the two `produceState`s with one `val gameMap by controller.gameMap.gameMap.collectAsState()` read only inside `if (aiPanelView == AiPanelView.GAME_MAP)` (so it is collected, and therefore computed, only while the Game map is shown: put the collection in a small `@Composable` helper or the `AiPanel` call branch). Pass `digest = gameMap?.digest`, `findings = gameMap?.findings.orEmpty()` to `AiPanel` (no `AiPanel` signature change needed). Remove the `HealthFinding`, `ProjectDigest`, `healthFindings`, `projectDigest`, and `produceState` imports from `WorkspaceScreen.kt`.

`creator/CLAUDE.md`: where the Game map is described (search "Game map"), add one sentence: computed once per burst of changes off the main thread (`GameMapState`), only while it is shown.

## Tests

`GameMapStateTest` (new; `runTest`, `StandardTestDispatcher` for both `scope` and `computeDispatcher`, a `MutableSharedFlow<Unit>` as `changes`, a counting `compute`):
- `nothingIsComputedWhileNothingCollects`: emit changes with no collector → count 0.
- `theFirstMapComesAtOnceAndQuickChangesComputeOnce`: start collecting → after `runCurrent()` count 1; emit twice 50 ms apart, `advanceTimeBy(249)` → still 1; `advanceTimeBy(2)` → 2 and `gameMap.value` is the second result.
- `aSlowComputationIsCancelledByANewerChange`: `compute` suspends on a `CompletableDeferred`; a change after the debounce cancels it (`mapLatest`), only the newer result lands.
- `aFailedComputationIsReportedAndTheNextChangeComputesAgain`: first `compute` throws `IllegalStateException("boom")` → reported once, value unchanged; next change → new value.
- `stoppingCollectingStopsComputing`: cancel the collector, emit → count unchanged.
- `stateFlowSourcesDontDelayTheFirstMap`: `changes` built as the wiring builds it, from two `MutableStateFlow`s with `drop(1)`; start collecting, `runCurrent()` → count 1 and `gameMap.value` set without advancing time (fails if the `drop(1)`s are left out).
A `context` test (existing `HealthChecksTest`/digest test file, or a new `GameMapReadingTest` against a real temporary Scenes folder via `SceneStore`) that `gameMap()` returns the same digest and findings as `projectDigest()` and `healthFindings()`.
