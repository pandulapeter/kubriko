# Rename the isometric state holder's `textureManager` to `textureResolver` and make `ControlOverlayManager`'s joystick backing flows `val`.

**Kind:** refactor  ·  **Severity:** low  ·  **Platforms:** all  ·  **Class:** Now
**Artifact:** unpublished (examples)
**Files:** `examples/demo-isometric-graphics/src/commonMain/kotlin/com/pandulapeter/kubriko/demoIsometricGraphics/implementation/IsometricGraphicsDemoStateHolderImpl.kt` (after D40), `examples/demo-isometric-graphics/src/commonMain/kotlin/com/pandulapeter/kubriko/demoIsometricGraphics/implementation/ui/IsometricGraphicsContent.kt`, `examples/demo-isometric-graphics/src/commonMain/kotlin/com/pandulapeter/kubriko/demoIsometricGraphics/implementation/ui/ControlOverlayManager.kt`

## Problem
- `val textureManager = TextureResolver()` (originally `IsometricGraphicsDemoStateHolder.kt:71`, registered at :84, read at `IsometricGraphicsContent.kt:98` as `stateHolder.textureManager.resolveTexture("map")`) is named after a type it is not; `LogicManager` already calls the same Manager `textureResolver` (`private val textureResolver by manager<TextureResolver>()`).
- `ControlOverlayManager.kt:60, :62, :71`: `private var _joystickOrigin = MutableStateFlow<Offset?>(null)`, `private var _joystickDirection = MutableStateFlow<AngleRadians?>(null)`, `private var _joystickSpeedFactor = MutableStateFlow(0f)` — never reassigned (only `.value` is written).

## Fix
Rename the property to `textureResolver` (both uses; grep `textureManager` repo-wide — no other hits at 2480325f) and change the three `var`s to `val`. Sync cost: none — `ui/` and the state-holder wiring are Kubriko-only (Tesselar's `ui` module has long diverged: `ui/input/ControlOverlayManager.kt`, `ui/overlay/JoystickOverlay.kt`, `ui/minimap/*`).

## Behaviour
No logic change.

## Public API
None.

## Tests
The existing ones (none).

## Verify
`./gradlew :examples:demo-isometric-graphics:compileKotlinDesktop`

## Manual check
none
