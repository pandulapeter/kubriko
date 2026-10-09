# Replace the module-global scene-editor flags of the games and demos with a `SceneEditorConnection` owned by the state holder.

**Kind:** refactor  ·  **Severity:** medium  ·  **Platforms:** desktop (expect/actual on all)  ·  **Class:** Planned
**Artifact:** unpublished (examples, app)
**Merges:** the games' and the demos' findings about the same globals (one plan for all four modules).
**Files:**
- Annoyed Penguins: `examples/game-annoyed-penguins/src/commonMain/kotlin/com/pandulapeter/kubriko/gameAnnoyedPenguins/implementation/ui/PlatformSpecificContent.kt`, `examples/game-annoyed-penguins/src/{androidMain,desktopMain,iosMain,webMain}/kotlin/com/pandulapeter/kubriko/gameAnnoyedPenguins/implementation/ui/PlatformSpecificContent.{android,desktop,ios,web}.kt`, `examples/game-annoyed-penguins/src/desktopMain/kotlin/com/pandulapeter/kubriko/gameAnnoyedPenguins/AnnoyedPenguinsGameSceneEditor.kt`, `examples/game-annoyed-penguins/src/commonMain/kotlin/com/pandulapeter/kubriko/gameAnnoyedPenguins/implementation/AnnoyedPenguinsGameStateHolderImpl.kt`, `examples/game-annoyed-penguins/src/commonMain/kotlin/com/pandulapeter/kubriko/gameAnnoyedPenguins/implementation/ui/MenuOverlay.kt`
- Blocky's Journey: the same five `PlatformSpecificContent*.kt` under `examples/game-blockys-journey/src/…/gameBlockysJourney/implementation/ui/`, `examples/game-blockys-journey/src/desktopMain/kotlin/com/pandulapeter/kubriko/gameBlockysJourney/BlockysJourneyGameSceneEditor.kt`, `examples/game-blockys-journey/src/commonMain/kotlin/com/pandulapeter/kubriko/gameBlockysJourney/implementation/BlockysJourneyGameStateHolderImpl.kt`, `examples/game-blockys-journey/src/commonMain/kotlin/com/pandulapeter/kubriko/gameBlockysJourney/implementation/ui/MenuOverlay.kt`
- demo-performance: `examples/demo-performance/src/{commonMain,androidMain,desktopMain,iosMain,webMain}/kotlin/com/pandulapeter/kubriko/demoPerformance/implementation/PlatformSpecificContent*.kt`, `examples/demo-performance/src/desktopMain/kotlin/com/pandulapeter/kubriko/demoPerformance/PerformanceDemoSceneEditor.kt`, `examples/demo-performance/src/commonMain/kotlin/com/pandulapeter/kubriko/demoPerformance/implementation/managers/PerformanceDemoManager.kt`, `…/implementation/PerformanceDemoStateHolderImpl.kt` (after D09), `…/implementation/ui/PerformanceDemoOverlay.kt` (after D12, which moves the `PlatformSpecificContent()` call there)
- demo-physics: the same files under `examples/demo-physics/…/demoPhysics/` (`PhysicsDemoSceneEditor.kt`, `PhysicsDemoManager.kt`, `PhysicsDemoStateHolderImpl.kt` after D14, `ui/PhysicsDemoOverlay.kt` after D17)
- the four desktopMain `composeResources/values/strings.xml`; `app/desktop/src/main/kotlin/com/pandulapeter/kubrikoShowcase/KubrikoShowcaseApp.kt` (the four `*SceneEditor(...)` calls, :138–149 at 2480325f; A50 may move them); with option B also `app/shared/src/commonMain/kotlin/com/pandulapeter/kubrikoShowcase/implementation/ui/ShowcaseStateHolders.kt` (after A08; A51's `ShowcaseSession` if that has landed) and the four factories' files (`*StateHolderFactory.kt` after G05/G06 for the games; the demos' entry files); with option C also `examples/shared/build.gradle.kts`, `examples/shared/src/commonMain/kotlin/com/pandulapeter/kubriko/shared/…` (desktopMain), `examples/shared/CLAUDE.md`; each touched module's `CLAUDE.md`
**Challenged:** amended — removed stale cross-references (old working ids GP6/G10/G11/D51; the demos' title plans are D13/D21, not D12/D18); updated the file list to the paths the Now plans leave (D09/D14 `*StateHolderImpl.kt`, D12/D17 overlay files that now call `PlatformSpecificContent()`, A08's `ShowcaseStateHolders.kt`, A50's desktop window split); rewrote option B, which as written would have created every pooled state holder from `app/desktop` (`getStateHolder()` creates on read) and left an open editor window bound to a holder that `ExampleScreen` disposes when the user leaves the example.

## Problem
The in-game "Editor" button and the separate desktop editor window talk through module-level globals that act as a service locator:
- `internal val isSceneEditorVisible = MutableStateFlow(false)` in `examples/game-annoyed-penguins/src/desktopMain/…/implementation/ui/PlatformSpecificContent.desktop.kt:20` and its Blocky's Journey twin; read by the menu button and by `AnnoyedPenguinsGameSceneEditor` / `BlockysJourneyGameSceneEditor` (`if (isSceneEditorVisible.collectAsState().value) { … onCloseRequest = { isSceneEditorVisible.value = false } }`).
- demo-performance and demo-physics: `internal val isSceneEditorVisible` plus `internal actual val sceneJson: MutableStateFlow<String>?` (desktop) / `null` (other platforms), five identical expect/actual files per module, `loadMap()`/`processJson()` with a silent `catch (_: MissingResourceException) {}`, and the `open_scene_editor` / `close_scene_editor` strings, all duplicated.
- `app/desktop/…/KubrikoShowcaseApp.kt:138–149` calls the four `*SceneEditor(defaultSceneFolderPath = …)` Composables, which each `remember` a second, editor-only state holder.
The globals are hidden shared state between the Showcase's game instance and the editor window: two Showcase windows, or a test, would share them; nothing in a signature shows the dependency.

Each `*SceneEditor.kt` also holds a `fun main()` used to launch the editor stand-alone from the IDE (JVM class `AnnoyedPenguinsGameSceneEditorKt` etc.; referenced only from the user's local `.idea/workspace.xml`).

Not a problem (checked): the editor-only state holder is never disposed, but its Kubriko instances are `by lazy` and never created in editor mode; its managers are handed to the editor's own Kubriko, which disposes them. Calling `dispose()` on it would instantiate the lazy instances just to dispose them — do not add that.

## Fix
- A small `SceneEditorConnection` (`isVisible: StateFlow<Boolean>`, `toggle()`, `close()`, and for the demos `sceneJson: MutableStateFlow<String>`) replacing the globals: held by the state holder Impl, which receives it from its creator (option B) or reads the module's single desktop instance (option A); `null` on platforms without the editor (expect/actual or a nullable parameter).
- The menu button / `LargeButton` read it from the state holder they already get.
- The editor window Composables take what they need explicitly (see Decision); `defaultSceneFolderPath` stays.
- The `main()` entry points keep their file names (`*SceneEditor.kt`) so the JVM class names stay valid for IDE run configurations.
- The window titles are already in `strings.xml` by then (G28/G29, demos D13/D21); keep them.

## Decision
How the desktop window reaches the connection:
- **A:** keep one process-wide connection per module, but as a single named `internal object …SceneEditorConnection` in its own desktopMain file. Smallest change; still a global.
- **B (recommended):** the connection is created once per entry by the Showcase's process-scoped pool (A08's `ShowcaseStateHolders.kt`, or A51's `ShowcaseSession`), passed into the factory (`createAnnoyedPenguinsGameStateHolder(…, sceneEditorConnection = …)`) and, from `app/desktop`, into `AnnoyedPenguinsGameSceneEditor(connection, defaultSceneFolderPath)`. It must **not** be reached through `ShowcaseEntry.getStateHolder()`: that creates the holder on read (calling it for four entries from `app/desktop` would build every game and demo at startup — Wallbreaker's Impl creates its `Kubriko` instances eagerly), and `ExampleScreen`'s `onDispose` disposes and drops the holder when the user leaves the example, while today's global keeps an open editor window working across that. The connection therefore lives as long as the process, like today's global, but is explicit in every signature. Needs `app/desktop` to see the pool's connections (today `getStateHolder()` is `internal` to `app/shared`).
- **C:** B plus one shared desktop `SceneEditorWindow(connection, title, serializationManager, customManagers, …)` in `examples/shared` desktopMain. Removes the four near-identical window Composables but makes `examples/shared` depend on `scene-editor` / `scene-editor-noop` (the `showcase.isSceneEditorEnabled` switch), which every demo and test module would then inherit.

## Behaviour
Same: the button opens and closes the window, closing the window resets the button, edits in the demos' connected mode reach the running demo.

## Public API
None published (examples and app are unpublished). The examples' `*SceneEditor` Composable signatures change under B/C.

## Tests
None (desktop windows). If the connection grows logic beyond a flag, test its toggling in `desktopTest`.

## Verify
`./gradlew :examples:game-annoyed-penguins:compileKotlinDesktop :examples:game-blockys-journey:compileKotlinDesktop :examples:demo-performance:compileKotlinDesktop :examples:demo-physics:compileKotlinDesktop`, the same modules with `compileKotlinWasmJs`, `compileAndroidMain` and `compileKotlinIosSimulatorArm64` (expect/actual), and `./gradlew :app:desktop:compileKotlin` — each with `-Pshowcase.isSceneEditorEnabled=true` and `false`.

## Manual check
Desktop Showcase with the scene editor enabled: open each of the four editors from its example, close it from the window and from the button, edit the performance/physics scene in connected mode and see the demo update; run each `main()` from the IDE.
