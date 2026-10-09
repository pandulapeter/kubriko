# Split `IsometricGraphicsContent` into named private parts and correct its joystick touch-target comment.

**Kind:** refactor  ·  **Severity:** medium  ·  **Platforms:** all  ·  **Class:** Now
**Artifact:** unpublished (examples)
**Files:** `examples/demo-isometric-graphics/src/commonMain/kotlin/com/pandulapeter/kubriko/demoIsometricGraphics/implementation/ui/IsometricGraphicsContent.kt`
**Challenged:** amended — `Joystick` reads `LocalDensity.current` for its verbatim `with(density)` layer blocks; `RenderState` is the nested `VolumetricRenderManager.RenderState`.

## Problem
`IsometricGraphicsContent` (`examples/demo-isometric-graphics/src/commonMain/kotlin/com/pandulapeter/kubriko/demoIsometricGraphics/implementation/ui/IsometricGraphicsContent.kt:74-257`) is one ~180-line body mixing: texture polling (`val image = remember { mutableStateOf<ImageBitmap?>(null) }` + `LaunchedEffect(isReadyToRender.value) { while (image.value == null) { … resolveTexture("map") … delay(50L.milliseconds) } }`, :94–103), joystick geometry and animations (:112–163), the grid `Modifier.drawBehind { … drawIsometricGrid(…) }` (:167–189) and two inline joystick Boxes (:193–230). `64.dp` appears twice (:114 and :197) and `20.dp`/`40.dp` describe the knob. The comment `// Touch target is 1.5x the visual size` (:116) contradicts `val joystickTriggerRadiusPx = joystickVisualRadiusPx * 2f`.

## Fix
- `@Composable private fun rememberMapTexture(textureResolver: TextureResolver, isReadyToRender: Boolean): State<ImageBitmap?>` — the `remember` + `LaunchedEffect` verbatim, keyed the same.
- `private fun Modifier.isometricGrid(renderState: State<VolumetricRenderManager.RenderState>, gridLinesPath: Path, isoMatrix: Matrix, gridMap: GridMap?, stroke: Stroke, size: State<Size>, lineCache: IsometricGridLineCache) = drawBehind { … }` with the block verbatim. **Pass the `State`s, not their values**, so `renderState.value` and `size.value` are still read in the draw phase — reading them at the call site would recompose the whole Content every tick. Keep the "Tick-cadence snapshot …" comment with `renderState`.
- `@Composable private fun Joystick(origin: () -> Offset, knobOffset: () -> Offset, alpha: Float)` = the outer `Box { Box(…base…); Box(…knob…) }` verbatim, with `origin()` / `knobOffset()` read **inside the `graphicsLayer` blocks** (pass `{ animatedJoystickOrigin }` / `{ animatedKnobOffset }`) so the per-frame animation stays a layer-only invalidation as today; `alpha` is already read in composition (`background` colour). Keep its outer `Box {}`. The two `graphicsLayer` blocks use `with(density) { … }` today, so `Joystick` reads `val density = LocalDensity.current` itself (same value the parent read) and keeps those blocks verbatim apart from the radius constants below.
- Hoist `private val JoystickBaseRadius = 64.dp`, `JoystickKnobRadius = 20.dp` and use `JoystickBaseRadius * 2` / `JoystickKnobRadius * 2` for the `size`s (values unchanged: 128 dp / 40 dp); keep `56.dp` travel and `16.dp` padding as they are or hoist them likewise.
- Comment: "Touch target is 1.5x the visual size" → "Touch target is twice the visual radius".
- Keep `if (JOYSTICK_ENABLED)` as its own standalone `if` around the `Joystick(...)` call.
- Do not change the `SideEffect` that pushes the layout into `ControlOverlayManager` or the inset reads (D46 and planned D55 handle those).
- Sync cost: none — `ui/` and the state-holder wiring are Kubriko-only (Tesselar's `ui` module has long diverged: `ui/input/ControlOverlayManager.kt`, `ui/overlay/JoystickOverlay.kt`, `ui/minimap/*`).

## Behaviour
Verbatim moves, no wrapper added or dropped, and every state that was read in the draw/layer phase still is.

## Public API
None.

## Tests
The existing ones (none).

## Verify
`./gradlew :examples:demo-isometric-graphics:compileKotlinDesktop` and `./gradlew :examples:demo-isometric-graphics:compileKotlinWasmJs`

## Manual check
Isometric demo on a touch device: joystick base/knob appear bottom-left at half opacity, follow the finger with the spring, grid steps with the world at a throttled frame rate; Layout Inspector / recomposition counts show Content does not recompose per frame while dragging.
