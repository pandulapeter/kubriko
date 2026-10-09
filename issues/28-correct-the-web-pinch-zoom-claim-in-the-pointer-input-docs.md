# Correct the web pinch-zoom claim in the pointer-input docs

**Kind:** docs  ·  **Severity:** low  ·  **Platforms:** Web (Wasm)
**Challenged:** amended — the trackpad-pinch claim is now conditional on the manual check, since Compose web only calls `preventDefault()` on a wheel event a handler consumed and Kubriko's scroll handler consumes nothing, so a Ctrl+wheel pinch may zoom the browser page as well as the game.
**Files:** `plugins/pointer-input/CLAUDE.md`, `plugins/pointer-input/README.md`, `plugins/pointer-input/src/webMain/kotlin/com/pandulapeter/kubriko/pointerInput/implementation/PlatformExtensions.web.kt` (only in the alternative below)

Documentation only (the alternative removes a comment in `plugin-pointer-input`). Run after plan 25, which edits the same
`CLAUDE.md` section.

## Problem

`plugins/pointer-input/CLAUDE.md`, "Multi-touch and platform differences":

> **Android**, **iOS**, and **Web**: `isMultiTouchEnabled = true`; all pointer IDs are forwarded.
> `detectTransformGestures` correctly fires `onPointerZoom` from pinch gestures on all three.

The web source says the opposite, at HEAD:

```kotlin
// PlatformExtensions.web.kt
// TODO: https://youtrack.jetbrains.com/issue/CMP-6957/Web.-detectTransformGestures-doesnt-catch-zoom-and-rotation-gestures
internal actual val isMultiTouchEnabled = true
```

and the README promises "Built-in support for drag and zoom (pinch/scroll) gestures" without a platform caveat. A game
developer relying on pinch-to-zoom on mobile browsers gets nothing and no warning. (Trackpad pinch on desktop browsers
arrives as a Ctrl+wheel event and goes through the scroll path, which plan 25 makes well-behaved.)

## Fix

1. `CLAUDE.md`: split the bullet — Android and iOS: `detectTransformGestures` fires `onPointerZoom` from pinches; Web:
   multi-touch pointers are forwarded, but touch pinch does not reach `onPointerZoom` (CMP-6957, the TODO in
   `PlatformExtensions.web.kt`); desktop-browser trackpad pinches arrive as Ctrl+wheel through the scroll path.
2. `README.md` Platform Limitations: add "**Web**: touch pinch gestures are not reported as zoom yet (Compose
   Multiplatform issue CMP-6957); mouse-wheel zoom works." Add "and trackpad pinch" (and keep the CLAUDE.md sentence
   about Ctrl+wheel) only if the manual check shows a desktop-browser trackpad pinch zooms the game **without** also
   zooming the browser page: Compose web (`ComposeWindowInternal.web.kt`, `onWheelEvent`) calls `preventDefault()`
   only when a handler consumed the event, and the `onPointerEvent(PointerEventType.Scroll)` handler in
   `PlatformExtensions.web.kt` consumes nothing. If the page zooms too, write instead that trackpad pinch also zooms
   the browser page, and leave fixing it (consuming the scroll change) to a separate plan.

Before writing, re-check CMP-6957 against Compose 1.12.1 on a phone browser (manual check below); if pinch does work
there now, instead delete the TODO in `PlatformExtensions.web.kt` and keep the docs as they are.

## Tests

None (documentation).

## Manual check

Showcase web build on a phone (Chrome Android, Safari iOS): pinch in `demo-isometric-graphics` and note whether the
view zooms. That result picks between the fix and the alternative above. Then, in Chrome and Safari on a Mac
trackpad, pinch in the same demo and note whether the browser page zooms as well; that picks the README wording in
step 2.
