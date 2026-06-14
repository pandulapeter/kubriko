<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# plugin-pointer-input internals

## Coordinate system
All positions delivered to `PointerInputAware` callbacks are in **screen pixels** (Compose `Offset`),
not scene units. The manager does not project to scene space; actors must do that themselves using
`ViewportManager` bounds if they need scene-space coordinates.

When `isActiveAboveViewport = false` (default), the modifier attaches to the viewport layer and
positions are relative to the viewport top-left. When `isActiveAboveViewport = true`, the modifier
attaches to the overlay layer and positions are adjusted by `rootOffset - viewportOffset` to stay
consistent with full-window coordinates.

## Event routing
`PointerInputManagerImpl` filters `ActorManager.allActors` into a `pointerInputAwareActors`
`StateFlow<List<PointerInputAware>>`. On every tick (`onUpdate`), if there are any pressed pointers
the manager calls `handleActivePointers` on **every** actor in registration order — there is no
spatial culling or hit-testing. The same applies to all other callbacks: all registered actors
receive every event regardless of position.

`pointersPressedSinceLastTick` is a one-tick latch (same idea as keyboard's): every press records its
id+position there. In `onUpdate` any latched id not already held is added back into the
`handleActivePointers` map at its press position, so a pointer tapped and released entirely between two
ticks (common at low/throttled frame rates) is still delivered for exactly one tick. The latch is
cleared at the end of every `onUpdate` and on focus loss. Discrete `onPointerPressed`/`onPointerReleased`
fire off-tick and were never affected; this only fixes the per-tick polling path. The merge allocates a
map only on the rare tap path.

## Gesture detection split
Raw press/release/move events come from a `pointerInput { awaitPointerEventScope }` loop.
Drag and zoom come from a second `gestureDetector` modifier chained via `.then()` using
`detectTransformGestures`. This means drag and zoom arrive asynchronously on the composition
coroutine, while raw pointer events arrive synchronously in the event loop.

## Multi-touch and platform differences
- `isMultiTouchEnabled = false` on **Desktop** (tracked JetBrains issue CMP-1609); pointer ID
  filtering drops any event whose `id.value != 0L`.
- **Android**, **iOS**, and **Web**: `isMultiTouchEnabled = true`; all pointer IDs are forwarded.
  `detectTransformGestures` correctly fires `onPointerZoom` from pinch gestures on all three.
- Scroll-to-zoom factor formula: Desktop `1f - scrollDelta.y * 0.05f`; Web `1f - scrollDelta.y * 0.005f`; iOS `1f - scrollDelta.y * 0.05f`.

## Cursor control (`tryToMoveHoveringPointer`)
Only functional on **Desktop** (uses `java.awt.Robot.mouseMove`). Skipped on Linux (Robot breaks
cursor behavior). No-op on Android, iOS, Web. The `densityMultiplier` (= `1 / density`) is
captured from `LocalDensity` in the manager's `Composable()` override and applied to convert
logical pixels to physical screen coordinates.

## Focus safety
On focus loss (`StateManager.isFocused = false`), `_pressedPointerPositions` is cleared to prevent
stuck-pointer state. New press events are guarded by `isFocused.value` checks inside the event loop.

## `isActiveAboveViewport` parameter
Determines whether input is captured from the full window (`processOverlayModifier`) or only when
the pointer is inside the viewport (`processModifier`). Both paths track their respective
`onGloballyPositioned` offset for the coordinate adjustment described above.
