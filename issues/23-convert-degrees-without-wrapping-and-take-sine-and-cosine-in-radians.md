# Convert degrees to radians without wrapping, and compute AngleDegrees' sine and cosine in radians

**Challenged:** sound

**Decision needed:** `AngleDegrees.rad` wraps into [0, 2π) (`(-90).deg.rad == 4.71`, `720.deg.rad == 0`) while `AngleRadians.deg` does not wrap; and `AngleDegrees.sin`/`.cos` pass the degree value to `kotlin.math.sin`/`cos` as if it were radians (`90.deg.sin == 0.894`). Fix both? — recommended: yes; `.rad` becomes a plain unit conversion (`raw * π/180`), `sin`/`cos` convert first.

**Kind:** bug  ·  **Severity:** medium  ·  **Platforms:** all  ·  **Artifact:** `engine`
**Files:** `engine/src/commonMain/kotlin/com/pandulapeter/kubriko/helpers/extensions/AngleDegreesExtensions.kt`, `engine/src/commonTest/kotlin/com/pandulapeter/kubriko/helpers/extensions/AngleDegreesExtensionsTest.kt` (new)

## Problem

`AngleDegreesExtensions.kt` at 0008d027:

```kotlin
val AngleDegrees.rad get() = (normalized * (PI / 180f).toFloat()).rad

val AngleDegrees.sin get() = sin(normalized)

val AngleDegrees.cos get() = cos(normalized)
```

versus `AngleRadiansExtensions.kt`:

```kotlin
val AngleRadians.deg get() = (raw * (180f / PI).toFloat()).deg
val AngleRadians.sin get() = sin(normalized)
```

Verified with a probe: `(-90f).deg.rad.raw == 4.712389`, `720f.deg.rad.raw == 0.0`, `90f.deg.sin == 0.89399666`, `180f.deg.cos == -0.5984601`.
- `.rad` wrapping breaks round trips (`x.deg.rad.deg != x` outside [0, 360)) and anything that interpolates or accumulates rotation in degrees (a spin from 0° to 720° converted per frame jumps back to 0; a −10° tilt becomes 350° and rotates the long way if lerped). As a rotation for drawing it is equivalent, which is why it went unnoticed; `AngleDegrees.normalized` already exists for callers who want wrapping.
- `sin`/`cos` are simply wrong for every input except 0.

In-repo users of `.rad`: `Blocky.kt` (0°–330°, unaffected), the scene editor's `RotationPropertyEditor`/`PropertyEditorMapper` (a 0–360 editor; unaffected in range). No in-repo caller uses `AngleDegrees.sin`/`cos`.

## Fix

```kotlin
val AngleDegrees.rad get() = (raw * (PI / 180f).toFloat()).rad
val AngleDegrees.sin get() = sin(rad.raw)
val AngleDegrees.cos get() = cos(rad.raw)
```

Update the KDoc of `.rad`: "Converts this angle from degrees to radians. The value is not wrapped; use [AngleDegrees.normalized] (or `AngleRadians.normalized`) for that." `sin`/`cos` KDoc stays.

**Alternative for `.rad`:** keep the wrapping and document it (and make `AngleRadians.deg` wrap too for symmetry). The `sin`/`cos` fix is not optional under either choice.

## Tests

`AngleDegreesExtensionsTest` (commonTest, `assertEquals(expected, actual, absoluteTolerance = 1e-5f)`):
- `radDoesNotWrap` — `(-90f).deg.rad.raw == -π/2`, `720f.deg.rad.raw == 4π`, `90f.deg.rad.raw == π/2`.
- `roundTrip` — for `-720f, -90f, 0f, 45f, 400f`: `x.deg.rad.deg.raw == x`.
- `sineAndCosineUseRadians` — `90f.deg.sin == 1f`, `0f.deg.cos == 1f`, `180f.deg.cos == -1f`, `(-90f).deg.sin == -1f`.

## Manual check

Scene editor (desktop): rotate an actor through the rotation property editor across 0/360 — it rotates as before.
