# Keep the examples' shader and camera time continuous over long sessions

**Kind:** bug (platform edge case)  ·  **Severity:** low  ·  **Platforms:** all
**Challenged:** sound
**Files:** `examples/shared/src/commonMain/kotlin/com/pandulapeter/kubriko/shared/ShaderTime.kt` (new),
`examples/shared/src/desktopTest/kotlin/com/pandulapeter/kubriko/shared/ShaderTimeTest.kt` (new),
`examples/demo-shader-animations/src/commonMain/kotlin/com/pandulapeter/kubriko/demoShaderAnimations/implementation/shaders/TimeDrivenShader.kt`,
`examples/game-wallbreaker/src/commonMain/kotlin/com/pandulapeter/kubriko/gameWallbreaker/implementation/actors/FogShader.kt`,
`examples/game-annoyed-penguins/src/commonMain/kotlin/com/pandulapeter/kubriko/gameAnnoyedPenguins/implementation/actors/FogShader.kt`,
`examples/game-space-squadron/src/commonMain/kotlin/com/pandulapeter/kubriko/gameSpaceSquadron/implementation/actors/GalaxyShader.kt`,
`examples/demo-performance/src/commonMain/kotlin/com/pandulapeter/kubriko/demoPerformance/implementation/actors/Camera.kt`,
`examples/demo-shader-animations/CLAUDE.md`

Ships in no published artifact (all of `examples/` is part of the Showcase).

## Problem

Two ways the examples' animation clocks break over time.

**1. The shader animations demo snaps back every 100 s.** `TimeDrivenShader.update`:

```kotlin
time = (metadataManager.activeRuntimeInMilliseconds.value % 100000L) / 1000f
```

None of the five shaders is periodic in `time` with a 100 s period, and their speeds are user-adjustable
(`speed` sliders 0..10, 0..8, 0..100), so no fixed wrap could be seamless: `GradientShader` uses
`sin(time * speed + …)`; `CloudShader` translates its noise by `time * speed` (`uv -= q - time`); `EtherShader` rotates
by `t * 0.4`/`t * 0.3` and offsets `q = p * 2.0 + t`; `NoodleShader` moves `p.z -= time * speed` through a rotation of
`p.z * .1`; `WarpShader` uses `sin(time * .0032)`, `cos(time * .005)` and `fract(time * 0.002)` on `(time - 2.0) * speed`.
Every one of them visibly jumps once per 100 s of active runtime — after under two minutes in the demo.

**2. Float millisecond accumulators lose precision.** Four actors accumulate the tick delta into a `Float`:

```kotlin
private var time = 0f
override fun update(deltaTimeInMilliseconds: Int) {
    time += deltaTimeInMilliseconds
    shaderState = shaderState.copy(time = time / 1000f)
}
```

