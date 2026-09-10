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
- `PlatformUtils.kt` (+ actuals) — `PlatformFocusEffect`, `PlatformFrameRateHint`, `getPlatform()`, `getDefaultFocusDebounce()`. Android debounce = 350 ms; Desktop = 0 ms
- `AxisAlignedBoundingBox.kt` — packs four 16-bit quantized coords into one `Long`. `QUANT_SHIFT = 4` means 16-unit precision; caps usable scene coords at ~±524 k scene units

## Initialization Order

`KubrikoImpl.init {}` initializes built-in Managers and TickSource immediately at construction. Custom Managers wait for `TickSource.start()` — with `viewportFrames()` this happens inside `InternalViewport`'s `LaunchedEffect`. Consequence: `Manager.scope` and `manager<T>()` delegates are unavailable until the viewport is composed.

`manager<T>()` delegates resolve at `initializeInternal` time (before `onInitialize` is called). `autoInitializingLazy {}` triggers after `onInitialize` returns.

## Tick Dispatch

`KubrikoImpl.onTick(delta)` iterates the managers in registration order via `managersForTick: Array<Manager>` (indexed loop; `List.forEach` would allocate an iterator per tick). `ActorManagerImpl.onUpdate` calls `update()` on every actor in the active Dynamic set — iterated through `activeDynamicMirror`, a tick-thread-private `ArrayList` refilled only when the published `activeDynamicActors` reference changes, because indexing the persistent list directly is a trie walk and iterating it allocates. No allocation in this hot path.

