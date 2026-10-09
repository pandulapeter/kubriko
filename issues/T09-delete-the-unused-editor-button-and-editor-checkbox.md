# Delete the unused EditorButton and EditorCheckbox components

**Kind:** refactor  ·  **Severity:** low  ·  **Platforms:** desktop  ·  **Class:** Now
**Artifact:** tool-scene-editor (internal code only)
**Files:** tools/scene-editor/src/desktopMain/kotlin/com/pandulapeter/kubriko/sceneEditor/implementation/userInterface/components/EditorButton.kt (deleted), tools/scene-editor/src/desktopMain/kotlin/com/pandulapeter/kubriko/sceneEditor/implementation/userInterface/components/EditorCheckbox.kt (deleted)

## Problem
`internal fun EditorButton(...)` (`components/EditorButton.kt`) and `internal fun EditorCheckbox(...)`
(`components/EditorCheckbox.kt`) have no callers anywhere in the repo (grep for `EditorButton` / `EditorCheckbox` finds only
their own declarations at 2480325f).

## Fix
Delete both files. Re-grep the repo for both names (must be zero hits). `EditorText` stays (many other callers).

## Behaviour
Dead code; nothing changes.

## Public API
None (internal).

## Tests
The existing ones.

## Verify
`./gradlew :tools:scene-editor:compileKotlinDesktop`

## Manual check
None.
