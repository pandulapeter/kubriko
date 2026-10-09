# Add the MPL-2.0 headers and KDoc to `SpriteResource` and `toSpriteResource`

**Kind:** docs  ·  **Severity:** medium  ·  **Platforms:** all  ·  **Class:** Now
**Artifact:** plugin-sprites
**Files:**
- `plugins/sprites/src/commonMain/kotlin/com/pandulapeter/kubriko/sprites/SpriteResource.kt`
- `plugins/sprites/src/commonMain/kotlin/com/pandulapeter/kubriko/sprites/helpers/DrawableResourceExtensions.kt`

## Problem
At 2480325f these are the only two `.kt` files under `plugins/` without the mandatory MPL-2.0 header (both start at
`package`; `grep -rL "Mozilla Public" plugins --include='*.kt'` lists exactly them). Their public declarations have no
KDoc:

```kotlin
data class SpriteResource(
    val drawableResource: DrawableResource,
    val rotation: Rotation = Rotation.NONE,
) {
    enum class Rotation {
        NONE, DEGREES_90, DEGREES_180, DEGREES_270
    }
}
```

```kotlin
fun DrawableResource.toSpriteResource(rotation: Rotation = Rotation.NONE) = SpriteResource(this, rotation)
```

## Fix
- Prepend the license header (copy it from `SpriteManager.kt`).
- KDoc: `SpriteResource` (a drawable plus the rotation the loader bakes into the bitmap; the key `SpriteManager`
  caches by), `drawableResource`, `rotation`, `Rotation` and each entry (clockwise or not — check the platform
  `ImageLoader` actuals before stating the direction), and `toSpriteResource`.
- Add a trailing comma after `DEGREES_270` only if the enum is reformatted to one entry per line; otherwise leave the
  line as is.

## Behaviour
Unchanged.

## Public API
None.

## Tests
The existing ones.

## Verify
`./gradlew :plugins:sprites:compileKotlinDesktop`

## Manual check
none
