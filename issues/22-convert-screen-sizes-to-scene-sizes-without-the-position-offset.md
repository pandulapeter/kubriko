# Convert screen sizes to scene sizes without the position offset

**Challenged:** sound

**Decision needed:** `Size.toSceneSize(...)` subtracts half the viewport like a position conversion, so any size comes out wrong (often negative). Fix the formula (a public behaviour change; Annoyed Penguins' three call sites are adjusted to keep their current numbers)? — recommended: yes, fix it and deprecate the `viewportSize` parameter it no longer needs.

**Kind:** bug  ·  **Severity:** medium  ·  **Platforms:** all  ·  **Artifact:** `engine` (plus the `game-annoyed-penguins` example)
**Files:** `engine/src/commonMain/kotlin/com/pandulapeter/kubriko/helpers/extensions/SizeExtensions.kt`, `engine/src/commonTest/kotlin/com/pandulapeter/kubriko/helpers/extensions/SizeExtensionsTest.kt` (new), `examples/game-annoyed-penguins/src/commonMain/kotlin/com/pandulapeter/kubriko/gameAnnoyedPenguins/implementation/actors/Penguin.kt`, `examples/game-annoyed-penguins/src/commonMain/kotlin/com/pandulapeter/kubriko/gameAnnoyedPenguins/implementation/actors/base/DestructiblePhysicsObject.kt`, `examples/game-annoyed-penguins/src/commonMain/kotlin/com/pandulapeter/kubriko/gameAnnoyedPenguins/implementation/actors/StarIndicator.kt`

## Problem

`SizeExtensions.kt` (0008d027 ~60-66) copied the offset formula from `Offset.toSceneOffset` without the camera term:

```kotlin
fun Size.toSceneSize(
    viewportSize: Size,
    viewportScaleFactor: Scale,
): SceneSize = SceneSize(
    width = (width - viewportSize.width / 2).sceneUnit,
    height = (height - viewportSize.height / 2).sceneUnit,
) / viewportScaleFactor
```

A size has no origin, so subtracting half the viewport is meaningless: a probe at 0008d027 converted `Size(100, 50)` in an 800×600 viewport at scale 2 to `(-150, -125)` instead of `(50, 25)`. Converting the viewport's own size returns *half* of it, which is the only way the repo uses it — all three call sites in Annoyed Penguins pass `viewportManager.size.value` and treat the result as half the viewport:

```kotlin
// Penguin.kt:88, DestructiblePhysicsObject.kt:58
body.position.y > viewportManager.bottomRight.value.y + viewportManager.size.value.toSceneSize(viewportManager).height
// StarIndicator.kt:66 → calculateIndicatorPosition(viewportSize = …) which reads it as halfWidth/halfHeight
viewportSize = viewportManager.size.value.toSceneSize(viewportManager),
```

## Fix

**Recommended:**
1. Add `fun Size.toSceneSize(viewportScaleFactor: Scale): SceneSize = SceneSize(width.sceneUnit, height.sceneUnit) / viewportScaleFactor` with KDoc ("Converts this screen [Size] to a [SceneSize] at the given viewport scale.").
2. Make the two-parameter overload call it and mark it `@Deprecated("A size does not depend on the viewport's size.", ReplaceWith("toSceneSize(viewportScaleFactor)"))`; update its KDoc to say the `viewportSize` argument is ignored.
3. The `ViewportManager` overload calls the new one with `currentScaleFactor()`.
4. Keep the example's behaviour identical by halving at the call sites: `Penguin.kt` and `DestructiblePhysicsObject.kt` use `viewportManager.size.value.toSceneSize(viewportManager).height / 2`; `StarIndicator.kt` passes `viewportManager.size.value.toSceneSize(viewportManager) / 2` and renames the parameter of `calculateIndicatorPosition` to `halfViewportSize` (its body already treats it as half).

**Alternative:** keep the formula and document it as "half the viewport minus…" — not sensible for any input other than the viewport's own size.

## Tests

`SizeExtensionsTest` (commonTest):
- `sizeIsDividedByScaleOnly` — `Size(100f, 50f).toSceneSize(Scale(2f, 2f))` is `SceneSize(50, 25)`.
- `deprecatedOverloadIgnoresViewportSize` — `Size(100f, 50f).toSceneSize(viewportSize = Size(800f, 600f), viewportScaleFactor = Scale(2f, 2f))` is also `SceneSize(50, 25)` (suppress the deprecation warning in the test).
- `nonUniformScale` — `Size(90f, 90f).toSceneSize(Scale(3f, 1f))` is `SceneSize(30, 90)`.

## Manual check

Desktop Showcase → Annoyed Penguins: star indicators still sit on the screen edge pointing at off-screen stars, and penguins/blocks falling below the screen are still removed at the same depth.
