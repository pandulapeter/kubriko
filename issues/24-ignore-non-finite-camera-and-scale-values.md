# Ignore non-finite camera positions and scale factors

**Challenged:** sound

**Decision needed:** `setScaleFactor(Float.NaN)` or a NaN camera position is stored as-is and the whole scene silently disappears until the game sets a valid value. Ignore non-finite input (keep the last valid value)? — recommended: yes, ignore it; the alternative is to throw `IllegalArgumentException`.

**Kind:** bug  ·  **Severity:** low  ·  **Platforms:** all  ·  **Artifact:** `engine`
**Files:** `engine/src/commonMain/kotlin/com/pandulapeter/kubriko/manager/ViewportManagerImpl.kt`, `engine/src/commonMain/kotlin/com/pandulapeter/kubriko/manager/ViewportManager.kt` (KDoc of the four setters), `engine/src/commonTest/kotlin/com/pandulapeter/kubriko/manager/ViewportManagerInputTest.kt` (new)

## Problem

`ViewportManagerImpl` (0008d027 ~100-126) stores whatever it is given:

```kotlin
override fun setCameraPosition(position: SceneOffset) = _cameraPosition.update { position }

override fun setScaleFactor(scaleFactor: Float) = _scaleFactor.update {
    scaleFactor.coerceIn(minimumScaleFactor, maximumScaleFactor).let { … }
}
```

`coerceIn` returns NaN for NaN (every comparison is false). A camera-follow that normalizes a zero-length vector, or a pinch zoom dividing by a zero distance, produces NaN; the live stress run confirmed the result — every actor culled away, nothing drawn, no error. `multiplyScaleFactor(NaN)` and `addToCameraPosition(Offset(NaN, …))` do the same. Infinite scale factors are already clamped by `coerceIn`; an infinite camera coordinate is not.

## Fix

- `setScaleFactor`, `multiplyScaleFactor`: return without updating when the argument is NaN (for `multiplyScaleFactor`, also when the product is NaN, e.g. `0 × ∞`).
- `setCameraPosition`: return without updating unless both `position.x.raw` and `position.y.raw` are finite (`isFinite()`).
- `addToCameraPosition`: same check on the incoming `offset.x`/`offset.y`.

These setters are called at most a few times per frame; the checks are primitive comparisons with no allocation.

KDoc of each setter: "Non-finite values are ignored." (Under the throwing alternative: "@throws IllegalArgumentException for non-finite values.")

## Tests

`ViewportManagerInputTest` (commonTest; a started Kubriko with `TickSource.manual()`, no viewport needed):
- `nanScaleFactorIsIgnored` — `setScaleFactor(2f)`, `setScaleFactor(Float.NaN)` → `rawScaleFactor.value == Scale(2f, 2f)`; same for `multiplyScaleFactor(Float.NaN)`.
- `nonFiniteCameraPositionIsIgnored` — `setCameraPosition(SceneOffset(10f.sceneUnit, 20f.sceneUnit))`, then `setCameraPosition(SceneOffset(Float.NaN.sceneUnit, 0f.sceneUnit))` and `SceneOffset(Float.POSITIVE_INFINITY.sceneUnit, 0f.sceneUnit)` → still `(10, 20)`; `addToCameraPosition(Offset(Float.NaN, 0f))` → unchanged.
- `validValuesStillApply` — `setScaleFactor(100f)` clamps to `maximumScaleFactor` as before.

## Manual check

None.
