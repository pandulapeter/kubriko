# Turn ActorManagerImpl's declaration comments into KDoc and drop the fixed-bug history

**Kind:** docs  ·  **Severity:** low  ·  **Platforms:** all  ·  **Class:** Now
**Artifact:** engine
**Files:**
- `engine/src/commonMain/kotlin/com/pandulapeter/kubriko/manager/ActorManagerImpl.kt`

Must land before E50 (the split of this file), which quotes the code as it reads after this plan.

## Problem

`code-style`: a comment that documents a declaration is KDoc, even on private members; `//` is for statements inside a
function body; no history of fixed bugs. `ActorManagerImpl.kt` at 2480325f documents eleven member declarations with
`//` blocks, one of them stale, and keeps two pieces of fixed-performance-bug history inside function bodies.

## Fix

Each replacement below keeps the meaning; the KDoc goes where the `//` block was. Line numbers are at 2480325f.

1. `:96-97`, above `private val layerIndices by autoInitializingLazy {` — the "either of these" is stale (only one
   declaration follows; the overlay list is now derived in `processBatch`):
   ```kotlin
   /** The distinct layer indices, sorted with `null` first. A headless instance never composes a layer, so it doesn't follow every change of its actor list. */
   ```
   (wrap to the file's 120-column style).
2. `:121`, above `private var sortedVisibleActorsByLayer` — `// Pre-grouped and pre-sorted draw caches — rebuilt in
   onUpdate() when inputs change, read in onDraw()` →
   `/** This and [sortedOverlayActorsByLayer] are the draw caches, grouped by layer and sorted by drawing order: rebuilt in [onUpdate] when their inputs change, read by each layer's Canvas. */`
3. `:125-127`, above `private val visibleScratch` →
   `/** This and [dynamicScratch] are reusable cull buffers. Only ever touched on the tick thread inside [onUpdate] and never published, so they add no cross-thread sharing and let culling run without allocating an intermediate list. */`
4. `:131-133`, above `private val activeDynamicMirror` → the same text as KDoc:
   `/** Mirror of the published active Dynamic list for the update loop: iterating the persistent list directly would allocate a trie iterator every frame, while the ArrayList mirror is indexable in O(1). Refilled only when the published list reference changes; tick-thread-private. */`
5. `:137-141`, above `private var drawCacheActors` → KDoc starting "This and the two arrays below are the draw-cache
   reuse snapshots:" followed by the existing text unchanged from "the published list reference identifies the culled
   set…" to "…Tick-thread-private."
6. `:146` `// Visibility cache invalidation tracking` above `lastVisibleActors` →
   `/** This and the two below record when, and against which inputs, the visible set was last culled. */`
7. `:151` `// Dynamic actor cache invalidation tracking` above `lastDynamicActors` →
   `/** This and the two below record when, and against which inputs, the active Dynamic set was last culled. */`
8. `:156` `// Overlay cache invalidation tracking` above `lastOverlayActors` →
   `/** The overlay list [sortedOverlayActorsByLayer] was last built from. */`
9. `:253` above `private fun Int?.encodeLayerIndex()` →
   `/** Long-encoded layerIndex snapshot value; Long.MIN_VALUE marks null (no Int maps to it). */`
10. `:445-449` above `private fun <T> List<T>.contentEquals(other: List<T>)` → the same five lines as KDoc, with
    "and lets us detect" reworded to "and tells when".
11. `:613-617` above `override fun add(vararg actors: Actor)` →
    ```kotlin
    /**
     * This and the other enqueuing functions below send on the caller's own thread rather than from a coroutine, which
     * is what makes operations reach the processor in the order they were issued: a coroutine each would leave that
     * order to the dispatcher, and a removal arriving before the addition it undoes would find nothing to remove. The
     * unbounded channel is what allows it - enqueueing never suspends. Only the ordering is the caller's; the
     * processing stays on the processor.
     */
    ```
12. History inside `updateVisibleActorsWithinViewport` (`:175-178`) becomes:
    ```kotlin
    // Cull into the reusable scratch buffer rather than a filtered copy. Iterate the source via its iterator:
    // `actors` is a persistent vector whose indexed get() is a trie walk, so a for-each is cheaper than indexing it.
    ```
13. History inside `processBatch` (`:482-484`) becomes:
    ```kotlin
    // One mutable working copy plus a membership index for the whole batch, so a batch of individual calls stays
    // linear and bulk removal never scans the whole list per removed actor.
    ```

Leave the statement-level comments that already explain a non-obvious "why" (`:88-90` comparator, `:101`, `:194-196`,
`:201`, `:204-213`, `:273-274`, `:358-359`, `:370`, `:394`, `:398-399`, `:410`, `:418`, `:430`) and the existing
KDoc blocks as they are. No code changes.

## Behaviour
Unchanged (comments only).

## Public API
None (`ActorManagerImpl` is internal).

## Tests
The existing ones.

## Verify
`./gradlew :engine:compileKotlinDesktop`

## Manual check
none
