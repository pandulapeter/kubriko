# Move the BlockysJourney scene editor window title into `strings.xml`.

**Kind:** refactor  ·  **Severity:** low  ·  **Platforms:** desktop  ·  **Class:** Now
**Artifact:** unpublished (examples)
**Files:** `examples/game-blockys-journey/src/desktopMain/kotlin/com/pandulapeter/kubriko/gameBlockysJourney/BlockysJourneyGameSceneEditor.kt`, `examples/game-blockys-journey/src/desktopMain/composeResources/values/strings.xml`

## Problem
`examples/game-blockys-journey/src/desktopMain/kotlin/com/pandulapeter/kubriko/gameBlockysJourney/BlockysJourneyGameSceneEditor.kt:48` passes `title = "Scene Editor - Blocky's Journey"` to `SceneEditor(...)` — a user-facing window title written as a literal.

## Fix
- The desktop source set already has resources: `examples/game-blockys-journey/src/desktopMain/composeResources/values/strings.xml` defines `editor`, read through the module's common `Res` (`kubriko.examples.game_blockys_journey.generated.resources.Res`, e.g. `Res.string.editor` in `PlatformSpecificContent.desktop.kt`). Add `<string name="scene_editor_title">Scene Editor - Blocky's Journey</string>` there (the same key the demos lane uses for its scene editor titles).
- Pass `title = stringResource(Res.string.scene_editor_title)`; import `kubriko.examples.game_blockys_journey.generated.resources.Res`, `kubriko.examples.game_blockys_journey.generated.resources.scene_editor_title` and `org.jetbrains.compose.resources.stringResource`. The call site is inside the `@Composable fun BlockysJourneyGameSceneEditor`, and `SceneEditor`'s `title` is a `String`.
- An unescaped apostrophe is fine: the module's common `strings.xml` already has `Blocky's Journey` in `close_confirmation`.

## Behaviour
Same window title text.

## Public API
None.

## Tests
The existing ones.

## Verify
`./gradlew :examples:game-blockys-journey:compileKotlinDesktop` and `./gradlew :app:desktop:compileKotlin`

## Manual check
Desktop Showcase with `showcase.isSceneEditorEnabled=true`: open the BlockysJourney editor from the game's menu; the window title reads "Scene Editor - Blocky's Journey".
