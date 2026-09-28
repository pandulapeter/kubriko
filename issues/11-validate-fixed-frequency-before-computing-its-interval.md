# Validate the fixed-frequency tick rate before computing its interval

**Challenged:** sound

**Kind:** bug  ·  **Severity:** low  ·  **Platforms:** all  ·  **Artifact:** `engine`
**Files:** `engine/src/commonMain/kotlin/com/pandulapeter/kubriko/helpers/TickSource.kt`, `engine/src/commonTest/kotlin/com/pandulapeter/kubriko/helpers/TickSourceFactoryTest.kt` (new)

Apply after plan 10 (same file).

## Problem

`FixedFrequencyTickSource` (0008d027 ~261-269) declares its property before its `init` check, and Kotlin runs initializers in declaration order:

```kotlin
internal class FixedFrequencyTickSource(
    ticksPerSecond: Int,
) : TickSource() {
    private val targetInterval = (1_000_000_000L / ticksPerSecond).nanoseconds
    private var job: Job? = null

    init {
        require(ticksPerSecond > 0) { "ticksPerSecond must be greater than 0." }
    }
```

`TickSource.fixedFrequency(0)` therefore throws `ArithmeticException: / by zero` instead of the intended `IllegalArgumentException` (verified with a probe at 0008d027); a negative value produces a negative interval that the `require` then rejects, so only `0` is affected. `fixedFrequency(Int.MAX_VALUE)` passes and yields a 0 ns interval (a busy loop capped only by `delay(1)`), which is harmless.

## Fix

Move the `init { require(...) }` block above `targetInterval` (or compute `targetInterval` inside `init` after the check). No behaviour change for valid input.

## Tests

`TickSourceFactoryTest` (commonTest):
- `fixedFrequencyRejectsZero` — `assertFailsWith<IllegalArgumentException> { TickSource.fixedFrequency(0) }`, and the message is `"ticksPerSecond must be greater than 0."`.
- `fixedFrequencyRejectsNegative` — same for `-1`.
- `fixedRateRejectsZero` — `assertFailsWith<IllegalArgumentException> { TickSource.fixedRate(0L) }` (already correct; guards the pattern).

## Manual check

None.
