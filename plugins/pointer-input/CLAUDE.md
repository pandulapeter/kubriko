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

The return value ("was the cursor actually moved, so expect one synthetic move event") is computed
synchronously inside `setPointerPosition` by comparing the target AWT coordinates against
`MouseInfo.getPointerInfo().location` before warping. It must NOT be derived by observing
`hoveringPointerPosition` around the warp: the synthetic move event only arrives through the AWT
queue later, so such a check just races the flow's background collector and returns timing noise —
which breaks the skip-one-event parity used by the relative-movement games (Wallbreaker,
Space Squadron).

## Cancellation resilience
Compose can cancel pointer input on the node at any time (on Android, `AndroidComposeView` does it
whenever the tool type or input source of a MotionEvent changes — a palm/finger reclassification
during a multi-finger gesture is enough). It signals this by synthesizing an event in which every
held pointer is reported as released with the up-transition already consumed, then immediately
re-delivers the still-down pointers as brand new presses.

That synthetic release closely resembles a real finger lift: it is recognized by the up-transition
being already consumed while the position and timestamp repeat the previous ones verbatim (the chained
`detectTransformGestures` also consumes releases, but only ones that carry an actual position change,
which is how an ordinary finger lift during a pinch stays distinguishable). Such a release is not
dispatched right away: the pointer stays in `_pressedPointerPositions` and
its id goes into `pointersPendingCancellation` with a short tick countdown. A pointer that comes
back is dropped from that map and continues as a regular `onPointerOffsetChanged`, so actors never
see a spurious press/release pair; a pointer that never returns (a genuine `ACTION_CANCEL`) is
released from `onUpdate` once the countdown expires. Unconsumed releases are dispatched immediately
as before, so normal taps keep their latency. The deferred release depends on ticks running: if the
`TickSource` is stopped during the grace window, the release lands on the first tick after it resumes.

## Focus safety
On focus loss (`StateManager.isFocused = false`), `_pressedPointerPositions`, `pendingPositionUpdates`,
the `pointersPressedSinceLastTick` latch and `pointersPendingCancellation` are all cleared. Before clearing, a synthetic
`onPointerReleased` is dispatched for every held pointer (at its last known position) — the platform
can steal an in-flight touch (e.g. dragging down the iOS status bar) without sending a release, so
actors that track pointer state across frames would otherwise be stuck with a ghost pointer. New press
events are guarded by `isFocused.value` checks inside the event loop.

## `isActiveAboveViewport` parameter
Determines whether input is captured from the full window (`processOverlayModifier`) or only when
the pointer is inside the viewport (`processModifier`). Both paths track their respective
`onGloballyPositioned` offset for the coordinate adjustment described above.

## Throttled position publishing and idle combines
A pointer's move-while-held events write into `pendingPositionUpdates` (a plain, in-place mutated
map) instead of `_pressedPointerPositions` directly; `onUpdate` flushes it into the persistent map
once per tick via a single `puttingAll`, so a high-frequency stream of move events pays one
structural-sharing map update per tick instead of one per raw event. `onPointerOffsetChanged` still
fires immediately per event — only the polled `pressedPointerPositions`/`handleActivePointers` path is
batched to tick cadence. Release events remove the id from `pendingPositionUpdates` too, so a
pointer released before the next flush can't be resurrected by a stale queued position.

`pressedPointerPositions` and `hoveringPointerPosition` derive from `SharingStarted.WhileSubscribed()`
combines, so they do no work while nothing is collecting them (idle-throttled ticks, or simply no UI
displaying pointer state). Each is wrapped in a local `SyncStateFlow` whose `.value` always recomputes
synchronously from the upstream flows' own `.value`s, so reads stay correct even while the underlying
combine is idle — this mirrors the engine's own `SyncStateFlow` (not reused directly: it's `internal`
to the `engine` module and not visible across the module boundary).
