# Split ActorManagerImpl into a batch processor, a culler with the draw caches, and the layer Composables

**Kind:** refactor  ·  **Severity:** high  ·  **Platforms:** all  ·  **Class:** Planned
**Artifact:** engine
**Challenged:** amended — the public `visibleActorsWithinViewport` / `activeDynamicActors` (and `allActors`) must stay readable from construction, so their backing `MutableStateFlow`s are created eagerly and handed to the `lateinit` culler instead of being created by it; demo-physics `CLAUDE.md` is located by its text, not line 48 (D22 edits that paragraph first).
**Rebased:** on 70de96c6 after the Now plans landed.
**Files:**
- `engine/src/commonMain/kotlin/com/pandulapeter/kubriko/manager/ActorManagerImpl.kt`
- `engine/src/commonMain/kotlin/com/pandulapeter/kubriko/manager/ActorBatchProcessor.kt` (new)
- `engine/src/commonMain/kotlin/com/pandulapeter/kubriko/manager/ActorCuller.kt` (new)
- `engine/src/commonMain/kotlin/com/pandulapeter/kubriko/manager/Layers.kt` (new)
- `engine/src/commonMain/kotlin/com/pandulapeter/kubriko/manager/IdentityContentEquals.kt` (new)
- `engine/src/commonMain/kotlin/com/pandulapeter/kubriko/manager/MetadataManagerImpl.kt` (KDoc reference only)
- `engine/CLAUDE.md`
- `examples/demo-physics/CLAUDE.md` (outside lane E: one reference, line 48)

E10 (this file's comments → KDoc) landed in 3ba97272 and E11 (the `MetadataManagerImpl._gameTime` KDoc) in
8c981458; everything below is quoted as it reads at 70de96c6. E53 builds on this plan (it injects the processor's
dispatcher).

## Problem

`ActorManagerImpl.kt` is 752 lines at 70de96c6 — past the ~500-line limit of `code-style` — and one class carries three
responsibilities that share almost no state:

1. **Batch processor** — the operation channel and its lifecycle: `operationChannel`, `isProcessingStarted`,
   `processorJob`, `isDisposing` (:82-86), `startProcessingOperations` (:312-330 with its KDoc, which hard-codes
   `scope.launch(Dispatchers.Default)` at :325), the teardown loop of `onDispose` (:332-346 with its KDoc),
   `processBatchStartingWith` (:348-364), `flattenActors` (:470-487), `processBatch` (:489-583),
   `publishDerivedActorLists` (:585-597), `runActorCallback` (:599-620), the five enqueuers (:622-651, with the
   ordering KDoc landed by E10) and `private sealed class Operation` (:747-751).
2. **Culler and draw caches** — the comparators (:87-95), `sortedVisibleActorsByLayer` /
   `sortedOverlayActorsByLayer`, the scratch buffers, the draw-cache snapshots and invalidation tracking (:123-167),
   `updateVisibleActorsWithinViewport` (:169-260), `encodeLayerIndex` (:262-263), `updateActiveDynamicActors`
   (:265-302) and the post-update part of `onUpdate` (:402-451). The viewport bounds test is written twice, once per
   cull:
   ```kotlin
   val leftBound = viewportCenter.x.raw - halfScaledWidth - edgeBuffer            // :180-183 and again :277-280
   val topBound = viewportCenter.y.raw - halfScaledHeight - edgeBuffer
   val rightBound = viewportCenter.x.raw + halfScaledWidth + edgeBuffer
   val bottomBound = viewportCenter.y.raw + halfScaledHeight + edgeBuffer
   ...
   aabb.left.raw <= rightBound &&                                                  // :195-198 and again :288-291
           aabb.top.raw <= bottomBound &&
           aabb.right.raw >= leftBound &&
           aabb.bottom.raw >= topBound
   ```
3. **Layer rendering** — `Composable(windowInsets)` (:653-664) and the private Composables `Layers` (:666-682, with
   its KDoc) and `Layer` (:684-745).

A test of the processor's ordering or of the cull cannot construct either without a whole Kubriko instance.

## Fix

Same package, verbatim moves (every KDoc and comment with its declaration), each new file with the MPL-2.0 header.

**`IdentityContentEquals.kt`** — `private fun <T> List<T>.contentEquals(other: List<T>)` (with its KDoc from E10)
(:454-468) becomes `internal` top-level, because both the processor (`publishDerivedActorLists`) and the culler use
it. No `contentEquals` extension on `List` exists in the stdlib or the package (rechecked at 70de96c6).

**`ActorBatchProcessor.kt`** — `internal class ActorBatchProcessor(private val shouldComposeLayers: Boolean, private
val log: (message: String, details: String?) -> Unit)`:
- owns `_allActors` and the three derived flows `dynamicActors`, `visibleActors`, `overlayActors` (with the KDoc
  "This and the two lists below are derived by the batch processor itself…"), exposed read-only as
  `allActors: StateFlow<ImmutableList<Actor>>`, `dynamicActors`, `visibleActors`, `overlayActors`;
- `fun start(kubrikoImpl: KubrikoImpl, scope: CoroutineScope)` = today's `startProcessingOperations` (same guard,
  same synchronous drain on the calling thread, then `scope.launch(Dispatchers.Default)`), storing `kubrikoImpl` for
  `onAdded(kubrikoImpl)` and `scope` for the rethrow `scope.launch { throw failure }`;
