# Dispose the scene editor's Kubriko instances and controller scope when it leaves composition, not when closing is requested

**Challenged:** amended — the plan now fixes where the `DisposableEffect` goes (before the first `Window(...)`), so Compose forgets both windows — and the editor/overlay/debug-overlay viewports inside them — before the Kubrikos they render are disposed.

**Kind:** bug  ·  **Severity:** medium  ·  **Platforms:** desktop
**Artifact:** `tool-scene-editor` (behaviour); KDoc only in `tool-scene-editor-api`
**Files:** `tools/scene-editor/src/desktopMain/kotlin/com/pandulapeter/kubriko/sceneEditor/implementation/InternalSceneEditor.kt`, `tools/scene-editor/src/desktopMain/kotlin/com/pandulapeter/kubriko/sceneEditor/implementation/EditorController.kt`, `tools/scene-editor-api/src/desktopMain/kotlin/com/pandulapeter/kubriko/sceneEditor/SceneEditorContract.kt`, `tools/scene-editor/CLAUDE.md`

**Decision needed:** The embedded `SceneEditor(...)` composable would dispose its Kubriko instances — and with them the `serializationManager` and `customManagers` the caller passed in — when it leaves composition, instead of at the moment it calls `onCloseRequest`. A host that declines a close keeps a working editor; a host that removes the editor without a close request no longer leaks it. Adopt this lifecycle? — recommended: yes (option A).

## Problem

`InternalSceneEditor` (~56-111) creates two `Kubriko` instances and an `EditorController` in `remember {}` and disposes them only inside the close handler:

```kotlin
lateinit var overlayKubriko: Kubriko

fun disposeAndClose() {
    editorKubriko.dispose()
    overlayKubriko.dispose()
    onCloseRequest()
}
...
Window(
    onCloseRequest = ::disposeAndClose,
    title = title,
) {
```

and the controller's own scope is never cancelled (`EditorController.kt` ~62):

```kotlin
override val coroutineContext = SupervisorJob() + Dispatchers.Default
```

It holds eager `stateIn` collectors on the editor's `ActorManager` and `ViewportManager` flows, plus the camera animation job.

1. **Leak.** When the host removes `SceneEditor(...)` from composition without the user closing it — e.g. the Showcase's `isSceneEditorVisible` flips from elsewhere — nothing is disposed: both `Kubriko` instances keep their coroutine scopes and the controller keeps collecting for the rest of the process. The controller scope is never cancelled on the normal close path either.
2. **Crash on a declined close.** `onCloseRequest` is documented as "Callback when the user attempts to close the editor". A host that asks "Discard changes?" and keeps the editor open is left with a composed `Window` whose Kubrikos are already disposed; the next recomposition or tick calls `get()` on a disposed instance and throws `IllegalStateException("Cannot access Managers on a disposed Kubriko instance.")`.

## Fix

Option A (recommended):

1. `EditorController`: add `fun dispose() { cameraAnimationJob?.cancel(); cancel() }` (`kotlinx.coroutines.cancel` on the `CoroutineScope`).
2. `InternalSceneEditor`:
   - Delete `disposeAndClose()` and the `lateinit var overlayKubriko`; declare `val overlayKubriko = remember { ... }` normally after `overlayManager` (the `lateinit` only existed because the close lambda referenced it).
   - `val currentOnCloseRequest by rememberUpdatedState(onCloseRequest)`; the controller's `onCloseRequest` lambda keeps its settings/file-dialog checks but ends in `currentOnCloseRequest()`; `Window(onCloseRequest = { currentOnCloseRequest() }, ...)`.
   - Add
     ```kotlin
     DisposableEffect(Unit) {
         onDispose {
             editorController.dispose()
             overlayKubriko.dispose()
             editorKubriko.dispose()
         }
     }
     ```
     Cancel the controller first so no collector runs against a disposed instance. Place this effect right after `overlayKubriko` is remembered and **before** the editor `Window(...)` (and the settings `Window`): Compose dispatches `onDispose` in reverse composition order, and a desktop `Window` disposes its content composition in its own `onDispose`, so this ordering tears down the windows' `KubrikoViewport`s (their effects, plan 12/13's focus reset, and the debug menu's `OverlayOnly` registration from plan 67, which disposes the debug overlay built on the editor instance) while `editorKubriko` and `overlayKubriko` are still alive, and only then disposes the instances. Placed after the windows, the instances would be disposed under still-composed viewports.
   `SceneEditor.show()` needs no change: `exitApplication` disposes the composition, which runs the effect.
3. `SceneEditorContract` (`tool-scene-editor-api`) KDoc of the composable `invoke` overloads: `@param onCloseRequest` → "Called when the user asks to close the editor (window close button or Escape). The editor stays open until the caller removes it from composition; it then disposes its Kubriko instances, including [serializationManager] and [customManagers]." Only KDoc changes in the API module.
4. `tools/scene-editor/CLAUDE.md` → Architecture: add "Both instances and the `EditorController` scope are disposed when `InternalSceneEditor` leaves composition (a `DisposableEffect`), never in the close handler."

Option B — keep disposing in the close handler but only after the host accepted (would need a new `onCloseRequest: () -> Boolean` signature — a public API change). Not recommended.

Examples lane note: the desktop wrappers (e.g. `examples/game-annoyed-penguins/src/desktopMain/.../AnnoyedPenguinsGameSceneEditor.kt` ~36) create a game state holder in `remember` and never dispose it. After this plan the editor disposes the managers it was given, so the remaining obligation for those wrappers is only the state holder itself; that is theirs to fix.

## Tests

None: the change is Compose lifecycle wiring. `EditorController` could be constructed in a test, but it needs a `PersistenceManager` (which writes a real preferences file on desktop) and a `KeyboardInputManager`, and the behaviour under test is the composition's `onDispose`, which plan 00's setup cannot drive.

## Manual check

Desktop, `./gradlew :app:desktop:run` with `showcase.isSceneEditorEnabled=true`:
1. Open Annoyed Penguins → Scene Editor, close the editor window, reopen it: it opens and edits normally.
2. Open the editor, then hide it from the Showcase side (switch to another game / toggle the scene-editor button off, whichever removes it) and reopen: it works; with `isLoggingEnabled` on for the editor instance (temporarily), the log shows "Disposed." for both instances when it disappears.
3. Temporarily change the Annoyed Penguins wrapper's `onCloseRequest` to do nothing, press the window's close button: the editor stays open and usable (before the fix the next interaction throws `Cannot access Managers on a disposed Kubriko instance`). Revert the temporary change.
4. Run the standalone editor (`PerformanceDemoSceneEditor.kt`'s `main`) and close it: the process exits cleanly.
