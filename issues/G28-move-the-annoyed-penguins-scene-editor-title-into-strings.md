# Move the AnnoyedPenguins scene editor window title into `strings.xml`.

**Kind:** refactor  ·  **Severity:** low  ·  **Platforms:** desktop  ·  **Class:** Now
**Artifact:** unpublished (examples)
**Files:** `examples/game-annoyed-penguins/src/desktopMain/kotlin/com/pandulapeter/kubriko/gameAnnoyedPenguins/AnnoyedPenguinsGameSceneEditor.kt`, `examples/game-annoyed-penguins/src/desktopMain/composeResources/values/strings.xml`
**Challenged:** amended — dropped the apostrophe bullet: the Annoyed Penguins title has none, and the module's common `strings.xml` does not contain `Blocky's Journey` (that is Blocky's Journey's string).

## Problem
`examples/game-annoyed-penguins/src/desktopMain/kotlin/com/pandulapeter/kubriko/gameAnnoyedPenguins/AnnoyedPenguinsGameSceneEditor.kt:48` passes `title = "Scene Editor - Annoyed Penguins"` to `SceneEditor(...)` — a user-facing window title written as a literal.

## Fix
- The desktop source set already has resources: `examples/game-annoyed-penguins/src/desktopMain/composeResources/values/strings.xml` defines `editor`, read through the module's common `Res` (`kubriko.examples.game_annoyed_penguins.generated.resources.Res`, e.g. `Res.string.editor` in `PlatformSpecificContent.desktop.kt`). Add `<string name="scene_editor_title">Scene Editor - Annoyed Penguins</string>` there (the same key the demos lane uses for its scene editor titles).
- Pass `title = stringResource(Res.string.scene_editor_title)`; import `kubriko.examples.game_annoyed_penguins.generated.resources.Res`, `kubriko.examples.game_annoyed_penguins.generated.resources.scene_editor_title` and `org.jetbrains.compose.resources.stringResource`. The call site is inside the `@Composable fun AnnoyedPenguinsGameSceneEditor`, and `SceneEditor`'s `title` is a `String`.

## Behaviour
Same window title text.

## Public API
None.

## Tests
The existing ones.

## Verify
`./gradlew :examples:game-annoyed-penguins:compileKotlinDesktop` and `./gradlew :app:desktop:compileKotlin`

## Manual check
Desktop Showcase with `showcase.isSceneEditorEnabled=true`: open the AnnoyedPenguins editor from the game's menu; the window title reads "Scene Editor - Annoyed Penguins".
