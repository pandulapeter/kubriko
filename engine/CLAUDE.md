<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# engine

The sealed-interface core of Kubriko: wires Managers, Actors, the tick loop, and the Compose rendering pipeline together.

## Key Internal Files

- `KubrikoImpl.kt` — sole concrete `Kubriko`; holds `managers: List<Manager>`, drives `onTick`
- `InternalViewport.kt` — actual Composable; runs `withFrameMillis` loop, feeds size/focus into Managers
- `ActorManagerImpl.kt` — batched add/remove via `Channel<Operation>`; owns draw-cache rebuilding
- `SyncStateFlow.kt` — computes `.value` synchronously, preventing 1-frame lag on combined viewport bounds
- `PlatformUtils.kt` (+ actuals) — `PlatformFocusEffect`, `getPlatform()`, `getDefaultFocusDebounce()`. Android debounce = 350 ms; Desktop = 0 ms
- `AxisAlignedBoundingBox.kt` — packs four 16-bit quantized coords into one `Long`. `QUANT_SHIFT = 4` means 16-unit precision; caps usable scene coords at ~±524 k scene units

## Initialization Order

`KubrikoImpl.init {}` initializes built-in Managers and TickSource immediately at construction. Custom Managers wait for `TickSource.start()` — with `viewportFrames()` this happens inside `InternalViewport`'s `LaunchedEffect`. Consequence: `Manager.scope` and `manager<T>()` delegates are unavailable until the viewport is composed.

`manager<T>()` delegates resolve at `initializeInternal` time (before `onInitialize` is called). `autoInitializingLazy {}` triggers after `onInitialize` returns.

## Tick Dispatch

`KubrikoImpl.onTick(delta)` iterates `managers` in registration order. `ActorManagerImpl.onUpdate` calls `update()` on every actor in `activeDynamicActors.value` — plain indexed `forEach` over a pre-filtered `ImmutableList`. No allocation in this hot path.

`FrameRate` throttling (`HALF`/`QUARTER`) is applied in `InternalViewport` by skipping `tickSource.tick()` calls based on a counter modulo `frameRate.factor`.

## Rendering Pipeline

`InternalViewport` uses two nested `Box` composables:
1. **Outer Box**: applies `processOverlayModifier` from all Managers
2. **Inner Box**: aspect-ratio layout, feeds size to `ViewportManagerImpl`, calls `Composable` for every Manager

`ActorManagerImpl.Composable` iterates `layerIndices`, creates a `Canvas` per layer with `processModifier` applied, then inside each `onDraw` applies `transformViewport` (translate + scale around camera) and iterates the pre-sorted `sortedVisibleActorsByLayer[layerIndex]`. Overlay actors are drawn without the viewport transform. `gameTime.value` is read solely to invalidate the Canvas on every tick.

## Draw-Cache Invalidation

Three caches rebuilt only on change (reference equality `!==`):
- `sortedVisibleActorsByLayer` — rebuilt on `visibleActors` change, viewport size change, or `invisibleActorMinimumRefreshTimeInMillis` elapsed (default `0` ⇒ every frame)
- `sortedOverlayActorsByLayer` — rebuilt only on `overlayActors` change
- `activeDynamicActors` — uses larger edge buffer (one viewport half-dimension) to avoid sleep-on-enter jitter

### Culling allocation model

With the default `invisibleActorMinimumRefreshTimeInMillis = 0` the visibility / active-dynamic cull runs every frame, so it is written to allocate as little as possible:
- Culling filters into reusable, tick-thread-private scratch buffers (`visibleScratch`, `dynamicScratch`) rather than `List.filter`.
- The public `visibleActorsWithinViewport` / `activeDynamicActors` StateFlows are re-published only when the culled set changed (identity-based `contentEquals` against the currently published list). Observable behavior is unchanged — equal sets never emitted before either — but the per-frame `ImmutableList` allocation is skipped in the steady state.
- `sortedVisibleActorsByLayer` / `sortedOverlayActorsByLayer` still build a **fresh** `HashMap` (one map, not two — no `mapValues`) on every rebuild and are never mutated after publishing. This is deliberate: a background `TickSource` runs `onUpdate` on `Dispatchers.Default` while the Canvas draws on the UI thread, so the render thread relies on the "published structures are immutable" invariant to read them lock-free. **Do not** switch these to reused/in-place-mutated buffers. Per-layer lists are always re-sorted (drawingOrder may change every frame, e.g. Y-sorted depth).

## Actor Batch Processing

`add`/`remove`/`removeAll` send `Operation` instances to an `UNLIMITED` `Channel`, processed on `Dispatchers.Default`. Processor drains full channel each cycle, then: flattens `Group` actors (BFS, cycle-guarded), evicts earlier `Unique` instances, assigns UUIDs to unnamed `Identifiable` actors, calls `onAdded` on main thread before updating `_allActors`. Removal switches from `List.contains` to `HashSet` above 10 items.

## Manager Composable Extension Points

Any Manager can override (all run every frame — keep allocation-free):
- `Composable(windowInsets)` — injects Compose UI inside the inner Box
- `processModifier(modifier, layerIndex, gameTime)` — injects a `Modifier` on each layer Canvas
- `processOverlayModifier(modifier)` — injects a `Modifier` on the outer overlay Box

## Gotchas

- `drawingOrder` comparator adds `+ 0f` to normalize `-0.0f` → `+0.0f`; without this TimSort throws `IllegalArgumentException`
- `AxisAlignedBoundingBox` quantizes to multiples of 16 — positions between steps appear at the next multiple in culling (conservative, intentional)
- `Manager.scope` is `KubrikoImpl` cast to `CoroutineScope` (`SupervisorJob + Dispatchers.Default`); child failures don't cancel the engine
- Default Managers are prepended; user-supplied same-type Manager wins via last-wins deduplication
- `SyncStateFlow.value` bypasses coroutine-backed `StateFlow` — safe only if underlying state is thread-safe
