# Clamp `ViewportManager.newInstance(initialScaleFactor)` to the minimum and maximum scale factors

**Challenged:** sound — multiplier/`AspectRatioMode` is independent of `_scaleFactor`, first `scaleFactor` emission only changes for out-of-range inputs, inverted/NaN bounds do not throw with `coerceAtLeast/AtMost`, all in-repo, Tesselar and contract-test configurations start in range.

**Kind:** bug  ·  **Severity:** low  ·  **Platforms:** all
**Files:** `engine/src/commonMain/kotlin/com/pandulapeter/kubriko/manager/ViewportManagerImpl.kt`, `engine/src/commonMain/kotlin/com/pandulapeter/kubriko/manager/ViewportManager.kt`, `engine/src/desktopTest/kotlin/com/pandulapeter/kubriko/manager/ViewportContractTest.kt`

**Decision needed:** should an `initialScaleFactor` outside `[minimumScaleFactor, maximumScaleFactor]` be clamped at construction like every later change, or kept as passed and documented as unclamped? — recommended: clamp at construction (option A).

## Problem

`setScaleFactor` and `multiplyScaleFactor` clamp to the bounds, but the constructor stores the initial value as passed. `ViewportManagerImpl.kt`:

```kotlin
    private val _scaleFactor = MutableStateFlow(Scale(initialScaleFactor, initialScaleFactor))
```

versus

```kotlin
    override fun setScaleFactor(scaleFactor: Float) {
        if (scaleFactor.isNaN()) return
        _scaleFactor.update {
            scaleFactor.coerceIn(minimumScaleFactor, maximumScaleFactor).let { adjustedScaleFactor ->
```

The public KDoc in `ViewportManager.kt` only says `@param initialScaleFactor The starting zoom level.` and `@param minimumScaleFactor The minimum zoom level allowed.`, so nothing warns that the bounds do not apply to the start value.

Confirmed with a desktop probe at 76992502: `ViewportManager.newInstance(initialScaleFactor = 50f, minimumScaleFactor = 0.5f, maximumScaleFactor = 2f)` reports `rawScaleFactor.value == scaleFactor.value == Scale(50, 50)`; the first `multiplyScaleFactor(1f)` then snaps it to `Scale(2, 2)`. So a game starts at a zoom level the player can never return to, and the first pinch or wheel event jumps the camera. It also bites without an explicit `initialScaleFactor`: `newInstance(minimumScaleFactor = 2f)` starts at the default `1f`, below its own minimum.

Callers (none currently out of range, so the fix changes no in-repo or Tesselar behaviour):
- `examples/demo-isometric-graphics/.../IsometricGraphicsDemoStateHolder.kt` — `initialScaleFactor = 0.04f`, `minimumScaleFactor = 0.001f` (default max 5).
- `examples/demo-performance/.../PerformanceDemoStateHolder.kt` — `initialScaleFactor = 0.5f` (defaults 0.2..5).
- `examples/game-annoyed-penguins/.../AnnoyedPenguinsGameStateHolder.kt` — default initial `1f`, range `0.25f..1f`.
- `tools/scene-editor/.../InternalSceneEditor.kt` — default initial `1f`, range `0.1f..10f`.
- Tests: `SyncStateFlowTest` (`initialScaleFactor = 2f`), `ManagerDeduplicationTest` (`3f`), both inside the default `0.2..5`.
- `../Tesselar`: `gameplay/.../logic/LogicKubriko.kt` (`minimumScaleFactor = 0.01f`), `editor/region/.../RegionViewportStateHolder.kt` (`minimumScaleFactor = 0.02f`), `ui/.../IsometricKubriko.kt` and `editor/region/.../RegionPreviewStateHolder.kt` (all defaults) — all start at the default `1f`, inside range. No Tesselar code passes `initialScaleFactor`.

## Fix

**Option A (recommended): clamp at construction.** In `ViewportManagerImpl.kt`:

