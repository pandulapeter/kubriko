# Rename SnapHelpers.kt to SceneOffsetSnapping.kt

**Kind:** refactor  ·  **Severity:** low  ·  **Platforms:** desktop  ·  **Class:** Now
**Artifact:** tool-scene-editor (internal code only)
**Files:** tools/scene-editor/src/desktopMain/kotlin/com/pandulapeter/kubriko/sceneEditor/implementation/helpers/SnapHelpers.kt (renamed), tools/scene-editor/src/desktopMain/kotlin/com/pandulapeter/kubriko/sceneEditor/implementation/helpers/SceneOffsetSnapping.kt (new name)

## Problem
Found while verifying the `*Helpers.kt` finding: `helpers/SnapHelpers.kt` holds a single function,
`internal fun SceneOffset.snapped(snapMode: Pair<Int, Int>)`. The code style forbids `*Helpers.kt` names.

## Fix
`git mv` to `helpers/SceneOffsetSnapping.kt`; content unchanged. Callers (`EditorController`, `OverlayManager`,
`ModifierExtensions`) import `helpers.snapped` by name. Grep the repo for `SnapHelpers` (none expected).

## Behaviour
Rename only.

## Public API
None.

## Tests
The existing ones.

## Verify
`./gradlew :tools:scene-editor:compileKotlinDesktop`

## Manual check
None.
