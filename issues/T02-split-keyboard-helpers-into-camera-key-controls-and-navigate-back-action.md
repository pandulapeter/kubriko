# Split KeyboardHelpers.kt into CameraKeyControls.kt and NavigateBackAction.kt

**Kind:** refactor  ·  **Severity:** low  ·  **Platforms:** desktop  ·  **Class:** Now
**Artifact:** tool-scene-editor (internal code only)
**Files:** tools/scene-editor/src/desktopMain/kotlin/com/pandulapeter/kubriko/sceneEditor/implementation/helpers/KeyboardHelpers.kt (deleted), tools/scene-editor/src/desktopMain/kotlin/com/pandulapeter/kubriko/sceneEditor/implementation/helpers/CameraKeyControls.kt (new), tools/scene-editor/src/desktopMain/kotlin/com/pandulapeter/kubriko/sceneEditor/implementation/helpers/NavigateBackAction.kt (new)

## Problem
`helpers/KeyboardHelpers.kt` is a "Helpers" grab bag (the code style forbids `*Helpers.kt`) holding two unrelated things:
the camera pan/zoom key handling (`private const val CAMERA_SPEED`, `private const val CAMERA_SPEED_DIAGONAL`,
`internal fun ViewportManager.handleKeys(keys: Set<Key>)` with its KDoc), and the Escape decision
(`internal enum class NavigateBackAction { DESELECT_ACTOR, DESELECT_TYPE, CLOSE, NONE }` plus
`internal fun navigateBackAction(hasSelectedActor, hasSelectedType, isSettingsOpen, isSceneModified, isTextInputFocused)`
with its KDoc). Its test is already called `tools/scene-editor/src/desktopTest/kotlin/com/pandulapeter/kubriko/sceneEditor/implementation/helpers/NavigateBackActionTest.kt`.

## Fix
Same package `...implementation.helpers`, MPL-2.0 header on both new files:
- `helpers/CameraKeyControls.kt` ← `CAMERA_SPEED`, `CAMERA_SPEED_DIAGONAL` (stay `private`), `ViewportManager.handleKeys`
  with its KDoc, and the imports it needs (`Offset`, `Key`, `KeyboardZoomState`, `zoomState`, `ViewportManager`).
- `helpers/NavigateBackAction.kt` ← `NavigateBackAction` and `navigateBackAction(...)` with its KDoc (no imports needed).
- Delete `KeyboardHelpers.kt`.

Callers (`EditorController.kt`, `actors/KeyboardInputListener.kt`) import by package-level name, so they need no change.
Grep the whole repo (docs, CLAUDE.md files, skills) for `KeyboardHelpers` and fix any reference (none at 2480325f).

## Behaviour
Verbatim move; nothing changes.

## Public API
None (internal).

## Tests
The existing ones (`NavigateBackActionTest` stays where it is; it now matches its subject's file name).

## Verify
`./gradlew :tools:scene-editor:compileKotlinDesktop :tools:scene-editor:desktopTest`

## Manual check
None.
