# Split EditorController into a scene document, scene file I/O, a camera animator and a selection holder, with injected scope and clocks

**Kind:** refactor  ·  **Severity:** medium  ·  **Platforms:** desktop  ·  **Class:** Planned
**Artifact:** tool-scene-editor (internal code only)
**Files:** tools/scene-editor/src/desktopMain/kotlin/com/pandulapeter/kubriko/sceneEditor/implementation/EditorController.kt, tools/scene-editor/src/desktopMain/kotlin/com/pandulapeter/kubriko/sceneEditor/implementation/InternalSceneEditor.kt, tools/scene-editor/src/desktopMain/kotlin/com/pandulapeter/kubriko/sceneEditor/implementation/overlay/OverlayManager.kt, tools/scene-editor/src/desktopMain/kotlin/com/pandulapeter/kubriko/sceneEditor/implementation/userInterface/EditorUserInterface.kt, tools/scene-editor/src/desktopMain/kotlin/com/pandulapeter/kubriko/sceneEditor/implementation/userInterface/panels/instanceManagerColumn/InstanceManagerColumn.kt, tools/scene-editor/src/desktopMain/kotlin/com/pandulapeter/kubriko/sceneEditor/implementation/userInterface/panels/instanceBrowserColumn/InstanceBrowserColumn.kt, tools/scene-editor/src/desktopMain/kotlin/com/pandulapeter/kubriko/sceneEditor/implementation/SceneDocument.kt (new), tools/scene-editor/src/desktopMain/kotlin/com/pandulapeter/kubriko/sceneEditor/implementation/SceneFiles.kt (new), tools/scene-editor/src/desktopMain/kotlin/com/pandulapeter/kubriko/sceneEditor/implementation/CameraAnimator.kt (new), tools/scene-editor/src/desktopMain/kotlin/com/pandulapeter/kubriko/sceneEditor/implementation/EditorSelection.kt (new), tools/scene-editor/src/desktopTest/kotlin/com/pandulapeter/kubriko/sceneEditor/implementation/SceneDocumentTest.kt (new), tools/scene-editor/src/desktopTest/kotlin/com/pandulapeter/kubriko/sceneEditor/implementation/CameraAnimatorTest.kt (new), tools/scene-editor/CLAUDE.md
**Challenged:** amended — step 2 accounts for the Now moves: `FileOperationError` stays in its own file (T01, landed in f1980da9) and `loadFile`/`saveFile` live in `helpers/SceneFileIo.kt` (T03, landed in e2905af4).
**Rebased:** on 70de96c6 after the Now plans landed.

## Problem
`EditorController` (522 lines at 70de96c6 — 528 at 2480325f, less the `FileOperationError` enum T01 moved out; every line number below is unchanged) is a god object holding nine unrelated jobs: scene/actor tracking (`sceneActors`,
`sceneActorIds`, `trackSceneActor`, `clearSceneActors`, l.163-165, 430-465), undo/redo + dirty flag (155-160, 332-386), file I/O and
the error snackbar state (144-151, 166, 402-495), camera animation (287-308, 512-520), selection and the placement preview (122-140,
154, 218-277), the filter text (93-106), preference pass-throughs (141-143, 168-173, 197-200, 315-319), text-input focus counting
(162, 202) and Escape handling (497-510). It builds its own `SupervisorJob() + Dispatchers.Default` (l.72), hard-codes
`launch(Dispatchers.Main)` (404, 477) and `TimeSource.Monotonic` (292), so none of it can be tested with virtual time. Its
`selectedUpdatableActor: StateFlow<Pair<Editable<*>?, Boolean>>` uses a flipping Boolean to force recomposition (122-129), and that
Pair leaks into `InstanceManagerColumn.kt:52` and `InstanceBrowserColumn.kt:58` (`selectedUpdatableInstance: Pair<Editable<*>?, Boolean>`).

## Fix
Land after T50 (selection bug) and T51 (preview ownership); one commit per step, each behaviour-preserving:
1. **`SceneDocument`** — the tracked actors and ids, `UndoRedoHistory`, `isSceneModified`, `pendingPropertyEditKey`,
   `take/restoreSnapshot`, `replace/add/removeSceneActor`, `onBeforePropertyChange`, `recordSnapshot`. Takes
   `serialize: (List<Editable<*>>) -> String`, `deserialize: (String) -> List<Editable<*>>` and an actor sink
   (`add`/`remove` on `ActorManager`) so it is testable without a Kubriko. Main-thread only, as today.
2. **`SceneFiles`** — current folder/file name, loading flag, `fileOperationError` (using `FileOperationError`, which stays in
   its own `FileOperationError.kt`, where T01 put it), `loadMap`, `saveScene`, `syncScene`; takes the scope plus `mainDispatcher` and
   `ioDispatcher` (default `Dispatchers.Main` / `Dispatchers.IO`; `loadFile`/`saveFile`, in `helpers/SceneFileIo.kt:16`/`:20`, where
   both hard-code `withContext(Dispatchers.IO)`, gain a dispatcher parameter).
3. **`CameraAnimator`** — `animateCameraTo`, the job, `easeInOut`, `isRoughlyAt`; takes scope, `ViewportManager` and a
   `TimeSource` (default `TimeSource.Monotonic`).
4. **`EditorSelection`** — selected actor, `selectedTypeId`, the preview (from T51), `canLocateSelectedActor`; replace the
   `Pair<Editable<*>?, Boolean>` toggle with `selectedActor: StateFlow<Editable<*>?>` plus `selectedActorRevision: StateFlow<Int>`
   (incremented by `notifySelectedActorUpdate`); `InstanceManagerColumn` / `InstanceBrowserColumn` take `selectedInstance` and
   `selectedInstanceRevision: Int` (read so the column recomposes, exactly as the toggle did).
5. **`EditorController`** keeps coordination only (filter, pref pass-throughs, focus count, Escape, click routing), receives its
   `CoroutineScope` from `InternalSceneEditor` (which creates `CoroutineScope(SupervisorJob() + Dispatchers.Default)` and cancels it
   in the existing `DisposableEffect`, `InternalSceneEditor.kt:110`) instead of implementing `CoroutineScope`.
Each step moves code verbatim where it can; widen `private` to `internal` only where another new file needs it. Update
`tools/scene-editor/CLAUDE.md` → "EditorController" to describe the collaborators.

## Behaviour
Unchanged: same flows, same dispatchers by default, same ordering of side effects (snapshot before mutate, mark modified after). The
revision counter replaces the Boolean toggle one-for-one (it changes on exactly the same calls). The challenger should check that
every `stateIn(this, Eagerly, …)` keeps the same scope and that disposal still cancels the camera job and every collector.

## Public API
None (internal).

## Tests
- `SceneDocumentTest` with string-based fake serialize/deserialize and a recording actor sink: a Unique actor replaces the tracked one
  of its class; undo after a delete restores the actor and re-selects the same id; undo/redo restore `isSceneModified`; consecutive
  `onBeforePropertyChange` with one key record one step.
- `CameraAnimatorTest` with `TestTimeSource` + `runTest`/`StandardTestDispatcher` (kotlinx-coroutines-test is on every test
  classpath): reaches the target after 350 ms of virtual time; stops when the camera is moved externally mid-animation. Use a
  `ViewportManager` from `newManualKubriko` (test-fixtures).

## Verify
`./gradlew :tools:scene-editor:compileKotlinDesktop :tools:scene-editor:desktopTest` after each step.

## Manual check
Full Scene Editor pass: load/save/new/sync (Connected mode via the Showcase), undo/redo, select/locate/delete, filter, placement,
drag in all three modes, Escape chain, settings window.
