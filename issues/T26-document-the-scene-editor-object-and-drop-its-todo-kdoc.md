# Document the SceneEditor object and drop the "TODO: Documentation" KDoc on its overrides

**Kind:** docs  ·  **Severity:** low  ·  **Platforms:** desktop  ·  **Class:** Now
**Artifact:** tool-scene-editor (published; KDoc only)
**Files:** tools/scene-editor/src/desktopMain/kotlin/com/pandulapeter/kubriko/sceneEditor/SceneEditor.kt

## Problem
The public `object SceneEditor : SceneEditorContract` has no KDoc, and both overrides carry a placeholder that hides the
contract's documentation in the IDE:
```kotlin
    /**
     * TODO: Documentation
     */
    override fun show(
```
(lines 19-21 and 39-41 at 2480325f).

## Fix
Delete both `/** TODO: Documentation */` blocks (the overrides then inherit the contract's KDoc). Add a class KDoc modelled on
the noop's: "The real implementation of [SceneEditorContract]: a Desktop-only visual editor for [Editable] actors. Depend on
`tool-scene-editor-noop` instead to strip it from release builds." (adjust wording; no behaviour claims beyond what the code does).

## Behaviour
Docs only.

## Public API
None (KDoc only).

## Tests
None.

## Verify
`./gradlew :tools:scene-editor:compileKotlinDesktop`

## Manual check
None.
