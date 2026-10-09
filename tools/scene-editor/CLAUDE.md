<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# scene-editor (Desktop only)

Real implementation of `SceneEditorContract`. All code lives in `desktopMain`.

## Architecture

The editor runs **two separate Kubriko instances**:

1. **`editorKubriko`** — hosts the scene being edited. Auto-configured with `ViewportManager` (Dynamic aspect ratio, scale 0.1–10), `StateManager(shouldAutoStart = false)`, `KeyboardInputManager`, `PointerInputManager`, `PersistenceManager("kubrikoSceneEditor")`, the caller-supplied `SerializationManager`, and any `customManagers`.
2. **`overlayKubriko`** — hosts the `OverlayManager` that draws selection highlights and actor placement previews on top of the scene viewport.

`InternalSceneEditor` (Composable) creates and wires both instances; `EditorController` is the central coordinator that both read.

Both instances and the editor's `CoroutineScope` (created there and passed to `EditorController`) are disposed when `InternalSceneEditor` leaves composition (a `DisposableEffect`), never in the close handler.

The debug menu dependency follows `showcase.isDebugMenuEnabled` for local Showcase builds; publishing requires the flag to be `true`, so the released artifact always depends on `tool-debug-menu`.

## `EditorController`

Coordinates the editor (filter, preference pass-throughs, text-input focus count, Escape handling, click routing) on the `CoroutineScope` `InternalSceneEditor` gives it (SupervisorJob + Dispatchers.Default, cancelled when the editor leaves composition), and delegates the rest to four collaborators: `SceneDocument` (the scene's actors, undo/redo and the dirty flag), `SceneFiles` (scene I/O and its errors), `CameraAnimator` (the locate animation, with an injectable `TimeSource`) and `EditorSelection` (selected actor, selected type and placement preview):

- **Selection**: `selectedActor: StateFlow<Editable<*>?>` plus `selectedActorRevision: StateFlow<Int>`, which `notifySelectedActorUpdate` increments so the property panel and the instance browser recompose when a setter is called.
- **Actor picking**: left-click calls `findActorOnPosition` which uses `isCollidingWith(boundingBoxCollisionMask)` on all `filteredVisibleActorsWithinViewport` and picks the one with the lowest `drawingOrder`. Right-click deletes the actor under the cursor. `boundingBoxCollisionMask` (in `extensions/PointBodyExtensions.kt`) builds the mask from a `BoxBody`'s **rendered** corners — scaled around its pivot, rotated, and translated exactly as it is drawn — so picking matches the on-screen shape for any scale/rotation/pivot, not just the unscaled box.
- **Placement preview**: when a type is selected in the browser (not yet placed), `previewOverlayActor` is a live instance whose `body.position` `OverlayManager` updates every frame to the snapped mouse scene coordinate. `EditorSelection` owns it on the main thread: `selectActorType` instantiates it for the selected type (or clears it), and placing it instantiates a fresh one, so the same type can be placed repeatedly until it is deselected.
- **Snap**: `snapMode: StateFlow<Pair<Int, Int>>` (x-grid, y-grid in scene units; 0 = disabled). Applied via `SceneOffset.snapped(snapMode)`.
- **Scene I/O** (`SceneFiles`): `loadMap(path)` / `saveScene(path)` use the `loadFile` / `saveFile` helpers (`helpers/SceneFileIo.kt`), which do the file I/O on the injected IO dispatcher (`Dispatchers.IO` by default); the results are applied on the injected main dispatcher (`Dispatchers.Main`), and scenes are serialized there before saving or syncing. A loaded file is refused when `deserializeSceneOrNull` rejects it (the deserializer throws, or returns nothing for content that is not an empty `[]` array). A refused load or a failed save shows a snackbar (`fileOperationError`) and leaves the scene, history, dirty flag and current file untouched. The open dialog filters for `.json` (ignored by Windows' native dialog). `syncScene()` is used in `Connected` mode, where a blank `sceneJson` is an empty scene and unreadable JSON starts empty with a snackbar.
- **Filter**: `filterText` filters the instance browser and visible-actor list by `typeId` (case-insensitive contains).
- **Undo/redo & dirty tracking**: `UndoRedoHistory` (in `helpers/`) keeps two bounded stacks of `SceneSnapshot(serializedScene, isSceneModified, actorIds)`. A snapshot is the serialized scene plus the unsaved-changes flag and the editor's id of every serialized actor, so undo/redo also restore the Save button state (`isSceneModified` drives whether Save is enabled and the `*` suffix on the file name). Pre-change snapshots are recorded at interaction boundaries — `onBeforeActorDrag` (drag start), before add/remove, and `onBeforePropertyChange(editKey)` for the property panel, where consecutive edits sharing an `editKey` coalesce into one step. Loading, `New`, and saving reset the dirty flag (and loading/`New` clear the history). Keyboard shortcuts (Ctrl/Cmd+Z, Ctrl/Cmd+Shift+Z, Ctrl/Cmd+Y) are handled by `KeyboardInputListener`. The scene's actors are tracked by `SceneDocument` (`sceneActors`, main thread only) rather than read back from `ActorManager`, whose batched updates lag; snapshots carry per-actor ids so undo/redo re-selects the same actor.

## Property inspector

`exposedMutableProperties` (`PropertyEditorMapper.kt`) uses Kotlin reflection to discover all `KMutableProperty` members of an actor class whose setter is annotated with `@Exposed`, sorted by name; `InstanceManagerColumn` remembers the list per selected actor class, and `toPropertyEditor` (same file) rebuilds the editor lambdas from it on recomposition. Property type is matched against a pre-built set of `KType` constants (`toPropertyEditorKind` in `PropertyEditorKind.kt`; `String` regardless of nullability). The body editor reads the actor's own `Positionable.body`. The displayed label is `@Exposed.name`. See `scene-editor-api/CLAUDE.md` for the full list of supported types.

## JSON scene format

Scene files are plain JSON produced by `SerializationManager.serializeActors(List<Editable<*>>)` and restored with `deserializeActors(String)`. The format is defined entirely by `plugin-serialization`; the editor just reads/writes files from disk. Default folder: `./src/commonMain/composeResources/files/scenes`. Default filename: `scene_untitled.json`.

## UI panels

- **Instance browser** (left column, `InstanceBrowserColumn`, 150 dp) — lists the placed actors, with a filter field and a visible-only toggle; click to select one.
- **Instance manager** (right column, `InstanceManagerColumn`, 220 dp) — with nothing selected, lists the actor types from `SerializationManager` (click to select a type for placement); with an actor selected, shows its header (deselect/locate/delete) and property editors.
- **File manager** (top row) — new/load/save via AWT `FileDialog`.
- **Metadata row** (bottom) — displays total actor count, snap settings, mouse scene coordinates, and the Translate/Scale/Rotate interaction-mode radios.
- **Settings window** — separate Compose Window (200×250 dp); contains color editor mode (RGB/HSV), angle editor mode (wheel/numeric), debug menu toggle.

## Navigation (keyboard)

`KeyboardInputListener` actor in `editorKubriko` handles Escape (deselect actor → deselect type → close editor, the last step only when the scene has no unsaved changes; with the Settings window open it closes that instead), acting on a release only when its press reached the editor (the desktop keyboard listener sees every window of the process, and only presses are focus-gated; the Settings window closes on its own Escape through its `onPreviewKeyEvent`), undo/redo shortcuts, the T/S/R interaction-mode shortcuts (gated while a text input is focused), and the arrow-key camera pan + `+`/`-` zoom (via `ViewportManager.handleKeys` in `helpers/CameraKeyControls.kt`). The editor deliberately pans with the arrow keys only — the plugin's `directionState` also accepts WASD, but the editor's own `handleKeys` ignores those letters so T/S (and W/A/D) stay free as shortcuts.

The active interaction mode lives in `EditorController.interactionMode` (default `Translate`, in-memory). It drives `handleMouseDrag` in `ModifierExtensions.kt`: a left-drag that started on the selected actor translates its `position` (snapped), or — for a `BoxBody` — sets `scale` (axis-independent: horizontal drag → horizontal scale, vertical drag → vertical scale, relative to the unscaled size) or `rotation` (orbit around the pivot at `body.position`). Shift-drag and middle-mouse always pan regardless of mode. The gesture's state is an `ActorDragState` remembered per editor viewport, and the scale/rotation math is the pure `draggedScale` / `draggedRotation` next to it (`extensions/ActorDragState.kt`).

`KeyboardInputManager` reads keys from a **global** source (a window-wide AWT listener on Desktop), so a focused Compose text field does **not** naturally steal key events from the camera handler — typing or moving the text cursor would otherwise pan/zoom the camera. To prevent this, text-input focus is tracked and the camera/zoom keys are suppressed while any field is focused:

- `LocalTextInputFocusReporter` (a `staticCompositionLocalOf`) carries a `(Boolean) -> Unit` reporter, provided in `InternalSceneEditor` around the editor UI. This avoids threading a focus callback through every property editor.
- The shared `ui-components` `TextInput` accepts an `onFocusChanged` callback and reports a **balanced** focus state — it emits `false` on dispose if it was focused, so the count can't leak when a focused field is removed (e.g. selecting a different actor). `EditorTextInput` is the only call site that forwards the local into it. The HEX field in `ColorPropertyEditor` (helpers in `HexColor.kt`) is a plain `BasicTextField` that reports to the local itself, with the same balanced on-dispose rule.
- `EditorController` keeps `focusedTextInputCount`; `KeyboardInputListener.handleActiveKeys` skips `ViewportManager.handleKeys` while it is `> 0`. Escape is gated too; undo/redo shortcuts are not.

## Persistence

User preferences (snap values, color/angle editor modes, debug menu visibility) are persisted via `PersistenceManager("kubrikoSceneEditor")` through the `UserPreferences` helper, which wraps `PersistenceManager` boolean/int accessors.