```kotlin
    private val _scaleFactor = initialScaleFactor.coerceAtLeast(minimumScaleFactor).coerceAtMost(maximumScaleFactor).let { MutableStateFlow(Scale(it, it)) }
```

Use `coerceAtLeast`/`coerceAtMost` rather than `coerceIn`: `coerceIn` throws `IllegalArgumentException` when `minimumScaleFactor > maximumScaleFactor`, and construction must not start throwing for an inverted range it accepts today (those callers already get the exception from their first `setScaleFactor`, unchanged). A NaN `initialScaleFactor` passes through both unchanged, as today. Runs once per instance, so no hot-path concern.

Update the KDoc in `ViewportManager.kt`'s `newInstance`:

```kotlin
         * @param initialScaleFactor The starting zoom level, clamped to [minimumScaleFactor]..[maximumScaleFactor] like every later change.
         * @param minimumScaleFactor The minimum zoom level allowed, applied to [initialScaleFactor], [setScaleFactor] and [multiplyScaleFactor].
         * @param maximumScaleFactor The maximum zoom level allowed, applied to [initialScaleFactor], [setScaleFactor] and [multiplyScaleFactor].
```

While in that file, correct the adjacent `rawScaleFactor` KDoc, which contradicts the code (the raw value *is* clamped; "raw" means before the aspect-ratio multiplier `InternalViewport` applies):

```kotlin
    /**
     * The scale factor set through [setScaleFactor] / [multiplyScaleFactor] (already clamped to
     * [minimumScaleFactor]..[maximumScaleFactor]), before the adjustment the [AspectRatioMode] applies.
     */
```

The signature and defaults are unchanged, so root `CLAUDE.md` needs no edit; the KDoc carries the contract.

**Option B: document it.** Keep the constructor, change the `@param initialScaleFactor` KDoc to "The starting zoom level. Not clamped to [minimumScaleFactor]..[maximumScaleFactor]; the first [setScaleFactor] or [multiplyScaleFactor] call clamps it." Not recommended: it documents a state the player cannot get back to, and no caller relies on it.

## Tests

Extend `engine/src/desktopTest/kotlin/com/pandulapeter/kubriko/manager/ViewportContractTest.kt` (it already has the `withViewport` helper built on the engine's `newTestKubriko`, and a `scaleFactorIsClampedToItsBounds` test for the setters). Add next to that test:

```kotlin
    @Test
    fun initialScaleFactorIsClampedToItsBounds() {
        withViewport(ViewportManager.newInstance(initialScaleFactor = 50f, minimumScaleFactor = 0.5f, maximumScaleFactor = 2f)) { viewportManager ->
            assertEquals(Scale(2f, 2f), viewportManager.rawScaleFactor.value)
        }
        withViewport(ViewportManager.newInstance(initialScaleFactor = 0.01f, minimumScaleFactor = 0.5f, maximumScaleFactor = 2f)) { viewportManager ->
            assertEquals(Scale(0.5f, 0.5f), viewportManager.rawScaleFactor.value)
        }
        withViewport(ViewportManager.newInstance(minimumScaleFactor = 2f, maximumScaleFactor = 4f)) { viewportManager ->
            assertEquals(Scale(2f, 2f), viewportManager.rawScaleFactor.value)
        }
        withViewport(ViewportManager.newInstance(initialScaleFactor = 1.5f, minimumScaleFactor = 0.5f, maximumScaleFactor = 2f)) { viewportManager ->
            assertEquals(Scale(1.5f, 1.5f), viewportManager.rawScaleFactor.value)
        }
    }
```

(With option B instead, assert the out-of-range value is kept until the first `multiplyScaleFactor(1f)`.) Run `./gradlew :engine:desktopTest --tests "com.pandulapeter.kubriko.manager.ViewportContractTest"`; `SyncStateFlowTest` and `ManagerDeduplicationTest` must stay green.

## Manual check

None.
