# Move FileOperationError out of EditorController.kt into a file of its own

**Kind:** refactor  ·  **Severity:** low  ·  **Platforms:** desktop  ·  **Class:** Now
**Artifact:** tool-scene-editor (internal code only)
**Files:** tools/scene-editor/src/desktopMain/kotlin/com/pandulapeter/kubriko/sceneEditor/implementation/EditorController.kt, tools/scene-editor/src/desktopMain/kotlin/com/pandulapeter/kubriko/sceneEditor/implementation/FileOperationError.kt (new)

## Problem
`EditorController.kt` (528 lines) ends with a second top-level type that `EditorUserInterface.kt` also uses:

```kotlin
internal enum class FileOperationError {
    LOAD_FAILED,
    SAVE_FAILED,
    CONNECTED_SCENE_INVALID,
}
```
(`EditorController.kt:524-528` at 2480325f). The code style asks for one top-level type per file, named after it.

## Fix
Move the enum verbatim into `tools/scene-editor/src/desktopMain/kotlin/com/pandulapeter/kubriko/sceneEditor/implementation/FileOperationError.kt` (same package `com.pandulapeter.kubriko.sceneEditor.implementation`,
MPL-2.0 header copied from a sibling file). Visibility stays `internal`. No import changes are needed in
`EditorUserInterface.kt` (it already imports `com.pandulapeter.kubriko.sceneEditor.implementation.FileOperationError`).
Grep the repo for `FileOperationError` in docs/CLAUDE.md files (none expected).

## Behaviour
Verbatim move; nothing changes.

## Public API
None (internal).

## Tests
The existing ones.

## Verify
`./gradlew :tools:scene-editor:compileKotlinDesktop`

## Manual check
None.
