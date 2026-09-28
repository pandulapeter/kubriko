# Fix Timer's edge cases and keep repeating timers on schedule

**Challenged:** amended — Annoyed Penguins' `GameplayManager` fades the level out with `alpha = remaining / 600` and skips the update only while `remainingTimeInMilliseconds == 600` (the value the old timer reset to after firing); with the overshoot carried, the tick that ends a level would snap the viewport back to ~98 % opacity for the ~100 ms before the scene is cleared, so a companion change to `GameplayManager.onUpdate` (a lane F file) is added; the manual check named Space Squadron, which uses no `Timer` — the only users are that `GameplayManager` and Blocky's turning timer.

**Decision needed:** a repeating `Timer` drops the overshoot of every period, so it drifts late (100 ms at 16 ms ticks fires 89 times in 10 s instead of 100). Carry the overshoot into the next period? — recommended: yes, carried modulo the period (at most one `onDone` per `update`, so a long stall does not cause a burst). Also: a zero/negative-duration one-shot timer should fire on its first `update`.

**Kind:** bug  ·  **Severity:** medium  ·  **Platforms:** all  ·  **Artifact:** `engine`
**Files:** `engine/src/commonMain/kotlin/com/pandulapeter/kubriko/helpers/Timer.kt`, `engine/src/commonTest/kotlin/com/pandulapeter/kubriko/helpers/TimerTest.kt` (new), `CLAUDE.md` (`Timer` section). Companion change in lane F's file `examples/game-annoyed-penguins/src/commonMain/kotlin/com/pandulapeter/kubriko/gameAnnoyedPenguins/implementation/managers/GameplayManager.kt` (also edited by plan 95, in `onInitialize`/`loadScene` — a different hunk); the orchestrator decides which lane carries it.

## Problem

`Timer` (0008d027) does its work in the setter of `remainingTimeInMilliseconds`:

```kotlin
var remainingTimeInMilliseconds = timeInMilliseconds
    private set(value) {
        field = if (value > 0) {
            value
        } else {
            onDone()
            if (shouldTriggerMultipleTimes) timeInMilliseconds else 0
        }
    }

fun update(deltaTimeInMilliseconds: Int) {
    if (remainingTimeInMilliseconds > 0 || shouldTriggerMultipleTimes) {
        remainingTimeInMilliseconds -= deltaTimeInMilliseconds
    }
}
```

Verified with a probe at 0008d027:
- `Timer(0) { … }` and `Timer(-5) { … }` (one-shot) never fire: the initializer bypasses the setter, and `update` skips because `remaining > 0` is false.
- A repeating 100 ms timer updated with 16 ms deltas for 10 s fires **89** times: each period resets to the full 100 ms and discards the 12 ms it overshot. A 1000 ms delta on a 100 ms repeating timer fires once and restarts at 100.
- `update(-50)` on a 100 ms timer leaves 150 ms remaining (negative deltas extend it). Negative deltas can reach it from a custom `TickSource` or a clock hiccup.

## Fix

Rewrite `update` (keep the public surface: constructor, `timeInMilliseconds`, `shouldTriggerMultipleTimes`, `onDone`, `remainingTimeInMilliseconds` with a private setter):

```kotlin
private var hasFinished = false

fun update(deltaTimeInMilliseconds: Int) {
    if (hasFinished) return
    remainingTimeInMilliseconds -= deltaTimeInMilliseconds.coerceAtLeast(0)
    if (remainingTimeInMilliseconds > 0) return
    if (shouldTriggerMultipleTimes && timeInMilliseconds > 0) {
        val overshoot = -remainingTimeInMilliseconds % timeInMilliseconds
        remainingTimeInMilliseconds = timeInMilliseconds - overshoot
    } else if (shouldTriggerMultipleTimes) {
        remainingTimeInMilliseconds = timeInMilliseconds
    } else {
        remainingTimeInMilliseconds = 0
        hasFinished = true
    }
    onDone()
}
```

with the setter reduced to a plain `private set`. `onDone()` runs after the state is consistent, so an `onDone` that reads `remainingTimeInMilliseconds` sees the next period. No allocation.

**Alternative (overshoot):** keep resetting to the full period (today's rate) and only fix the zero-duration and negative-delta cases.

KDoc: class — "A repeating timer keeps its schedule: time past the deadline counts toward the next period, and it fires at most once per `update`. A one-shot timer with a zero or negative duration fires on its first `update`. Negative deltas are ignored." Root `CLAUDE.md` → *Timer*: one line with the same rules.

### In-repo callers

- `Blocky.kt` (`turningTimer`, 650 ms, repeating): turns exactly every 650 ms instead of every ~656 ms at 16 ms ticks — its loop shape changes imperceptibly; no change needed.
- `GameplayManager.kt` (`gameEndTimer`, 600 ms, repeating) — **needs the companion change.** Its `onUpdate` is:

  ```kotlin
  if (collectedStarCount.value == totalStarCount.value && totalStarCount.value != 0) {
      gameEndTimer.update(deltaTimeInMilliseconds)
      if (gameEndTimer.remainingTimeInMilliseconds.toFloat() != GAME_END_DELAY) {
          _gameViewportAlpha.update { max(0f, gameEndTimer.remainingTimeInMilliseconds / GAME_END_DELAY) }
      }
  }
  ```

  The `!= GAME_END_DELAY` test is what keeps the faded-out alpha after `onDone` (which sets `_currentLevel` to `null`) while the old timer sat at exactly 600. With the overshoot carried, the timer sits at `600 - overshoot` after firing, so that same tick would set the alpha back to ~0.98, and the ticks until `loadScene(null)` resets the star counts on `scope` would keep fading from there. Gate on the level instead, which `onDone` clears synchronously:

  ```kotlin
  if (_currentLevel.value != null && collectedStarCount.value == totalStarCount.value && totalStarCount.value != 0) {
      gameEndTimer.update(deltaTimeInMilliseconds)
      if (_currentLevel.value != null) {
          _gameViewportAlpha.update { max(0f, gameEndTimer.remainingTimeInMilliseconds / GAME_END_DELAY) }
      }
  }
  ```

  (the next level's fade then starts at most one tick below full opacity, as it already did). Nothing in Tesselar uses `Timer`.

## Tests

`TimerTest` (commonTest):
- `zeroDurationOneShotFiresOnFirstUpdate` — `Timer(0)`, one `update(16)` → 1 fire; five more → still 1.
- `negativeDurationOneShotFiresOnce` — `Timer(-5)` same.
- `repeatingTimerKeepsItsRate` — `Timer(100, true)`, 625 × `update(16)` (10 000 ms) → exactly 100 fires.
- `longStallFiresOnceAndKeepsPhase` — `Timer(100, true)`, `update(1050)` → 1 fire, `remainingTimeInMilliseconds == 50`.
- `negativeDeltaIsIgnored` — `Timer(100)`, `update(-50)` → `remainingTimeInMilliseconds == 100`.
- `oneShotFiresOnce` — `Timer(100)`, 20 × `update(16)` → 1 fire, `remainingTimeInMilliseconds == 0`.

## Manual check

Desktop Showcase → Annoyed Penguins: collect the last star of a level — the level fades out smoothly to the menu with no flash of the finished level before it disappears. Blocky's Journey: Blocky still walks its loop.
