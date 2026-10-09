# Read the Performance demo's Scene Editor window title from string resources.

**Kind:** refactor  ·  **Severity:** low  ·  **Platforms:** desktop  ·  **Class:** Now
**Artifact:** unpublished (examples)
**Files:** `examples/demo-performance/src/desktopMain/kotlin/com/pandulapeter/kubriko/demoPerformance/PerformanceDemoSceneEditor.kt`, `examples/demo-performance/src/desktopMain/composeResources/values/strings.xml`

## Problem
`examples/demo-performance/src/desktopMain/kotlin/com/pandulapeter/kubriko/demoPerformance/PerformanceDemoSceneEditor.kt:46` passes `title = "Scene Editor - Performance Demo",` — a user-facing window title as a literal, against the strings rule. The desktopMain resources already exist (`open_scene_editor`, `close_scene_editor`, used by `PlatformSpecificContent.desktop.kt` via `kubriko.examples.demo_performance.generated.resources.Res`).

## Fix
- Add `<string name="scene_editor_title">Scene Editor - Performance Demo</string>` to `examples/demo-performance/src/desktopMain/composeResources/values/strings.xml`.
- In `PerformanceDemoSceneEditor` (a `@Composable`), pass `title = stringResource(Res.string.scene_editor_title)`, importing `org.jetbrains.compose.resources.stringResource`, `kubriko.examples.demo_performance.generated.resources.Res` and `…scene_editor_title`. Leave `main()`'s `SceneEditor.show(...)` alone (it uses the API default title).

Lane G's planned scene-editor connection plan touches the same file and strings.xml later; it re-checks them.

## Behaviour
Same text on desktop (resources load synchronously there).

## Public API
None.

## Tests
None.

## Verify
`./gradlew :examples:demo-performance:compileKotlinDesktop` and `./gradlew :app:desktop:compileKotlin`

## Manual check
Desktop Showcase with the scene editor enabled: open the Performance Scene Editor, the window title reads "Scene Editor - Performance Demo".
