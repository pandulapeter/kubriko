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

## `EditorController`

`CoroutineScope` (SupervisorJob + Dispatchers.Default) that owns all mutable editor state:

- **Selection**: `selectedUpdatableActor: StateFlow<Pair<Editable<*>?, Boolean>>` — the second Boolean is a toggle (`triggerActorUpdate`) that forces recomposition of the property panel when a setter is called.
- **Actor picking**: left-click calls `findActorOnPosition` which uses `isCollidingWith(boundingBoxCollisionMask)` on all `filteredVisibleActorsWithinViewport` and picks the one with the lowest `drawingOrder`. Right-click deletes the actor under the cursor. `boundingBoxCollisionMask` (in `extensions/PointBodyExtensions.kt`) builds the mask from a `BoxBody`'s **rendered** corners — scaled around its pivot, rotated, and translated exactly as it is drawn — so picking matches the on-screen shape for any scale/rotation/pivot, not just the unscaled box.
- **Placement preview**: when a type is selected in the browser (not yet placed), `previewOverlayActor` is a live instance whose `body.position` is updated every frame to the snapped mouse scene coordinate.
- **Snap**: `snapMode: StateFlow<Pair<Int, Int>>` (x-grid, y-grid in scene units; 0 = disabled). Applied via `SceneOffset.snapped(snapMode)`.
- **Scene I/O**: `loadMap(path)` / `saveScene(path)` use coroutine-based `loadFile` / `saveFile` helpers. `syncScene()` is used in `Connected` mode.
- **Filter**: `filterText` filters the instance browser and visible-actor list by `typeId` (case-insensitive contains).
- **Undo/redo & dirty tracking**: `UndoRedoHistory` (in `helpers/`) keeps two bounded stacks of `SceneSnapshot(serializedScene, isSceneModified)`. A snapshot is the serialized scene plus the unsaved-changes flag, so undo/redo also restore the Save button state (`isSceneModified` drives whether Save is enabled and the `*` suffix on the file name). Pre-change snapshots are recorded at interaction boundaries — `onBeforeActorDrag` (drag start), before add/remove, and `onBeforePropertyChange(editKey)` for the property panel, where consecutive edits sharing an `editKey` coalesce into one step. Loading, `New`, and saving reset the dirty flag (and loading/`New` clear the history). Keyboard shortcuts (Ctrl/Cmd+Z, Ctrl/Cmd+Shift+Z, Ctrl/Cmd+Y) are handled by `KeyboardInputListener`.

## Property inspector

`PropertyEditorMapper.kt` uses Kotlin reflection to discover all `KMutableProperty` members of the selected actor where the setter is annotated with `@Exposed`. Property type is matched against a pre-built set of `KType` constants (no allocation per-frame — discovery only happens on selection change). The displayed label is `@Exposed.name`, falling back to the `KMutableProperty.name` when the annotation's `name` is blank. See `scene-editor-api/CLAUDE.md` for the full list of supported types.

## JSON scene format

Scene files are plain JSON produced by `SerializationManager.serializeActors(List<Editable<*>>)` and restored with `deserializeActors(String)`. The format is defined entirely by `plugin-serialization`; the editor just reads/writes files from disk. Default folder: `./src/commonMain/composeResources/files/scenes`. Default filename: `scene_untitled.json`.

## UI panels

- **Instance browser** (left column) — lists actor types from `SerializationManager`; click to select a type for placement.
- **Instance manager** (right column) — shows all placed actors; click to select; also shows property editors for the selected actor.
- **File manager** (top row) — new/load/save via AWT `FileDialog`.
- **Metadata row** (bottom) — displays total actor count, snap settings, mouse scene coordinates, and the Translate/Scale/Rotate interaction-mode radios.
- **Settings window** — separate Compose Window (200×250 dp); contains color editor mode (RGB/HSV), angle editor mode (wheel/numeric), debug menu toggle.

## Navigation (keyboard)

`KeyboardInputListener` actor in `editorKubriko` handles Escape (deselect actor → deselect type → close editor), undo/redo shortcuts, the T/S/R interaction-mode shortcuts (gated while a text input is focused), and the arrow-key camera pan + `+`/`-` zoom (via `ViewportManager.handleKeys`). The editor deliberately pans with the arrow keys only — the plugin's `directionState` also accepts WASD, but the editor's own `handleKeys` ignores those letters so T/S (and W/A/D) stay free as shortcuts.

The active interaction mode lives in `EditorController.interactionMode` (default `Translate`, in-memory). It drives `handleMouseDrag` in `ModifierExtensions.kt`: a left-drag that started on the selected actor translates its `position` (snapped), or — for a `BoxBody` — sets `scale` (axis-independent: horizontal drag → horizontal scale, vertical drag → vertical scale, relative to the unscaled size) or `rotation` (orbit around the pivot at `body.position`). Shift-drag and middle-mouse always pan regardless of mode.

`KeyboardInputManager` reads keys from a **global** source (a window-wide AWT listener on Desktop), so a focused Compose text field does **not** naturally steal key events from the camera handler — typing or moving the text cursor would otherwise pan/zoom the camera. To prevent this, text-input focus is tracked and the camera/zoom keys are suppressed while any field is focused:

- `LocalTextInputFocusReporter` (a `staticCompositionLocalOf`) carries a `(Boolean) -> Unit` reporter, provided in `InternalSceneEditor` around the editor UI. This avoids threading a focus callback through every property editor.
- The shared `ui-components` `TextInput` accepts an `onFocusChanged` callback and reports a **balanced** focus state — it emits `false` on dispose if it was focused, so the count can't leak when a focused field is removed (e.g. selecting a different actor). `EditorTextInput` and the HEX field in `ColorPropertyEditor` are the only call sites that forward the local into it.
- `EditorController` keeps `focusedTextInputCount`; `KeyboardInputListener.handleActiveKeys` skips `ViewportManager.handleKeys` while it is `> 0`. Discrete shortcuts (Escape, undo/redo) are **not** gated.

## Persistence

User preferences (snap values, color/angle editor modes, debug menu visibility) are persisted via `PersistenceManager("kubrikoSceneEditor")` through the `UserPreferences` helper, which wraps `PersistenceManager` boolean/int accessors.