Update-rate throttling is applied in `InternalViewport` against `ViewportManager.targetFrameRate` (a runtime `StateFlow<TargetFrameRate>`). `DisplayDefault` ticks every display frame; `Limit(fps)` runs a subtract-interval time accumulator (`phaseInMilliseconds += frameDelta`; emit and `-= 1000f/fps` when it reaches the interval, keeping the remainder so non-integer-multiple refresh rates don't drift — resetting to zero would turn a 60 fps target on a 90 Hz panel into 45 fps), reaching the interval with half a display frame of tolerance so the tick lands on the display frame nearest the deadline rather than the first one past it (demanding a full interval postpones every near-miss by a whole frame and quantizes the achieved rate down to a fraction of the target: a 60 fps target on a 60 Hz panel yields 40); `DisplayDivider(divisor)` emits every `divisor`-th display frame (the old frame-divider behavior, tied to the panel's refresh rate). The emitted delta is always real time elapsed since the last tick, so logic stays time-correct. The loop holds four captured primitives (`lastFrameTime`, `lastProcessedFrameTime`, `phaseInMilliseconds`, `displayFramesSinceTick`) and allocates nothing per frame. The outer loop itself avoids waking at vsync when there is nothing to do: while gated off (unsized viewport, unfocused window, a stopped or disposed `TickSource`, or a non-viewport one) it suspends on the gate instead of polling `withFrameMillis`. The stopped case reads `TickSource.isRunningInternal`, an internal `StateFlow` mirror of the `protected` `isRunning` flag — without it a `tickSource.stop()` or a `kubriko.dispose()` under a still-mounted viewport kept requesting and processing every display frame to feed an `emitTick` that returns immediately. `InternalViewport` also drives `PlatformFrameRateHint(targetFrameRate)`, which on Android steps the physical panel refresh rate down to match — but only for `Limit` (an absolute rate the panel can settle on): it requests the slowest display mode at the current resolution whose rate still covers the target (some OEM panels ignore the softer `preferredRefreshRate` hint and stay in their highest mode), since a panel slower than the target would cap the achieved rate at the panel's own - a 90 fps target on a panel stepping 120, 80, 60 takes the 120 Hz mode and ticks three frames out of four. The float hint remains the fallback when the supported modes are unknown. `DisplayDivider` and `DisplayDefault` release both levers, since a divider counts real vsync pulses and lowering the panel would compound the reduction. No-op on the other platforms. Alongside it, `PlatformMaximumDisplayRefreshRateEffect` publishes the panel's ceiling into `MetadataManager.maximumDisplayRefreshRate` (Android: the fastest supported mode at the current resolution, kept current through a `DisplayManager.DisplayListener`; iOS: `UIScreen.maximumFramesPerSecond`; desktop: the primary screen's AWT display mode; web: `null`, since browsers only let a page measure it).

## Rendering Pipeline

`InternalViewport` uses two nested `Box` composables:
1. **Outer Box**: applies `processOverlayModifier` from all Managers
2. **Inner Box**: aspect-ratio layout, feeds size to `ViewportManagerImpl`, calls `Composable` for every Manager

`ActorManagerImpl.Composable` iterates `layerIndices`, creates a `Canvas` per layer with `processModifier` applied, then inside each `onDraw` applies `transformViewport` (translate + scale around camera) and iterates the pre-sorted `sortedVisibleActorsByLayer[layerIndex]`. Overlay actors are drawn without the viewport transform. `gameTime.value` is read solely to invalidate the Canvas on every tick. `gameTime` itself is `MetadataManagerImpl.gameTime`, a `mutableLongStateOf` written directly in `onUpdate` — a primitive Compose snapshot state, not a `collectAsState()` read of the public `totalRuntimeInMilliseconds` StateFlow, which stays around only as public API.

## Draw-Cache Invalidation

Three caches rebuilt only on change (reference equality `!==`):
- `sortedVisibleActorsByLayer` — rebuilt on `visibleActors` change, viewport size change, or `invisibleActorMinimumRefreshTimeInMillis` elapsed (default `100` ms; `0` reverts to every frame)
- `sortedOverlayActorsByLayer` — rebuilt only on `overlayActors` change
- `activeDynamicActors` — uses larger edge buffer (one viewport half-dimension) to avoid sleep-on-enter jitter

### Culling allocation model

The visibility / active-dynamic cull scan is throttled by `invisibleActorMinimumRefreshTimeInMillis` (default `100` ms, `0` reverts to every frame); correctness depends on the consumer's `viewportEdgeBuffer` outrunning camera-speed × this interval, since camera movement alone never forces an out-of-schedule re-cull. Whatever the interval, the scan is written to allocate as little as possible:
- Culling filters into reusable, tick-thread-private scratch buffers (`visibleScratch`, `dynamicScratch`) rather than `List.filter`.
- The public `visibleActorsWithinViewport` / `activeDynamicActors` StateFlows are re-published only when the culled set changed (identity-based `contentEquals` against the currently published list). Observable behavior is unchanged — equal sets never emitted before either — but the per-frame `ImmutableList` allocation is skipped in the steady state.
- `sortedVisibleActorsByLayer` / `sortedOverlayActorsByLayer` still build a **fresh** `HashMap` (one map, not two — no `mapValues`) on every rebuild and are never mutated after publishing. This is deliberate: a background `TickSource` runs `onUpdate` on `Dispatchers.Default` while the Canvas draws on the UI thread, so the render thread relies on the "published structures are immutable" invariant to read them lock-free. **Do not** switch these to reused/in-place-mutated buffers.
- Both sorted-by-layer maps are skipped entirely when `shouldComposeLayers` is false: `Composable()` already returns early on a headless instance, so nothing reads them. The culling itself still runs and `visibleActorsWithinViewport` / `activeDynamicActors` are published exactly as before — a headless instance is for logic and culling, and only the private render structures are gated.
- The `sortedVisibleActorsByLayer` rebuild is **skipped** when nothing changed: the published list reference identifies the culled set, and reusable primitive snapshots (`drawCacheDrawingOrders: FloatArray`, `drawCacheLayerIndices: LongArray`, null layer encoded as `Long.MIN_VALUE`) capture each actor's `drawingOrder`/`layerIndex` at the last rebuild. If the set, every drawingOrder, and every layerIndex are unchanged, the previously published map is still correct and the frame pays one read-only scan instead of HashMap + ArrayList + sort (the steady state for static scenes and menus). Any change — including per-frame drawingOrder mutation, e.g. Y-sorted depth — triggers a full rebuild with a fresh map, preserving the lock-free invariant above.

## Actor Batch Processing

`add`/`remove`/`removeAll` send `Operation` instances to an `UNLIMITED` `Channel`, processed on `Dispatchers.Default`. Processor drains full channel each cycle, then: flattens `Group` actors (BFS, cycle-guarded), evicts earlier `Unique` instances, assigns UUIDs to unnamed `Identifiable` actors, calls `onAdded` before updating `_allActors`, then `Disposable.dispose()` and `onRemoved` after.

**All three callbacks run on the processor's own `Dispatchers.Default` coroutine, not the main thread.** An actor's `onAdded`/`onRemoved`/`dispose` must dispatch anything main-thread-confined itself; the public KDoc on `Actor` and `ActorManager` says so too, and must keep saying so if the threading is ever revisited.

The whole batch runs against **one** mutable `ArrayList` working copy plus a `HashSet` membership index, and publishes a single `toImmutableList()` snapshot at the end — and only when something actually changed. Rebuilding the full list per operation made a batch of individual `add`/`remove` calls quadratic, and testing membership with `List.contains` made bulk removal O(removals × actors). The unique-replacement scan is skipped outright when the batch adds no `Unique` actors. The published list is never mutated, so old snapshots handed to consumers stay valid.

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
