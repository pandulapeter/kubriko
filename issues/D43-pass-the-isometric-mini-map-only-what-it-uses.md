# Pass the isometric `MiniMap` only what it reads instead of the whole state holder.

**Kind:** refactor  ·  **Severity:** low  ·  **Platforms:** all  ·  **Class:** Now
**Artifact:** unpublished (examples)
**Files:** `examples/demo-isometric-graphics/src/commonMain/kotlin/com/pandulapeter/kubriko/demoIsometricGraphics/implementation/ui/MiniMap.kt`, `examples/demo-isometric-graphics/src/commonMain/kotlin/com/pandulapeter/kubriko/demoIsometricGraphics/implementation/ui/IsometricGraphicsContent.kt`, `examples/demo-isometric-graphics/src/commonMain/kotlin/com/pandulapeter/kubriko/demoIsometricGraphics/implementation/IsometricGraphicsDemoStateHolderImpl.kt` (after D40), `examples/demo-isometric-graphics/CLAUDE.md`
**Challenged:** amended — pinned the exact flow types (`ImmutableList<Visible>` etc.) and the rewording of the `logicViewportManager` comment, which names no parameter.

## Problem
`internal fun MiniMap(stateHolder: IsometricGraphicsDemoStateHolderImpl, gridMap: GridMap?, modifier: Modifier = Modifier)` (`MiniMap.kt:64-67`) takes the whole holder, and does a service-locator lookup inside its `LaunchedEffect`: `val actorManager = stateHolder.logicKubriko.get<ActorManager>()` (:89). It only reads `stateHolder.logicKubriko` (the hidden viewport), `stateHolder.volumetricRenderManager.worldRotation`, `stateHolder.controlManager.cameraOffset` and the logic `ActorManager.visibleActorsWithinViewport`.

## Fix
- Impl: add `val logicVisibleActors = logicActorManager.visibleActorsWithinViewport` (`logicActorManager` stays private).
- `MiniMap(logicKubriko: Kubriko, logicVisibleActors: StateFlow<List<Actor>>, worldRotation: StateFlow<AngleRadians>, cameraOffset: StateFlow<SceneOffset>, gridMap: GridMap?, modifier: Modifier = Modifier)` — use the exact element types the existing flows declare. Inside: `kubriko = logicKubriko`, `worldRotation.collectAsState()`, `cameraOffset.collectAsState()`, and the `LaunchedEffect` loop passes `actors = logicVisibleActors.value` (still read inside the loop every `MINI_MAP_REFRESH_MS`, never collected). Drop the `get`/`ActorManager`/Impl imports. Keep every `//` comment, rewording "stateHolder.logicKubriko" to "`logicKubriko`" and "stateHolder.logicViewportManager" (not a parameter) to "`logicKubriko`'s `ViewportManager`". The element types are `StateFlow<ImmutableList<Visible>>` for `logicVisibleActors` (what `ActorManager.visibleActorsWithinViewport` declares; `MiniMapSampler.sample` takes `List<*>`), `StateFlow<AngleRadians>` / `StateFlow<SceneOffset>` as `VolumetricRenderManager.worldRotation` / `ControlManager.cameraOffset` declare them.
- `IsometricGraphicsContent`: `MiniMap(logicKubriko = stateHolder.logicKubriko, logicVisibleActors = stateHolder.logicVisibleActors, worldRotation = stateHolder.volumetricRenderManager.worldRotation, cameraOffset = stateHolder.controlManager.cameraOffset, gridMap = gridMap)`.
- CLAUDE.md "State-holder wiring": "The composables `IsometricGraphicsContent` and `MiniMap` take the state holder as a parameter" → only `IsometricGraphicsContent` takes the holder; `MiniMap` takes the flows it reads.
- Sync cost: none — `ui/` and the state-holder wiring are Kubriko-only (Tesselar's `ui` module has long diverged: `ui/input/ControlOverlayManager.kt`, `ui/overlay/JoystickOverlay.kt`, `ui/minimap/*`).

## Behaviour
Same reads at the same times, same tree.

## Public API
None.

## Tests
The existing ones (none).

## Verify
`./gradlew :examples:demo-isometric-graphics:compileKotlinDesktop`

## Manual check
Isometric demo: the minimap scrolls and rotates with the camera, markers update while walking.
