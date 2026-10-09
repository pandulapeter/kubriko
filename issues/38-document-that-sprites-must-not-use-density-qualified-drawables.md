# Document that sprites must come from the unqualified `drawable/` folder, because density-qualified variants are picked by screen density and decoded without scaling.

**Kind:** docs  ·  **Severity:** low  ·  **Platforms:** all
**Challenged:** amended — the rule is "no density qualifier" rather than "only the unqualified `drawable/` folder", since theme and language qualifiers do not change a sprite's pixel size.
**Files:** `plugins/sprites/src/commonMain/kotlin/com/pandulapeter/kubriko/sprites/SpriteManager.kt` (KDoc), `plugins/sprites/README.md`, `plugins/sprites/CLAUDE.md`

Ships in `io.github.pandulapeter.kubriko:plugin-sprites` (KDoc only, no code change).

## Problem
`SpriteManagerImpl.decodeImage` picks the resource variant with the device's density, but decodes it as if it were MDPI and targets MDPI, so it never scales:

```kotlin
private suspend fun decodeImage(spriteResource: SpriteResource): ImageBitmap = getDrawableResourceBytes(
    getSystemResourceEnvironment(),
    spriteResource.drawableResource
).toImageBitmap(
    DensityQualifier.MDPI.dpi,
    DensityQualifier.MDPI.dpi,
    spriteResource.rotation
)
```

`getDrawableResourceBytes` resolves the item through Compose Resources' `getResourceItemByEnvironment`, which filters by `environment.density`: same or higher density first, then lower. If a game ships `composeResources/drawable-mdpi/hero.png` and `drawable-xxhdpi/hero.png`, a 3× phone (or a desktop or web screen the environment reports as high density) gets the xxhdpi file at three times the pixel size, while an mdpi screen gets the small one. Sprite size in the scene, and `AnimatedSprite`'s pixel-based `frameSize` slicing, then depend on the device. Compose's own `imageResource` avoids this by scaling the variant's density to the screen's. Kubriko deliberately maps sprite pixels 1:1 (the module's `CLAUDE.md` says everything decodes at MDPI so "no scaling occurs for standard assets"), but nothing tells game authors that density folders break that.

No in-repo game or Tesselar ships density-qualified drawables today (every sprite lives in a plain `composeResources/drawable/`), so this is a trap for external games, not a live bug.

A code fix is not available cleanly: making the lookup density-independent needs a fixed-density `ResourceEnvironment`, whose constructor is `internal` to Compose Resources, or the chosen item's qualifiers through `getResourceItemByEnvironment`, which is `internal` too. Either way the lookup would change which file loads for existing games, so this plan only documents the rule.

## Fix
1. `SpriteManager` class KDoc: add that sprites are decoded at their file's pixel size (one image pixel per unit of the size the game draws it at), so they must not use density qualifiers: keep them in `drawable/` (theme or language qualifiers such as
   `drawable-dark/` are fine — they do not change pixel size). A density-qualified variant (`drawable-xxhdpi/` and so on) is chosen by the screen's density and is not scaled back, so the sprite's size would change from device to device.
2. `plugins/sprites/README.md`: one short paragraph with the same rule, next to the preloading section.
3. `plugins/sprites/CLAUDE.md`, "Platform differences in decoding", last bullet: replace "so no scaling occurs for standard assets" with the variant-selection detail above (variant picked by `getSystemResourceEnvironment()` density, decoded at its own pixel size), and state that density-qualified folders are not supported (other qualifiers are).

## Tests
None: documentation only.

## Manual check
None.
