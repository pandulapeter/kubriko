# Position the isometric joystick from the `windowInsets` the host passes instead of `WindowInsets.safeDrawing`.

**Kind:** bug  ·  **Severity:** low  ·  **Platforms:** android, ios (devices with side cutouts)  ·  **Class:** Now
**Artifact:** unpublished (examples)
**Files:** `examples/demo-isometric-graphics/src/commonMain/kotlin/com/pandulapeter/kubriko/demoIsometricGraphics/implementation/ui/IsometricGraphicsContent.kt`

## Problem
`IsometricGraphicsContent` takes `windowInsets: WindowInsets = WindowInsets.safeDrawing` and uses it for the info panel (`.windowInsetsPadding(windowInsets)`, :236), but the joystick ignores it (:119–121):
```kotlin
val safeDrawingInsets = WindowInsets.safeDrawing
val leftInsetPx = safeDrawingInsets.getLeft(density, layoutDirection).toFloat()
val bottomInsetPx = safeDrawingInsets.getBottom(density).toFloat()
```
`leftInsetPx`/`bottomInsetPx` place the joystick (`defaultJoystickPosition`) and, through the `SideEffect`, `ControlOverlayManager.isWithinJoystickRegion`'s hit area. Outside fullscreen the Showcase passes `WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Right)` (wide UI, where its side menu already absorbs the left inset) or `.only(Bottom + Horizontal)` (`app/shared/…/ShowcaseContent.kt:109-113`), so on a landscape phone with a left cutout the joystick is pushed right by an inset the host already handled. Tesselar's `ui/overlay/JoystickOverlay.kt` reads `WindowInsets.safeDrawing` too, but it is always fullscreen with no host insets, so it is correct there and needs no change.

## Fix
Replace those three lines with `val leftInsetPx = windowInsets.getLeft(density, layoutDirection).toFloat()` and `val bottomInsetPx = windowInsets.getBottom(density).toFloat()`; drop the `safeDrawing` import if unused (it is still the parameter default — keep it then).

## Behaviour
In fullscreen and standalone use nothing changes (the host passes `safeDrawing`). In the windowed Showcase the joystick and its touch region move left by the left inset the side menu already covers.

## Public API
None.

## Tests
None (composition-only).

## Verify
`./gradlew :examples:demo-isometric-graphics:compileKotlinDesktop` and `./gradlew :examples:demo-isometric-graphics:compileAndroidMain`

## Manual check
Android phone with a cutout, landscape, cutout on the left, Showcase not fullscreen: the joystick sits 16 dp from the content's left edge and responds to touches there; in fullscreen it still clears the cutout.
