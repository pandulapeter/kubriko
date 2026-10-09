# Replace the seven mutable layout `var`s the UI pushes into the isometric `ControlOverlayManager` with one immutable joystick layout and a single geometry function.

**Kind:** refactor  ·  **Severity:** low  ·  **Platforms:** all  ·  **Class:** Planned
**Artifact:** unpublished (examples)
**Rebased:** on 70de96c6 after the Now plans landed.
**Files:** `examples/demo-isometric-graphics/src/commonMain/kotlin/com/pandulapeter/kubriko/demoIsometricGraphics/implementation/ui/ControlOverlayManager.kt`, `examples/demo-isometric-graphics/src/commonMain/kotlin/com/pandulapeter/kubriko/demoIsometricGraphics/implementation/ui/IsometricGraphicsContent.kt`, `examples/demo-isometric-graphics/src/commonMain/kotlin/com/pandulapeter/kubriko/demoIsometricGraphics/implementation/ui/JoystickLayout.kt` (new), `examples/demo-isometric-graphics/src/desktopTest/kotlin/com/pandulapeter/kubriko/demoIsometricGraphics/implementation/ui/JoystickLayoutTest.kt` (new), `examples/demo-isometric-graphics/CLAUDE.md`

## Problem
`IsometricGraphicsContent` writes seven public `var`s of `ControlOverlayManager` (`isJoystickEnabled`, `joystickMaxRadiusPx`, `joystickVisualRadiusPx`, `joystickTriggerRadiusPx`, `paddingPx`, `leftInsetPx`, `bottomInsetPx` — `ControlOverlayManager.kt:58-70`) from a `SideEffect` (`IsometricGraphicsContent.kt:154-162`; the inputs are computed at :114–120, the insets from the `windowInsets` the host passes since a1a65980). The pointer callbacks read them from the input thread with no single consistent snapshot, and the joystick centre is computed twice: `defaultJoystickPosition` in the Composable (:122–127) and `centerX/centerY` in `isWithinJoystickRegion` (`ControlOverlayManager.kt:172-183`). An input-handling Manager also lives in `ui/`.

Tesselar (`../Tesselar/ui/src/commonMain/kotlin/com/pandulapeter/tesselar/ui/input/ControlOverlayManager.kt`) has already moved past this: the geometry and finger tracking live in a tested `JoystickTracker` (`ui/input/JoystickTracker.kt`, `JoystickTrackerTest.kt`), rendered by `ui/overlay/JoystickOverlay.kt`. So this copy is behind, not ahead — the sync cost is porting, not diverging.

## Fix
Options:
1. **Recommended:** `internal data class JoystickLayout(val isEnabled: Boolean, val visualRadiusPx: Float, val maxRadiusPx: Float, val triggerRadiusPx: Float, val paddingPx: Float, val leftInsetPx: Float, val bottomInsetPx: Float)` with pure `fun center(viewportHeight: Float): Offset` and `fun isWithinTriggerRegion(point: Offset, viewportHeight: Float): Boolean` (the existing clamp-toward-corner maths verbatim). The Composable builds it with `remember(…)` and publishes it through one `@Volatile var joystickLayout` (or a `StateFlow`) on the Manager; both the default position and the hit test call `center`.
2. Port Tesselar's `JoystickTracker` wholesale — larger, brings behaviour (springs, multi-finger rules) the demo does not have.
Moving `ControlOverlayManager` to an `input/` package (as Tesselar did) is optional; do it in the same plan only if the user wants package parity.

## Decision
Option 1 or 2 (recommended 1); move to `input/` yes/no (recommended no for now).

## Behaviour
Same joystick position and hit region; one consistent snapshot per pointer event.

## Public API
None.

## Tests
`JoystickLayoutTest`: `center`, a point at the visual centre, one inside the trigger radius, one just outside, and one in the bottom-left inset strip (clamped → inside).

## Verify
`./gradlew :examples:demo-isometric-graphics:desktopTest`

## Manual check
Touch device: the joystick engages on the same area as before, including the inset strip left of/below it.
