# Make `SceneOffset.clamp` and `SceneUnit.clamp` honour a `max` given without a `min`

**Challenged:** sound — NaN (value or either bound) and min > max give bit-identical results to today except the lone-max case; no new boxing (same `?.raw ?:` shape as today, `SceneOffset?` already boxed at the call site); no in-repo/Tesselar/doc caller passes a lone max. Shares `SceneOffsetExtensions.kt` with 05 (different functions): land 03 before 05, same lane.

**Kind:** bug  ·  **Severity:** low  ·  **Platforms:** all
**Files:** `engine/src/commonMain/kotlin/com/pandulapeter/kubriko/helpers/extensions/SceneOffsetExtensions.kt`, `engine/src/commonMain/kotlin/com/pandulapeter/kubriko/helpers/extensions/SceneUnitExtensions.kt`, `engine/src/commonTest/kotlin/com/pandulapeter/kubriko/helpers/extensions/SceneOffsetExtensionsTest.kt`, `engine/src/commonTest/kotlin/com/pandulapeter/kubriko/helpers/extensions/SceneUnitExtensionsTest.kt` (new)

**Decision needed:** a `clamp(max = …)` call with no `min` currently returns the value unchanged — fix it to clamp to `max` as the KDoc promises, or keep the behaviour and document that a lone `max` is ignored? — recommended: fix the behaviour (option A).

## Problem

Both public clamp helpers in the engine substitute the value itself for a missing bound. For a missing `max` that is harmless (`min(this, this) == this`), but for a missing `min` it undoes the upper clamp: `max(this, min(max, this))` is always `this`, because `min(max, this) <= this`.

`SceneOffsetExtensions.kt`:

```kotlin
/**
 * Returns a new [SceneOffset] clamped between the specified [min] and [max] offsets.
 */
fun SceneOffset.clamp(
    min: SceneOffset? = null,
    max: SceneOffset? = null,
) = SceneOffset(
    x = max((min ?: this).x.raw, min((max ?: this).x.raw, x.raw)).sceneUnit,
    y = max((min ?: this).y.raw, min((max ?: this).y.raw, y.raw)).sceneUnit
)
```

`SceneUnitExtensions.kt` has the same bug:

```kotlin
/**
 * Returns a new [SceneUnit] clamped between the specified [min] and [max] values.
 */
fun SceneUnit.clamp(
    min: SceneUnit? = null,
    max: SceneUnit? = null,
) = max(min?.raw ?: raw, min(max?.raw ?: raw, raw)).sceneUnit
```

Confirmed with a desktop probe at 76992502:
- `SceneOffset(1000, 7).clamp(max = SceneOffset(10, 5))` returns `SceneOffset(1000, 7)` (expected `(10, 5)`).
- `50f.sceneUnit.clamp(max = 10f.sceneUnit)` returns `50` (expected `10`).
- `(-5f).sceneUnit.clamp(min = 0f.sceneUnit)` returns `0` (min-only works).

The first review sweep already wrote the test for this and ignored it — `SceneOffsetExtensionsTest.kt`:

```kotlin
    // clamp(max = …) without a min never clamps: the missing min falls back to the offset itself, which max() then keeps.
    @Ignore
    @Test
    fun clampWithOnlyAMaximumBoundsThatSide() {
        assertEquals(offset(1000f, 5f), offset(1000f, 7f).clamp(max = offset(Float.MAX_VALUE, 5f)))
        assertEquals(offset(-40f, -40f), offset(-40f, -40f).clamp(max = offset(10f, 5f)))
    }
```

No other clamp helper exists in the engine (`SceneSize`, `Float`, `Int` extensions have none; `SceneOffset.clampWithin` / `constrainedWithin` take both corners and are correct).

Callers — none is affected by the fix, because every one passes either both bounds or only `min`:
- `engine/.../actor/body/BoxBody.kt` — three `pivot.clamp(min = SceneOffset.Zero, max = …)` calls (both bounds).
- `plugins/collision/.../mask/CircleCollisionMask.kt` — `initialRadius.clamp(min = SceneUnit.Zero)` (min only).
- `../Tesselar` — no caller of either `clamp` (grepped for `.clamp(` and `helpers.extensions.clamp`).

Who it hurts: an external consumer writing `position.clamp(max = limit)` gets no clamping at all, silently.

