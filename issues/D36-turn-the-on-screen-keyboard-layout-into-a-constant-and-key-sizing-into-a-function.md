# Turn the on-screen keyboard's layout into a constant and its key sizing into an internal function.

**Kind:** refactor  ·  **Severity:** low  ·  **Platforms:** all  ·  **Class:** Now
**Artifact:** unpublished (examples)
**Files:** `examples/test-input/src/commonMain/kotlin/com/pandulapeter/kubriko/testInput/implementation/ui/Keyboard.kt`

## Problem
`Keyboard` (`examples/test-input/src/commonMain/kotlin/com/pandulapeter/kubriko/testInput/implementation/ui/Keyboard.kt:32-142`) spells out six `KeyboardRow(keyWrappers = listOf(Key.…, …).map { it.toWrapper(activeKeys) })` calls, rebuilding six key lists on every recomposition (each key press), and the size decision is buried in `private fun Key.toWrapper(activeKeys: Set<Key>) = KeyWrapper(key = this, size = when (this) { … }, isPressed = this in activeKeys)` (:144–152), where no test can reach it (D37 needs that).

## Fix
- Add `private val KeyboardLayout: List<List<Key>> = listOf(listOf(Key.Escape, Key.F1, …), …)` holding the six rows in the same order with the same keys (trailing commas).
- Add `internal fun Key.keySize() = when (this) { … }` with the `when` verbatim (including the duplicated `Key.ShiftRight` — D37 fixes it as its own commit), and make `private enum class Size` `internal` (its return type). `toWrapper` becomes `KeyWrapper(key = this, size = keySize(), isPressed = this in activeKeys)`.
- The Column content becomes `KeyboardLayout.forEach { keys -> KeyboardRow(keyWrappers = keys.map { it.toWrapper(activeKeys) }) }`.
- Leave the modifier placement as is (`modifier` on the inner Column, outer `Box(Modifier.fillMaxSize())`).

## Behaviour
Same rows, order, wrappers and sizes; the `forEach` emits the same six `KeyboardRow`s into the Column.

## Public API
None.

## Tests
The existing ones (D37 adds one).

## Verify
`./gradlew :examples:test-input:compileKotlinDesktop`

## Manual check
Input test: the on-screen keyboard looks the same and highlights pressed keys.
