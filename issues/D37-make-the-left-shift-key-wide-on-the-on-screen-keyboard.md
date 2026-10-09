# Make the left Shift key wide on the input test's on-screen keyboard.

**Kind:** bug  ·  **Severity:** low  ·  **Platforms:** all  ·  **Class:** Now
**Artifact:** unpublished (examples)
**Files:** `examples/test-input/src/commonMain/kotlin/com/pandulapeter/kubriko/testInput/implementation/ui/Keyboard.kt`, `examples/test-input/src/desktopTest/kotlin/com/pandulapeter/kubriko/testInput/implementation/ui/KeyboardTest.kt` (new)

## Problem
`examples/test-input/src/commonMain/kotlin/com/pandulapeter/kubriko/testInput/implementation/ui/Keyboard.kt:147` (after D36 inside `Key.keySize()`):
```kotlin
Key.Escape, Key.Backspace, Key.Tab, Key.Enter, Key.CapsLock, Key.ShiftRight, Key.ShiftRight -> Size.WIDE
```
lists `Key.ShiftRight` twice, so `Key.ShiftLeft` falls to `else -> Size.NORMAL` and renders 30 dp wide while its right twin is 60 dp.

## Fix
Change the first `Key.ShiftRight` to `Key.ShiftLeft`.

## Behaviour
Intentional visual fix: the left Shift becomes 60 dp wide (`Size.WIDE`), matching right Shift; the bottom letter row gets 30 dp wider.

## Public API
None.

## Tests
Add `examples/test-input/src/desktopTest/kotlin/com/pandulapeter/kubriko/testInput/implementation/ui/KeyboardTest.kt` (license header, same package):
```kotlin
class KeyboardTest {

    @Test
    fun bothShiftKeysAreWide() {
        assertEquals(Size.WIDE, Key.ShiftLeft.keySize())
        assertEquals(Size.WIDE, Key.ShiftRight.keySize())
    }
}
```
(`androidx.compose.ui.input.key.Key` is a plain value class; no Skia needed.) It fails before the fix.

## Verify
`./gradlew :examples:test-input:desktopTest`

## Manual check
Input test: the left Shift key on the on-screen keyboard is as wide as the right one.
