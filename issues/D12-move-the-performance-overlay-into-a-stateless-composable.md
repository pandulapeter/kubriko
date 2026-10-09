# Move `PerformanceDemoManager`'s overlay UI into `ui/PerformanceDemoOverlay.kt`.

**Kind:** refactor  ·  **Severity:** low  ·  **Platforms:** all  ·  **Class:** Now
**Artifact:** unpublished (examples)
**Files:** `examples/demo-performance/src/commonMain/kotlin/com/pandulapeter/kubriko/demoPerformance/implementation/managers/PerformanceDemoManager.kt`, `examples/demo-performance/src/commonMain/kotlin/com/pandulapeter/kubriko/demoPerformance/implementation/ui/PerformanceDemoOverlay.kt` (new), `examples/demo-performance/CLAUDE.md`
**Challenged:** amended — corrected the stale cross-reference to the scene-editor connection plan (G55, not D51).

## Problem
After D11, `examples/demo-performance/src/commonMain/kotlin/com/pandulapeter/kubriko/demoPerformance/implementation/managers/PerformanceDemoManager.kt`'s `override fun Composable(windowInsets: WindowInsets) = Box {` (originally :82–130) mixes scene loading with the full overlay: `LoadingOverlay`, the Column with `InfoPanel`, the `AnimatedVisibility` mini map (building five lambdas over `actorManager`/`viewportManager`) and the scene-editor button box.

## Fix
1. Create `examples/demo-performance/src/commonMain/kotlin/com/pandulapeter/kubriko/demoPerformance/implementation/ui/PerformanceDemoOverlay.kt` (license header from a sibling) with:
   ```kotlin
   @Composable
   internal fun PerformanceDemoOverlay(
       windowInsets: WindowInsets,
       shouldShowLoadingIndicator: Boolean,
       areActorsLoaded: Boolean,
       totalRuntimeInMilliseconds: StateFlow<Long>,
       getViewportTopLeft: () -> SceneOffset,
       getViewportBottomRight: () -> SceneOffset,
       getAllVisibleActors: () -> List<Visible>,
       getAllVisibleActorsWithinViewport: () -> List<Visible>,
       getAllActiveDynamicActors: () -> List<Visible>,
       isSceneEditorEnabled: Boolean,
   )
   ```
   (match the exact types `MiniMap`'s parameters declare). Its body is the override's `Box { … }` verbatim, with `LoadingOverlay(shouldShowLoadingIndicator = shouldShowLoadingIndicator)`, `AnimatedVisibility(visible = areActorsLoaded, …)`, and the D11 line kept **inside the AnimatedVisibility content** as `gameTime = remember(totalRuntimeInMilliseconds) { totalRuntimeInMilliseconds.filter { it % 2 == 0L } }.collectAsState(0L).value`, so the per-tick recomposition stays confined to that scope. The `if (isSceneEditorEnabled) { Box(…) { PlatformSpecificContent() } }` block moves as is (`PlatformSpecificContent` is the module's own expect Composable).
2. The override collects `shouldShowLoadingIndicator` and `actorManager.allActors.collectAsState().value.isNotEmpty()` and calls `PerformanceDemoOverlay`, passing `metadataManager.totalRuntimeInMilliseconds` and the five lambdas exactly as they are written today. Trim imports.
3. CLAUDE.md: "`PerformanceDemoManager` — loads the scene JSON and owns the UI composable" becomes "loads the scene JSON; its `Composable` override delegates to `ui/PerformanceDemoOverlay`".

Lane G's planned scene-editor connection plan (G55) rewrites `PlatformSpecificContent`; it re-checks this file.

## Behaviour
Same tree and modifiers; `gameTime` is collected in the same scope as before. `areActorsLoaded` is now read in the override, so an actor-list change recomposes the overlay root instead of the Column — only at scene load.

## Public API
None.

## Tests
The existing ones (none).

## Verify
`./gradlew :examples:demo-performance:compileKotlinDesktop` and `./gradlew :examples:demo-performance:compileKotlinWasmJs`

## Manual check
Open Performance: loading overlay fades, info panel and animating mini map appear; on Desktop with the scene editor enabled the "Open Scene Editor" button sits bottom-right.
