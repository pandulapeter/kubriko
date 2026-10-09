# Pass the loading flag into EditorOverlay instead of the whole EditorController

**Kind:** refactor  ·  **Severity:** low  ·  **Platforms:** desktop  ·  **Class:** Now
**Artifact:** tool-scene-editor (internal code only)
**Files:** tools/scene-editor/src/desktopMain/kotlin/com/pandulapeter/kubriko/sceneEditor/implementation/overlay/EditorOverlay.kt, tools/scene-editor/src/desktopMain/kotlin/com/pandulapeter/kubriko/sceneEditor/implementation/userInterface/EditorUserInterface.kt

## Problem
`EditorOverlay(modifier, editorController: EditorController, overlayKubriko: Kubriko)` reads exactly one thing from the controller:
```kotlin
    AnimatedVisibility(
        visible = editorController.shouldShowLoadingIndicator.collectAsState().value,
```
(`EditorOverlay.kt:40`). The code style says a component takes only the state it uses, never a whole controller.

## Fix
Replace the `editorController` parameter with `shouldShowLoadingIndicator: Boolean` and use it as `visible`. Drop the
`EditorController` and `collectAsState` imports from `EditorOverlay.kt`. In `EditorUserInterface.kt` pass
`shouldShowLoadingIndicator = editorController.shouldShowLoadingIndicator.collectAsState().value` in place of
`editorController = editorController`.

## Behaviour
The same StateFlow is collected one composable higher (inside the `DebugMenu` content lambda), so the indicator shows and hides
exactly as before; only the recomposition scope of the collection moves.

## Public API
None (internal).

## Tests
The existing ones.

## Verify
`./gradlew :tools:scene-editor:compileKotlinDesktop`

## Manual check
Load a scene file in the Scene Editor: the small progress indicator still appears bottom-left during loading.
