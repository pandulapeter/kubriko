# Release held pointers on focus loss from the main thread, where the pointer maps are otherwise mutated

**Challenged:** sound

**Kind:** bug  ·  **Severity:** medium  ·  **Platforms:** all
**Artifact:** `plugin-pointer-input` (internal change only; the same `onPointerReleased` calls, on the thread every other pointer callback uses)
**Files:** `plugins/pointer-input/src/commonMain/kotlin/com/pandulapeter/kubriko/pointerInput/PointerInputManagerImpl.kt`, `plugins/pointer-input/CLAUDE.md`

## Problem

```kotlin
override fun onInitialize(kubriko: Kubriko) {
    stateManager.isFocused
        .filterNot { it }
        .onEach {
            val heldPointers = _pressedPointerPositions.value
            _pressedPointerPositions.update { persistentMapOf() }
            pendingPositionUpdates.clear()
            pointersPressedSinceLastTick.clear()
            pointersPendingCancellation.clear()
            heldPointers.forEach { (id, position) ->
                pointerInputAwareActors.value.forEach { it.onPointerReleased(id, position) }
            }
        }
        .launchIn(scope)
}
```

`scope` runs on `Dispatchers.Default`. `pendingPositionUpdates`, `pointersPressedSinceLastTick` and
`pointersPendingCancellation` are plain `HashMap`s that the Compose pointer-input loop writes on the main thread and
that `onUpdate` (on main with the default tick source) iterates and mutates — `pointersPendingCancellation` through
an explicit iterator. Focus is typically lost *during* a touch (the iOS status-bar pull-down this handler was written
for; a notification shade on Android; an Alt-Tab while dragging): the move and cancellation events of that same
gesture are being processed on main at that moment. Clearing the maps concurrently gives
`ConcurrentModificationException` (uncaught in the Kubriko scope: **crash on Android and iOS**; on Kotlin/Native the
iterator also throws when its map is modified) or a corrupted map that resurrects a pointer in the next flush. The
synthetic `onPointerReleased` calls also reach Actors on a background thread while their press/move callbacks run on
main.

## Fix

Collect on the main thread:

```kotlin
    .launchIn(scope + Dispatchers.Main)
```

(`import kotlinx.coroutines.Dispatchers` and `kotlinx.coroutines.plus`.) `Dispatchers.Main` exists on every target
(`asStateFlowOnMainThread`, used two lines above for `pointerInputAwareActors`, depends on it). A flag consumed in
`onUpdate` is not an alternative: the default tick source stops ticking while unfocused, which would hold the
synthetic releases back until the player returns.

Same out-of-scope note as the keyboard plan: with a coroutine-based `TickSource`, `onUpdate` itself runs on `Default`.

## Tests

None: needs Compose to deliver pointer events on main concurrently with a focus change set by `KubrikoViewport`
through an engine-internal call.

## Manual check

iOS device: in a pointer-driven game (Annoyed Penguins' slingshot, the Wallbreaker paddle), start a drag and pull
down the status bar / Control Center mid-drag, twenty times: no crash, and the game never keeps acting on the lost
touch. Android: the same with the notification shade. A breakpoint in the focus-loss block shows the main thread.

Update `plugins/pointer-input/CLAUDE.md` → Focus safety: say the clearing and the synthetic releases run on the main
thread.
