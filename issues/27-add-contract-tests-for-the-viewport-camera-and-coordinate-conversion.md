# Add contract tests for the viewport's camera, scale factor and scene–screen conversion

**Challenged:** amended — `screenToSceneRoundTrips` asserted that `SceneOffset.toOffset(viewportManager)` inverts `Offset.toSceneOffset(viewportManager)`. It does not: `toOffset` only multiplies by the scale, which makes it a vector conversion. So the test would fail at HEAD and after every plan, and it is replaced by the camera/corner mapping plus the delta round-trip. The out-of-bounds `initialScaleFactor` is not clamped and not documented, so it is no longer pinned. The `Dispatchers.Main` assumption was checked: the engine's `desktopMain` depends on `kotlinx-coroutines-swing`, and a probe collected `topLeft` through it from `runBlocking`.

**Kind:** test  ·  **Severity:** medium  ·  **Platforms:** all (runs on the desktop JVM)  ·  **Artifact:** `engine` (tests only)
**Files:** `engine/src/desktopTest/kotlin/com/pandulapeter/kubriko/manager/ViewportContractTest.kt` (new)

**Part of the testing extension.** It runs in lane A after `22`, `24` and `26`, and adds tests only.

## Problem

`ViewportManager` is how every game positions its camera and turns pointer input into scene positions. None of it is
tested. `topLeft`/`bottomRight` are `SyncStateFlow`s whose synchronous getter recomputes the conversion and must
agree with the combined flow:

```kotlin
override val topLeft by autoInitializingLazy {
    val combinedFlow = combine(cameraPosition, size, scaleFactor) { viewportCenter, viewportSize, scale ->
        Offset.Zero.toSceneOffset(
```

Two code paths compute the same value here, and nothing checks that they match. Plan `24` covers non-finite input
only. The aspect-ratio multiplier is computed inside `InternalViewport`'s composition (`scaleFactorMultiplier.update`),
so it can't be reached without a Compose UI test and is out of scope here.

## Fix

Add `ViewportContractTest` in `engine/src/desktopTest`. Use `newTestKubriko(ViewportManager.newInstance(...))` from
`ActorTestHarness` (plan `01`), which sizes the viewport to 1920×1080. Where a test needs another size, call the
internal `updateSize` directly. Read values through the public `ViewportManager` interface.
- `scaleFactorIsClampedToItsBounds` — build with `minimumScaleFactor = 0.5f` and `maximumScaleFactor = 2f`.
  `setScaleFactor(10f)` gives `2f` and `setScaleFactor(0.01f)` gives `0.5f`. Repeated `multiplyScaleFactor(1.5f)`
  never exceeds `2f`, and the reverse never goes below `0.5f`. Do not test an `initialScaleFactor` outside the
  bounds. At `e86d3748` it is stored unclamped, and neither `newInstance`'s KDoc nor any plan says what it should
  do. Pinning it would make a later fix look like a regression. List it in the lane report as an open question.
- `visibleAreaMatchesTheCamera` — for camera positions `(0, 0)` and `(300, -150)` and scale factors `1f` and `2f`:
  - `(topLeft + bottomRight) / 2 == cameraPosition`
  - `bottomRight - topLeft == SceneSize(1920 / scale, 1080 / scale)`, within tolerance.
- `syncGetterAgreesWithTheCollectedFlow` — collect `topLeft`, `bottomRight` and `scaleFactor` with
  `first { it == expected }` under a 2 s `withTimeout` inside `runBlocking`. Mutate camera, scale and size, and assert
  each collected value equals the synchronous `.value` read right after the mutation. The flows run on
  `Dispatchers.Main` (the Swing EDT on the desktop JVM), so do not use `runTest`'s virtual time here.
- `screenToSceneMapsTheVisibleArea` — at two cameras and two scales:
  - `Offset.Zero.toSceneOffset(viewportManager) == topLeft.value`;
  - `Offset(1920f, 1080f).toSceneOffset(viewportManager) == bottomRight.value`;
  - the center pixel `(960, 540)` maps to `cameraPosition.value`.

  `SceneOffset.toOffset(viewportManager)` only multiplies by the current scale. It converts a scene *vector*, not a
  position, and is not the inverse of `toSceneOffset`. So assert the delta round-trip:
  `(p.toSceneOffset(viewportManager) - q.toSceneOffset(viewportManager)).toOffset(viewportManager) == p - q`, within
  tolerance, for pixel pairs taken from the corners and the center. Also assert that `toOffset(viewportManager)` is the
  same before and after `setCameraPosition`. (Plan `26` records the KDoc gap.)
- `addToCameraPositionMovesByScreenPixels` — read `addToCameraPosition`'s KDoc to see whether the offset is in screen
  pixels divided by scale or in scene units, and assert exactly that at scales `1f` and `2f`.
- `sizeConversionIsPositionIndependent` — after plan `22`, `Size(100f, 50f).toSceneSize(viewportManager)` is the same
  at any camera position and equals `SceneSize(100 / scale, 50 / scale)`.
- `targetFrameRateRoundTrips` — `setTargetFrameRate(TargetFrameRate.Limit(30))` is reflected by `targetFrameRate.value`.
  The constructor's value is the initial one.

## Tests

This plan is the tests. Run `./gradlew :engine:desktopTest`. As in `26`, a test that fails because the code
contradicts its KDoc is `@Ignore`d with a reason and reported. It is not fixed under this plan.

## Manual check

None.
