# Extract the per-change `when` of `PointerInputManagerImpl.pointerInputHandling` into a private function

**Kind:** refactor  ·  **Severity:** low  ·  **Platforms:** all  ·  **Class:** Now
**Artifact:** plugin-pointer-input
**Files:**
- `plugins/pointer-input/src/commonMain/kotlin/com/pandulapeter/kubriko/pointerInput/PointerInputManagerImpl.kt`

## Problem
At 2480325f `pointerInputHandling` (:254–337) nests `Modifier.pointerInput(Unit)` → `awaitPointerEventScope` →
`while (true)` → `if (isInitialized.value)` → `event.changes.forEach { change ->` → a six-branch `when` (:264–320),
seven levels deep, before the `.gestureDetector(...)` chain:

```kotlin
event.changes.forEach { change ->
    if (!isMultiTouchEnabled && change.id.value != 0L) return@forEach
    val id = change.id
    val wasPressed = _pressedPointerPositions.value.containsKey(id)
    val isPressed = change.pressed
    when {
        !wasPressed && isPressed -> {
```

## Fix
Add, next to `releasePointer`:

```kotlin
private fun handlePointerChange(change: PointerInputChange, eventType: PointerEventType) {
    if (!isMultiTouchEnabled && change.id.value != 0L) return
    val id = change.id
    val wasPressed = _pressedPointerPositions.value.containsKey(id)
    val isPressed = change.pressed
    when {
        // the six branches moved verbatim, with `event.type` replaced by `eventType`
    }
}
```

and reduce the loop to `event.changes.forEach { change -> handlePointerChange(change, event.type) }`. Every name
the branches use (`stateManager`, `pointersPressedSinceLastTick`, `pointersPendingCancellation`,
`pendingPositionUpdates`, `mouseId`, `_hoveringPointerPosition`, `pointerInputAwareActors`,
`CANCELLATION_GRACE_PERIOD_IN_TICKS`, `isCancellation`, `releasePointer`) is a member of the class, so nothing needs
the `AwaitPointerEventScope` receiver. Import `PointerInputChange` if not yet imported.

## Behaviour
Unchanged: the function is called synchronously, in order, from the same coroutine, for the same changes; `return`
from it is exactly the old `return@forEach`; `event.type` is read once per change as before (it is a property of the
same event). No allocation is added (a non-capturing direct call; `forEach` over a `List` is inline already).

## Public API
None.

## Tests
The existing ones (pointer-input has none; see the gamepad/keyboard test plans for the seam pattern).

## Verify
`./gradlew :plugins:pointer-input:compileKotlinDesktop :plugins:pointer-input:compileAndroidMain`

## Manual check
On the desktop Showcase, a pointer demo (e.g. `demo-physics` or the pointer test example): press, drag, release and
hover still behave as before; on Android, a two-finger pinch still zooms and lifting one finger does not release the
other.
