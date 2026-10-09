# Rename isDebutMenuEnabled / onIsDebutMenuEnabledChanged to isDebugMenuEnabled / onIsDebugMenuEnabledChanged

**Kind:** refactor  ·  **Severity:** low  ·  **Platforms:** desktop  ·  **Class:** Now
**Artifact:** tool-scene-editor (internal code only)
**Files:** tools/scene-editor/src/desktopMain/kotlin/com/pandulapeter/kubriko/sceneEditor/implementation/userInterface/panels/settings/Settings.kt, tools/scene-editor/src/desktopMain/kotlin/com/pandulapeter/kubriko/sceneEditor/implementation/userInterface/panels/settings/DebugMenuSettings.kt, tools/scene-editor/src/desktopMain/kotlin/com/pandulapeter/kubriko/sceneEditor/implementation/InternalSceneEditor.kt

## Problem
The debug-menu toggle's parameters are misspelled "Debut":
- `Settings.kt:34-35` `isDebutMenuEnabled: Boolean, onIsDebutMenuEnabledChanged: (Boolean) -> Unit,` and the pass-through at 66-67;
- `DebugMenuSettings.kt:21-22` (parameters) and 26-27 (`isChecked = isDebutMenuEnabled, onCheckedChanged = onIsDebutMenuEnabledChanged`);
- `InternalSceneEditor.kt:177-178` (named arguments).

## Fix
Rename to `isDebugMenuEnabled` / `onIsDebugMenuEnabledChanged` at every site above. Afterwards `grep -rn "Debut" tools/` must be empty.

## Behaviour
Parameter rename of internal Composables; nothing changes.

## Public API
None (internal).

## Tests
The existing ones.

## Verify
`./gradlew :tools:scene-editor:compileKotlinDesktop`

## Manual check
None.
