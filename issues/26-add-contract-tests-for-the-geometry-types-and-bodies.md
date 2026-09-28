# Add contract tests for the geometry value types, their extensions and the bodies' bounding boxes

**Challenged:** amended — `SceneOffset.toOffset` converts a scene *vector* (it takes only the scale and ignores the camera; Annoyed Penguins' `Slingshot` uses it on a position difference), so the "inverse" assertion, which fails at HEAD by about 1 000 px, is replaced by the delta round-trip it actually supports; the bounding-box tests re-read the box through the getter, because it is one instance refreshed lazily in place (a probe held the reference, rotated the body and read the stale unrotated box); `clampWithin` and `constrainedWithin` are identical and documented identically, which the test now states; `SceneOffset` equality is bitwise, so `-0f` is not equal to `0f`.

**Kind:** test  ·  **Severity:** medium  ·  **Platforms:** all (runs on the desktop JVM)  ·  **Artifact:** `engine` (tests only)
**Files:** `engine/src/commonTest/kotlin/com/pandulapeter/kubriko/types/SceneTypesTest.kt` (new), `engine/src/commonTest/kotlin/com/pandulapeter/kubriko/helpers/extensions/SceneOffsetExtensionsTest.kt` (new), `engine/src/commonTest/kotlin/com/pandulapeter/kubriko/actor/body/BoxBodyTest.kt` (new)

**Part of the testing extension.** It runs at the end of lane A, after `22`–`24` changed the conversion and angle
helpers, so it pins the behaviour as fixed, not the bugs. It adds tests only; no production code changes.

## Problem

Every position, size, rotation and bounding box in a game goes through the value types in `engine/.../types/`
(`SceneUnit`, `SceneOffset`, `SceneSize`, `Scale`, `AngleRadians`, `AngleDegrees`), the helpers in
`helpers/extensions/` and `BoxBody.updateAxisAlignedBoundingBox`. None of them has a test. Plans `22` and `23`
found two of these helpers wrong (`Size.toSceneSize`, `AngleDegrees.rad`/`sin`/`cos`). Their tests cover those two
functions only. Nothing checks the rest, and nothing will catch a later rewrite, such as the raw-float AABB
maths that avoids boxing:

```kotlin
// Raw float math on the four corners; building an Array<SceneOffset> here would box every
// value class element, and this runs once per frame for every moving/rotating/scaling body.
override fun updateAxisAlignedBoundingBox(target: AxisAlignedBoundingBox) {
```

This code is rewritten for performance often (three passes so far), and a correctness mistake here moves every
collision and culling bound silently.

## Fix

Add three `commonTest` classes. They are pure logic with no Kubriko instance, so they compile on every target. Compare
floats with an explicit tolerance (`1e-4f` relative or absolute, whichever is larger) through a small private
`assertClose` helper in each file. Where the sign of zero or NaN-ness is the contract, use exact comparisons. Do not
duplicate `SizeExtensionsTest` (`22`), `AngleDegreesExtensionsTest` (`23`) or `ViewportManagerInputTest` (`24`);
read them first.

**`SceneTypesTest`** — one table-driven test per type, using the values `0f`, `-0f`, `1f`, `-3.5f`, `1e7f`,
`1e-7f`, `Float.NaN`, `±Float.POSITIVE_INFINITY`:
- `SceneUnit` operators (`plus`, `minus`, `times`/`div` by `Float`, `Int` and `SceneUnit`, unary ops, `compareTo`)
  equal the same `Float` operation on `raw`, bit for bit (`toRawBits`), including NaN propagation and `-0f`.
- `SceneOffset` operators equal the component-wise `SceneUnit` operation. `SceneOffset` wraps Compose's packed
  `Offset`, so its equality is bitwise: `SceneOffset(0f, -0f) != SceneOffset.Zero` (checked at `e86d3748`). NaN
  components work and do not throw (Compose `1.12.1`). `times(Scale)`/`div(Scale)` scale x by
  `horizontal` and y by `vertical`. The direction constants (`Left`, `Up`, `UpLeft`, `DownRight`, …) have the
  documented Y-down components; for example `Up == SceneOffset(0, -1)`.
- `SceneSize` `plus`/`minus` and `center`/`bottomRight` equal the component-wise values.
- `Scale` operators, with `Scale.Unit` as the identity for `times`.
- `AngleRadians` ↔ `AngleDegrees` round-trips within tolerance for `0`, `±90`, `180`, `359`, `720`, `-720`, with no
  wrapping (after `23`). `sin`/`cos` of both types match `kotlin.math` on the equivalent radians.

**`SceneOffsetExtensionsTest`:**
- `distanceTo` is symmetric and non-negative, and `a.distanceTo(a) == 0`.
- `length()` equals `distanceTo(Zero)`.
- `normalized()` has length 1 within tolerance for non-zero vectors, and `Zero.normalized() == Zero` (the helper
  divides by `SceneUnit.Unit` when the length is zero).
- `rotateAround(center, π/2)` maps `(1, 0)` around the origin to `(0, 1)`, the Y-down clockwise convention.
  Rotating by `2π` or by `θ` then `-θ` returns the start within tolerance, and rotating around the point itself is
  the identity.
- `angleTowards`/`directionTowards` agree and point along the axes for the four direction constants.
- `dot`/`cross`/`normal()` satisfy `a.dot(a.normal()) == 0` and `a.cross(b) == -b.cross(a)`.
- `clamp`, `clampWithin` and `constrainedWithin` leave an inside point unchanged and move an outside point onto the
  nearest edge. At `e86d3748`, `clampWithin` and `constrainedWithin` have identical bodies and equivalent KDoc, so
  assert that they return the same result for every input, and that `clamp(min = topLeft, max = bottomRight)` agrees
  with them. `clamp` with a `null` bound leaves that side unbounded.
- `lerp(start, stop, 0f) == start`, `lerp(…, 1f) == stop`, and `0.5f` gives the midpoint.
- `List<SceneOffset>.center` of a rectangle's four corners is its center.
- The explicit-parameter conversions. `SceneOffset.toOffset(viewportScaleFactor)` takes only the scale: it converts a
  scene-space *vector* to screen pixels (`x * horizontal`, `y * vertical`), not a position. It is **not** the inverse
  of `Offset.toSceneOffset(viewportCenter, viewportSize, viewportScaleFactor)`, and a probe at `e86d3748` got
  `(-1110, -40)` back for the pixel `(100, 100)` at camera `(-250, 400)`. For camera positions `(0,0)` and `(-250, 400)`,
  scale factors `Scale(1f, 1f)`, `Scale(0.2f, 0.2f)` and `Scale(5f, 2f)`, and viewport sizes `1920×1080` and `1×1`,
  assert:
  - the viewport's center pixel `(w/2, h/2)` maps to the camera position;
  - for two pixels `p` and `q`, `(p.toSceneOffset(…) - q.toSceneOffset(…)).toOffset(scale)` equals `p - q` within
    tolerance, whatever the camera and viewport size.
  `toOffset`'s KDoc ("Converts this [SceneOffset] to a screen [Offset]") does not say it is a vector conversion. That
  is a documentation gap, not a failing contract. Do not `@Ignore` anything for it; list it in the lane report as a
  follow-up docs item.

**`BoxBodyTest`** (`axisAlignedBoundingBox` is public; `position` is where the pivot sits in the scene).
`axisAlignedBoundingBox` returns one `AxisAlignedBoundingBox` instance that the body mutates in place, and only when
the getter runs after a change. Never keep the returned object across a change. Copy its `left`/`top`/`right`/`bottom`
into locals, change the property, then read `body.axisAlignedBoundingBox` again. A held reference keeps showing the old
bounds, and comparing the object with itself always passes.
- An unrotated, unscaled `BoxBody(initialPosition = (10, 20), initialSize = (4, 6))` has its pivot at the size's
  center, `(2, 3)`, and an AABB from `(8, 17)` to `(12, 23)`.
- Rotated by `π/2` around its center, it has an AABB from `(7, 18)` to `(13, 22)`. Rotated by `π`, it has the
  unrotated AABB.
- `scale = Scale(2f, 0.5f)` around a corner pivot `(0, 0)` gives an AABB from `position` to `position + (8, 3)`.
  A negative scale (`Scale(-1f, 1f)`) mirrors the AABB around the pivot and does not produce an inverted box
  (`left <= right`).
- The pivot is clamped into `[0, size]` on construction and on assignment. Shrinking `size` re-clamps an existing
  pivot.
- The AABB is recomputed after each property change (position, size, pivot, scale, rotation): snapshot its four
  values, change the property, read it again through the getter, and assert the values moved. Assigning an equal value does not change it.
- A NaN position gives a NaN AABB and does not throw. A zero size gives a degenerate box at `position`.
- `copyAsBoxBody()` equals the source in every property, and changing the copy leaves the source's AABB unchanged.

## Tests

This plan is the tests. Run `./gradlew :engine:desktopTest`, then `./gradlew :engine:build` (the tests must compile
for iOS and Wasm too). A test that fails because the production code disagrees with its KDoc or `CLAUDE.md` is a
finding. Do not change production code under this plan: mark the test `@Ignore("<one-line reason>")`, list it in the
lane report, and leave the fix for a follow-up plan.

## Manual check

None.
