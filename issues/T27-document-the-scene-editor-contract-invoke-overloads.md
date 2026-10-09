# Document the second and third invoke overloads of SceneEditorContract and fix the first one's KDoc

**Kind:** docs  ·  **Severity:** low  ·  **Platforms:** desktop  ·  **Class:** Now
**Artifact:** tool-scene-editor-api (published; KDoc only)
**Files:** tools/scene-editor-api/src/desktopMain/kotlin/com/pandulapeter/kubriko/sceneEditor/SceneEditorContract.kt

## Problem
At 2480325f:
- The first `invoke(defaultSceneFolderPath, serializationManager, customManagers, title, onCloseRequest)` overload's KDoc
  documents `@param defaultSceneFilename` and `@param sceneEditorMode`, which that overload does not have.
- The second overload (`invoke(defaultSceneFilename, defaultSceneFolderPath, serializationManager, customManagers, title, onCloseRequest)`)
  has only a `//` TODO about default arguments in member Composables, no KDoc.
- The third, abstract overload (with `sceneEditorMode`) — the one implementations override — has no KDoc at all.

## Fix
- First overload: keep the description; remove the two `@param`s it does not take, and say it opens no file and uses
  `SceneEditorMode.Normal`.
- Second overload: add KDoc ("Embeds the Scene Editor, loading [defaultSceneFilename] from [defaultSceneFolderPath] if not null, in
  [SceneEditorMode.Normal]") with its `@param`s. Keep the existing `// TODO: Remove this function once default arguments...` comment
  and link (it is a statement-level note; place it below the KDoc, above `@Composable`).
- Third overload: full KDoc with all seven `@param`s (move the `sceneEditorMode` and `onCloseRequest` wording from the first
  overload here; `onCloseRequest`'s "stays open until the caller removes it from composition; it then disposes its Kubriko
  instances, including [serializationManager] and [customManagers]" sentence belongs on every overload that takes it).
- Leave the inline `// TODO: = null,` default-value comments on the parameters as they are.

## Behaviour
Docs only.

## Public API
None (KDoc only).

## Tests
None.

## Verify
`./gradlew :tools:scene-editor-api:compileKotlinDesktop`

## Manual check
None.