- `fun dispose()` = the body of today's `onDispose` (`isDisposing.store(true)`, cancel, `Disposable.dispose()` on
  every actor still in `_allActors`, each through `runActorCallback`);
- `fun add(actors: List<Actor>)`, `fun remove(actors: List<Actor>)`, `fun removeAll()` doing today's `trySend`, so
  `ActorManagerImpl`'s five overrides become one-liners (`if (actors.isEmpty()) return;
  batchProcessor.add(actors.toList())`) — the `isEmpty` check and the `toList()` copy stay on the caller's thread,
  where they are today;
- `processBatchStartingWith`, `processBatch`, `flattenActors`, `publishDerivedActorLists`, `runActorCallback`,
  `Operation` move unchanged; `log(message = …, details = …)` calls go through the constructor's `log`.
- `ActorManagerImpl` creates it once as
  `private val batchProcessor = ActorBatchProcessor(shouldComposeLayers) { message, details -> log(message = message, details = details) }`
  (`log` is `protected` on `Manager`; the lambda is allocated once per manager, never per tick or per batch), calls
  `batchProcessor.start(kubrikoImpl, scope)` from `startProcessingOperations()` (which `KubrikoImpl` calls) and
  `batchProcessor.dispose()` from `onDispose()`; `override val allActors = batchProcessor.allActors`.

**`ActorCuller.kt`** — `internal class ActorCuller(viewportManager, metadataManager, farAwayActorSleepMargin,
invisibleActorMinimumRefreshTimeInMillis, shouldComposeLayers, batchProcessor)` (or take the three derived flows
instead of the processor — narrower; recommended), constructed in `onInitialize` once the managers are known
(`private lateinit var culler`):
- writes `_visibleActorsWithinViewport` and `_activeDynamicActors`, but does **not** create them: they stay
  construction-time fields of `ActorManagerImpl` (as at 70de96c6, :77-80) and are passed into the culler's constructor.
  `override val visibleActorsWithinViewport = _visibleActorsWithinViewport.asStateFlow()` and the
  `activeDynamicActors` equivalent stay initialized when the manager is constructed — a consumer may read them (or
  `allActors`, which the eagerly constructed `ActorBatchProcessor` backs) before the manager is initialized, e.g. on an
  `ActorManager.newInstance()` not yet passed to `Kubriko.newInstance`; delegating to a `lateinit` culler would turn
  that read into an `UninitializedPropertyAccessException`. The culler owns both comparators, `sortedVisibleActorsByLayer`, `sortedOverlayActorsByLayer`, the scratch buffers, the draw-cache
  snapshots, the invalidation tracking, `encodeLayerIndex`, `updateVisibleActorsWithinViewport`,
  `updateActiveDynamicActors`;
- two entry points carved out of `onUpdate` with the statements in their current order:
  `fun refreshActiveDynamicActorsBeforeUpdate(shouldPutFarAwayActorsToSleep): Boolean` (:367-386, returning
  `didCullDynamicActorsBeforeUpdate`) and `fun refreshAfterUpdate(didCullDynamicActorsBeforeUpdate)` (:402-451);
- `sortedVisibleActorsByLayer` and `sortedOverlayActorsByLayer` become `internal var … private set` read by
  `Layers.kt`. They must be read through the culler on every draw (as today's field reads are), never captured into
  a local at composition time.

`ActorManagerImpl.onUpdate` keeps the update loop (:388-400) and `activeDynamicMirror` (it is the update loop's, not the cull's)
between the two culler calls; the `kubrikoImpl.isDisposedInternal` early returns stay exactly where they are.

**`Layers.kt`** — the private `Layers` becomes `internal @Composable fun Layers(…)` (file named after it, per
`code-style`), with `Layer` staying `private` in the same file; they take the `layerIndices` state, `gameTime`,
`kubrikoImpl.managers`, `viewportManager` and the culler as parameters. `Composable(windowInsets)` stays in
`ActorManagerImpl` (it is the `Manager` override) and keeps its `Box` and `processModifierInternal` fold unchanged;
`layerIndices` stays in `ActorManagerImpl` (it uses the protected `asStateFlowOnMainThread`).
Option: name the moved Composable `ActorLayers` instead of `Layers` (clearer at package level); recommended to keep
`Layers` so the move stays verbatim.

## Decision

Whether to unify the two copies of the bounds test while moving them:
- **(a) Keep both copies verbatim** — zero risk; the duplication stays.
- **(b) One private helper with the identical arithmetic** — keep each function's four bound computations as they are
  (the edge buffer differs), and share only the comparison as
  ```kotlin
  private fun AxisAlignedBoundingBox.touchesBounds(leftBound: Float, topBound: Float, rightBound: Float, bottomBound: Float) =
      left.raw <= rightBound && top.raw <= bottomBound && right.raw >= leftBound && bottom.raw >= topBound
  ```
  in `ActorCuller`, i.e. the same four `<=`/`>=` on the same operands. Bit-identical results, no allocation (four floats and a
  receiver). **Recommended**, in a second commit after the verbatim split.
- **(c) Reuse the existing internal `isWithinViewportBounds(scaledHalfViewportSize, viewportCenter, viewportEdgeBuffer)`**
  from `helpers/extensions/AxesAlignedBoundingBoxExtensions.kt` (:38) — it evaluates `center + (half + buffer)` instead of
  `center + half + buffer`, so float rounding can flip an actor sitting exactly on the edge, and it builds a
  `SceneSize`. Not recommended.

## Behaviour
Unchanged: same statements in the same order on the same threads. Specifically preserved: the published maps are
fresh `HashMap`s never mutated after publishing (the render thread reads them lock-free while a background
`TickSource` runs `onUpdate`); the derived lists are published before `_allActors`; callbacks run on the processor
coroutine, or on the thread calling `start()` for operations queued before it; enqueueing happens on the caller's
thread; no per-tick allocation.

## Public API
None. `ActorManagerImpl` and every new class are internal; the public `ActorManager` flows keep their types and
emission behaviour.

## Tests
- The existing ones, all of them unchanged and green: `HotPathAllocationTest` (tick budget), `ActorLifecycleChurnTest`
  with `KUBRIKO_STRESS=1`, the batch tests in `manager/` (`BatchCallbackPairingTest`, `DuplicateAdditionTest`,
  `GroupFlatteningTest`, `ActorCallbackFailureTest`, moved there by E03 in 8df3c00c), `ViewportContractTest`.
- New `manager/ActorBatchProcessorTest` (desktopTest): constructs an `ActorBatchProcessor` against a manual-tick
  Kubriko from `newTestKubriko` (the processor still needs a `KubrikoImpl` for `onAdded`), and pins: operations issued
  before `start` apply synchronously on the starting thread in issue order; an add+remove in one batch pairs
  `onAdded` with `onRemoved` and never publishes; `dispose()` calls `Disposable.dispose()` and not `onRemoved()`.
  (These already hold through `ActorManager`; the point is that the class is now testable in isolation.)
- New `manager/ActorCullerTest`: a culler fed a fixed visible list and a sized viewport publishes exactly the actors
  whose AABB touches the bounds, including one touching an edge exactly (pins (b)'s bit-identical comparison).

## Verify
`./gradlew :engine:compileKotlinDesktop :engine:compileAndroidMain :engine:compileKotlinIosSimulatorArm64 :engine:compileKotlinWasmJs :engine:desktopTest`
and `KUBRIKO_STRESS=1 ./gradlew :engine:desktopTest --tests "com.pandulapeter.kubriko.lifecycle.ActorLifecycleChurnTest"`.
Compare the declarations before/after with `git diff --color-moved=dimmed-zebra`.

Docs in the same commit: `engine/CLAUDE.md` Key Internal Files (the `ActorManagerImpl.kt` bullet becomes three:
`ActorManagerImpl.kt` — the Manager, update loop and `Composable`; `ActorBatchProcessor.kt` — batched add/remove via
`Channel<Operation>`; `ActorCuller.kt` — culling and the draw caches; `Layers.kt` — the per-layer Canvases), and the
names in "Rendering Pipeline" (`ActorManagerImpl.Composable` iterates `layerIndices`… → `Layers`), "Draw-Cache
Invalidation" and "Actor Batch Processing" (`processBatch` now in `ActorBatchProcessor`).
`MetadataManagerImpl._gameTime`'s KDoc reference to the layer Canvas (:31, "see [ActorManagerImpl]'s layer Canvas")
points at `[Layers]`.
`examples/demo-physics/CLAUDE.md` (the `DynamicChain` paragraph, line 48 at 70de96c6, already trimmed by D22 in
9f499ba2) "BFS flatten order in `ActorManagerImpl`" → "in `ActorBatchProcessor`".
Grep the repo (and `../Tesselar/issues`, informational only) for `ActorManagerImpl.` member references afterwards.

## Manual check
Run the Showcase on desktop and web: a scene with many actors (Performance demo), the layered demos (Shader demo,
Isometric graphics) and a game with frequent spawning (Space Squadron) render and update as before.