## Fix

**Option A (recommended): make a missing bound mean "unbounded on that side".** Only the lone-`max` result changes; both-bounds and min-only results are bit-identical, including the existing tie-break when `min > max` (the `min` bound wins, because it is applied last) and NaN propagation. Keep it allocation-free: do not go through `min?.x` (a nullable `SceneUnit` is boxed); test the nullable `SceneOffset` directly.

`SceneOffsetExtensions.kt`:

```kotlin
/**
 * Returns a new [SceneOffset] with each component clamped between the matching components of [min] and [max].
 * A bound that is null leaves that side unbounded. If a component of [min] is greater than that of [max], [min] wins.
 */
fun SceneOffset.clamp(
    min: SceneOffset? = null,
    max: SceneOffset? = null,
) = SceneOffset(
    x = max(if (min == null) Float.NEGATIVE_INFINITY else min.x.raw, min(if (max == null) Float.POSITIVE_INFINITY else max.x.raw, x.raw)).sceneUnit,
    y = max(if (min == null) Float.NEGATIVE_INFINITY else min.y.raw, min(if (max == null) Float.POSITIVE_INFINITY else max.y.raw, y.raw)).sceneUnit,
)
```

`SceneUnitExtensions.kt`:

```kotlin
/**
 * Returns a new [SceneUnit] clamped between [min] and [max].
 * A bound that is null leaves that side unbounded. If [min] is greater than [max], [min] wins.
 */
fun SceneUnit.clamp(
    min: SceneUnit? = null,
    max: SceneUnit? = null,
) = max(min?.raw ?: Float.NEGATIVE_INFINITY, min(max?.raw ?: Float.POSITIVE_INFINITY, raw)).sceneUnit
```

(The parameters already shadow `kotlin.math.min`/`max` in the current code and the calls still resolve to the functions; keep the existing imports.) The public signatures, return types and defaults do not change.

In `SceneOffsetExtensionsTest.kt`, delete the explanatory line comment and the `@Ignore` above `clampWithOnlyAMaximumBoundsThatSide`, and drop the `kotlin.test.Ignore` import if nothing else uses it.

**Option B: keep the behaviour, document it.** Change both KDocs to say a `max` passed without a `min` is ignored, and turn the ignored test into one asserting the value comes back unchanged. Not recommended: the behaviour is an accident no caller relies on (every in-repo and Tesselar caller is unaffected by A), and the parameter names promise a clamp.

## Tests

- `SceneOffsetExtensionsTest.clampWithOnlyAMaximumBoundsThatSide` — un-ignored; must pass.
- `SceneOffsetExtensionsTest.clampingFunctionsAgree` — unchanged; must still pass (guards both-bounds and min-only).
- New `engine/src/commonTest/kotlin/com/pandulapeter/kubriko/helpers/extensions/SceneUnitExtensionsTest.kt` (MPL-2.0 header, package `com.pandulapeter.kubriko.helpers.extensions`, pure logic, no fixtures needed):

```kotlin
class SceneUnitExtensionsTest {

    @Test
    fun clampBoundsEachGivenSide() {
        assertEquals(10f, 50f.sceneUnit.clamp(max = 10f.sceneUnit).raw)
        assertEquals(-3f, (-3f).sceneUnit.clamp(max = 10f.sceneUnit).raw)
        assertEquals(0f, (-5f).sceneUnit.clamp(min = 0f.sceneUnit).raw)
        assertEquals(7f, 7f.sceneUnit.clamp(min = 0f.sceneUnit).raw)
        assertEquals(4f, 4f.sceneUnit.clamp().raw)
        assertEquals(10f, 50f.sceneUnit.clamp(min = 0f.sceneUnit, max = 10f.sceneUnit).raw)
        assertEquals(0f, (-50f).sceneUnit.clamp(min = 0f.sceneUnit, max = 10f.sceneUnit).raw)
    }

    @Test
    fun invertedBoundsLetTheMinimumWin() {
        assertEquals(5f, 7f.sceneUnit.clamp(min = 5f.sceneUnit, max = 1f.sceneUnit).raw)
    }
}
```

Run `./gradlew :engine:desktopTest --tests "com.pandulapeter.kubriko.helpers.extensions.*"`.

## Manual check

None.
