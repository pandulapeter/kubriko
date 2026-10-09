# Read the Physics demo's Scene Editor window title from string resources.

**Kind:** refactor  ·  **Severity:** low  ·  **Platforms:** desktop  ·  **Class:** Now
**Artifact:** unpublished (examples)
**Files:** `examples/demo-physics/src/desktopMain/kotlin/com/pandulapeter/kubriko/demoPhysics/PhysicsDemoSceneEditor.kt`, `examples/demo-physics/src/desktopMain/composeResources/values/strings.xml`

## Problem
`examples/demo-physics/src/desktopMain/kotlin/com/pandulapeter/kubriko/demoPhysics/PhysicsDemoSceneEditor.kt:46` passes `title = "Scene Editor - Physics Demo",` as a literal, against the strings rule. desktopMain resources already exist (`open_scene_editor` / `close_scene_editor`).

## Fix
Add `<string name="scene_editor_title">Scene Editor - Physics Demo</string>` to `examples/demo-physics/src/desktopMain/composeResources/values/strings.xml` and pass `title = stringResource(Res.string.scene_editor_title)` (imports `org.jetbrains.compose.resources.stringResource`, `kubriko.examples.demo_physics.generated.resources.Res`, `…scene_editor_title`). Leave `main()` alone.

Lane G's planned scene-editor connection plan touches the same files later; it re-checks them.

## Behaviour
Same text.

## Public API
None.

## Tests
None.

## Verify
`./gradlew :examples:demo-physics:compileKotlinDesktop` and `./gradlew :app:desktop:compileKotlin`

## Manual check
Desktop: the Physics Scene Editor window title reads "Scene Editor - Physics Demo".
