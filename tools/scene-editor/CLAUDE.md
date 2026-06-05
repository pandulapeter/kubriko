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
- **Actor picking**: left-click calls `findActorOnPosition` which uses `isCollidingWith(boundingBoxCollisionMask)` on all `filteredVisibleActorsWithinViewport` and picks the one with the lowest `drawingOrder`. Right-click deletes the actor under the cursor.
- **Placement preview**: when a type is selected in the browser (not yet placed), `previewOverlayActor` is a live instance whose `body.position` is updated every frame to the snapped mouse scene coordinate.
- **Snap**: `snapMode: StateFlow<Pair<Int, Int>>` (x-grid, y-grid in scene units; 0 = disabled). Applied via `SceneOffset.snapped(snapMode)`.
- **Scene I/O**: `loadMap(path)` / `saveScene(path)` use coroutine-based `loadFile` / `saveFile` helpers. `syncScene()` is used in `Connected` mode.
- **Filter**: `filterText` filters the instance browser and visible-actor list by `typeId` (case-insensitive contains).

## Property inspector

`PropertyEditorMapper.kt` uses Kotlin reflection to discover all `KMutableProperty` members of the selected actor where the setter is annotated with `@Exposed`. Property type is matched against a pre-built set of `KType` constants (no allocation per-frame — discovery only happens on selection change). See `scene-editor-api/CLAUDE.md` for the full list of supported types.

## JSON scene format

Scene files are plain JSON produced by `SerializationManager.serializeActors(List<Editable<*>>)` and restored with `deserializeActors(String)`. The format is defined entirely by `plugin-serialization`; the editor just reads/writes files from disk. Default folder: `./src/commonMain/composeResources/files/scenes`. Default filename: `scene_untitled.json`.

## UI panels

- **Instance browser** (left column) — lists actor types from `SerializationManager`; click to select a type for placement.
- **Instance manager** (right column) — shows all placed actors; click to select; also shows property editors for the selected actor.
- **File manager** (top row) — new/load/save via AWT `FileDialog`.
- **Metadata row** (bottom) — displays mouse scene coordinates and total actor count.
- **Settings window** — separate Compose Window (200×250 dp); contains color editor mode (RGB/Hex), angle editor mode (wheel/numeric), debug menu toggle.

## Navigation (keyboard)

`KeyboardInputListener` actor in `editorKubriko` handles Escape: deselect actor → deselect type → close editor.

## Persistence

User preferences (snap values, color/angle editor modes, debug menu visibility) are persisted via `PersistenceManager("kubrikoSceneEditor")` through the `UserPreferences` helper, which wraps `PersistenceManager` boolean/int accessors.
