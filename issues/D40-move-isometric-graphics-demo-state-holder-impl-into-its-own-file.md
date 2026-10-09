# Move `IsometricGraphicsDemoStateHolderImpl` out of `IsometricGraphicsDemoStateHolder.kt` into `IsometricGraphicsDemoStateHolderImpl.kt`.

**Kind:** refactor  ·  **Severity:** low  ·  **Platforms:** all  ·  **Class:** Now
**Artifact:** unpublished (examples)
**Files:** `examples/demo-isometric-graphics/src/commonMain/kotlin/com/pandulapeter/kubriko/demoIsometricGraphics/implementation/IsometricGraphicsDemoStateHolder.kt`, `examples/demo-isometric-graphics/src/commonMain/kotlin/com/pandulapeter/kubriko/demoIsometricGraphics/implementation/IsometricGraphicsDemoStateHolderImpl.kt` (new)

## Problem
`examples/demo-isometric-graphics/src/commonMain/kotlin/com/pandulapeter/kubriko/demoIsometricGraphics/implementation/IsometricGraphicsDemoStateHolder.kt` holds `sealed interface IsometricGraphicsDemoStateHolder : StateHolder` (:35, with its image/string resource gate) and `internal class IsometricGraphicsDemoStateHolderImpl(` (:59–132, with its KDoc at :50–58) plus `private const val LOG_TAG = "IsometricGraphics"` and `private const val LOG_TAG_LOGIC = "IsometricGraphicsLogic"` (:134–135). One top-level type per file.

## Fix
- Create `examples/demo-isometric-graphics/src/commonMain/kotlin/com/pandulapeter/kubriko/demoIsometricGraphics/implementation/IsometricGraphicsDemoStateHolderImpl.kt` (license header, same package) and move verbatim: the Impl's KDoc, the class (including its `// region` / `// endregion` markers) and both `LOG_TAG` constants.
- The old file keeps the interface and companion; split the imports.
- Grep the repo for `IsometricGraphicsDemoStateHolder.kt`; fix references. Sync cost: none — `ui/` and the state-holder wiring are Kubriko-only (Tesselar's `ui` module has long diverged: `ui/input/ControlOverlayManager.kt`, `ui/overlay/JoystickOverlay.kt`, `ui/minimap/*`).

## Behaviour
Verbatim move within one package.

## Public API
None (examples are unpublished).

## Tests
The existing ones (none).

## Verify
`./gradlew :examples:demo-isometric-graphics:compileKotlinDesktop` and `./gradlew :app:desktop:compileKotlin`

## Manual check
none
