# Move the isometric mini map's `MiniMapSampler` and `MiniMapBuffer` out of `MiniMap.kt` into files of their own.

**Kind:** refactor  ·  **Severity:** low  ·  **Platforms:** all  ·  **Class:** Now
**Artifact:** unpublished (examples)
**Files:** `examples/demo-isometric-graphics/src/commonMain/kotlin/com/pandulapeter/kubriko/demoIsometricGraphics/implementation/ui/MiniMap.kt`, `examples/demo-isometric-graphics/src/commonMain/kotlin/com/pandulapeter/kubriko/demoIsometricGraphics/implementation/ui/MiniMapBuffer.kt` (new), `examples/demo-isometric-graphics/src/commonMain/kotlin/com/pandulapeter/kubriko/demoIsometricGraphics/implementation/ui/MiniMapSampler.kt` (new)

## Problem
`examples/demo-isometric-graphics/src/commonMain/kotlin/com/pandulapeter/kubriko/demoIsometricGraphics/implementation/ui/MiniMap.kt` (307 lines) holds the `MiniMap` Composable plus two pure, allocation-free classes: `private class MiniMapBuffer` (:174–214) and `private class MiniMapSampler` (:221–307). They are a self-contained sampling algorithm hidden in a Composable's file, out of reach of a test.

## Fix
- `MiniMapBuffer.kt`: `private class MiniMapBuffer` verbatim → `internal class MiniMapBuffer`, with its preceding `//` comment block (:170–173) and its `private companion object`.
- `MiniMapSampler.kt`: `private class MiniMapSampler` verbatim → `internal class MiniMapSampler`, with its comment block (:216–220), including `quantize` and its `private companion object`.
- License headers, package `…implementation.ui`, imports split (`Color`, `toArgb`, `cos`, `sin`, `round`, `deg`, `rad`, `MiniMapMarker`, `RenderableCuboidHolder`, `PlanarCuboidRenderer` move with the sampler; drop the unused ones from `MiniMap.kt`). Check the package has no other `MiniMapBuffer`/`MiniMapSampler` (it does not). Comments stay `//` here — D44 converts them to KDoc.
- Sync cost: none — `ui/` and the state-holder wiring are Kubriko-only (Tesselar's `ui` module has long diverged: `ui/input/ControlOverlayManager.kt`, `ui/overlay/JoystickOverlay.kt`, `ui/minimap/*`).

## Behaviour
Verbatim move; only visibility widens from private to internal.

## Public API
None.

## Tests
The existing ones. (A sampler test would need `RenderableCuboidHolder` fixtures; not part of this move.)

## Verify
`./gradlew :examples:demo-isometric-graphics:compileKotlinDesktop`

## Manual check
none