in `game-wallbreaker` `FogShader`, `game-annoyed-penguins` `FogShader`, `game-space-squadron` `GalaxyShader`, and
`acc += deltaTimeInMilliseconds` in `demo-performance` `Camera` (`(acc / 5000f).rad` drives the camera's circle).
Probe with Java `float`: at 2^24 ms (**4.7 h**) the ulp is 2, so a 17 ms tick already advances by 16; at 2^26 ms
(18.6 h) both 16 and 17 advance by 16 (ulp 8); at 2^28 ms (**74.6 h**) a 16 ms tick advances by 0 and the animation
**freezes** (17 ms jumps by 32). These shaders tick whenever their example is shown, so a Showcase left open on one
(a kiosk, a demo screen) gets there.

## Fix

**Shader time** — one helper shared by all four shaders, in the `examples/shared` module every example already depends
on (`api(projects.examples.shared)`):

```kotlin
// examples/shared/src/commonMain/kotlin/com/pandulapeter/kubriko/shared/ShaderTime.kt
/**
 * Converts a shader clock in milliseconds to the seconds passed to a `time` uniform, wrapping once an hour: ...
 */
fun shaderTimeInSeconds(timeInMilliseconds: Long) = (timeInMilliseconds % SHADER_TIME_WRAP_IN_MILLISECONDS) / 1000f

private const val SHADER_TIME_WRAP_IN_MILLISECONDS = 3_600_000L
```

The KDoc says why it wraps: a `Float` in seconds is precise to ~0.25 ms up to an hour (ulp of 3600f is 2.4e-4), and the
shaders are not periodic, so the wrap is a visible jump once an hour instead of a precision loss that grows without bound.
(`examples/shared` is not published, but its public functions follow `code-style`'s KDoc rule like any other module.)

- `TimeDrivenShader.update`: `time = shaderTimeInSeconds(metadataManager.activeRuntimeInMilliseconds.value)`.
- The three `FogShader`/`GalaxyShader` actors: replace `private var time = 0f` with
  `private var timeInMilliseconds = 0L`, then `timeInMilliseconds += deltaTimeInMilliseconds` and
  `shaderState = shaderState.copy(time = shaderTimeInSeconds(timeInMilliseconds))`. (A `Long` never overflows here.)
- `demo-performance` `Camera`: its angle *is* periodic (`acc / 5000f` radians repeats every 2π × 5000 ms ≈ 31.4 s), so
  wrap seamlessly there instead: `acc = (acc + deltaTimeInMilliseconds) % CAMERA_PERIOD_IN_MILLISECONDS` with
  `private const val CAMERA_PERIOD_IN_MILLISECONDS = 2f * PI.toFloat() * 5000f` in its companion (`kotlin.math.PI`),
  keeping `acc` a `Float` that now stays below ~31 416.

Options for the wrap length: 1 h (recommended — a jump once an hour of continuous viewing, precision still sub-millisecond);
2^14 s ≈ 4.5 h (ulp ~1 ms, rarer jump); no wrap with a `Long` clock (no jump, but the precision loss of the seconds value
returns after a few days, freezing animations at ~270 000 s where the ulp is 16 ms). Not a public API question — any is
fine; prefer 1 h.

`examples/demo-shader-animations/CLAUDE.md`: the `Dynamic` contract paragraph's
"`MetadataManager.activeRuntimeInMilliseconds % 100000 / 1000f`" becomes "`shaderTimeInSeconds(MetadataManager.activeRuntimeInMilliseconds)`
(examples/shared; wraps once an hour)". The game modules' CLAUDE.md files only call these shaders "time-driven"; no change.

## Tests

New `ShaderTimeTest` in `examples/shared/src/desktopTest/kotlin/com/pandulapeter/kubriko/shared/` (the source set
exists — `LoadingDismissalTest`, `SceneEditorConnectionTest`):

- `noJumpAtTheOldHundredSecondWrap`: `shaderTimeInSeconds(100_016L) - shaderTimeInSeconds(100_000L)` is 0.016 within 1e-4
  (and `shaderTimeInSeconds(100_000L)` is 100, not 0).
- `wrapsOnceAnHour`: `shaderTimeInSeconds(3_600_000L)` is 0 and `shaderTimeInSeconds(3_600_016L)` is 0.016 within 1e-4.
- `sixteenMillisecondStepsStayDistinctUpToTheWrap`: `shaderTimeInSeconds(3_599_984L) - shaderTimeInSeconds(3_599_968L)`
  is 0.016 within 1e-3.
- `staysContinuousAfterDaysOfRuntime`: for `t = 300L * 3_600_000L + 1_234_567L` (300 h), the 16 ms step is 0.016 within
  1e-3 (fails for a plain `t / 1000f`).

The `Camera` wrap is covered by the manual check (its `update` needs a `ViewportManager`; a one-liner wrap is not worth
extracting).

## Manual check

1. Open the shader animations demo, keep it focused for a little over 100 s, and watch the shader across the 100 s mark
   (it jumps there today): no jump.
2. Optional long-run check: temporarily start the three `FogShader`/`GalaxyShader` clocks at `74L * 3_600_000L` and
   `Camera.acc` at `3e8f` before the fix to see the freeze, then confirm smooth motion after it. Revert.
